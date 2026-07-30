package com.applens.ui

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.applens.R
import com.applens.data.ActivityStat
import com.applens.data.HookRule
import com.applens.data.HookRuleStore
import com.applens.data.SamplingReport
import com.applens.databinding.ActivityAnalysisResultBinding
import com.applens.utils.HookExecutor

class AnalysisResultActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAnalysisResultBinding
    private lateinit var pkg: String
    private lateinit var label: String
    private lateinit var store: HookRuleStore
    private val adapter = ActivityStatAdapter { stat -> onStatClick(stat) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAnalysisResultBinding.inflate(layoutInflater)
        setContentView(binding.root)

        pkg = intent.getStringExtra(EXTRA_PACKAGE) ?: run { finish(); return }
        label = intent.getStringExtra(EXTRA_LABEL) ?: pkg
        store = HookRuleStore(this)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            title = label
        }
        binding.toolbar.navigationIcon?.setTint(ContextCompat.getColor(this, R.color.hyper_text_primary))

        binding.recycler.layoutManager = LinearLayoutManager(this)
        binding.recycler.adapter = adapter

        val report = lastReport
        if (report == null) {
            finish(); return
        }
        render(report)
    }

    private fun render(report: SamplingReport) {
        // 顶部 Top Activity
        val top = report.topActivity
        if (top != null) {
            binding.tvTopActivityName.text = top.activityFullName
            binding.tvTopActivityDesc.text = String.format(
                "出现 %d 次 / 占比 %.1f%% / 平均 CPU %.1f%% / 平均内存 %d KB",
                top.count, top.percentOf(report.sampleCount), top.avgCpu, top.avgMemKb
            )
            // 推断作用
            val desc = describeActivity(top)
            val suggest = suggestAction(top, report)
            binding.tvActivityRole.text = desc
            binding.tvSuggestion.text = suggest
        } else {
            binding.tvTopActivityName.text = "未采集到 Activity"
            binding.tvTopActivityDesc.text = "可能权限未授权，或应用未在前台"
        }

        // 概览
        binding.tvSampleCount.text = report.sampleCount.toString()
        binding.tvDuration.text = "${report.durationMs / 1000}s"

        // 列表
        adapter.submitList(report.stats)
    }

    /**
     * 简单推断 Activity 的作用
     */
    private fun describeActivity(stat: ActivityStat): String {
        val name = stat.activityName.lowercase()
        return when {
            "splash" in name || "launch" in name -> "启动页 / 闪屏页"
            "main" in name -> "主界面"
            "login" in name || "auth" in name -> "登录 / 认证页"
            "guide" in name || "onboard" in name -> "引导页"
            "web" in name || "browser" in name -> "网页容器"
            "ad" in name || "advert" in name -> "广告页"
            "setting" in name -> "设置页"
            "detail" in name -> "详情页"
            "list" in name -> "列表页"
            "push" in name || "process" in name -> "后台推送/进程"
            else -> "未知作用"
        }
    }

    /**
     * 给出建议
     */
    private fun suggestAction(stat: ActivityStat, report: SamplingReport): String {
        val name = stat.activityName.lowercase()
        val ratio = stat.percentOf(report.sampleCount)
        return when {
            "splash" in name || "launch" in name -> "启动页，应用启动后不再需要，可考虑 Hook 禁用"
            "ad" in name || "advert" in name -> "广告页，建议 Hook 禁用"
            "guide" in name || "onboard" in name -> "引导页，可考虑禁用"
            "main" in name -> "主界面，保留"
            "login" in name -> "登录页，保留"
            ratio < 5f -> "出现时长很短，可能为过渡页，可考虑禁用"
            else -> "暂无明确建议，请结合应用功能判断"
        }
    }

    private fun onStatClick(stat: ActivityStat) {
        // 弹出 Hook 操作选项
        val actions = arrayOf(
            getString(R.string.hook_disable),
            getString(R.string.hook_enable),
            getString(R.string.hook_block),
            getString(R.string.hook_tag),
            getString(R.string.hook_remove)
        )
        AlertDialog.Builder(this)
            .setTitle(stat.activityFullName)
            .setItems(actions) { _, which ->
                val action = when (which) {
                    0 -> HookRule.Action.DISABLE
                    1 -> HookRule.Action.ENABLE
                    2 -> HookRule.Action.BLOCK
                    3 -> HookRule.Action.HOOK_TAG
                    else -> {
                        store.remove(pkg, stat.activityFullName)
                        return@setItems
                    }
                }
                val rule = HookRule(
                    packageName = pkg,
                    activityFullName = stat.activityFullName,
                    action = action,
                    createdAt = System.currentTimeMillis()
                )
                store.add(rule)
                // 立即执行
                if (action == HookRule.Action.DISABLE || action == HookRule.Action.ENABLE ||
                    action == HookRule.Action.BLOCK
                ) {
                    Thread {
                        val r = HookExecutor.apply(rule)
                        runOnUiThread {
                            AlertDialog.Builder(this)
                                .setTitle("执行结果")
                                .setMessage(r.message)
                                .setPositiveButton("好的", null)
                                .show()
                        }
                    }.start()
                }
            }
            .show()
    }

    companion object {
        const val EXTRA_PACKAGE = "package"
        const val EXTRA_LABEL = "label"
        @Volatile
        var lastReport: SamplingReport? = null
    }
}
