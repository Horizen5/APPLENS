package com.applens.core

/**
 * 组件控制器接口（参考 Blocker 的 IController 设计）
 *
 * 统一抽象三种控制模式：PM / IFW / IFW+PM
 * 调用方无需感知底层实现差异
 */
interface IController {

    /** 控制器类型 */
    val type: ControllerType

    /**
     * 禁用组件
     * @param packageName 包名
     * @param componentName 组件全名（如 com.foo/.MainActivity）
     * @return 操作结果
     */
    suspend fun disableComponent(packageName: String, componentName: String): ControllerResult

    /**
     * 启用组件
     */
    suspend fun enableComponent(packageName: String, componentName: String): ControllerResult

    /**
     * 查询组件是否已启用
     */
    suspend fun checkComponentEnabled(packageName: String, componentName: String): Boolean

    /**
     * 批量禁用（按包聚合优化 IO）
     * @param components 组件列表 (packageName, componentName)
     */
    suspend fun batchDisable(components: List<Pair<String, String>>): List<ControllerResult> {
        return components.map { (pkg, comp) ->
            disableComponent(pkg, comp)
        }
    }

    /**
     * 批量启用
     */
    suspend fun batchEnable(components: List<Pair<String, String>>): List<ControllerResult> {
        return components.map { (pkg, comp) ->
            enableComponent(pkg, comp)
        }
    }
}

/**
 * 控制器类型枚举
 */
enum class ControllerType(val displayName: String, val description: String) {
    /** PackageManager 模式：直接调用 pm disable/enable，组件被真正禁用 */
    PM("PM 模式", "直接禁用组件，效果确定，但应用可检测到禁用状态"),

    /** Intent Firewall 模式：写 IFW 规则文件，组件看起来仍启用但无法被 Intent 唤起 */
    IFW("IFW 模式", "防火墙拦截，隐蔽性强，应用无法检测屏蔽"),

    /** IFW + PM 双层屏蔽：同时写 IFW 规则 + 调 PM 禁用，双重保险 */
    IFW_PLUS_PM("IFW+PM 模式", "双层屏蔽，兼顾隐蔽性与确定性")
}

/**
 * 控制器操作结果
 */
data class ControllerResult(
    val success: Boolean,
    val message: String,
    val controllerType: ControllerType
) {
    companion object {
        fun success(type: ControllerType, msg: String) = ControllerResult(true, msg, type)
        fun failure(type: ControllerType, msg: String) = ControllerResult(false, msg, type)
    }
}
