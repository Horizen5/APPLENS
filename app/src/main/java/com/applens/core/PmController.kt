package com.applens.core

import com.applens.utils.ShellUtils

/**
 * PM 控制器：通过 pm enable/disable 命令控制组件
 *
 * 参考 Blocker 的 RootController 实现：
 * - 使用 pm 命令行方式（兼容性最好）
 * - 组件被真正禁用/启用，状态持久化到 package_restrictions.xml
 * - 缺点：应用可检测到组件被禁用并自行恢复
 */
class PmController : IController {

    override val type: ControllerType = ControllerType.PM

    override suspend fun disableComponent(packageName: String, componentName: String): ControllerResult {
        if (!ShellUtils.isRootAvailable) {
            return ControllerResult.failure(type, "需要 root 权限")
        }
        val target = formatComponent(packageName, componentName)
        val result = ShellUtils.exec("pm disable $target")
        return if (result.success) {
            ControllerResult.success(type, "PM 禁用成功: $target")
        } else {
            ControllerResult.failure(type, "PM 禁用失败: ${result.error.ifBlank { "exit ${result.code}" }}")
        }
    }

    override suspend fun enableComponent(packageName: String, componentName: String): ControllerResult {
        if (!ShellUtils.isRootAvailable) {
            return ControllerResult.failure(type, "需要 root 权限")
        }
        val target = formatComponent(packageName, componentName)
        val result = ShellUtils.exec("pm enable $target")
        return if (result.success) {
            ControllerResult.success(type, "PM 启用成功: $target")
        } else {
            ControllerResult.failure(type, "PM 启用失败: ${result.error.ifBlank { "exit ${result.code}" }}")
        }
    }

    override suspend fun checkComponentEnabled(packageName: String, componentName: String): Boolean {
        val target = formatComponent(packageName, componentName)
        val result = ShellUtils.exec("pm list packages -d ${packageName} 2>/dev/null", useRoot = ShellUtils.isRootAvailable)
        // 如果包在 disabled 列表中，说明组件可能被禁用
        // 更准确的方式：dumpsys package <pkg> | grep <component>
        val dumpResult = ShellUtils.exec(
            "dumpsys package $packageName 2>/dev/null | grep -A1 '$componentName' | head -2",
            useRoot = ShellUtils.isRootAvailable
        )
        if (dumpResult.success) {
            val output = dumpResult.output.lowercase()
            // disabled=true 表示已禁用
            return !output.contains("disabled=true")
        }
        return true // 默认视为启用
    }

    /**
     * 批量禁用：按包聚合，一次 su 执行所有命令（参考 Blocker 的批量优化）
     */
    override suspend fun batchDisable(components: List<Pair<String, String>>): List<ControllerResult> {
        if (!ShellUtils.isRootAvailable) {
            return components.map { ControllerResult.failure(type, "需要 root 权限") }
        }
        if (components.isEmpty()) return emptyList()

        // 按包分组，生成批量命令
        val cmds = components.map { (pkg, comp) ->
            "pm disable ${formatComponent(pkg, comp)}"
        }
        val result = ShellUtils.execBatch(cmds)
        return if (result.success) {
            components.map { (pkg, comp) ->
                ControllerResult.success(type, "PM 禁用: ${formatComponent(pkg, comp)}")
            }
        } else {
            // 批量失败，逐个重试
            components.map { (pkg, comp) ->
                val r = ShellUtils.exec("pm disable ${formatComponent(pkg, comp)}")
                if (r.success) ControllerResult.success(type, "PM 禁用: ${formatComponent(pkg, comp)}")
                else ControllerResult.failure(type, "失败: ${formatComponent(pkg, comp)}")
            }
        }
    }

    override suspend fun batchEnable(components: List<Pair<String, String>>): List<ControllerResult> {
        if (!ShellUtils.isRootAvailable) {
            return components.map { ControllerResult.failure(type, "需要 root 权限") }
        }
        if (components.isEmpty()) return emptyList()

        val cmds = components.map { (pkg, comp) ->
            "pm enable ${formatComponent(pkg, comp)}"
        }
        val result = ShellUtils.execBatch(cmds)
        return if (result.success) {
            components.map { (pkg, comp) ->
                ControllerResult.success(type, "PM 启用: ${formatComponent(pkg, comp)}")
            }
        } else {
            components.map { (pkg, comp) ->
                val r = ShellUtils.exec("pm enable ${formatComponent(pkg, comp)}")
                if (r.success) ControllerResult.success(type, "PM 启用: ${formatComponent(pkg, comp)}")
                else ControllerResult.failure(type, "失败: ${formatComponent(pkg, comp)}")
            }
        }
    }

    /** 格式化组件名：确保 componentName 包含包名前缀 */
    private fun formatComponent(packageName: String, componentName: String): String {
        return if (componentName.startsWith(packageName)) {
            componentName
        } else if (componentName.startsWith(".")) {
            "$packageName$componentName"
        } else {
            "$packageName/$componentName"
        }
    }
}
