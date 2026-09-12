package com.v2ray.ang.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.v2ray.ang.R
import com.v2ray.ang.databinding.ActivityHotfoxServerDetailsBinding
import com.v2ray.ang.vpn.HotfoxRoutingStore
import com.v2ray.ang.vpn.HotfoxServerSelection
import com.v2ray.ang.vpn.HotfoxShadowStore

/**
 * Debug-only visual of server details. Does not write server/subscription stores.
 */
class HotfoxUiQaServerDetailsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        HotfoxSystemUi.applyDarkEditorialBars(this)
        val binding = ActivityHotfoxServerDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        HotfoxSystemUi.hideScrollbars(binding.root)
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
        binding.tvServerName.text = "Амстердам"
        binding.tvServerCountry.text = "Нидерланды"
        binding.imgServerFlag.visibility = View.VISIBLE
        binding.imgServerFlag.setImageResource(R.drawable.hf_flag_nl)
        binding.tvRowStatus.text = "18 ms"
        binding.tvRowLoad.text = "12%"
        binding.tvRowRouting.text = HotfoxRoutingStore.load().uiLabel()
        binding.tvRowShadow.setText(
            if (HotfoxShadowStore.isShadowAuto()) R.string.hotfox_onboarding_use_auto else R.string.hotfox_value_off,
        )
        binding.tvRowAuto.setText(
            if (HotfoxServerSelection.isAutoMode()) R.string.hotfox_value_on else R.string.hotfox_value_off,
        )
        binding.btnSelectServer.isEnabled = false
    }
}
