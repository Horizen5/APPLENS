package com.applens.core

import com.applens.utils.ShellUtils

/**
 * IFW 控制器：通过 Intent Firewall 规则文件控制组件
 *
 * 参考 Blocker 的 IfwController + IntentFirewall 实现：
 * - 规则文件路径：/data/system/ifw/<package>.xml
 * - 组件看起来仍启用，但 Intent 无法唤起（隐蔽性强）
 * - 应用无法检测屏蔽，不会自行恢复
 * - 内存缓存 + 按包聚合批量 IO，减少 root 文件读写
 */
class IfwController : IController {

    override val type: ControllerType = ControllerType.IFW

    companion object {
        private const val IFW_DIR = "/data/system/ifw"
    }

    /** 内存缓存：包名 → 规则，避免重复 IO（参考 Blocker 的缓存设计） */
    private val cache = mutableMapOf<String, IfwXmlSerializer.IfwRules>()

    override suspend fun disableComponent(packageName: String, componentName: String): ControllerResult {
        if (!ShellUtils.isRootAvailable) {
            return ControllerResult.failure(type, "需要 root 权限")
        }

        val fullComponentName = formatComponent(packageName, componentName)
        val componentType = IfwXmlSerializer.ComponentType.fromComponentName(componentName)

        // 读取现有规则（带缓存）
        var rules = loadRules(packageName)
        rules = rules.addComponentFilter(fullComponentName, componentType)

        // 写回 IFW 文件
        val result = saveRules(packageName, rules)
        return if (result) {
            ControllerResult.success(type, "IFW 屏蔽: $fullComponentName")
        } else {
            ControllerResult.failure(type, "IFW 写入失败: $packageName")
        }
    }

    override suspend fun enableComponent(packageName: String, componentName: String): ControllerResult {
        if (!ShellUtils.isRootAvailable) {
            return ControllerResult.failure(type, "需要 root 权限")
        }

        val fullComponentName = formatComponent(packageName, componentName)
        var rules = loadRules(packageName)
        rules = rules.removeComponentFilter(fullComponentName)

        val result = saveRules(packageName, rules)
        return if (result) {
            ControllerResult.success(type, "IFW 解除: $fullComponentName")
        } else {
            ControllerResult.failure(type, "IFW 写入失败: $packageName")
        }
    }

    override suspend fun checkComponentEnabled(packageName: String, componentName: String): Boolean {
        val fullComponentName = formatComponent(packageName, componentName)
        val rules = loadRules(packageName)
        // 如果在屏蔽列表中，说明已被 IFW 拦截
        return !rules.allComponents().contains(fullComponentName)
    }

    /**
     * 批量禁用：按包聚合，每个包只读写一次 IFW 文件（参考 Blocker 的批量优化）
     */
    override suspend fun batchDisable(components: List<Pair<String, String>>): List<ControllerResult> {
        if (!ShellUtils.isRootAvailable) {
            return components.map { ControllerResult.failure(type, "需要 root 权限") }
        }
        if (components.isEmpty()) return emptyList()

        // 按包名分组
        val byPackage = components.groupBy({ it.first }, { it.second })

        val results = mutableListOf<ControllerResult>()
        for ((packageName, compList) in byPackage) {
            // 每个包只读一次、写一次
            var rules = loadRules(packageName)
            for (componentName in compList) {
                val full = formatComponent(packageName, componentName)
                val componentType = IfwXmlSerializer.ComponentType.fromComponentName(componentName)
                rules = rules.addComponentFilter(full, componentType)
            }
            val success = saveRules(packageName, rules)
            for (componentName in compList) {
                val full = formatComponent(packageName, componentName)
                if (success) {
                    results.add(ControllerResult.success(type, "IFW 屏蔽: $full"))
                } else {
                    results.add(ControllerResult.failure(type, "IFW 失败: $full"))
                }
            }
        }
        return results
    }

    override suspend fun batchEnable(components: List<Pair<String, String>>): List<ControllerResult> {
        if (!ShellUtils.isRootAvailable) {
            return components.map { ControllerResult.failure(type, "需要 root 权限") }
        }
        if (components.isEmpty()) return emptyList()

        val byPackage = components.groupBy({ it.first }, { it.second })

        val results = mutableListOf<ControllerResult>()
        for ((packageName, compList) in byPackage) {
            var rules = loadRules(packageName)
            for (componentName in compList) {
                val full = formatComponent(packageName, componentName)
                rules = rules.removeComponentFilter(full)
            }
            val success = saveRules(packageName, rules)
            for (componentName in compList) {
                val full = formatComponent(packageName, componentName)
                if (success) {
                    results.add(ControllerResult.success(type, "IFW 解除: $full"))
                } else {
                    results.add(ControllerResult.failure(type, "IFW 失败: $full"))
                }
            }
        }
        return results
    }

    /**
     * 读取 IFW 规则（带缓存）
     */
    private fun loadRules(packageName: String): IfwXmlSerializer.IfwRules {
        cache[packageName]?.let { return it }

        val filePath = "$IFW_DIR/$packageName.xml"
        val result = ShellUtils.exec("cat '$filePath' 2>/dev/null")
        val rules = if (result.success && result.output.isNotBlank()) {
            IfwXmlSerializer.deserialize(result.output, packageName)
        } else {
            IfwXmlSerializer.IfwRules(packageName)
        }
        cache[packageName] = rules
        return rules
    }

    /**
     * 写入 IFW 规则（写后失效缓存）
     */
    private fun saveRules(packageName: String, rules: IfwXmlSerializer.IfwRules): Boolean {
        val filePath = "$IFW_DIR/$packageName.xml"

        if (rules.isEmpty()) {
            // 规则为空时删除文件（参考 Blocker 的行为）
            ShellUtils.exec("rm -f '$filePath' 2>/dev/null")
            cache.remove(packageName)
            return true
        }

        val xml = IfwXmlSerializer.serialize(rules)
        // 写入临时文件 → chmod 644 → 移动到 IFW 目录
        val tmpFile = "/data/local/tmp/ifw_${packageName}_${System.currentTimeMillis()}.xml"
        val writeCmd = """
            cat > '$tmpFile' << 'IFWEOF'
$xml
IFWEOF
            chmod 644 '$tmpFile'
            cp '$tmpFile' '$filePath'
            chmod 644 '$filePath'
            rm -f '$tmpFile'
        """.trimIndent()

        val result = ShellUtils.exec(writeCmd)
        // 失效缓存
        cache.remove(packageName)
        return result.success
    }

    /** 格式化组件名为 IFW 格式：packageName/componentName */
    private fun formatComponent(packageName: String, componentName: String): String {
        return if (componentName.startsWith(packageName)) {
            componentName
        } else if (componentName.startsWith(".")) {
            "$packageName$componentName"
        } else {
            "$packageName/$componentName"
        }
    }

    /** 清除缓存 */
    fun clearCache() {
        cache.clear()
    }
}
