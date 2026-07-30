package com.applens.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import com.applens.R
import com.applens.databinding.FragmentSettingsBinding

class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

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

        val colorStyles = arrayOf("经典蓝", "活力紫", "青春绿")
        val themeModes = arrayOf("浅色", "深色", "跟随系统")

        binding.itemColorStyle.setOnClickListener {
            AlertDialog.Builder(requireContext())
                .setTitle("选择色彩风格")
                .setItems(colorStyles) { _, which ->
                    binding.tvColorStyleValue.text = colorStyles[which]
                    // 实际应用切换色彩的逻辑可以在这里扩展
                    Toast.makeText(requireContext(), "已切换为：${colorStyles[which]}", Toast.LENGTH_SHORT).show()
                }
                .show()
        }

        binding.itemTheme.setOnClickListener {
            AlertDialog.Builder(requireContext())
                .setTitle("选择主题模式")
                .setItems(themeModes) { _, which ->
                    binding.tvThemeModeValue.text = themeModes[which]
                    // 实际应用切换主题的逻辑可以在这里扩展
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
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}