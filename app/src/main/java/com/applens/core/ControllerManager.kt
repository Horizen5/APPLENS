package com.applens.core

import android.content.Context
import android.content.SharedPreferences

/**
 * 控制器管理器（参考 Blocker 的 GetControllerUseCase + DI 设计）
 *
 * 根据用户设置的控制模式偏好，返回对应的 IController 实例
 * 调用方无需感知底层控制器选择逻辑
 */
object ControllerManager {

    private const val PREFS_NAME = "app_settings"
    private const val KEY_CONTROLLER_TYPE = "controller_type"

    private val pmController by lazy { PmController() }
    private val ifwController by lazy { IfwController() }
    private val combinedController by lazy { CombinedController(pmController, ifwController) }

    /**
     * 获取当前控制器类型（从 SharedPreferences 读取用户设置）
     */
    fun getControllerType(context: Context): ControllerType {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val name = prefs.getString(KEY_CONTROLLER_TYPE, ControllerType.PM.name)
        return runCatching { ControllerType.valueOf(name!!) }.getOrDefault(ControllerType.PM)
    }

    /**
     * 设置控制器类型
     */
    fun setControllerType(context: Context, type: ControllerType) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_CONTROLLER_TYPE, type.name).apply()
    }

    /**
     * 获取控制器实例（根据用户偏好自动选择）
     * 参考 Blocker 的 GetControllerUseCase：用 Flow 驱动，此处简化为同步获取
     */
    fun getController(context: Context): IController {
        return when (getControllerType(context)) {
            ControllerType.PM -> pmController
            ControllerType.IFW -> ifwController
            ControllerType.IFW_PLUS_PM -> combinedController
        }
    }
}
