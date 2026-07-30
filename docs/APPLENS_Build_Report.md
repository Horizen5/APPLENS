# APPLENS 构建全过程详细报告

> **项目名称**: APPLENS（Android Activity 分析工具）  
> **当前版本**: v1.0.2 (versionCode: 3)  
> **包名**: com.applens  
> **构建日期**: 2026-07-30  
> **构建环境**: 远程沙箱 (CI=true, non-interactive)

---

## 目录

1. [沙箱初始环境清单](#一沙箱初始环境清单)
2. [构建 Android APK 所需环境](#二构建-android-apk-所需环境)
3. [环境差异对比：缺什么、下了什么、多大](#三环境差异对比缺什么下了什么多大)
4. [构建过程中遇到的所有问题及解决方案](#四构建过程中遇到的所有问题及解决方案)
5. [完整对话时间线](#五完整对话时间线)
6. [最终产物信息](#六最终产物信息)
7. [如果换环境：一键搭建指南](#七如果换环境一键搭建指南)
8. [经验总结](#八经验总结)

---

## 一、沙箱初始环境清单

### 1.1 硬件资源

| 项目 | 值 |
|------|-----|
| 操作系统 | Ubuntu 24.04.3 LTS (Noble Numbat) |
| 内核 | Linux 6.18.5 |
| CPU | 3 核 |
| 内存 | 5.8 GB（无 Swap） |
| 磁盘 | 1.5 TB（已用 209 GB，可用 1.2 TB） |
| 交互模式 | 非交互式 (CI=true, no TTY, stdin=EOF) |

### 1.2 预装开发工具

| 组件 | 版本 | 路径 / 说明 | 大小 |
|------|------|------------|------|
| **JDK 8** (Temurin) | 8.0.482+8 | `~/.local/share/mise/installs/java/8.0.482` | - |
| **JDK 11** | 11.0.2 | `~/.local/share/mise/installs/java/11.0.2` | - |
| **JDK 17** | 17.0.2 | `~/.local/share/mise/installs/java/17.0.2` | 308 MB |
| **JDK 25** (默认) | 25.0.2 | `~/.local/share/mise/installs/java/25.0.2` | - |
| **Gradle** | 8.14.4 | 通过 mise 管理，系统全局可用 | - |
| **Python** | 3.14.4 | 系统默认 | - |
| **mise** | - | 版本管理工具，管理上述 Java/Gradle 版本 | - |
| **gh (GitHub CLI)** | - | 已安装，可操作 GitHub API | - |

### 1.3 预装 Android SDK

| 组件 | 版本 | 路径 | 大小 |
|------|------|------|------|
| cmdline-tools | latest | `/opt/android-sdk/cmdline-tools/latest` | 148 MB |
| Platform android-33 | API 33 | `/opt/android-sdk/platforms/android-33` | 141 MB |
| Platform android-34 | API 34 | `/opt/android-sdk/platforms/android-34` | 138 MB |
| Build Tools | 34.0.0 | `/opt/android-sdk/build-tools/34.0.0` | 151 MB |
| Platform Tools | latest | `/opt/android-sdk/platform-tools` | 22 MB |

### 1.4 网络环境

| 项目 | 值 |
|------|-----|
| HTTP 代理 | `http://127.0.0.1:18080` |
| HTTPS 代理 | `http://127.0.0.1:18080` |
| 环境变量 | `http_proxy`, `https_proxy`, `HTTPS_PROXY` 均已设置 |
| 预览代理 | 公开端口 16000（构建期间被占用） |

### 1.5 环境变量

```bash
http_proxy=http://127.0.0.1:18080
https_proxy=http://127.0.0.1:18080
HTTPS_PROXY=http://127.0.0.1:18080
PREVIEW_PROXY_PUBLIC_PORT=16000
```

---

## 二、构建 Android APK 所需环境

### 2.1 必需组件

| 组件 | 版本要求 | 原因 |
|------|---------|------|
| JDK | 17 | AGP 8.2.2 不支持 JDK 21+，JDK 17 是 LTS 且最稳定 |
| Gradle | 8.x (8.10+ 推荐) | AGP 8.2 需要 Gradle 8.0+ |
| Android SDK Platform | 34 (API 34) | `androidx.lifecycle:2.7.0` 要求 compileSdk 34 |
| Android Build Tools | 34.0.0 | 与 Platform 34 匹配 |
| AGP (Android Gradle Plugin) | 8.2.2 | 构建工具，通过 build.gradle 声明 |
| Kotlin Gradle Plugin | 1.9.22 | 编译 Kotlin 代码 |
| Maven 仓库 | 可访问 | 下载 AndroidX、Material 等依赖 |

### 2.2 项目依赖列表

```groovy
implementation 'androidx.core:core-ktx:1.12.0'
implementation 'androidx.appcompat:appcompat:1.6.1'
implementation 'com.google.android.material:material:1.11.0'
implementation 'androidx.constraintlayout:constraintlayout:2.1.4'
implementation 'androidx.lifecycle:lifecycle-viewmodel-ktx:2.7.0'
implementation 'androidx.lifecycle:lifecycle-livedata-ktx:2.7.0'
implementation 'androidx.recyclerview:recyclerview:1.3.2'
implementation 'androidx.swiperefreshlayout:swiperefreshlayout:1.1.0'
implementation 'androidx.cardview:cardview:1.0.0'
implementation 'org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3'
```

---

## 三、环境差异对比：缺什么、下了什么、多大

### 3.1 原环境已有 vs 需要的

| 组件 | 原环境已有 | 构建需要 | 状态 |
|------|-----------|---------|------|
| JDK 17 | ✅ 已有 17.0.2 | ✅ 需要 17 | 无需下载 |
| Gradle | ✅ 已有 8.14.4 | ✅ 需要 8.x | 无需下载 |
| Android Platform 34 | ✅ 已有 | ✅ 需要 | 无需下载 |
| Build Tools 34.0.0 | ✅ 已有 | ✅ 需要 | 无需下载 |
| Gradle 代理配置 | ❌ 缺失 | ✅ 需要 | 需手动配置 |
| 国内 Maven 镜像 | ❌ 缺失 | ✅ 需要 | 需手动配置 |
| AGP 8.2.2 | ❌ 缺失 | ✅ 需要 | Gradle 自动下载 |
| Kotlin Plugin 1.9.22 | ❌ 缺失 | ✅ 需要 | Gradle 自动下载 |
| AndroidX 依赖库 | ❌ 缺失 | ✅ 需要 | Gradle 自动下载 |

### 3.2 Gradle 自动下载的内容

以下组件通过 Gradle 构建时自动下载到 `~/.gradle/caches/`：

| 组件 | 大小（约） | 来源 |
|------|-----------|------|
| AGP 8.2.2 | ~120 MB | 阿里云镜像 |
| Kotlin Gradle Plugin 1.9.22 | ~80 MB | 阿里云镜像 |
| Kotlin Stdlib 1.9.22 | ~30 MB | 阿里云镜像 |
| AndroidX 库 (11个) | ~50 MB | 阿里云镜像 |
| Material Components 1.11.0 | ~20 MB | 阿里云镜像 |
| ConstraintLayout 2.1.4 | ~5 MB | 阿里云镜像 |
| 其他传递依赖 | ~50 MB | 阿里云镜像 |
| **Gradle 缓存总计** | **1.6 GB** | 包含 transforms、modules 等 |

### 3.3 如果换全新环境需要下载什么

| 需下载项 | 大小 | 下载地址 |
|---------|------|---------|
| JDK 17 | ~190 MB | https://adoptium.net/temurin/releases/?version=17 |
| Gradle 8.14.4 | ~150 MB | https://services.gradle.org/distributions/gradle-8.14.4-bin.zip |
| Android cmdline-tools | ~130 MB | https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip |
| Platform android-34 | ~138 MB | `sdkmanager "platforms;android-34"` |
| Build Tools 34.0.0 | ~151 MB | `sdkmanager "build-tools;34.0.0"` |
| Platform Tools | ~22 MB | `sdkmanager "platform-tools"` |
| AGP + 依赖 (Gradle 自动) | ~1.6 GB | 构建时自动下载 |
| **总计** | **约 2.4 GB** | |

---

## 四、构建过程中遇到的所有问题及解决方案

共经历 **17 次构建尝试**，6 大类问题：

### 问题 1：Gradle Wrapper 脚本损坏

| 项目 | 详情 |
|------|------|
| **错误现象** | `Error opening zip file or JAR manifest missing`；`ClassNotFoundException: org.gradle.launcher.GradleMain`；`daemon disappeared unexpectedly` |
| **根因** | 沙箱环境修改了 `gradlew` 脚本：`DEFAULT_JVM_OPTS` 中硬编码了不存在的 javaagent 路径 `/workspace/lib/agents/gradle-instrumentation-agent-8.10.2.jar`；`CLASSPATH` 指向不存在的 `lib/gradle-gradle-cli-main-8.10.2.jar` |
| **影响** | JVM 启动即崩溃，Gradle daemon 无法存活 |
| **解决方式** | 1. 删除 `DEFAULT_JVM_OPTS` 中的 javaagent 参数<br>2. 放弃 `gradlew`，直接使用系统 Gradle 8.14.4 |
| **涉及文件** | `gradlew` |
| **耗时** | 约 10 分钟（3 次构建尝试） |

### 问题 2：网络代理未配置导致 Gradle 假死（最严重）

| 项目 | 详情 |
|------|------|
| **错误现象** | Gradle 启动后卡在 `Evaluating root project`，数分钟无任何输出，进程看似运行但完全无进展 |
| **根因** | 环境有 HTTP 代理 `127.0.0.1:18080`，`curl` 等工具通过环境变量 `HTTPS_PROXY` 走代理，但 **Gradle 默认不读取系统代理环境变量**，直接发起 TCP 连接到阿里云 CDN IP `106.8.159.241:443`，TCP 握手被网络策略阻断，一直停留在 `SYN-SENT` 状态 |
| **排查过程** | 1. 用 `jstack <pid>` 抓取线程堆栈<br>2. 发现关键线程卡在 `NioSocketImpl.timedFinishConnect` → `SSLConnectionSocketFactory.connectSocket` → `HttpClientHelper.performHttpRequest` → `DefaultCacheAwareExternalResourceAccessor`<br>3. 用 `ss -tp` 查看 TCP 连接状态，发现 `SYN-SENT` 状态连接 |
| **解决方式** | 在 `gradle.properties` 中显式添加代理配置：<br>```systemProp.http.proxyHost=127.0.0.1```<br>```systemProp.http.proxyPort=18080```<br>```systemProp.https.proxyHost=127.0.0.1```<br>```systemProp.https.proxyPort=18080``` |
| **涉及文件** | `gradle.properties` |
| **耗时** | 约 15 分钟（4 次构建尝试，最耗时的问题） |

### 问题 3：Maven 仓库配置冲突

| 项目 | 详情 |
|------|------|
| **错误现象** | `Build was configured to prefer settings repositories over project repositories but repository 'maven' was added by build file 'build.gradle'` |
| **根因** | `settings.gradle` 中设置了 `RepositoriesMode.FAIL_ON_PROJECT_REPOS`，禁止项目级仓库声明，但 `build.gradle` 的 `allprojects` 块中定义了 `maven {}` 仓库 |
| **解决方式** | 1. 将 `RepositoriesMode.FAIL_ON_PROJECT_REPOS` 改为 `RepositoriesMode.PREFER_SETTINGS`<br>2. 移除 `google()` 和 `gradlePluginPortal()`（被墙），只保留阿里云镜像<br>3. 在 `buildscript` 块中直接声明 AGP 和 Kotlin Plugin 的 classpath |
| **涉及文件** | `settings.gradle`、`build.gradle` |
| **耗时** | 约 5 分钟（2 次构建尝试） |

### 问题 4：compileSdk 版本过低

| 项目 | 详情 |
|------|------|
| **错误现象** | `Dependency 'androidx.lifecycle:lifecycle-viewmodel-ktx:2.7.0' requires 'compileSdkVersion 34' but the build is using 33` |
| **根因** | 项目 `compileSdk` 设为 33，但 `androidx.lifecycle:2.7.0` 要求 compileSdk 34 |
| **解决方式** | 将 `compileSdk` 和 `targetSdk` 从 33 升级到 34 |
| **涉及文件** | `app/build.gradle` |
| **耗时** | 1 分钟 |

### 问题 5：XML 布局命名空间缺失

| 项目 | 详情 |
|------|------|
| **错误现象** | 编译报错：`error: attribute tools:ignore not found`（5 个文件） |
| **根因** | 5 个布局 XML 文件使用了 `tools:ignore` 属性但未在根元素声明 `xmlns:tools="http://schemas.android.com/tools"` |
| **解决方式** | 逐个在根元素添加 `xmlns:tools` 命名空间声明 |
| **涉及文件** | `activity_main.xml`, `activity_app_list.xml`, `activity_sampling.xml`, `activity_analysis_result.xml`, `activity_hook_manager.xml` |
| **耗时** | 2 分钟 |

### 问题 6：Material3 样式名不存在

| 项目 | 详情 |
|------|------|
| **错误现象** | `style 'Widget.Material3.Chip' not found` |
| **根因** | Material Components 1.11.0 中 `Widget.Material3.Chip` 已被拆分为更具体的子样式 |
| **解决方式** | 将 `Widget.Material3.Chip` 改为 `Widget.Material3.Chip.Assist` |
| **涉及文件** | `themes.xml` |
| **耗时** | 1 分钟 |

### 问题 7：缺少应用图标资源

| 项目 | 详情 |
|------|------|
| **错误现象** | 打包报错：`resource mipmap/ic_launcher not found` |
| **根因** | `AndroidManifest.xml` 引用 `@mipmap/ic_launcher` 但项目无 mipmap 资源目录 |
| **解决方式** | 1. 创建矢量图标 `ic_launcher.xml`（drawable）<br>2. 修改 AndroidManifest 引用为 `@drawable/ic_launcher`<br>3. 创建 `ic_launcher_round.xml` 同样指向 drawable |
| **涉及文件** | `AndroidManifest.xml`, `drawable/ic_launcher.xml` |
| **耗时** | 2 分钟 |

### 问题 8：Kotlin 反射语法错误

| 项目 | 详情 |
|------|------|
| **错误现象** | 编译报错：`unresolved reference: int` 在 `field.int.get(null)` |
| **根因** | Kotlin 反射中 `Field.int` 不是有效的属性访问方式 |
| **解决方式** | 将 `moveField.int.get(null)` 改为 `moveField.get(null) as Int` |
| **涉及文件** | `ActivitySampler.kt` |
| **耗时** | 1 分钟 |

### 问题 9：UI 主题切换（v1.0.2 新增）

| 项目 | 详情 |
|------|------|
| **用户需求** | 从深色主题切换到 HyperOS/KernelSU 明亮风格 |
| **修改内容** | 1. `themes.xml`: `Theme.Material3.Dark.NoActionBar` → `Theme.Material3.Light.NoActionBar`<br>2. `windowLightStatusBar`: `false` → `true`<br>3. 所有颜色值从深色改为浅色：<br>&nbsp;&nbsp;- 背景 `#0B0F19` → `#F5F6FA`<br>&nbsp;&nbsp;- 卡片 `#161B2C` → `#FFFFFF`<br>&nbsp;&nbsp;- 主色 `#FF6B3D`(橙) → `#0066FF`(蓝)<br>&nbsp;&nbsp;- 文字 `#FFFFFF` → `#1D2129`<br>4. 次级按钮文字颜色改为蓝色 |
| **涉及文件** | `colors.xml`, `themes.xml` |
| **耗时** | 5 分钟 |

---

## 五、完整对话时间线

以下为本次会话从开始到完成的完整交互记录：

### 第一阶段：环境检查与项目创建（上一轮会话）

| 序号 | 用户消息 | 助手操作 | 结果 |
|------|---------|---------|------|
| 1 | "帮我看看当前云端环境" | 检查 OS、CPU、内存、Java、Gradle、Android SDK | 确认环境基本可用，但 Java 默认 25 不兼容 |
| 2 | "我需要你帮我做一个apk" | 创建 ActivityScanner 项目，编写全部源码 | 项目结构搭建完成 |
| 3 | "是不是卡了" | 检查 Gradle 构建状态 | 发现 gradlew 损坏 |
| 4 | "下载什么呢还缺什么" | 分析缺失组件 | 确认需要配置代理和镜像 |
| 5 | "显示进度" | 尝试构建并输出日志 | 发现网络假死 |
| 6 | "你好，卡在哪里了？网络有问题吗" | 用 jstack 排查 | 发现 TCP SYN-SENT 状态 |
| 7 | 提供 Clash 订阅链接 + "你通过这个，下载等待时间超过1分钟就视为超时，你要不停向我汇报进度" | 配置代理，添加阿里云镜像 | Gradle 能下载依赖了 |
| 8 | "进行到哪里了，缺什么我下载给你，把链接给我" | 汇报进度 | 修复仓库冲突 |
| 9 | "需要多久，你能汇报进度吗？就算卡了也要汇报" | 逐个修复编译错误 | 5 个编译错误逐一修复 |

### 第二阶段：构建成功与输出（当前会话）

| 序号 | 用户消息 | 助手操作 | 结果 |
|------|---------|---------|------|
| 10 | "什么意思，为什么显示已停止运行，你说十几秒但是好几分钟还没反应" | 解释 Gradle daemon 崩溃 + 网络假死原因 | 用户理解了卡顿原因 |
| 11 | "可以" | 复制 APK 到 /workspace | APK 可用 |
| 12 | "软件发给我" | 启动 HTTP 服务器 | 预览服务问题 |
| 13 | "http://localhost:8899/ 无法访问，请启动服务" + 详细报告需求 | 生成详细文字报告 | 报告输出 |
| 14 | "给我出具一份报告，DOCx" | 用 python-docx 生成 DOCX 报告 | `ActivityScanner_Build_Report.docx` 生成 |
| 15 | "那个APP的包名改成APPLENS 软件也叫这个。构建完，然后把软件输出给我" | 修改包名、应用名，重新构建 | v1.0.1 生成 |
| 16 | "把那个liangxin去掉，每更新一次版本号往后推一个" | 移除旧包名引用，版本号+1 | v1.0.1 确认 |
| 17 | "我要的是发到当前的对话框中，我上面不显示你输出的文件" | 启动文件下载服务 | 预览窗口尝试 |
| 18 | "你是呆还是傻？你给我发个你自己的local host是什么意思？" | 停止服务，尝试其他方式 | 预览代理端口被占用 |
| 19 | 提供 GitHub Token + "推送到我的GitHub上" + 详细报告需求 | 认证 GitHub，创建仓库，推送代码 | https://github.com/Horizen5/APPLENS 创建成功 |
| 20 | "我要的是Hyper OS或者是Kernal SU那种明亮风格的UI" | 修改 colors/themes 为明亮风格，重新构建 v1.0.2 | v1.0.2 构建并推送成功 |
| 21 | "为什么卡了" | clean 后重新构建 | 成功，23 秒 |
| 22 | "把出问题的报告重新推送，我要更详细的..." | 收集环境信息，编写本报告 | 进行中 |

### 时间消耗分布

| 阶段 | 耗时（约） | 占比 |
|------|-----------|------|
| 环境检查与项目创建 | 15 分钟 | 12% |
| Gradle Wrapper 修复 | 10 分钟 | 8% |
| 网络代理排查与修复 | 15 分钟 | 12% |
| 仓库配置修复 | 5 分钟 | 4% |
| 代码编译错误修复 | 10 分钟 | 8% |
| 首次成功构建 | 1 分钟 | 1% |
| 包名修改与版本更新 | 5 分钟 | 4% |
| GitHub 推送 | 3 分钟 | 2% |
| UI 主题切换与重新构建 | 8 分钟 | 6% |
| 文件输出尝试（预览服务等） | 15 分钟 | 12% |
| 用户沟通与解释 | 20 分钟 | 16% |
| 报告编写 | 15 分钟 | 12% |
| 其他（等待、缓存等） | 10 分钟 | 3% |
| **总计** | **约 2 小时** | 100% |

---

## 六、最终产物

### 6.1 APK 文件

| 属性 | 值 |
|------|-----|
| 文件名 | `APPLENS-v1.0.2.apk` |
| 大小 | 5.7 MB |
| 路径 | `/workspace/APPLENS-v1.0.2.apk` |
| 包名 | `com.applens` |
| 应用名 | APPLENS |
| 版本号 | 1.0.2 (versionCode: 3) |
| compileSdk | 34 |
| minSdk | 24 (Android 7.0+) |
| targetSdk | 34 |
| 构建工具 | Gradle 8.14.4 |
| AGP 版本 | 8.2.2 |
| Kotlin 版本 | 1.9.22 |
| JDK | OpenJDK 17.0.2 |
| UI 主题 | HyperOS/KernelSU 明亮风格 |

### 6.2 源代码文件清单

| 类别 | 文件 | 功能 |
|------|------|------|
| **数据层** | `ActivitySample.kt` | Activity 采样数据模型 |
| | `AppInfo.kt` | 应用信息数据模型 |
| | `HookRule.kt` | Hook 规则数据模型 |
| **工具层** | `ShellUtils.kt` | Root/Shell 命令执行 |
| | `ActivitySampler.kt` | UsageStatsManager 采样 |
| | `HookExecutor.kt` | pm/am 命令管理 Activity |
| **服务层** | `SamplingService.kt` | 前台服务进行 10 秒采样 |
| **UI 层** | `MainActivity.kt` | 主界面 |
| | `AppListActivity.kt` | 应用选择列表 |
| | `SamplingActivity.kt` | 采样进度页 |
| | `AnalysisResultActivity.kt` | 分析结果展示 |
| | `HookManagerActivity.kt` | Hook 管理页面 |
| | 4 个 Adapter | RecyclerView 适配器 |
| **资源** | 8 个布局 XML | HyperOS 风格 UI |
| | `colors.xml` | 明亮风格配色 |
| | `themes.xml` | Material3 Light 主题 |
| | `strings.xml` | 字符串资源 |
| **构建** | `build.gradle` | 项目级构建配置 |
| | `app/build.gradle` | 模块级构建配置 |
| | `settings.gradle` | 仓库与项目设置 |
| | `gradle.properties` | JVM 参数 + 代理配置 |
| | `AndroidManifest.xml` | 权限和组件声明 |

### 6.3 功能清单

1. **Root/ADB 权限获取** — 通过 `su` 或 `sh` 执行特权命令
2. **应用扫描与选择** — 列出已安装应用，支持搜索过滤
3. **10秒自动采样** — 前台服务 + `UsageStatsManager` 监听 Activity 切换事件
4. **Activity 占用分析** — 统计各 Activity 前台停留时间并排序展示
5. **Hook 管理** — 通过 `pm disable` / `am force-stop` 等命令管理不必要的 Activity
6. **HyperOS/KernelSU 明亮风格 UI** — 白色卡片、蓝色主调、圆角、渐变按钮

---

## 七、如果换环境：一键搭建指南

### 7.1 环境安装脚本

```bash
#!/bin/bash
set -e

echo "===== 1. 安装 JDK 17 ====="
apt update && apt install -y openjdk-17-jdk

echo "===== 2. 下载 Android cmdline-tools ====="
mkdir -p /opt/android-sdk/cmdline-tools
cd /tmp
wget -q https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip
unzip -q commandlinetools-linux-11076708_latest.zip
mv cmdline-tools /opt/android-sdk/cmdline-tools/latest

echo "===== 3. 配置环境变量 ====="
export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64
export ANDROID_HOME=/opt/android-sdk
export PATH=$JAVA_HOME/bin:$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools:$PATH

echo "===== 4. 安装 SDK 组件 ====="
yes | sdkmanager "platforms;android-34"
yes | sdkmanager "build-tools;34.0.0"
yes | sdkmanager "platform-tools"

echo "===== 5. 安装 Gradle ====="
cd /tmp
wget -q https://services.gradle.org/distributions/gradle-8.14.4-bin.zip
unzip -q gradle-8.14.4-bin.zip -d /opt/
ln -sf /opt/gradle-8.14.4/bin/gradle /usr/local/bin/gradle

echo "===== 环境安装完成 ====="
```

### 7.2 gradle.properties 关键配置

如果在国内网络环境，必须在 `gradle.properties` 中配置：

```properties
# JVM 参数
org.gradle.jvmargs=-Xmx2048m -Dfile.encoding=UTF-8
org.gradle.parallel=true
org.gradle.caching=true

# 代理配置（如果有代理）
systemProp.http.proxyHost=127.0.0.1
systemProp.http.proxyPort=18080
systemProp.https.proxyHost=127.0.0.1
systemProp.https.proxyPort=18080

# AndroidX
android.useAndroidX=true
android.nonTransitiveRClass=true
```

### 7.3 settings.gradle 仓库配置

```groovy
pluginManagement {
    repositories {
        maven { url 'https://maven.aliyun.com/repository/public' }
        maven { url 'https://maven.aliyun.com/repository/google' }
        maven { url 'https://maven.aliyun.com/repository/gradle-plugin' }
        google()
        mavenCentral()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.PREFER_SETTINGS)
    repositories {
        maven { url 'https://maven.aliyun.com/repository/public' }
        maven { url 'https://maven.aliyun.com/repository/google' }
        google()
        mavenCentral()
    }
}
```

### 7.4 构建命令

```bash
export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64
export ANDROID_HOME=/opt/android-sdk
export PATH=$JAVA_HOME/bin:$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools:$PATH

cd /workspace/ActivityScanner
gradle clean assembleDebug --no-daemon --console=plain

# APK 输出路径: app/build/outputs/apk/debug/app-debug.apk
```

---

## 八、经验总结

### 8.1 关键教训

| 序号 | 教训 | 说明 |
|------|------|------|
| 1 | **Gradle 不读系统代理** | 必须在 `gradle.properties` 中用 `systemProp.https.proxyHost` 显式配置 |
| 2 | **国内用阿里云镜像** | `google()` 和 `gradlePluginPortal()` 在国内被墙，必须用镜像 |
| 3 | **JDK 17 最稳** | AGP 8.2 不支持 JDK 21+，JDK 17 是最佳选择 |
| 4 | **不要改 gradlew** | 沙箱可能注入 agent 路径，导致 JVM 崩溃；优先用系统 Gradle |
| 5 | **compileSdk 跟依赖走** | `lifecycle 2.7.0+` 要求 compileSdk 34，升级依赖时同步检查 |
| 6 | **XML 命名空间** | 用 `tools:` 属性必须声明 `xmlns:tools` |
| 7 | **排查假死用 jstack** | Gradle 无响应时，`jstack` + `ss -tp` 是排查利器 |
| 8 | **预览服务限制** | 沙箱 localhost 端口外部不可达，文件传输用 GitHub 或 IDE 文件浏览器 |

### 8.2 排查工具速查

| 工具 | 用途 | 命令 |
|------|------|------|
| `jstack` | 抓取 JVM 线程堆栈 | `jstack <pid>` |
| `ss -tp` | 查看 TCP 连接状态 | `ss -tp \| grep SYN` |
| `jps` | 列出 Java 进程 | `jps -l` |
| `strace` | 跟踪系统调用 | `strace -p <pid> -e network` |
| `curl -v` | 测试网络连通 | `curl -v https://maven.aliyun.com` |

---

*报告生成时间: 2026-07-30*  
*项目仓库: https://github.com/Horizen5/APPLENS*  
*当前版本: APPLENS v1.0.2*
