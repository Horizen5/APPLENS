package com.applens.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.applens.data.HookRule
import com.applens.databinding.ItemHookRuleBinding

class HookRuleAdapter(
    private val onApply: (HookRule) -> Unit,
    private val onRevert: (HookRule) -> Unit,
    private val onRemove: (HookRule) -> Unit
) : ListAdapter<HookRule, HookRuleAdapter.VH>(DIFF) {

    companion object {
        val DIFF = object : DiffUtil.ItemCallback<HookRule>() {
            override fun areItemsTheSame(o: HookRule, n: HookRule) =
                o.packageName == n.packageName && o.activityFullName == n.activityFullName
            override fun areContentsTheSame(o: HookRule, n: HookRule) = o == n
        }
    }

    inner class VH(val binding: ItemHookRuleBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val b = ItemHookRuleBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(b)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val rule = getItem(position)
        with(holder.binding) {
            tvActivity.text = rule.activityFullName
            tvPackage.text = rule.packageName
            tvAction.text = when (rule.action) {
                HookRule.Action.DISABLE -> "禁用"
                HookRule.Action.ENABLE -> "启用"
                HookRule.Action.BLOCK -> "采样期阻止"
                HookRule.Action.HOOK_TAG -> "待 Hook 标记"
            }
            btnApply.setOnClickListener { onApply(rule) }
            btnRevert.setOnClickListener { onRevert(rule) }
            btnRemove.setOnClickListener { onRemove(rule) }
        }
    }
}
