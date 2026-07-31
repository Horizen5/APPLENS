package com.applens.utils

import android.content.Context
import com.applens.core.ControllerManager
import com.applens.core.ControllerResult
import com.applens.core.ControllerType
import com.applens.data.HookRule
import kotlinx.coroutines.runBlocking

/**
 * Hook 规则执行器（重构后基于 IController 架构）
 *
 * 参考 Blocker 的策略模式：
 * - 不再直接拼 pm 命令字符串，而是通过 ControllerManager 获取控制器
 * - 支持 PM / IFW / IFW+PM 三种控制模式
 * - 控制器类型由用户在设置中选择
 *
 * 注意：
 * - DISABLE：通过当前控制器禁用 Activity
 * - ENABLE：通过当前控制器启用 Activity
 * - BLOCK：采样期间 force-stop（仅临时阻断）
 * - HOOK_TAG：仅标记，不执行
 */
object HookExecutor {

    data class ExecResult(val success: Boolean, val message: String, val controllerType: ControllerType? = null)

    /**
     * 执行规则（需要 Context 来获取控制器偏好）
     */
    fun apply(context: Context, rule: HookRule): ExecResult {
        if (!ShellUtils.isRootAvailable) {
            return ExecResult(false, "需要 root 权限才能执行此操作")
        }

        when (rule.action) {
            HookRule.Action.HOOK_TAG -> return ExecResult(true, "仅标记，不执行")
            HookRule.Action.BLOCK -> {
                val r = ShellUtils.exec("am force-stop ${rule.packageName}")
                return if (r.success) ExecResult(true, "已强制停止: ${rule.packageName}")
                else ExecResult(false, "停止失败: ${r.error}")
            }
            else -> {}
        }

        val controller = ControllerManager.getController(context)
        val result = runBlocking {
            when (rule.action) {
                HookRule.Action.DISABLE -> controller.disableComponent(rule.packageName, rule.activityFullName)
                HookRule.Action.ENABLE -> controller.enableComponent(rule.packageName, rule.activityFullName)
                else -> ControllerResult.success(controller.type, "跳过")
            }
        }

        return ExecResult(result.success, result.message, result.controllerType)
    }

    /**
     * 撤销规则（反操作）
     */
    fun revert(context: Context, rule: HookRule): ExecResult {
        if (!ShellUtils.isRootAvailable) {
            return ExecResult(false, "需要 root 权限才能执行此操作")
        }

        when (rule.action) {
            HookRule.Action.HOOK_TAG -> return ExecResult(true, "仅标记，无需撤销")
            HookRule.Action.BLOCK -> {
                val r = ShellUtils.exec("am force-stop ${rule.packageName}")
                return if (r.success) ExecResult(true, "已停止: ${rule.packageName}")
                else ExecResult(false, "停止失败: ${r.error}")
            }
            else -> {}
        }

        val controller = ControllerManager.getController(context)
        val result = runBlocking {
            // 反操作：DISABLE → enable, ENABLE → disable
            when (rule.action) {
                HookRule.Action.DISABLE -> controller.enableComponent(rule.packageName, rule.activityFullName)
                HookRule.Action.ENABLE -> controller.disableComponent(rule.packageName, rule.activityFullName)
                else -> ControllerResult.success(controller.type, "跳过")
            }
        }

        return ExecResult(result.success, result.message, result.controllerType)
    }

    /**
     * 批量执行（参考 Blocker 的批量操作）
     */
    fun batchApply(context: Context, rules: List<HookRule>): List<ExecResult> {
        if (!ShellUtils.isRootAvailable) {
            return rules.map { ExecResult(false, "需要 root 权限") }
        }

        val controller = ControllerManager.getController(context)
        val toDisable = rules.filter { it.action == HookRule.Action.DISABLE }
            .map { it.packageName to it.activityFullName }

        val results = mutableListOf<ExecResult>()
        if (toDisable.isNotEmpty()) {
            val batchResults = runBlocking { controller.batchDisable(toDisable) }
            results.addAll(batchResults.map {
                ExecResult(it.success, it.message, it.controllerType)
            })
        }

        // 非 DISABLE 的规则逐个执行
        rules.filter { it.action != HookRule.Action.DISABLE }.forEach { rule ->
            results.add(apply(context, rule))
        }

        return results
    }
}
