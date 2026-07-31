package com.applens.core

/**
 * IFW (Intent Firewall) XML 序列化/反序列化
 *
 * 参考 Blocker 的 IfwXmlSerializer 设计：
 * - 生成 AOSP 兼容的 IFW XML 格式
 * - 规则文件路径：/data/system/ifw/<package>.xml
 * - 组件类型映射：ACTIVITY → activity, SERVICE → service, RECEIVER → broadcast
 * - IFW 不支持 ContentProvider 屏蔽
 *
 * XML 格式示例：
 * <rules>
 *   <activity block="true">
 *     <component-filter name="com.foo/.MainActivity" />
 *   </activity>
 *   <service block="true">
 *     <component-filter name="com.foo/.BgService" />
 *   </service>
 *   <broadcast block="true">
 *     <component-filter name="com.foo/.BootReceiver" />
 *   </broadcast>
 * </rules>
 */
object IfwXmlSerializer {

    /** 组件类型 */
    enum class ComponentType(val tagName: String) {
        ACTIVITY("activity"),
        SERVICE("service"),
        RECEIVER("broadcast"),  // IFW 中 Receiver 对应 broadcast 节点
        PROVIDER("");           // IFW 不支持 Provider

        companion object {
            fun fromComponentName(name: String): ComponentType {
                // 通过类名启发式推断类型
                return when {
                    name.contains("Activity", ignoreCase = true) -> ACTIVITY
                    name.contains("Service", ignoreCase = true) -> SERVICE
                    name.contains("Receiver", ignoreCase = true) -> RECEIVER
                    else -> ACTIVITY // 默认按 Activity 处理
                }
            }
        }
    }

    /**
     * IFW 规则（不可变模型，参考 Blocker 的 IfwRules）
     * 函数式更新：每次操作返回新实例
     */
    data class IfwRules(
        val packageName: String,
        val activities: Set<String> = emptySet(),
        val services: Set<String> = emptySet(),
        val receivers: Set<String> = emptySet()
    ) {
        fun addComponentFilter(componentName: String, type: ComponentType): IfwRules {
            return when (type) {
                ComponentType.ACTIVITY -> copy(activities = activities + componentName)
                ComponentType.SERVICE -> copy(services = services + componentName)
                ComponentType.RECEIVER -> copy(receivers = receivers + componentName)
                ComponentType.PROVIDER -> this // 不支持
            }
        }

        fun removeComponentFilter(componentName: String): IfwRules {
            return copy(
                activities = activities - componentName,
                services = services - componentName,
                receivers = receivers - componentName
            )
        }

        fun isEmpty(): Boolean = activities.isEmpty() && services.isEmpty() && receivers.isEmpty()

        fun allComponents(): Set<String> = activities + services + receivers
    }

    /**
     * 将 IfwRules 序列化为 AOSP 兼容的 IFW XML
     */
    fun serialize(rules: IfwRules): String {
        val sb = StringBuilder()
        sb.append("<rules>\n")

        if (rules.activities.isNotEmpty()) {
            sb.append("  <activity block=\"true\">\n")
            rules.activities.sorted().forEach { name ->
                sb.append("    <component-filter name=\"$name\" />\n")
            }
            sb.append("  </activity>\n")
        }

        if (rules.services.isNotEmpty()) {
            sb.append("  <service block=\"true\">\n")
            rules.services.sorted().forEach { name ->
                sb.append("    <component-filter name=\"$name\" />\n")
            }
            sb.append("  </service>\n")
        }

        if (rules.receivers.isNotEmpty()) {
            sb.append("  <broadcast block=\"true\">\n")
            rules.receivers.sorted().forEach { name ->
                sb.append("    <component-filter name=\"$name\" />\n")
            }
            sb.append("  </broadcast>\n")
        }

        sb.append("</rules>")
        return sb.toString()
    }

    /**
     * 从 XML 文本反序列化为 IfwRules
     */
    fun deserialize(xml: String, packageName: String): IfwRules {
        var activities = emptySet<String>()
        var services = emptySet<String>()
        var receivers = emptySet<String>()

        // 简单的正则解析（不依赖 XML 库，保持轻量）
        val activityBlock = extractBlock(xml, "activity")
        if (activityBlock != null) {
            activities = extractComponentFilters(activityBlock)
        }

        val serviceBlock = extractBlock(xml, "service")
        if (serviceBlock != null) {
            services = extractComponentFilters(serviceBlock)
        }

        val broadcastBlock = extractBlock(xml, "broadcast")
        if (broadcastBlock != null) {
            receivers = extractComponentFilters(broadcastBlock)
        }

        return IfwRules(packageName, activities, services, receivers)
    }

    private fun extractBlock(xml: String, tagName: String): String? {
        val pattern = Regex("<$tagName[^>]*>([\\s\\S]*?)</$tagName>")
        return pattern.find(xml)?.groupValues?.get(1)
    }

    private fun extractComponentFilters(block: String): Set<String> {
        val pattern = Regex("<component-filter\\s+name=\"([^\"]+)\"")
        return pattern.findAll(block).map { it.groupValues[1] }.toSet()
    }
}
