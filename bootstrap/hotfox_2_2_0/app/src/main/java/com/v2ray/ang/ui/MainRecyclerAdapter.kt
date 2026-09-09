package com.v2ray.ang.ui

import android.annotation.SuppressLint
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.v2ray.ang.R
import com.v2ray.ang.contracts.MainAdapterListener
import com.v2ray.ang.databinding.ItemRecyclerFooterBinding
import com.v2ray.ang.databinding.ItemRecyclerMainBinding
import com.v2ray.ang.dto.entities.ProfileItem
import com.v2ray.ang.dto.entities.ServersCache
import com.v2ray.ang.extension.isComplexType
import com.v2ray.ang.handler.AngConfigManager
import com.v2ray.ang.handler.MmkvManager
import com.v2ray.ang.helper.ItemTouchHelperAdapter
import com.v2ray.ang.helper.ItemTouchHelperViewHolder
import com.v2ray.ang.viewmodel.MainViewModel
import com.v2ray.ang.vpn.HotfoxLatencyDisplay
import com.v2ray.ang.vpn.HotfoxServerListContract
import com.v2ray.ang.vpn.HotfoxServerPresentation
import com.v2ray.ang.vpn.HotfoxServerSelection
import com.v2ray.ang.vpn.ServerAvailability
import java.util.Collections

class MainRecyclerAdapter(
    private val mainViewModel: MainViewModel,
    private val adapterListener: MainAdapterListener?
) : RecyclerView.Adapter<MainRecyclerAdapter.BaseViewHolder>(), ItemTouchHelperAdapter {
    private var data: MutableList<ServersCache> = mutableListOf()

    @SuppressLint("NotifyDataSetChanged")
    fun setData(newData: MutableList<ServersCache>?, position: Int = -1) {
        data = newData?.toMutableList() ?: mutableListOf()

        if (position >= 0 && position in data.indices) {
            notifyItemChanged(position + 1)
        } else {
            notifyDataSetChanged()
        }
    }

    override fun getItemCount() = HotfoxServerListContract.itemCount(data.size)

    override fun onBindViewHolder(holder: BaseViewHolder, position: Int) {
        if (holder !is MainViewHolder) return
        val context = holder.itemMainBinding.root.context
        if (position == HotfoxServerListContract.AUTO_ROW_INDEX) {
            bindAutoRow(holder)
            return
        }
        val dataIndex = position - 1
        if (dataIndex !in data.indices) return
        val guid = data[dataIndex].guid
        val profile = data[dataIndex].profile
        val presentation = HotfoxServerPresentation.fromRemark(profile.remarks)
        holder.itemMainBinding.tvName.text = presentation.title
        holder.itemMainBinding.tvStatistics.text = presentation.country
            ?: profile.description.takeIf { !it.isNullOrBlank() }
            ?: AngConfigManager.generateDescription(profile)
        holder.itemMainBinding.tvType.visibility = View.GONE

        val aff = MmkvManager.decodeServerAffiliationInfo(guid)
        val delay = aff?.testDelayMillis ?: 0L
        val health = HotfoxServerSelection.health.snapshot(guid)
        holder.itemMainBinding.tvTestResult.text = HotfoxLatencyDisplay.format(health = health, delayMs = delay)
        holder.itemMainBinding.ivFavorite.alpha = if (aff?.favorite == true) 1f else 0.34f
        holder.itemMainBinding.layoutFavorite.visibility = View.VISIBLE
        holder.itemMainBinding.ivFavorite.setColorFilter(
            ContextCompat.getColor(
                context,
                if (aff?.favorite == true) R.color.hotfox_orange else R.color.hotfox_editorial_text_secondary,
            )
        )
        holder.itemMainBinding.layoutFavorite.setOnClickListener {
            val newValue = !MmkvManager.isServerFavorite(guid)
            MmkvManager.setServerFavorite(guid, newValue)
            holder.itemMainBinding.ivFavorite.alpha = if (newValue) 1f else 0.34f
            holder.itemMainBinding.ivFavorite.setColorFilter(
                ContextCompat.getColor(
                    context,
                    if (newValue) R.color.hotfox_orange else R.color.hotfox_editorial_text_secondary,
                )
            )
        }
        holder.itemMainBinding.tvTestResult.setTextColor(
            ContextCompat.getColor(
                context,
                when {
                    health.availability == ServerAvailability.DEAD || delay < 0L -> R.color.colorPingRed
                    health.probeInFlight -> R.color.hotfox_editorial_text_dim
                    delay in 1L..80L -> R.color.hotfox_success_bright
                    else -> R.color.hotfox_editorial_text_dim
                },
            )
        )

        val isSelected = !HotfoxServerSelection.isAutoMode() && guid == MmkvManager.getSelectServer()
        holder.itemMainBinding.tvName.setTextColor(
            ContextCompat.getColor(
                context,
                if (isSelected) R.color.hotfox_editorial_text else R.color.hotfox_editorial_text,
            )
        )
        holder.itemMainBinding.tvName.alpha = if (isSelected) 1f else 0.88f

        holder.itemMainBinding.layoutSubscription.visibility = View.GONE
        holder.itemMainBinding.layoutShare.visibility = View.GONE
        holder.itemMainBinding.layoutEdit.visibility = View.GONE
        holder.itemMainBinding.layoutRemove.visibility = View.GONE
        holder.itemMainBinding.layoutMore.visibility = View.GONE
        holder.itemMainBinding.layoutMore.setOnClickListener(null)

        holder.itemMainBinding.infoContainer.setOnClickListener {
            adapterListener?.onSelectServer(guid)
        }
        holder.itemMainBinding.infoContainer.setOnLongClickListener {
            adapterListener?.onShare(guid, profile, dataIndex, true)
            true
        }
    }

    private fun bindAutoRow(holder: MainViewHolder) {
        val context = holder.itemMainBinding.root.context
        val auto = HotfoxServerSelection.isAutoMode()
        holder.itemMainBinding.tvName.text = context.getString(R.string.hotfox_auto_server)
        holder.itemMainBinding.tvStatistics.text = context.getString(R.string.hotfox_auto_server_hint)
        holder.itemMainBinding.tvTestResult.text = if (auto) "●" else ""
        holder.itemMainBinding.tvTestResult.setTextColor(ContextCompat.getColor(context, R.color.hotfox_orange))
        holder.itemMainBinding.tvType.visibility = View.GONE
        holder.itemMainBinding.layoutFavorite.visibility = View.INVISIBLE
        holder.itemMainBinding.layoutMore.visibility = View.GONE
        holder.itemMainBinding.tvName.alpha = if (auto) 1f else 0.72f
        holder.itemMainBinding.infoContainer.setOnClickListener {
            adapterListener?.onSelectServer(HotfoxServerSelection.AUTO_GUID)
        }
        holder.itemMainBinding.infoContainer.setOnLongClickListener(null)
    }

    fun removeServerSub(guid: String, position: Int) {
        val idx = data.indexOfFirst { it.guid == guid }
        if (idx >= 0) {
            data.removeAt(idx)
            notifyItemRemoved(idx + 1)
            notifyItemRangeChanged(idx + 1, data.size - idx)
        }
    }

    fun setSelectServer(fromPosition: Int, toPosition: Int) {
        notifyItemChanged(0)
        if (fromPosition in data.indices) notifyItemChanged(fromPosition + 1)
        if (toPosition in data.indices && toPosition != fromPosition) notifyItemChanged(toPosition + 1)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder {
        return when (viewType) {
            HotfoxServerListContract.VIEW_TYPE_ITEM, HotfoxServerListContract.VIEW_TYPE_AUTO ->
                MainViewHolder(ItemRecyclerMainBinding.inflate(LayoutInflater.from(parent.context), parent, false))
            else ->
                FooterViewHolder(ItemRecyclerFooterBinding.inflate(LayoutInflater.from(parent.context), parent, false))
        }
    }

    override fun getItemViewType(position: Int): Int {
        return HotfoxServerListContract.viewType(position, data.size)
    }

    open class BaseViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        fun onItemSelected() {
            itemView.setBackgroundColor(Color.TRANSPARENT)
        }

        fun onItemClear() {
            itemView.setBackgroundColor(0)
        }
    }

    class MainViewHolder(val itemMainBinding: ItemRecyclerMainBinding) :
        BaseViewHolder(itemMainBinding.root), ItemTouchHelperViewHolder

    class FooterViewHolder(val itemFooterBinding: ItemRecyclerFooterBinding) :
        BaseViewHolder(itemFooterBinding.root)

    override fun onItemMove(fromPosition: Int, toPosition: Int): Boolean {
        val from = fromPosition - 1
        val to = toPosition - 1
        if (from !in data.indices || to !in data.indices) return false
        mainViewModel.swapServer(from, to)
        Collections.swap(data, from, to)
        notifyItemMoved(fromPosition, toPosition)
        return true
    }

    override fun onItemMoveCompleted() {
    }

    override fun onItemDismiss(position: Int) {
    }
}
