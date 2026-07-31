package com.applens.ui

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RadioGroup
import android.widget.SeekBar
import androidx.fragment.app.Fragment
import com.applens.BuildConfig
import com.applens.R
import com.applens.core.ControllerManager
import com.applens.core.ControllerType
import com.applens.databinding.FragmentSettingsBinding

class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    companion object {
        const val PREFS_NAME = "app_settings"
        const val KEY_ANIM_SPEED = "anim_speed"
        const val KEY_FOLLOW_SYSTEM = "follow_system"
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

        // 跟随系统 Switch
        binding.switchFollowSystem.isChecked = prefs.getBoolean(KEY_FOLLOW_SYSTEM, true)
        binding.switchFollowSystem.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean(KEY_FOLLOW_SYSTEM, isChecked).apply()
        }

        // 控制器模式选择
        setupControllerType()

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

    private fun setupControllerType() {
        val currentType = ControllerManager.getControllerType(requireContext())

        // 设置当前选中项
        when (currentType) {
            ControllerType.PM -> binding.rbPm.isChecked = true
            ControllerType.IFW -> binding.rbIfw.isChecked = true
            ControllerType.IFW_PLUS_PM -> binding.rbIfwPm.isChecked = true
        }
        updateControllerDesc(currentType)

        // 监听切换
        binding.rgController.setOnCheckedChangeListener { _, checkedId ->
            val newType = when (checkedId) {
                R.id.rb_pm -> ControllerType.PM
                R.id.rb_ifw -> ControllerType.IFW
                R.id.rb_ifw_pm -> ControllerType.IFW_PLUS_PM
                else -> ControllerType.PM
            }
            ControllerManager.setControllerType(requireContext(), newType)
            updateControllerDesc(newType)
        }
    }

    private fun updateControllerDesc(type: ControllerType) {
        binding.tvControllerDesc.text = type.description
    }

    private fun updateSpeedText(speed: Float) {
        binding.tvSpeedValue.text = String.format("%.2fx", speed)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
