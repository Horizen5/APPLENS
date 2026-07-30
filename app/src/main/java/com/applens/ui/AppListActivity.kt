package com.applens.ui

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.applens.R
import com.applens.data.AppInfo
import com.applens.data.AppRepository
import com.applens.databinding.ActivityAppListBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AppListActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAppListBinding
    private val adapter = AppListAdapter { selected -> onAppClick(selected) }
    private var allApps: List<AppInfo> = emptyList()
    private var filterMode = FilterMode.USER

    enum class FilterMode { USER, SYSTEM, ALL }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAppListBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            title = getString(R.string.action_select_app)
        }
        binding.toolbar.navigationIcon?.setTint(ContextCompat.getColor(this, R.color.hyper_text_primary))

        binding.recycler.layoutManager = LinearLayoutManager(this)
        binding.recycler.adapter = adapter

        binding.swipe.setOnRefreshListener { loadApps() }
        binding.chipGroup.setOnCheckedStateChangeListener { group, _ ->
            filterMode = when (group.checkedChipId) {
                R.id.chip_user -> FilterMode.USER
                R.id.chip_system -> FilterMode.SYSTEM
                R.id.chip_all -> FilterMode.ALL
                else -> FilterMode.USER
            }
            applyFilter()
        }

        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) { applyFilter(s?.toString().orEmpty()) }
        })

        loadApps()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_app_list, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) { finish(); return true }
        return super.onOptionsItemSelected(item)
    }

    private fun loadApps() {
        binding.swipe.isRefreshing = true
        lifecycleScope.launch {
            val list = withContext(Dispatchers.IO) {
                AppRepository.listAll(packageManager)
            }
            allApps = list
            binding.swipe.isRefreshing = false
            applyFilter()
        }
    }

    private fun applyFilter(keyword: String = binding.etSearch.text?.toString().orEmpty()) {
        val kw = keyword.trim().lowercase()
        val filtered = allApps.filter { a ->
            val modeOk = when (filterMode) {
                FilterMode.USER -> a.isUserApp
                FilterMode.SYSTEM -> a.isSystem
                FilterMode.ALL -> true
            }
            val kwOk = kw.isEmpty() ||
                a.label.lowercase().contains(kw) ||
                a.packageName.lowercase().contains(kw)
            modeOk && kwOk
        }
        adapter.submitList(filtered)
        binding.tvEmpty.isVisible = filtered.isEmpty()
    }

    private fun onAppClick(app: AppInfo) {
        val intent = Intent(this, SamplingActivity::class.java).apply {
            putExtra(SamplingActivity.EXTRA_PACKAGE, app.packageName)
            putExtra(SamplingActivity.EXTRA_LABEL, app.label)
        }
        startActivity(intent)
    }
}
