package com.applens.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.applens.R
import com.applens.databinding.ActivityMainBinding
import com.applens.utils.ActivitySampler
import com.applens.utils.ShellUtils

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val sampler by lazy { ActivitySampler(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        updateStatus()
        bindActions()
    }

    override fun onResume() {
        super.onResume()
        updateStatus()
    }

    private fun updateStatus() {
        // Root 状态
        if (ShellUtils.isRootAvailable) {
            binding.tvRootStatus.text = getString(R.string.status_root_ok)
            binding.ivRootIcon.setImageResource(android.R.drawable.checkbox_on_background)
            binding.ivRootIcon.setColorFilter(ContextCompat.getColor(this, R.color.hyper_green))
        } else {
            binding.tvRootStatus.text = getString(R.string.status_root_no)
            binding.ivRootIcon.setImageResource(android.R.drawable.ic_dialog_info)
            binding.ivRootIcon.setColorFilter(ContextCompat.getColor(this, R.color.hyper_yellow))
        }

        // 使用情况访问权限
        if (sampler.hasUsageStatsPermission()) {
            binding.tvUsageStatus.text = getString(R.string.status_usage_ok)
            binding.ivUsageIcon.setImageResource(android.R.drawable.checkbox_on_background)
            binding.ivUsageIcon.setColorFilter(ContextCompat.getColor(this, R.color.hyper_green))
        } else {
            binding.tvUsageStatus.text = getString(R.string.status_usage_no)
            binding.ivUsageIcon.setImageResource(android.R.drawable.ic_dialog_info)
            binding.ivUsageIcon.setColorFilter(ContextCompat.getColor(this, R.color.hyper_yellow))
        }

        // 悬浮窗权限
        if (android.provider.Settings.canDrawOverlays(this)) {
            binding.tvOverlayStatus.text = getString(R.string.status_overlay_ok)
            binding.ivOverlayIcon.setImageResource(android.R.drawable.checkbox_on_background)
            binding.ivOverlayIcon.setColorFilter(ContextCompat.getColor(this, R.color.hyper_green))
        } else {
            binding.tvOverlayStatus.text = getString(R.string.status_overlay_no)
            binding.ivOverlayIcon.setImageResource(android.R.drawable.ic_dialog_info)
            binding.ivOverlayIcon.setColorFilter(ContextCompat.getColor(this, R.color.hyper_yellow))
        }
    }

    private fun bindActions() {
        binding.btnGrantRoot.setOnClickListener {
            // 触发 root 授权弹窗（执行一个无害命令）
            Thread {
                ShellUtils.exec("id")
                runOnUiThread { updateStatus() }
            }.start()
        }
        binding.btnGrantUsage.setOnClickListener {
            sampler.requestUsageStatsPermission()
        }
        binding.btnGrantOverlay.setOnClickListener {
            sampler.requestOverlayPermission()
        }
        binding.btnSelectApp.setOnClickListener {
            startActivity(Intent(this, AppListActivity::class.java))
        }
        binding.btnHookManager.setOnClickListener {
            startActivity(Intent(this, HookManagerActivity::class.java))
        }
    }
}
