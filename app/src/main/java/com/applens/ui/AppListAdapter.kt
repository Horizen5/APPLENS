package com.applens.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.applens.data.AppInfo
import com.applens.databinding.ItemAppBinding

class AppListAdapter(
    private val onClick: (AppInfo) -> Unit
) : ListAdapter<AppInfo, AppListAdapter.VH>(DIFF) {

    companion object {
        val DIFF = object : DiffUtil.ItemCallback<AppInfo>() {
            override fun areItemsTheSame(o: AppInfo, n: AppInfo) = o.packageName == n.packageName
            override fun areContentsTheSame(o: AppInfo, n: AppInfo) = o == n
        }
    }

    inner class VH(val binding: ItemAppBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val b = ItemAppBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(b)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val app = getItem(position)
        with(holder.binding) {
            ivIcon.setImageDrawable(app.icon)
            tvLabel.text = app.label
            tvPackage.text = app.packageName
            chipSystem.visibility = if (app.isSystem) android.view.View.VISIBLE else android.view.View.GONE
            root.setOnClickListener { onClick(app) }
        }
    }
}
