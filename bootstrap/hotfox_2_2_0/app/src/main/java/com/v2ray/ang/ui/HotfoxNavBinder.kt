package com.v2ray.ang.ui

import android.content.Intent
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.v2ray.ang.R

object HotfoxNavBinder {
    enum class Destination { HOME, SERVERS, SUBSCRIPTION, SETTINGS }

    fun bind(
        root: View,
        selected: Destination,
        onHome: () -> Unit,
        onServers: () -> Unit,
        onSubscription: () -> Unit,
        onSettings: () -> Unit,
        connectionLabelRes: Int = R.string.hotfox_nav_home,
        connectionActive: Boolean = selected == Destination.HOME,
    ) {
        val homeItem = root.findViewById<View>(R.id.nav_home_item)
        val serversItem = root.findViewById<View>(R.id.nav_servers_item)
        val subItem = root.findViewById<View>(R.id.nav_subscription_item)
        val settingsItem = root.findViewById<View>(R.id.nav_settings_item)
        homeItem?.setOnClickListener { onHome() }
        serversItem?.setOnClickListener { onServers() }
        subItem?.setOnClickListener { onSubscription() }
        settingsItem?.setOnClickListener { onSettings() }

        val homeIcon = root.findViewById<ImageView>(R.id.nav_home_icon)
        val serversIcon = root.findViewById<ImageView>(R.id.nav_servers_icon)
        val subIcon = root.findViewById<ImageView>(R.id.nav_subscription_icon)
        val settingsIcon = root.findViewById<ImageView>(R.id.nav_settings_icon)
        val homeLabel = root.findViewById<TextView>(R.id.nav_connection)
        val serversLabel = root.findViewById<TextView>(R.id.nav_servers)
        val subLabel = root.findViewById<TextView>(R.id.nav_subscription)
        val settingsLabel = root.findViewById<TextView>(R.id.nav_settings)

        val cream = ContextCompat.getColor(root.context, R.color.hf_v13_text)
        val muted = ContextCompat.getColor(root.context, R.color.hf_v13_muted)
        val orange = ContextCompat.getColor(root.context, R.color.hf_v13_orange)

        homeIcon?.setImageResource(R.drawable.ic_hotfox_home)
        serversIcon?.setImageResource(R.drawable.ic_hotfox_servers)
        subIcon?.setImageResource(R.drawable.ic_hotfox_crown)
        settingsIcon?.setImageResource(R.drawable.ic_hotfox_settings)
        homeIcon?.imageTintList = android.content.res.ColorStateList.valueOf(
            if (selected == Destination.HOME) orange else muted,
        )
        serversIcon?.imageTintList = android.content.res.ColorStateList.valueOf(
            if (selected == Destination.SERVERS) orange else muted,
        )
        subIcon?.imageTintList = android.content.res.ColorStateList.valueOf(
            if (selected == Destination.SUBSCRIPTION) orange else muted,
        )
        settingsIcon?.imageTintList = android.content.res.ColorStateList.valueOf(
            if (selected == Destination.SETTINGS) orange else muted,
        )

        homeLabel?.setText(connectionLabelRes)
        homeLabel?.setTextColor(
            if (selected == Destination.HOME) {
                if (connectionActive) cream else muted
            } else {
                muted
            },
        )
        serversLabel?.setTextColor(if (selected == Destination.SERVERS) cream else muted)
        subLabel?.setTextColor(if (selected == Destination.SUBSCRIPTION) cream else muted)
        settingsLabel?.setTextColor(if (selected == Destination.SETTINGS) cream else muted)
    }

    fun openMain(activity: android.app.Activity, extra: String? = null) {
        val intent = Intent(activity, MainActivity::class.java)
            .putExtra(MainActivity.EXTRA_SKIP_ONBOARDING, true)
            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        extra?.let { intent.putExtra(MainActivity.EXTRA_OPEN_SECTION, it) }
        activity.startActivity(intent)
    }
}
