package com.applens.utils

import android.util.Log
import java.io.BufferedReader
import java.io.InputStreamReader

/**
 * Root / Shell 命令执行工具
 * 优先尝试 root（su），失败则降级使用普通 shell（sh）
 */
object ShellUtils {

    private const val TAG = "ShellUtils"

    data class Result(
        val success: Boolean,
        val output: String,
        val error: String,
        val code: Int
    )

    /** 是否拥有 root 权限 */
    val isRootAvailable: Boolean by lazy {
        try {
            val p = Runtime.getRuntime().exec(arrayOf("su", "-c", "id"))
            val out = BufferedReader(InputStreamReader(p.inputStream)).readText()
            val code = p.waitFor()
            code == 0 && out.contains("uid=0")
        } catch (e: Exception) {
            Log.w(TAG, "root 不可用: ${e.message}")
            false
        }
    }

    /**
     * 以 root（优先）或普通用户身份执行命令
     * @param cmd 要执行的命令
     * @param useRoot 是否强制使用 root
     */
    fun exec(cmd: String, useRoot: Boolean = true): Result {
        val parts = if (useRoot && isRootAvailable) {
            arrayOf("su", "-c", cmd)
        } else {
            arrayOf("sh", "-c", cmd)
        }
        return try {
            val p = Runtime.getRuntime().exec(parts)
            val out = BufferedReader(InputStreamReader(p.inputStream)).readText()
            val err = BufferedReader(InputStreamReader(p.errorStream)).readText()
            val code = p.waitFor()
            Result(code == 0, out, err, code)
        } catch (e: Exception) {
            Log.e(TAG, "exec 失败: ${e.message}")
            Result(false, "", e.message ?: "unknown", -1)
        }
    }

    /** 批量执行命令（一次性传给 su，性能更好） */
    fun execBatch(cmds: List<String>, useRoot: Boolean = true): Result {
        val script = cmds.joinToString("\n")
        val parts = if (useRoot && isRootAvailable) {
            arrayOf("su", "-c", script)
        } else {
            arrayOf("sh", "-c", script)
        }
        return try {
            val p = Runtime.getRuntime().exec(parts)
            val out = BufferedReader(InputStreamReader(p.inputStream)).readText()
            val err = BufferedReader(InputStreamReader(p.errorStream)).readText()
            val code = p.waitFor()
            Result(code == 0, out, err, code)
        } catch (e: Exception) {
            Result(false, "", e.message ?: "unknown", -1)
        }
    }
}
