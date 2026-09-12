package com.v2ray.ang.ui

import kotlin.math.max
import kotlin.math.min

/**
 * Full-viewport fox + planet composition.
 *
 * The hero is an independent visual layer, not a measured Column row.
 * Planet may overflow the viewport. Fox uses FIT (never cropped).
 *
 * Host units are pixels of the full-screen hero view.
 */
object HotfoxHeroComposition {
    /** Owner-imported master fox after white-key, unresized: 1063 x 1186. */
    const val FOX_INTRINSIC_WIDTH = 1063f
    const val FOX_INTRINSIC_HEIGHT = 1186f
    const val FOX_ASPECT = FOX_INTRINSIC_WIDTH / FOX_INTRINSIC_HEIGHT

    const val MIN_FOX_WIDTH_FRAC = 0.62f
    const val START_FOX_WIDTH_FRAC = 0.74f

    enum class Variant {
        SPLASH,
        PAGE,
        SUPPORT,
        ;

        fun toMode(): HotFoxHeroMode = when (this) {
            SPLASH -> HotFoxHeroMode.SPLASH
            PAGE -> HotFoxHeroMode.HOME_DISCONNECTED
            SUPPORT -> HotFoxHeroMode.SUBSCRIPTION_INPUT
        }
    }

    data class PlanetTransform(
        val scale: Float,
        val translateX: Float,
        val translateY: Float,
        val alpha: Float,
        val diameter: Float,
        val centerX: Float,
        val centerY: Float,
        val left: Int,
        val top: Int,
    )

    data class FoxLayout(
        val widthPx: Int,
        val heightPx: Int,
        val left: Int,
        val top: Int,
        val centerX: Float,
        val centerY: Float,
        val widthFrac: Float,
    ) {
        val sizePx: Int get() = widthPx
        val bottomMarginPx: Int get() = 0
        val centerVertically: Boolean get() = false
    }

    data class GlowLayout(
        val diameter: Int,
        val left: Int,
        val top: Int,
        val alpha: Float,
    )

    fun spec(mode: HotFoxHeroMode): ModeSpec = when (mode) {
        HotFoxHeroMode.SPLASH -> ModeSpec(
            foxWidthFrac = 0.76f,
            foxCenterXFrac = 0.535f,
            foxCenterYFrac = 0.52f,
            planetOverflow = 1.50f,
            planetCenterXFrac = 0.50f,
            planetCenterYFrac = 0.42f,
            planetAlpha = 1f,
            glowAlpha = 0.42f,
        )
        HotFoxHeroMode.HOME_DISCONNECTED -> ModeSpec(
            foxWidthFrac = 0.78f,
            foxCenterXFrac = 0.52f,
            foxCenterYFrac = 0.38f,
            planetOverflow = 1.46f,
            planetCenterXFrac = 0.46f,
            planetCenterYFrac = 0.38f,
            planetAlpha = 1f,
            glowAlpha = 0.36f,
        )
        HotFoxHeroMode.HOME_CONNECTING -> ModeSpec(
            foxWidthFrac = 0.78f,
            foxCenterXFrac = 0.56f,
            foxCenterYFrac = 0.41f,
            planetOverflow = 1.44f,
            planetCenterXFrac = 0.46f,
            planetCenterYFrac = 0.36f,
            planetAlpha = 1f,
            glowAlpha = 0.40f,
        )
        HotFoxHeroMode.HOME_CONNECTED -> ModeSpec(
            foxWidthFrac = 0.78f,
            foxCenterXFrac = 0.56f,
            foxCenterYFrac = 0.40f,
            planetOverflow = 1.42f,
            planetCenterXFrac = 0.46f,
            planetCenterYFrac = 0.36f,
            planetAlpha = 1f,
            glowAlpha = 0.32f,
        )
        HotFoxHeroMode.SUBSCRIPTION -> ModeSpec(
            foxWidthFrac = 0.80f,
            foxCenterXFrac = 0.55f,
            foxCenterYFrac = 0.56f,
            planetOverflow = 1.50f,
            planetCenterXFrac = 0.50f,
            planetCenterYFrac = 0.44f,
            planetAlpha = 1f,
            glowAlpha = 0.38f,
        )
        HotFoxHeroMode.SUBSCRIPTION_INPUT -> ModeSpec(
            foxWidthFrac = 0.78f,
            foxCenterXFrac = 0.56f,
            foxCenterYFrac = 0.58f,
            planetOverflow = 1.52f,
            planetCenterXFrac = 0.46f,
            planetCenterYFrac = 0.58f,
            planetAlpha = 0.94f,
            glowAlpha = 0.30f,
        )
    }

    data class ModeSpec(
        val foxWidthFrac: Float,
        val foxCenterXFrac: Float,
        val foxCenterYFrac: Float,
        val planetOverflow: Float,
        val planetCenterXFrac: Float,
        val planetCenterYFrac: Float,
        val planetAlpha: Float,
        val glowAlpha: Float,
    )

    fun contentBox(
        hostW: Float,
        hostH: Float,
        insetTop: Float,
        insetBottom: Float,
    ): Pair<Float, Float> {
        val top = insetTop.coerceAtLeast(0f)
        val bottom = (hostH - insetBottom).coerceAtLeast(top + 1f)
        return top to bottom
    }

    fun foxWidthFrac(hostW: Float, hostH: Float, mode: HotFoxHeroMode): Float {
        val spec = spec(mode)
        if (hostW <= 0f || hostH <= 0f) return spec.foxWidthFrac
        val aspect = hostH / hostW
        var frac = spec.foxWidthFrac
        if (aspect < 1.85f) {
            frac *= 0.94f
        }
        if (aspect < 1.70f) {
            frac *= 0.94f
        }
        return max(MIN_FOX_WIDTH_FRAC, min(0.82f, frac))
    }

    fun planet(
        hostW: Float,
        hostH: Float,
        drawableW: Float,
        drawableH: Float,
        mode: HotFoxHeroMode,
        insetTop: Float = 0f,
        insetBottom: Float = 0f,
    ): PlanetTransform {
        if (hostW <= 0f || hostH <= 0f || drawableW <= 0f || drawableH <= 0f) {
            return PlanetTransform(1f, 0f, 0f, 1f, 0f, 0f, 0f, 0, 0)
        }
        val spec = spec(mode)
        val (contentTop, contentBottom) = contentBox(hostW, hostH, insetTop, insetBottom)
        val contentH = (contentBottom - contentTop).coerceAtLeast(1f)
        val overflow = spec.planetOverflow.coerceIn(1.25f, 1.60f)
        val diameter = hostW * overflow
        val scale = diameter / drawableW
        val cx = hostW * spec.planetCenterXFrac
        val cy = contentTop + contentH * spec.planetCenterYFrac
        val left = (cx - diameter / 2f)
        val top = (cy - drawableH * scale / 2f)
        return PlanetTransform(
            scale = scale,
            translateX = left,
            translateY = top,
            alpha = spec.planetAlpha,
            diameter = diameter,
            centerX = cx,
            centerY = cy,
            left = left.toInt(),
            top = top.toInt(),
        )
    }

    fun planet(
        hostW: Float,
        hostH: Float,
        drawableW: Float,
        drawableH: Float,
        variant: Variant,
    ): PlanetTransform = planet(hostW, hostH, drawableW, drawableH, variant.toMode())

    fun fox(
        hostW: Float,
        hostH: Float,
        mode: HotFoxHeroMode,
        insetTop: Float = 0f,
        insetBottom: Float = 0f,
        drawableW: Float = FOX_INTRINSIC_WIDTH,
        drawableH: Float = FOX_INTRINSIC_HEIGHT,
    ): FoxLayout {
        if (hostW <= 0f || hostH <= 0f) {
            return FoxLayout(1, 1, 0, 0, 0f, 0f, MIN_FOX_WIDTH_FRAC)
        }
        val spec = spec(mode)
        val (contentTop, contentBottom) = contentBox(hostW, hostH, insetTop, insetBottom)
        val contentH = (contentBottom - contentTop).coerceAtLeast(1f)
        val aspect = if (drawableH > 0f) drawableW / drawableH else FOX_ASPECT
        var widthFrac = foxWidthFrac(hostW, hostH, mode)
        var width = hostW * widthFrac
        var height = width / aspect
        var cx = hostW * spec.foxCenterXFrac
        // foxCenterYFrac is the optical HEAD center, not the bitmap midpoint.
        val opticalHeadFrac = 0.36f
        val headY = contentTop + contentH * spec.foxCenterYFrac
        var cy = headY + (0.50f - opticalHeadFrac) * height

        // Keep ears/muzzle on-screen. Shrink only after position can't save it.
        val minTop = contentTop + contentH * 0.04f
        if (cy - height / 2f < minTop) {
            cy = minTop + height / 2f
        }
        if (cy - height / 2f < 0f) {
            val maxH = (cy * 2f - 4f).coerceAtLeast(hostW * MIN_FOX_WIDTH_FRAC / aspect)
            if (height > maxH) {
                height = maxH
                width = height * aspect
                widthFrac = width / hostW
            }
        }
        val edge = hostW * 0.03f
        if (cx - width / 2f < edge) {
            cx = edge + width / 2f
        }
        if (cx + width / 2f > hostW - edge) {
            cx = hostW - edge - width / 2f
        }
        val left = (cx - width / 2f).toInt()
        val top = (cy - height / 2f).toInt()
        return FoxLayout(
            widthPx = width.toInt().coerceAtLeast(1),
            heightPx = height.toInt().coerceAtLeast(1),
            left = left,
            top = top,
            centerX = cx,
            centerY = cy,
            widthFrac = widthFrac,
        )
    }

    fun fox(hostW: Float, hostH: Float, variant: Variant): FoxLayout =
        fox(hostW, hostH, variant.toMode())

    fun glow(
        hostW: Float,
        hostH: Float,
        mode: HotFoxHeroMode,
        insetTop: Float = 0f,
        insetBottom: Float = 0f,
    ): GlowLayout {
        val planet = planet(hostW, hostH, 1024f, 1024f, mode, insetTop, insetBottom)
        val spec = spec(mode)
        val diameter = (planet.diameter * 1.08f).toInt().coerceAtLeast(1)
        return GlowLayout(
            diameter = diameter,
            left = (planet.centerX - diameter / 2f).toInt(),
            top = (planet.centerY - diameter / 2f).toInt(),
            alpha = spec.glowAlpha,
        )
    }

    fun foxFitsInHost(
        hostW: Float,
        hostH: Float,
        mode: HotFoxHeroMode,
        insetTop: Float = 0f,
        insetBottom: Float = 0f,
    ): Boolean {
        val layout = fox(hostW, hostH, mode, insetTop, insetBottom)
        // Ears and muzzle must stay inside the viewport. Neck may overflow the bottom.
        return layout.left >= -1 &&
            layout.left + layout.widthPx <= hostW + 1f &&
            layout.top >= -1 &&
            layout.widthPx >= hostW * MIN_FOX_WIDTH_FRAC - 1f
    }

    fun foxFitsInHost(hostW: Float, hostH: Float, variant: Variant): Boolean =
        foxFitsInHost(hostW, hostH, variant.toMode())

    fun planetOverflowsHost(
        hostW: Float,
        hostH: Float,
        drawableW: Float,
        drawableH: Float,
        mode: HotFoxHeroMode,
        insetTop: Float = 0f,
        insetBottom: Float = 0f,
    ): Boolean {
        val planet = planet(hostW, hostH, drawableW, drawableH, mode, insetTop, insetBottom)
        return planet.diameter > hostW
    }

    fun planetOverflowsHost(
        hostW: Float,
        hostH: Float,
        drawableW: Float,
        drawableH: Float,
        variant: Variant,
    ): Boolean = planetOverflowsHost(hostW, hostH, drawableW, drawableH, variant.toMode())

    fun homeStateDeltaPx(hostH: Float, insetTop: Float = 0f, insetBottom: Float = 0f): Float {
        val a = fox(400f, hostH, HotFoxHeroMode.HOME_DISCONNECTED, insetTop, insetBottom).centerY
        val b = fox(400f, hostH, HotFoxHeroMode.HOME_CONNECTED, insetTop, insetBottom).centerY
        return kotlin.math.abs(a - b)
    }
}
