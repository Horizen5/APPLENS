package com.applens.data

/**
 * 单次采样点：某一时刻的前台 Activity
 */
data class ActivitySample(
    val timestamp: Long,           // 采样时刻
    val packageName: String,
    val activityName: String,      // 完整 Activity 名（不含包名前缀的简短形式）
    val activityFullName: String,  // 完整 Activity 名
    val cpuUsage: Float = 0f,      // 该进程 CPU 占用百分比（如有）
    val memUsageKb: Long = 0L      // 该进程内存占用 KB（如有）
)

/**
 * 采样统计结果
 */
data class ActivityStat(
    val activityFullName: String,
    val packageName: String,
    val activityName: String,
    val count: Int,                // 出现次数
    val totalDurationMs: Long,     // 累计停留时长（按相邻采样间隔估算）
    val avgCpu: Float,             // 平均 CPU
    val maxCpu: Float,             // 最大 CPU
    val avgMemKb: Long,            // 平均内存
    val firstSeen: Long,
    val lastSeen: Long,
) {
    /** 占总采样时间的百分比（保留 1 位小数） */
    fun percentOf(totalSamples: Int): Float =
        if (totalSamples <= 0) 0f else count.toFloat() * 100f / totalSamples
}

/**
 * 采样报告
 */
data class SamplingReport(
    val packageName: String,
    val appLabel: String,
    val startTime: Long,
    val endTime: Long,
    val durationMs: Long,
    val samples: List<ActivitySample>,
    val stats: List<ActivityStat>
) {
    val topActivity: ActivityStat? get() = stats.maxByOrNull { it.count }
    val sampleCount: Int get() = samples.size
}
