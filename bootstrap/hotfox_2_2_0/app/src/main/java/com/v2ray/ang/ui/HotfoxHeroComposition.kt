package com.v2ray.ang.ui

import kotlin.math.min

/**
 * Single composition system for the fox + planet hero.
 *
 * Planet is an oversized filled sphere that is allowed to overflow the host
 * (cinematic full-bleed). Fox is never cropped: it is a square FIT_CENTER
 * box that always fits inside the host.
 *
 * Viewport units are pixels of the hero host, not dp. Callers convert.
 */
object HotfoxHeroComposition {
    enum class Variant {
        SPLASH,
        PAGE,
        SUPPORT,
    }

    data class PlanetTransform(
        val scale: Float,
        val translateX: Float,
        val translateY: Float,
        val alpha: Float,
        val diameter: Float,
        val centerX: Float,
        val centerY: Float,
    )

    data class FoxLayout(
        val sizePx: Int,
        val bottomMarginPx: Int,
        val centerVertically: Boolean,
    )

    fun planet(
        hostW: Float,
        hostH: Float,
        drawableW: Float,
        drawableH: Float,
        variant: Variant,
    ): PlanetTransform {
        if (hostW <= 0f || hostH <= 0f || drawableW <= 0f || drawableH <= 0f) {
            return PlanetTransform(1f, 0f, 0f, 1f, 0f, 0f, 0f)
        }
        val overflow = when (variant) {
            Variant.SPLASH -> 1.55f
            Variant.PAGE -> 1.48f
            Variant.SUPPORT -> 1.42f
        }
        val diameter = hostW * overflow
        val scale = diameter / drawableW
        val cx = hostW * 0.50f
        val cy = hostH * when (variant) {
            Variant.SPLASH -> 0.50f
            Variant.PAGE -> 0.82f
            Variant.SUPPORT -> 0.78f
        }
        val tx = cx - drawableW * scale / 2f
        val ty = cy - drawableH * scale / 2f
        val alpha = when (variant) {
            Variant.SUPPORT -> 0.88f
            else -> 1f
        }
        return PlanetTransform(scale, tx, ty, alpha, diameter, cx, cy)
    }

    fun fox(hostW: Float, hostH: Float, variant: Variant): FoxLayout {
        if (hostW <= 0f || hostH <= 0f) {
            return FoxLayout(1, 0, centerVertically = true)
        }
        val widthFrac = when (variant) {
            Variant.SPLASH -> 0.86f
            Variant.PAGE -> 0.78f
            Variant.SUPPORT -> 0.70f
        }
        val heightCap = when (variant) {
            Variant.SPLASH -> 0.58f
            Variant.PAGE -> 0.86f
            Variant.SUPPORT -> 0.80f
        }
        val size = min(hostW * widthFrac, hostH * heightCap).toInt().coerceAtLeast(1)
        val remaining = (hostH - size).coerceAtLeast(0f)
        val centerVertically = variant == Variant.SPLASH
        val bottom = if (centerVertically) {
            (remaining * 0.12f).toInt()
        } else {
            (remaining * 0.22f).toInt()
        }
        return FoxLayout(size, bottom, centerVertically)
    }

    fun foxFitsInHost(hostW: Float, hostH: Float, variant: Variant): Boolean {
        val layout = fox(hostW, hostH, variant)
        return layout.sizePx <= hostW + 0.5f &&
            layout.sizePx + layout.bottomMarginPx <= hostH + 0.5f
    }

    fun planetOverflowsHost(
        hostW: Float,
        hostH: Float,
        drawableW: Float,
        drawableH: Float,
        variant: Variant,
    ): Boolean {
        val planet = planet(hostW, hostH, drawableW, drawableH, variant)
        return planet.diameter > hostW
    }
}
