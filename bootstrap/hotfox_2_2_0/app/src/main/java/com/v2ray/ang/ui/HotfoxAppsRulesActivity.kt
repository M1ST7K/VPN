package com.v2ray.ang.ui

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.v2ray.ang.R
import com.v2ray.ang.core.CoreServiceManager
import com.v2ray.ang.databinding.ActivityHotfoxAppsRulesBinding
import com.v2ray.ang.databinding.ItemRecyclerBypassListBinding
import com.v2ray.ang.dto.AppInfo
import com.v2ray.ang.util.AppManagerUtil
import com.v2ray.ang.viewmodel.PerAppProxyViewModel
import com.v2ray.ang.vpn.HotfoxRoutingMode
import com.v2ray.ang.vpn.HotfoxRoutingStore
import kotlinx.coroutines.launch

class HotfoxAppsRulesActivity : AppCompatActivity() {
    private lateinit var binding: ActivityHotfoxAppsRulesBinding
    private val proxyVm: PerAppProxyViewModel by viewModels()
    private var appsAll: List<AppInfo> = emptyList()
    private val adapter = InstalledAppAdapter()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHotfoxAppsRulesBinding.inflate(layoutInflater)
        setContentView(binding.root)
        HotfoxChrome.bindBack(this)
        binding.recyclerApps.layoutManager = LinearLayoutManager(this)
        binding.recyclerApps.adapter = adapter
        binding.chipAppsInclude.setOnClickListener { setExclude(false) }
        binding.chipAppsExclude.setOnClickListener { setExclude(true) }
        binding.rowAppsCustom.setOnClickListener {
            startActivity(Intent(this, RoutingSettingActivity::class.java))
        }
        binding.btnAppsOpen.setOnClickListener {
            if (CoreServiceManager.isRunning()) {
                CoreServiceManager.restartForRouting(this)
            }
            finish()
        }
        binding.etAppsSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit
            override fun afterTextChanged(s: Editable?) {
                filter(s?.toString().orEmpty())
            }
        })
        paintChips()
        renderCounts()
        loadApps()
    }

    override fun onResume() {
        super.onResume()
        paintChips()
        renderCounts()
        adapter.notifyDataSetChanged()
    }

    private fun loadApps() {
        lifecycleScope.launch {
            val loaded = runCatching {
                AppManagerUtil.loadNetworkAppList(this@HotfoxAppsRulesActivity)
            }.getOrElse { emptyList() }
            appsAll = loaded
            filter(binding.etAppsSearch.text?.toString().orEmpty())
            renderCounts()
        }
    }

    private fun filter(query: String) {
        val key = query.trim().uppercase()
        val visible = if (key.isEmpty()) {
            appsAll
        } else {
            appsAll.filter {
                it.appName.uppercase().contains(key) || it.packageName.uppercase().contains(key)
            }
        }
        adapter.submit(visible)
        binding.tvAppsEmpty.visibility = if (visible.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun setExclude(exclude: Boolean) {
        HotfoxRoutingStore.saveMode(
            if (exclude) HotfoxRoutingMode.EXCLUDE_APPS else HotfoxRoutingMode.INCLUDE_APPS,
        )
        if (CoreServiceManager.isRunning()) {
            CoreServiceManager.restartForRouting(this)
        }
        paintChips()
    }

    private fun paintChips() {
        val exclude = HotfoxRoutingStore.load().mode == HotfoxRoutingMode.EXCLUDE_APPS
        binding.chipAppsInclude.setBackgroundResource(if (!exclude) R.drawable.hotfox_chip_on else R.drawable.hotfox_chip_off)
        binding.chipAppsExclude.setBackgroundResource(if (exclude) R.drawable.hotfox_chip_on else R.drawable.hotfox_chip_off)
        val on = ContextCompat.getColor(this, R.color.hotfox_cta_on_orange)
        val off = ContextCompat.getColor(this, R.color.hotfox_cream)
        binding.chipAppsInclude.setTextColor(if (!exclude) on else off)
        binding.chipAppsExclude.setTextColor(if (exclude) on else off)
    }

    private fun renderCounts() {
        val custom = HotfoxRoutingStore.load().rules.size
        binding.tvAppsCustomCount.text = if (custom > 0) {
            getString(R.string.hotfox_apps_custom_count, custom)
        } else {
            getString(R.string.hotfox_server_unavailable_metric)
        }
        binding.tvAppsCustomCount.setTextColor(
            ContextCompat.getColor(
                this,
                if (custom > 0) R.color.hotfox_success else R.color.hotfox_cream_muted,
            ),
        )
        val selected = proxyVm.getAll().size
        val total = appsAll.size
        binding.tvAppsSelectedCount.text = if (total > 0) {
            getString(R.string.hotfox_apps_selected_count, selected, total)
        } else {
            getString(R.string.hotfox_server_unavailable_metric)
        }
    }

    private inner class InstalledAppAdapter : RecyclerView.Adapter<InstalledAppAdapter.Holder>() {
        private var items: List<AppInfo> = emptyList()

        fun submit(next: List<AppInfo>) {
            items = next
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
            val itemBinding = ItemRecyclerBypassListBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return Holder(itemBinding)
        }

        override fun getItemCount(): Int = items.size

        override fun onBindViewHolder(holder: Holder, position: Int) {
            holder.bind(items[position])
        }

        inner class Holder(private val itemBinding: ItemRecyclerBypassListBinding) :
            RecyclerView.ViewHolder(itemBinding.root) {
            fun bind(app: AppInfo) {
                itemBinding.icon.setImageDrawable(app.appIcon)
                itemBinding.name.text = app.appName
                itemBinding.packageName.text = if (app.isSystemApp) {
                    getString(R.string.hotfox_apps_system)
                } else {
                    app.packageName
                }
                itemBinding.checkBox.isChecked = proxyVm.contains(app.packageName)
                itemBinding.root.setOnClickListener {
                    proxyVm.toggle(app.packageName)
                    itemBinding.checkBox.isChecked = proxyVm.contains(app.packageName)
                    renderCounts()
                }
            }
        }
    }
}
