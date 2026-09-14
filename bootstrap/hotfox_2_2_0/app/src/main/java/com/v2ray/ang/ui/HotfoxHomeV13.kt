package com.v2ray.ang.ui

import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.v2ray.ang.R
import com.v2ray.ang.commerce.CommercialPresentationState
import com.v2ray.ang.vpn.HotfoxMotion
import com.v2ray.ang.vpn.HotfoxShadowStore

/**
 * V13 Home chrome: one hero scene, status chip, CTA styles, Home-active nav.
 * Does not change VPN/session truth.
 */
object HotfoxHomeV13 {
    fun isPro(state: CommercialPresentationState): Boolean = when (state) {
        CommercialPresentationState.HOTFOX_ACTIVE,
        CommercialPresentationState.EXTERNAL_ACTIVE,
        CommercialPresentationState.EXPIRING_SOON,
        CommercialPresentationState.ENTITLEMENT_ACTIVE_SYNC_FAILED,
        -> true
        else -> false
    }

    internal fun apply(
        activity: MainActivity,
        visual: MainActivity.ConnectionVisualState,
        onHome: Boolean,
    ) {
        val root = activity.findViewById<View>(android.R.id.content) ?: activity.window.decorView
        val host = activity.findViewById<View>(R.id.drawer_layout) ?: root
        host.findViewById<View>(R.id.hf_header)?.isVisible = !onHome
        host.findViewById<HotFoxHeroArtwork>(R.id.home_hero)?.apply {
            isVisible = false
            setHeroLayers(showPlanet = false, showFox = false)
        }
        host.findViewById<ImageView>(R.id.home_planet_backdrop)?.isVisible = onHome
        host.findViewById<View>(R.id.img_art_ring)?.isVisible = false

        val hint = host.findViewById<View>(R.id.layout_disconnected_hint)
        val stages = host.findViewById<View>(R.id.layout_connecting_stages)
        val metrics = host.findViewById<View>(R.id.layout_protected_metrics)
        val note = host.findViewById<View>(R.id.layout_connection_note)
        val stageLine = host.findViewById<TextView>(R.id.tv_connection_stage)
        val chip = host.findViewById<TextView>(R.id.tv_home_status_chip)
        val title = host.findViewById<TextView>(R.id.tv_connect_label)
        val subtitle = host.findViewById<TextView>(R.id.tv_vpn_status)
        val cta = host.findViewById<TextView>(R.id.connect_action)
        val orange = ContextCompat.getColor(activity, R.color.hf_v13_orange)
        val muted = ContextCompat.getColor(activity, R.color.hf_v13_muted)
        val text = ContextCompat.getColor(activity, R.color.hf_v13_text)
        val green = ContextCompat.getColor(activity, R.color.hf_v13_green)

        fun android.widget.TextView.startIcon(res: Int, tint: Int, dp: Int = 18) {
            val d = ContextCompat.getDrawable(context, res)?.mutate() ?: return
            d.setTint(tint)
            val px = (dp * resources.displayMetrics.density).toInt().coerceAtLeast(1)
            d.setBounds(0, 0, px, px)
            setCompoundDrawablesRelative(d, null, null, null)
            compoundDrawablePadding = (8 * resources.displayMetrics.density).toInt()
        }

        when (visual) {
            MainActivity.ConnectionVisualState.CONNECTING -> {
                hint?.isVisible = false
                stages?.isVisible = true
                metrics?.isVisible = false
                note?.visibility = View.GONE
                stageLine?.isVisible = true
                if (stageLine?.text.isNullOrBlank()) {
                    stageLine?.setText(R.string.hotfox_home_checking)
                }
                chip?.setText(R.string.hotfox_home_status_connecting)
                chip?.setTextColor(orange)
                chip?.setBackgroundResource(R.drawable.hf_v13_chip_idle)
                chip?.startIcon(R.drawable.ic_hotfox_dots, orange, 14)
                title?.setText(R.string.hotfox_home_title_connecting)
                title?.setTextColor(text)
                subtitle?.setText(R.string.hotfox_home_subtitle_connecting)
                subtitle?.setTextColor(muted)
                subtitle?.setCompoundDrawablesRelative(null, null, null, null)
                cta?.setBackgroundResource(R.drawable.hf_v13_cancel)
                cta?.setTextColor(orange)
                cta?.setText(R.string.hotfox_home_cancel)
                cta?.startIcon(R.drawable.ic_hotfox_stop, orange, 18)
                host.findViewById<LinearProgressIndicator>(R.id.home_connecting_progress)?.let { bar ->
                    bar.post {
                        val reduced = HotfoxMotion.reducedMotion(activity)
                        try {
                            if (reduced) {
                                if (bar.isIndeterminate) {
                                    bar.isVisible = false
                                    bar.isIndeterminate = false
                                    bar.isVisible = true
                                }
                                bar.setProgressCompat(42, false)
                            } else if (!bar.isIndeterminate) {
                                bar.isVisible = false
                                bar.isIndeterminate = true
                                bar.isVisible = true
                            }
                        } catch (_: RuntimeException) {
                            // Keep the XML indicator mode rather than crashing Home chrome.
                        }
                    }
                }
            }
            MainActivity.ConnectionVisualState.CONNECTED -> {
                hint?.isVisible = false
                stages?.isVisible = false
                metrics?.isVisible = true
                note?.visibility = View.GONE
                stageLine?.isVisible = false
                chip?.setText(R.string.hotfox_home_status_on)
                chip?.setTextColor(green)
                chip?.setBackgroundResource(R.drawable.hf_v13_chip_on)
                chip?.startIcon(R.drawable.ic_hotfox_shield_check, green, 14)
                title?.setText(R.string.hotfox_home_title_connected)
                title?.setTextColor(text)
                if (subtitle?.text.isNullOrBlank() || subtitle?.text == activity.getString(R.string.hotfox_disconnected_body) ||
                    subtitle?.text == activity.getString(R.string.hotfox_home_subtitle_disconnected) ||
                    subtitle?.text == activity.getString(R.string.hotfox_connecting_body) ||
                    subtitle?.text == activity.getString(R.string.hotfox_home_subtitle_connecting)
                ) {
                    subtitle?.text = activity.getString(R.string.hotfox_home_subtitle_online, "00:00:00")
                }
                subtitle?.setTextColor(muted)
                subtitle?.startIcon(R.drawable.ic_hotfox_timer, muted, 14)
                cta?.setBackgroundResource(R.drawable.hf_v13_disconnect)
                cta?.setTextColor(text)
                cta?.setText(R.string.hotfox_disconnect)
                cta?.startIcon(R.drawable.ic_hotfox_power, text, 20)
            }
            MainActivity.ConnectionVisualState.ERROR -> {
                hint?.isVisible = false
                stages?.isVisible = false
                metrics?.isVisible = false
                note?.visibility = View.GONE
                stageLine?.isVisible = stageLine?.text?.isNotBlank() == true
                chip?.setText(R.string.hotfox_home_status_off)
                chip?.setTextColor(muted)
                chip?.setBackgroundResource(R.drawable.hf_v13_chip_idle)
                chip?.startIcon(R.drawable.ic_hotfox_shield, muted, 14)
                title?.setText(R.string.hotfox_home_title_disconnected)
                title?.setTextColor(text)
                subtitle?.setText(R.string.hotfox_home_subtitle_disconnected)
                subtitle?.setTextColor(muted)
                subtitle?.setCompoundDrawablesRelative(null, null, null, null)
                cta?.setBackgroundResource(R.drawable.hf_v13_primary)
                cta?.setTextColor(text)
                cta?.setText(R.string.hotfox_connect)
                cta?.startIcon(R.drawable.ic_hotfox_power, text, 20)
            }
            else -> {
                hint?.isVisible = true
                stages?.isVisible = false
                metrics?.isVisible = false
                note?.visibility = View.GONE
                stageLine?.isVisible = false
                chip?.setText(R.string.hotfox_home_status_off)
                chip?.setTextColor(muted)
                chip?.setBackgroundResource(R.drawable.hf_v13_chip_idle)
                chip?.startIcon(R.drawable.ic_hotfox_shield, muted, 14)
                title?.setText(R.string.hotfox_home_title_disconnected)
                title?.setTextColor(text)
                subtitle?.setText(R.string.hotfox_home_subtitle_disconnected)
                subtitle?.setTextColor(muted)
                subtitle?.setCompoundDrawablesRelative(null, null, null, null)
                cta?.setBackgroundResource(R.drawable.hf_v13_primary)
                cta?.setTextColor(text)
                cta?.setText(R.string.hotfox_connect)
                cta?.startIcon(R.drawable.ic_hotfox_power, text, 20)
            }
        }
    }

    fun bindPro(chip: View?, visible: Boolean, onClick: () -> Unit) {
        chip ?: return
        chip.isVisible = visible
        chip.setOnClickListener { onClick() }
    }

    fun bindShadowSwitch(sw: androidx.appcompat.widget.SwitchCompat?) {
        sw ?: return
        val current = HotfoxShadowStore.isShadowAuto()
        if (sw.isChecked != current) {
            sw.setOnCheckedChangeListener(null)
            sw.isChecked = current
        }
        sw.setOnCheckedChangeListener { _, checked ->
            if (HotfoxShadowStore.isShadowAuto() == checked) return@setOnCheckedChangeListener
            HotfoxShadowStore.setShadowAuto(checked)
        }
    }
}
