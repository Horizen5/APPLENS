package com.applens.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/**
 * Hook 管理配置：记录用户对哪些 Activity 做了什么操作
 *
 * 操作类型：
 *  - DISABLE  通过 pm disable 禁用
 *  - ENABLE   通过 pm enable 启用
 *  - BLOCK    通过 am force-stop 在出现时强制停止（轮询模式）
 *  - HOOK_TAG 仅记录，标记为"待 Hook"的候选项
 */
data class HookRule(
    val packageName: String,
    val activityFullName: String,
    val action: Action,
    val createdAt: Long,
    val note: String = ""
) {
    enum class Action { DISABLE, ENABLE, BLOCK, HOOK_TAG }

    fun toJson(): JSONObject = JSONObject().apply {
        put("pkg", packageName)
        put("act", activityFullName)
        put("action", action.name)
        put("ts", createdAt)
        put("note", note)
    }

    companion object {
        fun fromJson(o: JSONObject): HookRule = HookRule(
            packageName = o.getString("pkg"),
            activityFullName = o.getString("act"),
            action = Action.valueOf(o.getString("action")),
            createdAt = o.getLong("ts"),
            note = o.optString("note", "")
        )
    }
}

class HookRuleStore(context: Context) {

    private val prefs = context.getSharedPreferences("hook_rules", Context.MODE_PRIVATE)

    fun loadAll(): List<HookRule> {
        val raw = prefs.getString("rules", "[]") ?: "[]"
        val arr = JSONArray(raw)
        return (0 until arr.length()).map { HookRule.fromJson(arr.getJSONObject(it)) }
    }

    fun saveAll(rules: List<HookRule>) {
        val arr = JSONArray()
        rules.forEach { arr.put(it.toJson()) }
        prefs.edit().putString("rules", arr.toString()).apply()
    }

    fun add(rule: HookRule) {
        val all = loadAll().toMutableList()
        // 同包同 Activity 替换
        all.removeAll { it.packageName == rule.packageName && it.activityFullName == rule.activityFullName }
        all.add(rule)
        saveAll(all)
    }

    fun remove(packageName: String, activityFullName: String) {
        val all = loadAll().toMutableList()
        all.removeAll { it.packageName == packageName && it.activityFullName == activityFullName }
        saveAll(all)
    }

    fun find(packageName: String, activityFullName: String): HookRule? =
        loadAll().firstOrNull { it.packageName == packageName && it.activityFullName == activityFullName }
}
