package com.applens.utils

import android.app.AppOpsManager
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Process
import android.provider.Settings
import com.applens.data.ActivitySample
import com.applens.data.ActivityStat
import com.applens.data.SamplingReport

/**
 * Activity 采样器
 *
 * 采样策略：
 * 1. 优先使用 UsageStatsManager.queryEvents 获取最近的前台 Activity
 * 2. 同时通过 dumpsys activity activities / dumpsys meminfo 获取 CPU/内存
 * 3. 每 500ms 采样一次，持续 10s（默认 20 个采样点）
 */
class ActivitySampler(private val context: Context) {

    data class Config(
        val durationMs: Long = 10_000L,
        val intervalMs: Long = 500L
    )

    /** 检查是否有"使用情况访问"权限 */
    fun hasUsageStatsPermission(): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            appOps.unsafeCheckOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName
            )
        } else {
            @Suppress("DEPRECATION")
            appOps.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName
            )
        }
        return mode == AppOpsManager.MODE_ALLOWED
    }

    /** 跳转到"使用情况访问"权限页 */
    fun requestUsageStatsPermission() {
        val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    /** 跳转到"显示在其他应用上层"权限页 */
    fun requestOverlayPermission() {
        val intent = Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:${context.packageName}")
        ).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
        context.startActivity(intent)
    }

    /**
     * 执行一次完整的采样
     * @param appLabel 应用名（仅用于报告显示）
     * @param packageName 被采样应用包名
     * @param onTick 每次采样回调（用于 UI 进度更新）
     */
    suspend fun sample(
        packageName: String,
        appLabel: String,
        config: Config = Config(),
        onTick: (Int, Int, ActivitySample?) -> Unit = { _, _, _ -> }
    ): SamplingReport {
        val startTime = System.currentTimeMillis()
        val samples = mutableListOf<ActivitySample>()
        val totalTicks = (config.durationMs / config.intervalMs).toInt().coerceAtLeast(1)

        for (i in 0 until totalTicks) {
            val sample = takeOneSample(packageName, startTime + i * config.intervalMs)
            if (sample != null) samples.add(sample)
            onTick(i + 1, totalTicks, sample)
            if (i < totalTicks - 1) {
                kotlinx.coroutines.delay(config.intervalMs)
            }
        }
        val endTime = System.currentTimeMillis()
        val stats = aggregate(samples)
        return SamplingReport(
            packageName = packageName,
            appLabel = appLabel,
            startTime = startTime,
            endTime = endTime,
            durationMs = endTime - startTime,
            samples = samples,
            stats = stats
        )
    }

    /** 采样一次 */
    private fun takeOneSample(targetPackage: String, ts: Long): ActivitySample? {
        // 1. 通过 UsageStatsManager 获取最新前台 Activity
        val usm = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val now = System.currentTimeMillis()
        val events = usm.queryEvents(now - 2000, now)
        val event = try {
            val moveField = android.app.usage.UsageEvents.Event::class.java
                .getDeclaredField("MOVE_TO_FOREGROUND")
            val maskField = android.app.usage.UsageEvents.Event::class.java
                .getDeclaredField("MASK_STATE")
            moveField.get(null) as Int
            maskField.get(null) as Int
            1
        } catch (e: Exception) { 1 }

        var pkg: String? = null
        var cls: String? = null
        val event2 = android.app.usage.UsageEvents.Event()
        while (events.hasNextEvent()) {
            events.getNextEvent(event2)
            val state = event2.eventType and 0x7FFFFFFF
            if (state == 1 /* MOVE_TO_FOREGROUND */ ||
                event2.eventType == android.app.usage.UsageEvents.Event.MOVE_TO_FOREGROUND
            ) {
                if (event2.packageName == targetPackage) {
                    pkg = event2.packageName
                    cls = event2.className
                }
            }
        }

        // 2. 如果 UsageStats 没拿到，用 dumpsys 兜底
        if (pkg == null || cls == null) {
            val r = ShellUtils.exec(
                "dumpsys activity activities 2>/dev/null | grep -E 'ResumedActivity|mResumedActivity' | head -1",
                useRoot = ShellUtils.isRootAvailable
            )
            if (r.success) {
                // 例：mResumedActivity: ActivityRecord{... u0 com.foo/.MainActivity ...}
                val line = r.output.lineSequence().firstOrNull { it.isNotBlank() } ?: ""
                val m = Regex("([a-zA-Z0-9_.]+)/([a-zA-Z0-9_.$]+)").find(line)
                if (m != null) {
                    pkg = m.groupValues[1]
                    cls = if (m.groupValues[3].startsWith(".")) {
                        pkg + m.groupValues[3]
                    } else m.groupValues[3]
                    if (pkg != targetPackage) {
                        // 前台不是目标应用
                        pkg = null
                    }
                }
            }
        }

        if (pkg == null || cls == null) return null

        val full = if (cls!!.startsWith(".")) pkg + cls else cls!!
        val short = full.removePrefix("$pkg.")

        // 3. 获取 CPU / 内存（top / dumpsys meminfo）
        var cpu = 0f
        var mem = 0L
        val topRes = ShellUtils.exec(
            "top -b -n 1 -o PID,%,%MEM,CMDLINE 2>/dev/null | grep -E '${Regex.escape(pkg!!)}$' | head -1",
            useRoot = ShellUtils.isRootAvailable
        )
        if (topRes.success) {
            val line = topRes.output.lineSequence().firstOrNull { it.isNotBlank() }
            if (line != null) {
                val cols = line.trim().split(Regex("\\s+"))
                // 找到 %CPU 列（含 % 的列）
                for (c in cols) {
                    val v = c.removeSuffix("%").toFloatOrNull()
                    if (v != null && c.contains("%") && cpu == 0f) cpu = v
                }
                // 内存列：含 %MEM 或者直接 KB
                for (c in cols) {
                    if (c.endsWith("M") || c.endsWith("K")) {
                        val v = c.dropLast(1).toFloatOrNull()
                        if (v != null) {
                            mem = if (c.endsWith("M")) (v * 1024).toLong() else v.toLong()
                            break
                        }
                    }
                }
            }
        }
        // 兜底用 dumpsys meminfo
        if (mem == 0L) {
            val memRes = ShellUtils.exec(
                "dumpsys meminfo $pkg 2>/dev/null | grep -E 'TOTAL PSS|TOTAL' | head -1",
                useRoot = ShellUtils.isRootAvailable
            )
            if (memRes.success) {
                val m = Regex("(\\d+)").find(memRes.output)
                if (m != null) mem = m.groupValues[1].toLong()
            }
        }

        return ActivitySample(
            timestamp = ts,
            packageName = pkg!!,
            activityName = short,
            activityFullName = full,
            cpuUsage = cpu,
            memUsageKb = mem
        )
    }

    /** 聚合统计 */
    private fun aggregate(samples: List<ActivitySample>): List<ActivityStat> {
        if (samples.isEmpty()) return emptyList()
        val byAct = samples.groupBy { it.activityFullName }
        return byAct.map { (full, list) ->
            val pkg = list.first().packageName
            val short = list.first().activityName
            ActivityStat(
                activityFullName = full,
                packageName = pkg,
                activityName = short,
                count = list.size,
                totalDurationMs = list.size.toLong() * 500L,
                avgCpu = list.map { it.cpuUsage }.average().toFloat(),
                maxCpu = list.maxOf { it.cpuUsage },
                avgMemKb = list.map { it.memUsageKb }.average().toLong(),
                firstSeen = list.first().timestamp,
                lastSeen = list.last().timestamp
            )
        }.sortedByDescending { it.count }
    }
}
