package com.v2ray.ang.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.v2ray.ang.databinding.ItemRecyclerBypassListBinding
import com.v2ray.ang.dto.AppInfo

/**
 * Debug-only app rows. Checkboxes follow in-memory presentation data and
 * do not write the production per-app proxy store.
 */
internal class HotfoxUiQaAppAdapter(
    private val apps: List<AppInfo>
) : RecyclerView.Adapter<HotfoxUiQaAppAdapter.Holder>() {

    class Holder(val binding: ItemRecyclerBypassListBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        return Holder(ItemRecyclerBypassListBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val app = apps[position]
        holder.binding.icon.setImageDrawable(app.appIcon)
        holder.binding.name.text = app.appName
        holder.binding.packageName.text = app.packageName
        holder.binding.checkBox.isChecked = app.isSelected == 1
        holder.itemView.setOnClickListener(null)
    }

    override fun getItemCount(): Int = apps.size
}
