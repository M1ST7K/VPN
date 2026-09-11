package com.v2ray.ang.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.v2ray.ang.R
import com.v2ray.ang.databinding.ItemRecyclerMainBinding

internal class HotfoxUiQaServerAdapter(
    private val rows: List<HotfoxUiVisualOverride.ReferenceServerRow>
) : RecyclerView.Adapter<HotfoxUiQaServerAdapter.Holder>() {

    class Holder(val binding: ItemRecyclerMainBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val binding = ItemRecyclerMainBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return Holder(binding)
    }

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val binding = holder.binding
        val context = binding.root.context
        binding.tvType.visibility = View.GONE
        binding.layoutFavorite.visibility = View.INVISIBLE
        binding.layoutMore.visibility = View.GONE
        binding.layoutShare.visibility = View.GONE
        binding.layoutEdit.visibility = View.GONE
        binding.layoutRemove.visibility = View.GONE
        binding.layoutSubscription.visibility = View.GONE
        binding.infoContainer.setOnClickListener(null)
        binding.infoContainer.setOnLongClickListener(null)
        if (position == 0) {
            binding.tvName.text = context.getString(R.string.hotfox_servers_best_auto)
            binding.tvStatistics.text = context.getString(R.string.hotfox_auto_server_hint)
            binding.ivFlag.visibility = View.VISIBLE
            binding.ivFlag.setImageResource(R.drawable.hf_globe_orange)
            binding.tvTestResult.text = "●"
            binding.tvTestResult.setTextColor(ContextCompat.getColor(context, R.color.hotfox_orange))
            binding.imgRowChevron.visibility = View.GONE
            binding.tvName.alpha = 1f
            return
        }
        val row = rows[position - 1]
        binding.tvName.text = row.title
        binding.tvStatistics.text = row.country
        binding.ivFlag.visibility = View.VISIBLE
        binding.ivFlag.setImageResource(row.flagRes)
        binding.tvTestResult.text = row.pingLabel
        binding.tvTestResult.setTextColor(ContextCompat.getColor(context, R.color.hotfox_success_bright))
        binding.imgRowChevron.visibility = View.VISIBLE
        binding.tvName.alpha = 0.88f
    }

    override fun getItemCount(): Int = rows.size + 1
}
