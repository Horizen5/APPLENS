package com.applens.core

/**
 * 组合控制器：IFW + PM 双层屏蔽
 *
 * 参考 Blocker 的 CombinedController 设计：
 * - 禁用时：同时写 IFW 规则 + 调 PM 禁用
 * - 启用时：双层恢复（先解除 IFW，再 PM 启用）
 * - 查询时：任一层启用即返回启用（OR 语义）
 * - 兼顾隐蔽性（IFW）与确定性（PM）
 */
class CombinedController(
    private val pmController: PmController = PmController(),
    private val ifwController: IfwController = IfwController()
) : IController {

    override val type: ControllerType = ControllerType.IFW_PLUS_PM

    override suspend fun disableComponent(packageName: String, componentName: String): ControllerResult {
        // 先 IFW 屏蔽（隐蔽层），再 PM 禁用（确定层）
        val ifwResult = ifwController.disableComponent(packageName, componentName)
        val pmResult = pmController.disableComponent(packageName, componentName)

        return if (ifwResult.success && pmResult.success) {
            ControllerResult.success(type, "IFW+PM 双层禁用: $packageName/$componentName")
        } else if (ifwResult.success || pmResult.success) {
            ControllerResult.success(type, "部分成功 — IFW:${ifwResult.success} PM:${pmResult.success}")
        } else {
            ControllerResult.failure(type, "双层禁用失败 — IFW:${ifwResult.message} PM:${pmResult.message}")
        }
    }

    override suspend fun enableComponent(packageName: String, componentName: String): ControllerResult {
        // 先 PM 启用，再解除 IFW
        val pmResult = pmController.enableComponent(packageName, componentName)
        val ifwResult = ifwController.enableComponent(packageName, componentName)

        return if (pmResult.success && ifwResult.success) {
            ControllerResult.success(type, "IFW+PM 双层恢复: $packageName/$componentName")
        } else {
            ControllerResult.failure(type, "恢复不完全 — IFW:${ifwResult.message} PM:${pmResult.message}")
        }
    }

    override suspend fun checkComponentEnabled(packageName: String, componentName: String): Boolean {
        // 任一层启用即视为启用（OR 语义，参考 Blocker）
        return pmController.checkComponentEnabled(packageName, componentName) ||
               ifwController.checkComponentEnabled(packageName, componentName)
    }

    override suspend fun batchDisable(components: List<Pair<String, String>>): List<ControllerResult> {
        // 先批量 IFW，再批量 PM
        val ifwResults = ifwController.batchDisable(components)
        val pmResults = pmController.batchDisable(components)

        return components.indices.map { i ->
            val ifwOk = ifwResults.getOrNull(i)?.success ?: false
            val pmOk = pmResults.getOrNull(i)?.success ?: false
            if (ifwOk && pmOk) {
                ControllerResult.success(type, "IFW+PM 禁用: ${components[i].first}/${components[i].second}")
            } else if (ifwOk || pmOk) {
                ControllerResult.success(type, "部分禁用: ${components[i].first}/${components[i].second}")
            } else {
                ControllerResult.failure(type, "禁用失败: ${components[i].first}/${components[i].second}")
            }
        }
    }

    override suspend fun batchEnable(components: List<Pair<String, String>>): List<ControllerResult> {
        val pmResults = pmController.batchEnable(components)
        val ifwResults = ifwController.batchEnable(components)

        return components.indices.map { i ->
            val ifwOk = ifwResults.getOrNull(i)?.success ?: false
            val pmOk = pmResults.getOrNull(i)?.success ?: false
            if (ifwOk && pmOk) {
                ControllerResult.success(type, "IFW+PM 恢复: ${components[i].first}/${components[i].second}")
            } else {
                ControllerResult.failure(type, "恢复不完全: ${components[i].first}/${components[i].second}")
            }
        }
    }
}
