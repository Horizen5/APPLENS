package com.applens.ui

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
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

        // 设置版本号
        binding.tvVersion.text = getString(R.string.about_version, BuildConfig.VERSION_NAME)

        val colorStyles = arrayOf("经典蓝", "活力紫", "青春绿")
        val themeModes = arrayOf("浅色", "深色", "跟随系统")

        binding.itemColorStyle.setOnClickListener {
            AlertDialog.Builder(requireContext())
                .setTitle("选择色彩风格")
                .setItems(colorStyles) { _, which ->
                    binding.tvColorStyleValue.text = colorStyles[which]
                    Toast.makeText(requireContext(), "已切换为：${colorStyles[which]}", Toast.LENGTH_SHORT).show()
                }
                .show()
        }

        binding.itemTheme.setOnClickListener {
            AlertDialog.Builder(requireContext())
                .setTitle("选择主题模式")
                .setItems(themeModes) { _, which ->
                    binding.tvThemeModeValue.text = themeModes[which]
                    Toast.makeText(requireContext(), "已切换为：${themeModes[which]}", Toast.LENGTH_SHORT).show()
                }
                .show()
        }

        binding.itemAbout.setOnClickListener {
            val aboutFragment = AboutFragment()
            parentFragmentManager.beginTransaction()
                .setCustomAnimations(
                    android.R.anim.fade_in,
                    android.R.anim.fade_out,
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
        binding.tvSpeedValue.text = String.format("%.2fx", savedSpeed)

        binding.seekSpeed.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val speed = progress / 100f
                binding.tvSpeedValue.text = String.format("%.2fx", speed)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                val speed = seekBar?.progress?.div(100f) ?: 1.0f
                prefs.edit().putFloat(KEY_ANIM_SPEED, speed).apply()
            }
        })
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}