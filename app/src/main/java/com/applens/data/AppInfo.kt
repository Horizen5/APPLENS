package com.applens.data

import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable

/**
 * 已安装应用信息
 */
data class AppInfo(
    val packageName: String,
    val label: String,
    val isSystem: Boolean,
    val versionName: String?,
    val versionCode: Long,
    val sourceDir: String,
    val enabled: Boolean,
    val uid: Int,
    val firstInstallTime: Long,
    val lastUpdateTime: Long,
    val icon: Drawable? = null
) {
    val isUserApp: Boolean get() = !isSystem
}

object AppRepository {

    /**
     * 列出所有已安装应用（含第三方）
     */
    fun listAll(pm: PackageManager): List<AppInfo> {
        val packages = pm.getInstalledPackages(0)
        return packages.map { pkg ->
            val ai: ApplicationInfo = pkg.applicationInfo!!
            AppInfo(
                packageName = pkg.packageName,
                label = pm.getApplicationLabel(ai).toString(),
                isSystem = (ai.flags and ApplicationInfo.FLAG_SYSTEM) != 0,
                versionName = pkg.versionName,
                versionCode = if (android.os.Build.VERSION.SDK_INT >= 28)
                    pkg.longVersionCode else pkg.versionCode.toLong(),
                sourceDir = ai.sourceDir,
                enabled = ai.enabled,
                uid = ai.uid,
                firstInstallTime = pkg.firstInstallTime,
                lastUpdateTime = pkg.lastUpdateTime,
                icon = pm.getApplicationIcon(ai)
            )
        }.sortedWith(compareBy({ !it.isUserApp }, { it.label.lowercase() }))
    }

    /**
     * 仅列出第三方应用
     */
    fun listUserApps(pm: PackageManager): List<AppInfo> =
        listAll(pm).filter { it.isUserApp }
}
