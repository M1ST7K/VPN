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
        binding = ActivityHotfoxShadowBinding.inflate(layoutInflater)
        setContentView(binding.root)
        HotfoxChrome.bindBack(this)
        binding.btnShadowToggle.setOnClickListener {
            HotfoxShadowStore.setShadowAuto(!HotfoxShadowStore.isShadowAuto())
            render()
        }
        binding.rowShadowMore.setOnClickListener {
            com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
                .setTitle(R.string.hotfox_shadow_more)
                .setMessage(R.string.hotfox_shadow_note)
                .setPositiveButton(android.R.string.ok, null)
                .show()
        }
        render()
    }

    private fun render() {
        val on = HotfoxShadowStore.isShadowAuto()
        binding.tvShadowStatus.setText(if (on) R.string.hotfox_shadow_status_on else R.string.hotfox_shadow_status_off)
        binding.tvShadowModeValue.setText(if (on) R.string.hotfox_shadow_mode_auto else R.string.hotfox_shadow_mode_off)
        binding.btnShadowToggle.setText(if (on) R.string.hotfox_shadow_toggle_on else R.string.hotfox_shadow_toggle_off)
    }
}
