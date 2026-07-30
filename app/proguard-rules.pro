# 保留 Kotlin 元数据
-keep class kotlin.Metadata { *; }
-keepclassmembers class * {
    @androidx.annotation.Keep *;
}

# 保留 AppInfo / Activity 等数据类
-keep class com.liangxin.activityscanner.data.** { *; }
