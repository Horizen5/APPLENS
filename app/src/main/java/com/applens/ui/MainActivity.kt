package com.applens.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.applens.R
import com.applens.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private var currentFragment: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 默认显示首页
        if (savedInstanceState == null) {
            switchFragment("home")
        }

        // 底部导航点击
        binding.bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> { switchFragment("home"); true }
                R.id.nav_apps -> { switchFragment("apps"); true }
                R.id.nav_hooks -> { switchFragment("hooks"); true }
                else -> false
            }
        }

        // 设置默认选中项
        binding.bottomNav.selectedItemId = R.id.nav_home
    }

    private fun switchFragment(tag: String) {
        if (currentFragment == tag) return

        val fragment = when (tag) {
            "home" -> HomeFragment()
            "apps" -> AppsFragment()
            "hooks" -> HooksFragment()
            else -> return
        }

        supportFragmentManager.beginTransaction()
            .replace(R.id.nav_host_fragment, fragment, tag)
            .commit()
        currentFragment = tag
    }
}