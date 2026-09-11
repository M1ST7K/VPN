package com.v2ray.ang.ui

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.v2ray.ang.R
import com.v2ray.ang.core.CoreServiceManager
import com.v2ray.ang.databinding.ActivityHotfoxServerDetailsBinding
import com.v2ray.ang.handler.MmkvManager
import com.v2ray.ang.vpn.HotfoxLatencyDisplay
import com.v2ray.ang.vpn.HotfoxRoutingStore
import com.v2ray.ang.vpn.HotfoxServerPresentation
import com.v2ray.ang.vpn.HotfoxServerSelection
import com.v2ray.ang.vpn.HotfoxShadowStore
import com.v2ray.ang.vpn.ServerAvailability

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
        HotfoxChrome.bindBack(this)
        HotfoxChrome.bindBottomNav(this, HotfoxChrome.SERVERS)
        guid = intent.getStringExtra(EXTRA_GUID).orEmpty()
        binding.rowDetailsRouting.setOnClickListener {
            startActivity(Intent(this, HotfoxRoutingPrivacyActivity::class.java))
        }
        binding.rowDetailsShadow.setOnClickListener {
            startActivity(Intent(this, HotfoxShadowActivity::class.java))
        }
        binding.rowDetailsAuto.setOnClickListener {
            HotfoxServerSelection.selectAuto()
            render()
        }
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

    override fun onResume() {
        super.onResume()
        render()
    }

    private fun render() {
        val profile = guid.takeIf { it.isNotBlank() }?.let(MmkvManager::decodeServerConfig)
        val presentation = HotfoxServerPresentation.fromRemark(profile?.remarks)
        binding.tvDetailsTitle.text = presentation.country ?: presentation.title
        binding.tvDetailsCountry.text = when {
            presentation.country != null && presentation.title != presentation.country -> presentation.title
            else -> getString(R.string.hotfox_server_unavailable_metric)
        }
        val aff = guid.takeIf { it.isNotBlank() }?.let(MmkvManager::decodeServerAffiliationInfo)
        val health = guid.takeIf { it.isNotBlank() }?.let { HotfoxServerSelection.health.snapshot(it) }
        binding.tvDetailsLatency.text = HotfoxLatencyDisplay.format(health = health, delayMs = aff?.testDelayMillis)
        binding.tvDetailsMode.text = HotfoxRoutingStore.load().uiLabel()
        binding.tvDetailsNote.text = getString(R.string.hotfox_server_manual_note)
        binding.tvDetailsLoad.setText(R.string.hotfox_server_unavailable_metric)
        binding.tvDetailsLoad.setTextColor(ContextCompat.getColor(this, R.color.hotfox_cream_muted))
        val statusRes = when (health?.availability) {
            ServerAvailability.HEALTHY,
            ServerAvailability.DEGRADED -> R.string.hotfox_server_available
            ServerAvailability.DEAD -> R.string.hotfox_server_unavailable
            else -> R.string.hotfox_server_unavailable_metric
        }
        binding.tvDetailsStatus.setText(statusRes)
        binding.tvDetailsStatus.setTextColor(
            ContextCompat.getColor(
                this,
                if (statusRes == R.string.hotfox_server_available) R.color.hotfox_success else R.color.hotfox_cream_muted,
            ),
        )
        binding.tvDetailsShadow.setText(
            if (HotfoxShadowStore.isShadowAuto()) R.string.hotfox_shadow_mode_auto else R.string.hotfox_shadow_mode_off,
        )
        val auto = HotfoxServerSelection.isAutoMode()
        binding.tvDetailsAuto.setText(
            if (auto) R.string.hotfox_server_autoselect_on else R.string.hotfox_server_autoselect_off,
        )
        binding.tvDetailsAuto.setTextColor(
            ContextCompat.getColor(
                this,
                if (auto) R.color.hotfox_success else R.color.hotfox_cream_muted,
            ),
        )
        binding.btnDetailsSelect.isEnabled = guid.isNotBlank()
    }
}
