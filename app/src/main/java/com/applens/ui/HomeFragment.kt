package com.applens.ui

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import com.applens.R
import com.applens.databinding.FragmentHomeBinding
import com.applens.utils.ActivitySampler
import com.applens.utils.ShellUtils

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private val sampler by lazy { ActivitySampler(requireContext()) }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onResume() {
        super.onResume()
        updateStatus()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        bindActions()
    }

    private fun updateStatus() {
        val ctx = requireContext()
        val hasRoot = ShellUtils.isRootAvailable
        val hasUsage = sampler.hasUsageStatsPermission()
        val hasOverlay = android.provider.Settings.canDrawOverlays(ctx)

        // Root 状态
        binding.tvRootStatus.text = if (hasRoot) getString(R.string.status_root_ok) else getString(R.string.status_root_no)
        binding.ivRootIcon.isVisible = hasRoot
        binding.ivRootIcon.setColorFilter(ContextCompat.getColor(ctx, R.color.lsp_green))
        binding.btnGrantRoot.isVisible = !hasRoot

        // 使用情况访问权限
        binding.tvUsageStatus.text = if (hasUsage) getString(R.string.status_usage_ok) else getString(R.string.status_usage_no)
        binding.ivUsageIcon.isVisible = hasUsage
        binding.ivUsageIcon.setColorFilter(ContextCompat.getColor(ctx, R.color.lsp_green))
        binding.btnGrantUsage.isVisible = !hasUsage

        // 悬浮窗权限
        binding.tvOverlayStatus.text = if (hasOverlay) getString(R.string.status_overlay_ok) else getString(R.string.status_overlay_no)
        binding.ivOverlayIcon.isVisible = hasOverlay
        binding.ivOverlayIcon.setColorFilter(ContextCompat.getColor(ctx, R.color.lsp_green))
        binding.btnGrantOverlay.isVisible = !hasOverlay

        // 状态卡片背景
        val allOk = hasRoot && hasUsage && hasOverlay
        binding.statusCard.setBackgroundResource(
            if (allOk) R.drawable.bg_status_green else R.drawable.bg_status_yellow
        )
    }

    private fun bindActions() {
        binding.btnGrantRoot.setOnClickListener {
            Thread {
                ShellUtils.exec("id")
                activity?.runOnUiThread { updateStatus() }
            }.start()
        }
        binding.btnGrantUsage.setOnClickListener {
            sampler.requestUsageStatsPermission()
        }
        binding.btnGrantOverlay.setOnClickListener {
            sampler.requestOverlayPermission()
        }
        binding.btnStartScan.setOnClickListener {
            startActivity(Intent(requireContext(), AppListActivity::class.java))
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}