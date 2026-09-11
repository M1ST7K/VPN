package com.v2ray.ang.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.v2ray.ang.R
import com.v2ray.ang.databinding.ActivityHotfoxShadowBinding
import com.v2ray.ang.vpn.HotfoxShadowStore

class HotfoxShadowActivity : AppCompatActivity() {
    private lateinit var binding: ActivityHotfoxShadowBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        HotfoxSystemUi.applyDarkEditorialBars(this)
        binding = ActivityHotfoxShadowBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.root.findViewById<android.view.View>(R.id.btn_header_back)?.apply {
            visibility = android.view.View.VISIBLE
            setOnClickListener { finish() }
        }
        render()
        binding.btnShadowKeepAuto.setOnClickListener {
            HotfoxShadowStore.setShadowAuto(true)
            render()
            finish()
        }
    }

    private fun render() {
        val auto = HotfoxShadowStore.isShadowAuto()
        binding.tvShadowMode.setText(if (auto) R.string.hotfox_onboarding_use_auto else R.string.hotfox_onboarding_choose_manual)
        binding.tvShadowStatus.setText(if (auto) R.string.hotfox_value_on else R.string.hotfox_value_off)
    }
}
