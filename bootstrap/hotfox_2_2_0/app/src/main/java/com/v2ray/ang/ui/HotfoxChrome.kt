package com.v2ray.ang.ui

import android.content.Intent
import android.content.res.ColorStateList
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.widget.TextViewCompat
import com.v2ray.ang.R

/**
 * Shared chrome for the locked 4-destination bottom bar and sub-screen back control.
 * Visual destinations follow screen 05: Главная / Серверы / Подписка / Настройки.
 */
object HotfoxChrome {
    const val HOME = 0
    const val SERVERS = 1
    const val SUBSCRIPTION = 2
    const val SETTINGS = 3

    fun bindBack(activity: AppCompatActivity) {
        activity.findViewById<android.view.View>(R.id.btn_header_back)?.setOnClickListener {
            activity.finish()
        }
    }

    fun bindBottomNav(activity: AppCompatActivity, selected: Int) {
        val home = activity.findViewById<TextView>(R.id.nav_connection) ?: return
        val servers = activity.findViewById<TextView>(R.id.nav_servers) ?: return
        val sub = activity.findViewById<TextView>(R.id.nav_subscription) ?: return
        val settings = activity.findViewById<TextView>(R.id.nav_settings) ?: return
        paint(activity, home, selected == HOME)
        paint(activity, servers, selected == SERVERS)
        paint(activity, sub, selected == SUBSCRIPTION)
        paint(activity, settings, selected == SETTINGS)
        home.setOnClickListener { openMain(activity, MainActivity.SECTION_CONNECTION) }
        servers.setOnClickListener { openMain(activity, MainActivity.SECTION_SERVERS) }
        sub.setOnClickListener { openMain(activity, MainActivity.SECTION_SUBSCRIPTION) }
        settings.setOnClickListener {
            if (activity is HotfoxSettingsActivity) return@setOnClickListener
            activity.startActivity(Intent(activity, HotfoxSettingsActivity::class.java))
            if (activity !is MainActivity) activity.finish()
        }
    }

    private fun openMain(activity: AppCompatActivity, section: String) {
        if (activity is MainActivity) return
        activity.startActivity(
            Intent(activity, MainActivity::class.java)
                .putExtra(MainActivity.EXTRA_OPEN_SECTION, section)
                .putExtra(MainActivity.EXTRA_SKIP_ONBOARDING, true)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP),
        )
        activity.finish()
    }

    private fun paint(activity: AppCompatActivity, view: TextView, active: Boolean) {
        val color = ContextCompat.getColor(
            activity,
            if (active) R.color.hotfox_orange else R.color.hotfox_cream_muted,
        )
        view.setTextColor(color)
        TextViewCompat.setCompoundDrawableTintList(view, ColorStateList.valueOf(color))
    }
}
