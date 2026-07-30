package com.applens.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
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
        
        // Root 状态
        if (ShellUtils.isRootAvailable) {
            binding.tvRootStatus.text = getString(R.string.status_root_ok)
            binding.ivRootIcon.setImageResource(android.R.drawable.checkbox_on_background)
            binding.ivRootIcon.setColorFilter(ContextCompat.getColor(ctx, R.color.hyper_green))
        } else {
            binding.tvRootStatus.text = getString(R.string.status_root_no)
            binding.ivRootIcon.setImageResource(android.R.drawable.ic_dialog_info)
            binding.ivRootIcon.setColorFilter(ContextCompat.getColor(ctx, R.color.hyper_yellow))
        }

        // 使用情况访问权限
        if (sampler.hasUsageStatsPermission()) {
            binding.tvUsageStatus.text = getString(R.string.status_usage_ok)
            binding.ivUsageIcon.setImageResource(android.R.drawable.checkbox_on_background)
            binding.ivUsageIcon.setColorFilter(ContextCompat.getColor(ctx, R.color.hyper_green))
        } else {
            binding.tvUsageStatus.text = getString(R.string.status_usage_no)
            binding.ivUsageIcon.setImageResource(android.R.drawable.ic_dialog_info)
            binding.ivUsageIcon.setColorFilter(ContextCompat.getColor(ctx, R.color.hyper_yellow))
        }

        // 悬浮窗权限
        if (android.provider.Settings.canDrawOverlays(ctx)) {
            binding.tvOverlayStatus.text = getString(R.string.status_overlay_ok)
            binding.ivOverlayIcon.setImageResource(android.R.drawable.checkbox_on_background)
            binding.ivOverlayIcon.setColorFilter(ContextCompat.getColor(ctx, R.color.hyper_green))
        } else {
            binding.tvOverlayStatus.text = getString(R.string.status_overlay_no)
            binding.ivOverlayIcon.setImageResource(android.R.drawable.ic_dialog_info)
            binding.ivOverlayIcon.setColorFilter(ContextCompat.getColor(ctx, R.color.hyper_yellow))
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
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}