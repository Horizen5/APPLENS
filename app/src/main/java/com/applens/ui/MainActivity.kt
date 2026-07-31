package com.applens.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.applens.BuildConfig
import com.applens.core.ControllerManager
import com.applens.core.ControllerType
import com.applens.data.AppInfo
import com.applens.data.AppRepository
import com.applens.data.HookRule
import com.applens.data.HookRuleRepository
import com.applens.ui.screen.AboutScreen
import com.applens.ui.screen.AppsScreen
import com.applens.ui.screen.HomeScreen
import com.applens.ui.screen.HooksScreen
import com.applens.ui.screen.SettingsScreen
import com.applens.ui.theme.AccentBlue
import com.applens.ui.theme.AppLensTheme
import com.applens.ui.theme.TextTertiary
import com.applens.utils.ActivitySampler
import com.applens.utils.HookExecutor
import com.applens.utils.ShellUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AppLensTheme {
                MainScreen()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var selectedTab by remember { mutableIntStateOf(0) }
    var showAbout by remember { mutableStateOf(false) }

    // 权限状态
    var hasRoot by remember { mutableStateOf(false) }
    var hasUsage by remember { mutableStateOf(false) }
    var hasOverlay by remember { mutableStateOf(false) }
    val sampler = remember { ActivitySampler(context) }

    // 应用列表
    var apps by remember { mutableStateOf<List<AppInfo>>(emptyList()) }
    var isLoadingApps by remember { mutableStateOf(false) }

    // Hook 规则
    var hookRules by remember { mutableStateOf<List<HookRule>>(emptyList()) }

    // 设置
    val prefs = remember { context.getSharedPreferences("app_settings", Context.MODE_PRIVATE) }
    var followSystem by remember { mutableStateOf(prefs.getBoolean("follow_system", true)) }
    var controllerType by remember { mutableStateOf(ControllerManager.getControllerType(context)) }
    var animSpeed by remember { mutableFloatStateOf(prefs.getFloat("anim_speed", 1.0f)) }

    // 刷新权限
    fun refreshPermissions() {
        hasRoot = ShellUtils.isRootAvailable
        hasUsage = sampler.hasUsageStatsPermission()
        hasOverlay = android.provider.Settings.canDrawOverlays(context)
    }

    // 加载应用列表
    fun loadApps() {
        isLoadingApps = true
        scope.launch {
            val list = withContext(Dispatchers.IO) {
                AppRepository.listAll(context.packageManager)
            }
            apps = list
            isLoadingApps = false
        }
    }

    // 加载 Hook 规则
    fun loadRules() {
        scope.launch {
            val rules = withContext(Dispatchers.IO) {
                HookRuleRepository.listAll(context)
            }
            hookRules = rules
        }
    }

    // 初始加载
    LaunchedEffect(Unit) {
        refreshPermissions()
        loadApps()
        loadRules()
    }

    // About 页面覆盖
    if (showAbout) {
        AboutScreen(
            versionName = BuildConfig.VERSION_NAME,
            onGithubClick = {
                val url = "https://github.com/Horizen5/APPLENS"
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                context.startActivity(intent)
            },
            onBack = { showAbout = false }
        )
        return
    }

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = Color.White,
                tonalElevation = 0.dp,
            ) {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = { Icon(Icons.Filled.Home, contentDescription = null) },
                    label = { Text("首页") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = AccentBlue,
                        selectedTextColor = AccentBlue,
                        unselectedIconColor = TextTertiary,
                        unselectedTextColor = TextTertiary,
                        indicatorColor = Color.Transparent
                    )
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = { Icon(Icons.Filled.Apps, contentDescription = null) },
                    label = { Text("应用") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = AccentBlue,
                        selectedTextColor = AccentBlue,
                        unselectedIconColor = TextTertiary,
                        unselectedTextColor = TextTertiary,
                        indicatorColor = Color.Transparent
                    )
                )
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    icon = { Icon(Icons.Filled.Shield, contentDescription = null) },
                    label = { Text("Hook") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = AccentBlue,
                        selectedTextColor = AccentBlue,
                        unselectedIconColor = TextTertiary,
                        unselectedTextColor = TextTertiary,
                        indicatorColor = Color.Transparent
                    )
                )
                NavigationBarItem(
                    selected = selectedTab == 3,
                    onClick = { selectedTab = 3 },
                    icon = { Icon(Icons.Filled.Settings, contentDescription = null) },
                    label = { Text("设置") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = AccentBlue,
                        selectedTextColor = AccentBlue,
                        unselectedIconColor = TextTertiary,
                        unselectedTextColor = TextTertiary,
                        indicatorColor = Color.Transparent
                    )
                )
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            when (selectedTab) {
                0 -> HomeScreen(
                    hasRoot = hasRoot,
                    hasUsage = hasUsage,
                    hasOverlay = hasOverlay,
                    onGrantRoot = {
                        scope.launch {
                            withContext(Dispatchers.IO) { ShellUtils.exec("id") }
                            refreshPermissions()
                        }
                    },
                    onGrantUsage = { sampler.requestUsageStatsPermission() },
                    onGrantOverlay = { sampler.requestOverlayPermission() },
                    onStartScan = { selectedTab = 1 }
                )
                1 -> AppsScreen(
                    apps = apps,
                    isLoading = isLoadingApps,
                    onRefresh = {
                        apps = emptyList()
                        loadApps()
                    },
                    onAppClick = { app ->
                        val intent = Intent(context, SamplingActivity::class.java).apply {
                            putExtra(SamplingActivity.EXTRA_PACKAGE, app.packageName)
                            putExtra(SamplingActivity.EXTRA_LABEL, app.label)
                        }
                        context.startActivity(intent)
                    }
                )
                2 -> HooksScreen(
                    rules = hookRules,
                    controllerMode = controllerType.displayName,
                    onApply = { rule ->
                        scope.launch {
                            withContext(Dispatchers.IO) {
                                HookExecutor.apply(context, rule)
                            }
                            loadRules()
                        }
                    },
                    onRevert = { rule ->
                        scope.launch {
                            withContext(Dispatchers.IO) {
                                HookExecutor.revert(context, rule)
                            }
                            loadRules()
                        }
                    },
                    onRemove = { rule ->
                        HookRuleRepository.remove(context, rule.packageName, rule.activityFullName)
                        loadRules()
                    },
                    onBatchApply = {
                        scope.launch {
                            withContext(Dispatchers.IO) {
                                HookExecutor.batchApply(context, hookRules)
                            }
                            loadRules()
                        }
                    }
                )
                3 -> SettingsScreen(
                    followSystem = followSystem,
                    onFollowSystemChange = {
                        followSystem = it
                        prefs.edit().putBoolean("follow_system", it).apply()
                    },
                    controllerType = controllerType,
                    onControllerTypeChange = {
                        controllerType = it
                        ControllerManager.setControllerType(context, it)
                    },
                    animSpeed = animSpeed,
                    onAnimSpeedChange = {
                        animSpeed = it
                        prefs.edit().putFloat("anim_speed", it).apply()
                    },
                    versionName = BuildConfig.VERSION_NAME,
                    onAboutClick = { showAbout = true }
                )
            }
        }
    }
}
