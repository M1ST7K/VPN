package com.v2ray.ang.ui

import android.app.Activity
import android.graphics.Rect
import android.view.View
import com.v2ray.ang.R

/**
 * Debug-only Home scaffold overlap check. Does not change production layout.
 */
object HotfoxHomeSlotGuard {
    private const val EPS = 2

    fun verify(activity: Activity) {
        val header = activity.findViewById<View>(R.id.header_status_slot) ?: return
        val hero = activity.findViewById<View>(R.id.hero_slot) ?: return
        val state = activity.findViewById<View>(R.id.state_panel_slot) ?: return
        val settings = activity.findViewById<View>(R.id.layout_connection_rows) ?: return
        val footer = activity.findViewById<View>(R.id.layout_connection_note)
        val nav = activity.findViewById<View>(R.id.hotfox_bottom_nav) ?: return
        if (hero.visibility != View.VISIBLE) return
        val headerR = rect(header)
        val heroR = rect(hero)
        val stateR = rect(state)
        val settingsR = rect(settings)
        val navR = rect(nav)
        require(headerR.bottom <= heroR.top + EPS) { "Header overlaps HeroSlot $headerR $heroR" }
        require(heroR.bottom <= stateR.top + EPS) { "HeroSlot overlaps StatePanel $heroR $stateR" }
        require(stateR.bottom <= settingsR.top + EPS) { "StatePanel overlaps QuickSettings $stateR $settingsR" }
        if (footer != null && footer.visibility == View.VISIBLE && footer.height > 0) {
            val footerR = rect(footer)
            require(settingsR.bottom <= footerR.top + EPS) { "QuickSettings overlaps footer $settingsR $footerR" }
            require(footerR.bottom <= navR.top + EPS) { "Footer overlaps BottomNav $footerR $navR" }
        } else {
            require(settingsR.bottom <= navR.top + EPS) { "QuickSettings overlaps BottomNav $settingsR $navR" }
        }
    }

    private fun rect(view: View): Rect {
        val loc = IntArray(2)
        view.getLocationOnScreen(loc)
        return Rect(loc[0], loc[1], loc[0] + view.width, loc[1] + view.height)
    }
}
