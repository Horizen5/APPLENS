package com.applens.ui

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import androidx.fragment.app.Fragment
import com.applens.BuildConfig
import com.applens.R
import com.applens.databinding.FragmentSettingsBinding

class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    companion object {
        const val PREFS_NAME = "app_settings"
        const val KEY_ANIM_SPEED = "anim_speed"
    }

    private lateinit var prefs: SharedPreferences

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        prefs = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        // 版本号
        binding.tvVersion.text = getString(R.string.about_version, BuildConfig.VERSION_NAME)

        // 跟随系统
        binding.itemFollowSystem.setOnClickListener {
            // 切换跟随系统状态
            val current = prefs.getBoolean("follow_system", true)
            prefs.edit().putBoolean("follow_system", !current).apply()
            updateFollowSystemText()
        }
        updateFollowSystemText()

        // 关于
        binding.itemAbout.setOnClickListener {
            val aboutFragment = AboutFragment()
            parentFragmentManager.beginTransaction()
                .setCustomAnimations(
                    android.R.anim.fade_in,
                    android.R.anim.fade_out
                )
                .replace(R.id.nav_host_fragment, aboutFragment, "about")
                .addToBackStack(null)
                .commit()
        }

        // 动画速度滑块
        val savedSpeed = prefs.getFloat(KEY_ANIM_SPEED, 1.0f)
        val progress = (savedSpeed * 100).toInt()
        binding.seekSpeed.progress = progress
        updateSpeedText(savedSpeed)

        binding.seekSpeed.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val speed = progress / 100f
                updateSpeedText(speed)
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                val speed = seekBar?.progress?.div(100f) ?: 1.0f
                prefs.edit().putFloat(KEY_ANIM_SPEED, speed).apply()
            }
        })
    }

    private fun updateSpeedText(speed: Float) {
        binding.tvSpeedValue.text = String.format("%.2fx", speed)
    }

    private fun updateFollowSystemText() {
        val enabled = prefs.getBoolean("follow_system", true)
        binding.tvFollowSystemValue.text = if (enabled) "已开启" else "已关闭"
        binding.tvFollowSystemValue.setTextColor(
            if (enabled) {
                android.graphics.Color.parseColor("#5B8CFF")
            } else {
                android.graphics.Color.parseColor("#999999")
            }
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}