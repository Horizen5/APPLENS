package com.applens.ui

import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.applens.R
import com.applens.data.HookRule
import com.applens.data.HookRuleStore
import com.applens.databinding.ActivityHookManagerBinding
import com.applens.utils.HookExecutor

class HookManagerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHookManagerBinding
    private lateinit var store: HookRuleStore
    private val adapter = HookRuleAdapter(
        onApply = { rule -> execRule(rule, isRevert = false) },
        onRevert = { rule -> execRule(rule, isRevert = true) },
        onRemove = { rule ->
            store.remove(rule.packageName, rule.activityFullName)
            refresh()
        }
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHookManagerBinding.inflate(layoutInflater)
        setContentView(binding.root)
        store = HookRuleStore(this)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            title = getString(R.string.menu_hooks)
        }
        binding.toolbar.navigationIcon?.setTint(ContextCompat.getColor(this, R.color.hyper_text_primary))

        binding.recycler.layoutManager = LinearLayoutManager(this)
        binding.recycler.adapter = adapter
    }

    override fun onResume() {
        super.onResume()
        refresh()
    }

    private fun refresh() {
        val rules = store.loadAll()
        adapter.submitList(rules)
        binding.tvEmpty.visibility = if (rules.isEmpty()) android.view.View.VISIBLE else android.view.View.GONE
    }

    private fun execRule(rule: HookRule, isRevert: Boolean) {
        Thread {
            val r = if (isRevert) HookExecutor.revert(rule) else HookExecutor.apply(rule)
            runOnUiThread {
                AlertDialog.Builder(this)
                    .setTitle(if (isRevert) "撤销结果" else "执行结果")
                    .setMessage(r.message)
                    .setPositiveButton("好的", null)
                    .show()
            }
        }.start()
    }
}
