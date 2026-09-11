package com.v2ray.ang.ui

import android.app.Activity
import android.os.Build
import android.view.View
import android.view.ViewGroup
import android.widget.ScrollView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.updatePadding
import com.v2ray.ang.R

/**
 * Dark HotFox screens always use light system-bar icons.
 * Does not change VPN / entitlement / connection truth.
 */
object HotfoxSystemUi {
    fun applyDarkEditorialBars(activity: Activity) {
        val window = activity.window
        val bg = activity.getColor(R.color.hf_asset_background)
        window.statusBarColor = bg
        window.navigationBarColor = bg
        WindowCompat.setDecorFitsSystemWindows(window, true)
        val controller = WindowInsetsControllerCompat(window, window.decorView)
        controller.isAppearanceLightStatusBars = false
        controller.isAppearanceLightNavigationBars = false
        if (Build.VERSION.SDK_INT >= 29) {
            window.isNavigationBarContrastEnforced = false
        }
    }

    fun hideScrollbars(root: View) {
        if (root is ScrollView) {
            root.isVerticalScrollBarEnabled = false
            root.isHorizontalScrollBarEnabled = false
        }
        if (root is ViewGroup) {
            for (i in 0 until root.childCount) {
                hideScrollbars(root.getChildAt(i))
            }
        }
    }

    fun padScrollAboveBottomNav(scroll: View, nav: View?) {
        if (nav == null) return
        nav.post {
            val extra = nav.height
            if (extra > 0) {
                scroll.updatePadding(bottom = extra)
            }
        }
        ViewCompat.setOnApplyWindowInsetsListener(scroll) { v, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.navigationBars())
            val navH = nav.height
            v.updatePadding(bottom = navH + bars.bottom)
            insets
        }
    }
}
