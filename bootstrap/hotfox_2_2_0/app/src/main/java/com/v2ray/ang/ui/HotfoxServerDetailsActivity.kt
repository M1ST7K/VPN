package com.v2ray.ang.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.v2ray.ang.R
import com.v2ray.ang.core.CoreServiceManager
import com.v2ray.ang.databinding.ActivityHotfoxServerDetailsBinding
import com.v2ray.ang.handler.MmkvManager
import com.v2ray.ang.vpn.HotfoxLatencyDisplay
import com.v2ray.ang.vpn.HotfoxServerPresentation
import com.v2ray.ang.vpn.HotfoxServerSelection

class HotfoxServerDetailsActivity : AppCompatActivity() {
    companion object {
        const val EXTRA_GUID = "hotfox_server_guid"
    }

    private lateinit var binding: ActivityHotfoxServerDetailsBinding
    private var guid: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHotfoxServerDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        guid = intent.getStringExtra(EXTRA_GUID).orEmpty()
        render()
        binding.btnDetailsSelect.setOnClickListener {
            if (guid.isNotBlank()) {
                HotfoxServerSelection.selectManual(guid)
                if (CoreServiceManager.isRunning()) {
                    CoreServiceManager.restartForRouting(this)
                }
                finish()
            }
        }
    }

    private fun render() {
        val profile = guid.takeIf { it.isNotBlank() }?.let(MmkvManager::decodeServerConfig)
        val presentation = HotfoxServerPresentation.fromRemark(profile?.remarks)
        binding.tvDetailsTitle.text = presentation.title
        binding.tvDetailsCountry.text = presentation.country ?: getString(R.string.hotfox_server_unavailable_metric)
        val aff = guid.takeIf { it.isNotBlank() }?.let(MmkvManager::decodeServerAffiliationInfo)
        val health = guid.takeIf { it.isNotBlank() }?.let { HotfoxServerSelection.health.snapshot(it) }
        binding.tvDetailsLatency.text = HotfoxLatencyDisplay.format(health = health, delayMs = aff?.testDelayMillis)
        binding.tvDetailsMode.text = if (HotfoxServerSelection.isAutoMode()) {
            getString(R.string.hotfox_auto_server)
        } else {
            getString(R.string.hotfox_selected_server_label)
        }
        binding.tvDetailsNote.text = getString(R.string.hotfox_server_unavailable_metric)
        binding.btnDetailsSelect.isEnabled = guid.isNotBlank()
    }
}
