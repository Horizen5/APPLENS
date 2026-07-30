package com.applens.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.applens.data.ActivityStat
import com.applens.databinding.ItemActivityStatBinding

class ActivityStatAdapter(
    private val onClick: (ActivityStat) -> Unit
) : ListAdapter<ActivityStat, ActivityStatAdapter.VH>(DIFF) {

    companion object {
        val DIFF = object : DiffUtil.ItemCallback<ActivityStat>() {
            override fun areItemsTheSame(o: ActivityStat, n: ActivityStat) =
                o.activityFullName == n.activityFullName
            override fun areContentsTheSame(o: ActivityStat, n: ActivityStat) = o == n
        }
    }

    inner class VH(val binding: ItemActivityStatBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val b = ItemActivityStatBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(b)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val s = getItem(position)
        with(holder.binding) {
            tvName.text = s.activityFullName
            tvMeta.text = String.format(
                "出现 %d 次 · 占比 %.1f%% · CPU 平均 %.1f%% / 最大 %.1f%% · 内存 %d KB",
                s.count, s.percentOf(currentList.size), s.avgCpu, s.maxCpu, s.avgMemKb
            )
            root.setOnClickListener { onClick(s) }
        }
    }
}
