package com.v2ray.ang.ui

import android.app.Activity
import android.app.Application
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.WindowManager
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.v2ray.ang.R
import com.v2ray.ang.dto.AppInfo
import com.v2ray.ang.vpn.ConnectionUiMapper

/**
 * Debug-only view painter. Applies screenshot presentation after production
 * layouts inflate. Never writes VPN/security/subscription stores.
 */
object HotfoxUiQaPainter : Application.ActivityLifecycleCallbacks {
    private val main = Handler(Looper.getMainLooper())
    private var paintGeneration = 0

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
        if (HotfoxUiVisualOverride.holdSplash && activity is HotfoxSplashActivity) {
            activity.skipAutoAdvance = true
        }
        if (HotfoxUiVisualOverride.appsFixture && activity is PerAppProxyActivity) {
            activity.window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }
    }

    override fun onActivityStarted(activity: Activity) = Unit

    override fun onActivityResumed(activity: Activity) {
        paint(activity)
        main.post { paint(activity) }
        main.postDelayed({ paint(activity) }, 450L)
        main.postDelayed({ paint(activity) }, 1600L)
        main.postDelayed({ paint(activity) }, 4000L)
        scheduleRepeatingPaint(activity)
    }

    private fun scheduleRepeatingPaint(activity: Activity) {
        val needsRepeat = HotfoxUiVisualOverride.subscriptionFixture ||
            HotfoxUiVisualOverride.serversFixture ||
            HotfoxUiVisualOverride.showAddSheet ||
            HotfoxUiVisualOverride.connectionChrome != null ||
            HotfoxUiVisualOverride.appsFixture
        if (!needsRepeat) return
        // Repeats only fixture labels/state. Production applyRunningState can
        // overwrite chrome and the VPN-permission stage line; geometry stays in XML.
        paintGeneration += 1
        val generation = paintGeneration
        val tick = object : Runnable {
            override fun run() {
                if (generation != paintGeneration) return
                if (activity.isFinishing || activity.isDestroyed) return
                paint(activity)
                main.postDelayed(this, 700L)
            }
        }
        main.postDelayed(tick, 700L)
    }

    override fun onActivityPaused(activity: Activity) {
        paintGeneration += 1
    }
    override fun onActivityStopped(activity: Activity) = Unit
    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit
    override fun onActivityDestroyed(activity: Activity) = Unit

    private fun paint(activity: Activity) {
        if (activity.isFinishing) return
        when (activity) {
            is MainActivity -> paintMain(activity)
            is PerAppProxyActivity -> paintApps(activity)
        }
    }

    private fun paintMain(activity: MainActivity) {
        val chrome = HotfoxUiVisualOverride.connectionChrome
        if (chrome != null) {
            val visual = when (chrome) {
                HotfoxUiVisualOverride.CHROME_CONNECTING -> MainActivity.ConnectionVisualState.CONNECTING
                HotfoxUiVisualOverride.CHROME_CONNECTED -> MainActivity.ConnectionVisualState.CONNECTED
                else -> MainActivity.ConnectionVisualState.DISCONNECTED
            }
            val headline = when (visual) {
                MainActivity.ConnectionVisualState.CONNECTING -> ConnectionUiMapper.Headline.CONNECTING
                MainActivity.ConnectionVisualState.CONNECTED -> ConnectionUiMapper.Headline.CONNECTED
                else -> ConnectionUiMapper.Headline.DISCONNECTED
            }
            activity.applyConnectionChrome(visual, headline)
            if (visual == MainActivity.ConnectionVisualState.CONNECTED ||
                visual == MainActivity.ConnectionVisualState.CONNECTING
            ) {
                activity.hideConnectionStageLine()
            }
            val headlineRes = when (visual) {
                MainActivity.ConnectionVisualState.CONNECTED -> R.string.hotfox_headline_connected
                MainActivity.ConnectionVisualState.CONNECTING -> R.string.hotfox_headline_connecting
                else -> R.string.hotfox_headline_disconnected
            }
            activity.updateStatusText(
                headlineRes,
                if (visual == MainActivity.ConnectionVisualState.CONNECTED) {
                    R.color.hf_asset_green
                } else {
                    R.color.hf_asset_cream
                },
            )
            val connect = activity.findViewById<TextView>(R.id.connect_action)
            connect?.text = when (visual) {
                MainActivity.ConnectionVisualState.CONNECTED -> activity.getString(R.string.hotfox_disconnect)
                MainActivity.ConnectionVisualState.CONNECTING -> activity.getString(R.string.hotfox_stop)
                else -> activity.getString(R.string.hotfox_connect)
            }
            if (visual == MainActivity.ConnectionVisualState.CONNECTED) {
                activity.findViewById<TextView>(R.id.tv_vpn_status)?.text = "00:00:00"
                activity.findViewById<TextView>(R.id.tv_downloaded)?.text = "0 MB"
                activity.findViewById<TextView>(R.id.tv_uploaded)?.text = "0 MB"
            }
        }
        if (HotfoxUiVisualOverride.showAddSheet) {
            activity.presentAddConnectionSheet()
        }
        if (HotfoxUiVisualOverride.subscriptionFixture) {
            activity.findViewById<View>(R.id.layout_premium_onboarding)?.isVisible = false
            activity.findViewById<View>(R.id.layout_subscription_details)?.isVisible = true
            activity.findViewById<TextView>(R.id.tv_subscription_state)?.text = "● HotFox Premium"
            activity.findViewById<TextView>(R.id.tv_subscription_expire)?.text = "31.12.2026"
            activity.findViewById<TextView>(R.id.tv_subscription_remaining)?.text = "110 дней"
            activity.findViewById<TextView>(R.id.tv_subscription_servers)?.text = "6"
            activity.findViewById<TextView>(R.id.tv_subscription_url)?.text = "https://sub.example/***"
        }
        if (HotfoxUiVisualOverride.serversFixture) {
            val recycler = activity.findViewById<RecyclerView>(R.id.recycler_view) ?: return
            recycler.adapter = HotfoxUiQaServerAdapter(HotfoxUiVisualOverride.referenceServers)
        }
    }

    private fun paintApps(activity: PerAppProxyActivity) {
        if (!HotfoxUiVisualOverride.appsFixture) return
        activity.window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
        val rows = HotfoxUiVisualOverride.referenceApps
        if (rows.isEmpty()) return
        val apps = rows.map { row ->
            AppInfo(
                appName = row.appName,
                packageName = row.packageName,
                appIcon = HotfoxUiQaAppIcons.drawable(activity, row.packageName),
                isSystemApp = false,
                isSelected = if (row.selected) 1 else 0,
            )
        }
        val recycler = activity.findViewById<RecyclerView>(R.id.recycler_view) ?: return
        recycler.isVerticalScrollBarEnabled = false
        recycler.overScrollMode = View.OVER_SCROLL_NEVER
        recycler.adapter = HotfoxUiQaAppAdapter(apps)
        activity.findViewById<TextView>(R.id.tv_selected_count)?.text =
            apps.count { it.isSelected == 1 }.toString()
        activity.findViewById<TextView>(R.id.tv_custom_rules_count)?.text = "3"
    }
}
