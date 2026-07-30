package com.applens.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
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

        binding.itemTheme.setOnClickListener {
            Toast.makeText(requireContext(), "主题设置开发中", Toast.LENGTH_SHORT).show()
        }

        binding.itemAbout.setOnClickListener {
            // 跳转到关于页面（可复用 AboutFragment）
            // 这里简单起见，使用 AlertDialog 或直接 replace
            val aboutFragment = AboutFragment()
            parentFragmentManager.beginTransaction()
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