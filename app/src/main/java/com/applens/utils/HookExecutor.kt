package com.applens.utils

import com.applens.data.HookRule

/**
 * Hook 规则执行器：通过 root + pm/am 实际执行禁用/启用/阻止
 *
 * 注意：
 * - DISABLE：使用 `pm disable <pkg>/<activity>` 永久禁用该 Activity
 *   （仅 root 可用；禁用后该 Activity 将无法启动）
 * - ENABLE：`pm enable <pkg>/<activity>`
 * - BLOCK：在采样期间持续 `am force-stop <pkg>`（轮询）
 *   （仅推荐用于临时阻断；本工具不做轮询守护进程）
 */
object HookExecutor {

    data class ExecResult(val success: Boolean, val message: String)

    fun apply(rule: HookRule): ExecResult {
        if (!ShellUtils.isRootAvailable) {
            return ExecResult(false, "需要 root 权限才能执行此操作")
        }
        val target = "${rule.packageName}/${rule.activityFullName}"
        val r = when (rule.action) {
            HookRule.Action.DISABLE -> {
                ShellUtils.exec("pm disable $target")
            }
            HookRule.Action.ENABLE -> {
                ShellUtils.exec("pm enable $target")
            }
            HookRule.Action.BLOCK -> {
                ShellUtils.exec("am force-stop ${rule.packageName}")
            }
            HookRule.Action.HOOK_TAG -> {
                return ExecResult(true, "仅标记，不执行")
            }
        }
        return if (r.success) ExecResult(true, "执行成功: ${rule.action.name} -> $target")
        else ExecResult(false, "执行失败: ${r.error.ifBlank { "exit ${r.code}" }}")
    }

    fun revert(rule: HookRule): ExecResult {
        if (!ShellUtils.isRootAvailable) {
            return ExecResult(false, "需要 root 权限才能执行此操作")
        }
        val target = "${rule.packageName}/${rule.activityFullName}"
        // 反操作
        val r = when (rule.action) {
            HookRule.Action.DISABLE -> ShellUtils.exec("pm enable $target")
            HookRule.Action.ENABLE -> ShellUtils.exec("pm disable $target")
            HookRule.Action.BLOCK -> ShellUtils.exec("am force-stop ${rule.packageName}")
            HookRule.Action.HOOK_TAG -> return ExecResult(true, "仅标记，无需撤销")
        }
        return if (r.success) ExecResult(true, "已撤销: $target")
        else ExecResult(false, "撤销失败: ${r.error.ifBlank { "exit ${r.code}" }}")
    }
}
