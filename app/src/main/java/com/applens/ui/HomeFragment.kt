package com.applens.ui

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
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
        binding.ivRootIcon.setImageResource(
            if (hasRoot) R.drawable.ic_check_green else R.drawable.ic_cross_red
        )
        binding.btnGrantRoot.visibility = if (hasRoot) View.GONE else View.VISIBLE

        // 使用情况访问权限
        binding.tvUsageStatus.text = if (hasUsage) getString(R.string.status_usage_ok) else getString(R.string.status_usage_no)
        binding.ivUsageIcon.setImageResource(
            if (hasUsage) R.drawable.ic_check_green else R.drawable.ic_cross_red
        )
        binding.btnGrantUsage.visibility = if (hasUsage) View.GONE else View.VISIBLE

        // 悬浮窗权限
        binding.tvOverlayStatus.text = if (hasOverlay) getString(R.string.status_overlay_ok) else getString(R.string.status_overlay_no)
        binding.ivOverlayIcon.setImageResource(
            if (hasOverlay) R.drawable.ic_check_green else R.drawable.ic_cross_red
        )
        binding.btnGrantOverlay.visibility = if (hasOverlay) View.GONE else View.VISIBLE

        // 状态卡片
        val allOk = hasRoot && hasUsage && hasOverlay
        if (allOk) {
            binding.statusCard.setBackgroundResource(R.drawable.bg_status_green)
            binding.ivStatusIcon.setImageResource(R.drawable.ic_check_green)
            binding.tvStatusDesc.text = getString(R.string.status_all_ok)
            binding.tvStatusDesc.setTextColor(ContextCompat.getColor(ctx, R.color.lsp_green))
        } else {
            binding.statusCard.setBackgroundResource(R.drawable.bg_status_yellow)
            binding.ivStatusIcon.setImageResource(R.drawable.ic_cross_red)
            binding.tvStatusDesc.text = getString(R.string.status_partial)
            binding.tvStatusDesc.setTextColor(ContextCompat.getColor(ctx, R.color.lsp_red))
        }
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