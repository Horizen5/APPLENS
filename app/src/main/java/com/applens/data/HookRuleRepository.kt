package com.applens.data

import android.content.Context

object HookRuleRepository {
    private var store: HookRuleStore? = null

    fun init(context: Context) {
        store = HookRuleStore(context.applicationContext)
    }

    fun listAll(context: Context): List<HookRule> {
        if (store == null) init(context)
        return store!!.loadAll()
    }

    fun add(context: Context, rule: HookRule) {
        if (store == null) init(context)
        store!!.add(rule)
    }

    fun remove(context: Context, packageName: String, activityFullName: String) {
        if (store == null) init(context)
        store!!.remove(packageName, activityFullName)
    }

    fun find(context: Context, packageName: String, activityFullName: String): HookRule? {
        if (store == null) init(context)
        return store!!.find(packageName, activityFullName)
    }
}