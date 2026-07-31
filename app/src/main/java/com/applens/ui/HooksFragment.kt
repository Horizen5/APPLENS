package com.applens.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.applens.core.ControllerManager
import com.applens.data.HookRule
import com.applens.data.HookRuleRepository
import com.applens.databinding.FragmentHooksBinding
import com.applens.utils.HookExecutor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class HooksFragment : Fragment() {

    private var _binding: FragmentHooksBinding? = null
    private val binding get() = _binding!!
    private val adapter = HookRuleAdapter(
        onApply = { rule -> execRule(rule, isRevert = false) },
        onRevert = { rule -> execRule(rule, isRevert = true) },
        onRemove = { rule ->
            HookRuleRepository.remove(requireContext(), rule.packageName, rule.activityFullName)
            loadRules()
        }
    )

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHooksBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.recycler.layoutManager = LinearLayoutManager(requireContext())
        binding.recycler.adapter = adapter

        // 批量执行按钮
        binding.btnBatchApply.setOnClickListener {
            execBatch()
        }

        loadRules()
    }

    override fun onResume() {
        super.onResume()
        loadRules()
    }

    private fun loadRules() {
        lifecycleScope.launch {
            val rules = withContext(Dispatchers.IO) {
                HookRuleRepository.listAll(requireContext())
            }
            adapter.submitList(rules)
            binding.tvEmpty.isVisible = rules.isEmpty()
            binding.btnBatchApply.isVisible = rules.isNotEmpty()
        }
    }

    private fun execRule(rule: HookRule, isRevert: Boolean) {
        val ctx = requireContext()
        Thread {
            val r = if (isRevert) HookExecutor.revert(ctx, rule) else HookExecutor.apply(ctx, rule)
            activity?.runOnUiThread {
                AlertDialog.Builder(requireContext())
                    .setTitle(if (isRevert) "撤销结果" else "执行结果")
                    .setMessage(r.message)
                    .setPositiveButton("好的", null)
                    .show()
            }
        }.start()
    }

    private fun execBatch() {
        val ctx = requireContext()
        lifecycleScope.launch {
            val rules = withContext(Dispatchers.IO) {
                HookRuleRepository.listAll(ctx)
            }
            val results = withContext(Dispatchers.IO) {
                HookExecutor.batchApply(ctx, rules)
            }
            val successCount = results.count { it.success }
            val failCount = results.size - successCount
            AlertDialog.Builder(ctx)
                .setTitle("批量执行结果")
                .setMessage("成功: $successCount  失败: $failCount\n\n${results.joinToString("\n") { it.message }}")
                .setPositiveButton("好的", null)
                .show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
