package com.applens.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.applens.databinding.FragmentHomeBinding

class AboutFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // 复用首页布局但只显示声明部分，或者简单返回一个 TextView
        val tv = android.widget.TextView(requireContext()).apply {
            text = "APPLENS\n\nActivity 分析工具\n\n版本 1.0.5\n\n本工具仅供学习研究使用"
            textSize = 16f
            setTextColor(android.graphics.Color.parseColor("#666666"))
            gravity = android.view.Gravity.CENTER
            setPadding(48, 48, 48, 48)
        }
        return tv
    }
}