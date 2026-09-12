package com.v2ray.ang.ui

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.v2ray.ang.R
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
        const val EXTRA_GUID = "guid"
    }

    private lateinit var binding: ActivityHotfoxServerDetailsBinding
    private var guid: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        HotfoxSystemUi.applyDarkEditorialBars(this)
        guid = intent.getStringExtra(EXTRA_GUID).orEmpty()
        if (guid.isBlank() || guid == HotfoxServerSelection.AUTO_GUID) {
            finish()
            return
        }
        binding = ActivityHotfoxServerDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        HotfoxSystemUi.hideScrollbars(binding.root)
        HotfoxSystemUi.constrainReadingWidth(binding.root)
        binding.root.findViewById<View>(R.id.btn_header_back)?.apply {
            visibility = View.VISIBLE
            setOnClickListener { finish() }
        }
        HotfoxNavBinder.bind(
            binding.root,
            HotfoxNavBinder.Destination.SERVERS,
            onHome = { HotfoxNavBinder.openMain(this, MainActivity.SECTION_CONNECTION) },
            onServers = { finish() },
            onSubscription = { HotfoxNavBinder.openMain(this, MainActivity.SECTION_SUBSCRIPTION) },
            onSettings = { startActivity(Intent(this, HotfoxSettingsActivity::class.java)) },
        )
        binding.rowServerRouting.setOnClickListener {
            startActivity(Intent(this, HotfoxRoutingPrivacyActivity::class.java))
        }
        binding.rowServerShadow.setOnClickListener {
            startActivity(Intent(this, HotfoxShadowActivity::class.java))
        }
        binding.btnSelectServer.setOnClickListener {
            setResult(Activity.RESULT_OK, Intent().putExtra(EXTRA_GUID, guid))
            finish()
        }
        render()
    }

    override fun onResume() {
        super.onResume()
        if (::binding.isInitialized) render()
    }

    private fun render() {
        val profile = MmkvManager.decodeServerConfig(guid)
        if (profile == null) {
            finish()
            return
        }
        val presentation = HotfoxServerPresentation.fromRemark(profile.remarks)
        val flag = when (presentation.country) {
            "Нидерланды" -> R.drawable.hf_flag_nl
            "Германия" -> R.drawable.hf_flag_de
            "Франция" -> R.drawable.hf_flag_fr
            "Великобритания" -> R.drawable.hf_flag_gb
            "США" -> R.drawable.hf_flag_us
            "Канада" -> R.drawable.hf_flag_ca
            else -> 0
        }
        val aff = MmkvManager.decodeServerAffiliationInfo(guid)
        val delay = aff?.testDelayMillis ?: 0L
        val health = HotfoxServerSelection.health.snapshot(guid)
        displayServer(
            title = presentation.title,
            country = presentation.country ?: getString(R.string.hotfox_unknown),
            flagRes = flag,
            statusLabel = HotfoxLatencyDisplay.format(health = health, delayMs = delay),
            loadLabel = getString(R.string.hotfox_unknown),
            selectEnabled = true,
        )
        binding.imgServerHealth.setImageResource(
            when (health.availability) {
                ServerAvailability.HEALTHY -> R.drawable.hf_signal_green
                ServerAvailability.DEGRADED -> R.drawable.hf_signal_cream
                ServerAvailability.DEAD -> R.drawable.hf_signal_cream
                ServerAvailability.UNKNOWN -> R.drawable.hf_signal_cream
            },
        )
        binding.tvRowRouting.text = HotfoxRoutingStore.load().uiLabel()
        binding.tvRowShadow.setText(
            if (HotfoxShadowStore.isShadowAuto()) R.string.hotfox_onboarding_use_auto else R.string.hotfox_value_off,
        )
        binding.tvRowAuto.setText(
            if (HotfoxServerSelection.isAutoMode()) R.string.hotfox_value_on else R.string.hotfox_value_off,
        )
    }

    internal fun displayServer(
        title: String,
        country: String,
        flagRes: Int,
        statusLabel: String,
        loadLabel: String,
        selectEnabled: Boolean,
    ) {
        binding.tvServerName.text = title
        binding.tvServerCountry.text = country
        if (flagRes != 0) {
            binding.imgServerFlag.visibility = View.VISIBLE
            binding.imgServerFlag.setImageResource(flagRes)
        } else {
            binding.imgServerFlag.visibility = View.GONE
        }
        binding.tvRowStatus.text = statusLabel
        // No production load metric exists; do not invent a percentage.
        binding.tvRowLoad.text = loadLabel
        binding.btnSelectServer.isEnabled = selectEnabled
    }
}
