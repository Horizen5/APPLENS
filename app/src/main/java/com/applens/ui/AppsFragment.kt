package com.applens.ui

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.applens.R
import com.applens.data.AppInfo
import com.applens.data.AppRepository
import com.applens.databinding.FragmentAppsBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AppsFragment : Fragment() {

    private var _binding: FragmentAppsBinding? = null
    private val binding get() = _binding!!
    private val adapter = AppListAdapter { selected -> onAppClick(selected) }

    companion object {
        // 静态缓存：只加载一次，后续打开复用
        private var cachedApps: List<AppInfo>? = null
        private var cachedAt: Long = 0
        private const val CACHE_TTL_MS = 5 * 60 * 1000L // 5 分钟
    }

    private var allApps: List<AppInfo> = emptyList()
    private var filterMode = FilterMode.ALL

    enum class FilterMode { USER, SYSTEM, ALL }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAppsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.recycler.layoutManager = LinearLayoutManager(requireContext())
        binding.recycler.adapter = adapter

        binding.swipe.setOnRefreshListener {
            cachedApps = null // 手动刷新时清除缓存
            loadApps()
        }

        binding.chipGroup.setOnCheckedStateChangeListener { group, _ ->
            filterMode = when (group.checkedChipId) {
                R.id.chip_user -> FilterMode.USER
                R.id.chip_system -> FilterMode.SYSTEM
                R.id.chip_all -> FilterMode.ALL
                else -> FilterMode.ALL
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

    private fun loadApps() {
        val now = System.currentTimeMillis()
        cachedApps?.let { cached ->
            if (now - cachedAt < CACHE_TTL_MS) {
                allApps = cached
                applyFilter()
                return
            }
        }

        binding.swipe.isRefreshing = true
        lifecycleScope.launch {
            val list = withContext(Dispatchers.IO) {
                AppRepository.listAll(requireContext().packageManager)
            }
            allApps = list
            cachedApps = list
            cachedAt = System.currentTimeMillis()
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
        val intent = Intent(requireContext(), SamplingActivity::class.java).apply {
            putExtra(SamplingActivity.EXTRA_PACKAGE, app.packageName)
            putExtra(SamplingActivity.EXTRA_LABEL, app.label)
        }
        startActivity(intent)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}