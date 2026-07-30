package com.applens.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import com.applens.R
import com.applens.data.AppRepository
import com.applens.data.SamplingReport
import com.applens.utils.ActivitySampler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * 采样前台服务：保证采样期间不被系统杀死
 *
 * 启动方式：
 *   val intent = Intent(context, SamplingService::class.java).apply {
 *       putExtra("package", pkg)
 *       putExtra("label", label)
 *   }
 *   ContextCompat.startForegroundService(context, intent)
 *
 * 结果通过 LocalBroadcast 或静态字段回调（这里用静态字段简化）
 */
class SamplingService : Service() {

    companion object {
        const val CHANNEL_ID = "sampling_channel"
        const val NOTIFICATION_ID = 1001

        const val EXTRA_PACKAGE = "package"
        const val EXTRA_LABEL = "label"

        // 简单回调容器（UI 通过此字段拿结果）
        @Volatile
        var lastReport: SamplingReport? = null
        @Volatile
        var isRunning: Boolean = false
        @Volatile
        var progress: Int = 0
        var onProgress: ((Int, Int) -> Unit)? = null
        var onComplete: ((SamplingReport) -> Unit)? = null
        var onError: ((String) -> Unit)? = null
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var wakeLock: PowerManager.WakeLock? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createChannel()
        startForeground(NOTIFICATION_ID, buildNotification("准备采样..."))
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val ch = NotificationChannel(
                CHANNEL_ID,
                "采样服务",
                NotificationManager.IMPORTANCE_LOW
            ).apply { description = "Activity 采样前台服务" }
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.createNotificationChannel(ch)
        }
    }

    private fun buildNotification(text: String): Notification =
        NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Activity 扫描中")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setOngoing(true)
            .setProgress(100, 0, true)
            .build()

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val pkg = intent?.getStringExtra(EXTRA_PACKAGE) ?: run {
            stopSelf(); return START_NOT_STICKY
        }
        val label = intent?.getStringExtra(EXTRA_LABEL) ?: pkg

        if (isRunning) return START_NOT_STICKY
        isRunning = true
        progress = 0

        // 持有唤醒锁
        val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "ActivityScanner::Sampling")
        wakeLock?.acquire(15_000)

        scope.launch {
            try {
                val sampler = ActivitySampler(this@SamplingService)
                // 启动目标应用
                val launch = packageManager.getLaunchIntentForPackage(pkg)
                if (launch != null) {
                    launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    startActivity(launch)
                    kotlinx.coroutines.delay(1500) // 等待应用启动
                }
                val report = sampler.sample(
                    packageName = pkg,
                    appLabel = label,
                    config = ActivitySampler.Config(durationMs = 10_000, intervalMs = 500)
                ) { cur, total, _ ->
                    progress = (cur * 100 / total)
                    val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                    nm.notify(NOTIFICATION_ID, buildNotification("进度 $progress% ($cur/$total)"))
                    onProgress?.invoke(cur, total)
                }
                lastReport = report
                onComplete?.invoke(report)
            } catch (e: Exception) {
                onError?.invoke(e.message ?: "未知错误")
            } finally {
                wakeLock?.release()
                isRunning = false
                stopSelf()
            }
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        if (wakeLock?.isHeld == true) wakeLock?.release()
        scope.cancel()
        isRunning = false
    }
}
