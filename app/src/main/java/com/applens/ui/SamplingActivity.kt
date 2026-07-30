package com.applens.ui

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.applens.R
import com.applens.data.SamplingReport
import com.applens.databinding.ActivitySamplingBinding
import com.applens.service.SamplingService
import com.applens.utils.ActivitySampler

class SamplingActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySamplingBinding
    private lateinit var pkg: String
    private lateinit var label: String
    private val sampler by lazy { ActivitySampler(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySamplingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        pkg = intent.getStringExtra(EXTRA_PACKAGE) ?: run { finish(); return }
        label = intent.getStringExtra(EXTRA_LABEL) ?: pkg

        setSupportActionBar(binding.toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            title = label
        }
        binding.toolbar.navigationIcon?.setTint(ContextCompat.getColor(this, R.color.hyper_text_primary))

        binding.tvAppName.text = label
        binding.tvPackage.text = pkg

        // 检查权限
        if (!sampler.hasUsageStatsPermission()) {
            binding.tvHint.text = "请先在主页授予「使用情况访问」权限"
            binding.btnStart.isEnabled = false
        }

        binding.btnStart.setOnClickListener {
            startSampling()
        }

        // 监听进度
        SamplingService.onProgress = { cur, total ->
            runOnUiThread {
                val pct = cur * 100 / total
                binding.progressBar.progress = pct
                binding.tvProgress.text = getString(R.string.sampling_progress, cur, total)
            }
        }
        SamplingService.onComplete = { report ->
            runOnUiThread { onSampleDone(report) }
        }
        SamplingService.onError = { msg ->
            runOnUiThread {
                binding.tvHint.text = "采样出错：$msg"
                binding.btnStart.isEnabled = true
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        SamplingService.onProgress = null
        SamplingService.onComplete = null
        SamplingService.onError = null
    }

    private fun startSampling() {
        binding.btnStart.isEnabled = false
        binding.tvHint.text = getString(R.string.sampling_desc, label)
        binding.progressBar.progress = 0

        val intent = Intent(this, SamplingService::class.java).apply {
            putExtra(SamplingService.EXTRA_PACKAGE, pkg)
            putExtra(SamplingService.EXTRA_LABEL, label)
        }
        ContextCompat.startForegroundService(this, intent)
    }

    private fun onSampleDone(report: SamplingReport) {
        binding.btnStart.isEnabled = true
        binding.tvHint.text = getString(R.string.sampling_done)
        // 跳转结果页
        AnalysisResultActivity.lastReport = report
        val intent = Intent(this, AnalysisResultActivity::class.java).apply {
            putExtra(AnalysisResultActivity.EXTRA_PACKAGE, pkg)
            putExtra(AnalysisResultActivity.EXTRA_LABEL, label)
        }
        startActivity(intent)
    }

    companion object {
        const val EXTRA_PACKAGE = "package"
        const val EXTRA_LABEL = "label"
    }
}
