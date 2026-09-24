package com.v2ray.ang.vpn

import android.content.Context
import android.os.Build
import android.provider.Settings
import android.view.HapticFeedbackConstants
import android.view.View
import android.view.accessibility.AccessibilityManager

/**
 * Reduced-motion and restrained haptics for 3.0 Premium UX.
 * Haptics fire only on intentional user actions, never on background state churn.
 */
object HotfoxMotion {
    fun reducedMotion(
        animatorDurationScale: Float,
        transitionAnimationScale: Float = 1f,
    ): Boolean = animatorDurationScale == 0f || transitionAnimationScale == 0f

    fun reducedMotion(context: Context): Boolean {
        val resolver = context.contentResolver
        val animator = runCatching {
            Settings.Global.getFloat(resolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f)
        }.getOrDefault(1f)
        val transition = runCatching {
            Settings.Global.getFloat(resolver, Settings.Global.TRANSITION_ANIMATION_SCALE, 1f)
        }.getOrDefault(1f)
        return reducedMotion(animator, transition)
    }

    fun durationMs(base: Long, reduced: Boolean): Long = if (reduced) 0L else base
}

object HotfoxHaptics {
    fun confirm(view: View, reducedMotion: Boolean) {
        if (reducedMotion) return
        val kind = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            HapticFeedbackConstants.CONFIRM
        } else {
            HapticFeedbackConstants.CONTEXT_CLICK
        }
        view.performHapticFeedback(kind)
    }

    fun isTouchExplorationEnabled(context: Context): Boolean {
        val manager = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as? AccessibilityManager
        return manager?.isTouchExplorationEnabled == true
    }
}
