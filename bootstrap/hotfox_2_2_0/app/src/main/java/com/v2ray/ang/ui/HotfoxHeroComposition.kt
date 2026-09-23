package com.v2ray.ang.ui

import kotlin.math.abs
import kotlin.math.max

/**
 * Locked HOME fox geometry.
 *
 * Fractions are of the phone screen, not the leftover HeroSlot. The planet
 * backdrop is a separate full-screen layer and is not positioned here.
 * Disconnected / Connecting / Connected must use these exact values.
 */
object HomeHeroGeometry {
    const val FOX_WIDTH = 0.72f
    const val FOX_CENTER_X = 0.52f
    const val FOX_CENTER_Y = 0.36f
    const val FOX_MAX_HEIGHT = 0.38f
    /** Keep ears below the brand row. */
    const val FOX_MIN_TOP_FRAC = 0.14f
    /** Keep the muzzle above the Home CTA band (~y=0.519 on 1032×2292). */
    const val FOX_SAFE_BOTTOM_FRAC = 0.495f
    const val PLANET_WIDTH = 1.28f
    const val PLANET_CENTER_X = 0.44f
    const val PLANET_CENTER_Y = 0.52f
    const val PLANET_ALPHA = 1f
    const val GLOW_ALPHA = 0.28f
}

/**
 * Full-screen Home planet. Lives behind every Home region; fox is composed
 * against the same phone canvas and may overflow the short HeroSlot.
 * Diameter covers the longer viewport side so the mass fills the phone, not a slot crop.
 */
object HomePlanetBackdropGeometry {
    const val COVER = 1.22f
    const val CENTER_X = 0.56f
    const val CENTER_Y = 0.40f
    const val ALPHA = 1f
    const val GLOW_ALPHA = 0.22f

    fun diameter(hostW: Float, hostH: Float): Float = max(hostW, hostH) * COVER
}

object HotfoxHeroComposition {
    /** Owner-imported master fox after white-key, unresized: 1063 x 1186. */
    const val FOX_INTRINSIC_WIDTH = 1063f
    const val FOX_INTRINSIC_HEIGHT = 1186f
    const val FOX_ASPECT = FOX_INTRINSIC_WIDTH / FOX_INTRINSIC_HEIGHT

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
        val layoutWidth: Int = 0,
        val layoutHeight: Int = 0,
    ) {
        val right: Int get() = left + layoutWidth.coerceAtLeast(diameter.toInt())
        val bottom: Int get() = top + layoutHeight.coerceAtLeast(diameter.toInt())
    }

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
        val bottom: Int get() = top + heightPx
    }

    data class GlowLayout(
        val diameter: Int,
        val left: Int,
        val top: Int,
        val alpha: Float,
    )

    data class ModeSpec(
        val foxWidthFrac: Float,
        val foxCenterXFrac: Float,
        val foxCenterYFrac: Float,
        val planetOverflow: Float,
        val planetCenterXFrac: Float,
        val planetCenterYFrac: Float,
        val planetAlpha: Float,
        val glowAlpha: Float,
        val useLockedHome: Boolean = false,
        val planetCoverViewport: Boolean = false,
        val foxTopMinFrac: Float = 0f,
        val foxBottomMaxFrac: Float = 1f,
    )

    fun isHome(mode: HotFoxHeroMode): Boolean = when (mode) {
        HotFoxHeroMode.HOME_DISCONNECTED,
        HotFoxHeroMode.HOME_CONNECTING,
        HotFoxHeroMode.HOME_CONNECTED,
        -> true
        else -> false
    }

    fun spec(mode: HotFoxHeroMode): ModeSpec = if (isHome(mode)) {
        ModeSpec(
            foxWidthFrac = HomeHeroGeometry.FOX_WIDTH,
            foxCenterXFrac = HomeHeroGeometry.FOX_CENTER_X,
            foxCenterYFrac = HomeHeroGeometry.FOX_CENTER_Y,
            planetOverflow = HomeHeroGeometry.PLANET_WIDTH,
            planetCenterXFrac = HomeHeroGeometry.PLANET_CENTER_X,
            planetCenterYFrac = HomeHeroGeometry.PLANET_CENTER_Y,
            planetAlpha = HomeHeroGeometry.PLANET_ALPHA,
            glowAlpha = HomeHeroGeometry.GLOW_ALPHA,
            useLockedHome = true,
        )
    } else when (mode) {
        HotFoxHeroMode.SPLASH -> ModeSpec(
            foxWidthFrac = 0.96f,
            foxCenterXFrac = 0.50f,
            foxCenterYFrac = 0.42f,
            planetOverflow = 1.22f,
            planetCenterXFrac = 0.34f,
            planetCenterYFrac = 0.40f,
            planetAlpha = 0.82f,
            glowAlpha = 0f,
            planetCoverViewport = true,
            foxTopMinFrac = 0.04f,
            foxBottomMaxFrac = 0.82f,
        )
        HotFoxHeroMode.SUBSCRIPTION -> ModeSpec(
            foxWidthFrac = 0.72f,
            foxCenterXFrac = 0.50f,
            foxCenterYFrac = 0.50f,
            planetOverflow = 1.22f,
            planetCenterXFrac = 0.40f,
            planetCenterYFrac = 0.48f,
            planetAlpha = 0.74f,
            glowAlpha = 0f,
            planetCoverViewport = true,
            foxTopMinFrac = 0.24f,
            foxBottomMaxFrac = 0.74f,
        )
        HotFoxHeroMode.SUBSCRIPTION_INPUT -> ModeSpec(
            foxWidthFrac = 0.50f,
            foxCenterXFrac = 0.54f,
            foxCenterYFrac = 0.80f,
            planetOverflow = 1.22f,
            planetCenterXFrac = 0.40f,
            planetCenterYFrac = 0.46f,
            planetAlpha = 0.58f,
            glowAlpha = 0.10f,
            planetCoverViewport = true,
            foxTopMinFrac = 0.56f,
            foxBottomMaxFrac = 0.96f,
        )
        else -> spec(HotFoxHeroMode.HOME_DISCONNECTED)
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
        if (spec.planetCoverViewport) {
            return coverViewportPlanet(
                hostW = hostW,
                hostH = hostH,
                drawableW = drawableW,
                drawableH = drawableH,
                cover = spec.planetOverflow,
                cxFrac = spec.planetCenterXFrac,
                cyFrac = spec.planetCenterYFrac,
                alpha = spec.planetAlpha,
            )
        }
        val overflow = spec.planetOverflow
        val diameter = hostW * overflow
        val scale = diameter / drawableW
        val cx = hostW * spec.planetCenterXFrac
        val cy = hostH * spec.planetCenterYFrac
        val left = cx - diameter / 2f
        val top = cy - drawableH * scale / 2f
        val layout = diameter.toInt().coerceAtLeast(1)
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
            layoutWidth = layout,
            layoutHeight = layout,
        )
    }

    /**
     * Size the planet so its layout rect covers the phone. The onboarding PNG is
     * 1080×1400, not square: a width-based square leaves a hard crop line on-screen.
     */
    private fun coverViewportPlanet(
        hostW: Float,
        hostH: Float,
        drawableW: Float,
        drawableH: Float,
        cover: Float,
        cxFrac: Float,
        cyFrac: Float,
        alpha: Float,
    ): PlanetTransform {
        val aspect = drawableW / drawableH
        val needW = hostW * cover
        val needH = hostH * cover
        var layoutH = needH
        var layoutW = layoutH * aspect
        if (layoutW < needW) {
            layoutW = needW
            layoutH = layoutW / aspect
        }
        val cx = hostW * cxFrac
        val cy = hostH * cyFrac
        val left = cx - layoutW / 2f
        val top = cy - layoutH / 2f
        return PlanetTransform(
            scale = layoutW / drawableW,
            translateX = left,
            translateY = top,
            alpha = alpha,
            diameter = max(layoutW, layoutH),
            centerX = cx,
            centerY = cy,
            left = left.toInt(),
            top = top.toInt(),
            layoutWidth = layoutW.toInt().coerceAtLeast(1),
            layoutHeight = layoutH.toInt().coerceAtLeast(1),
        )
    }

    fun planet(
        hostW: Float,
        hostH: Float,
        drawableW: Float,
        drawableH: Float,
        variant: Variant,
    ): PlanetTransform = planet(hostW, hostH, drawableW, drawableH, variant.toMode())

    fun fullscreenPlanet(
        hostW: Float,
        hostH: Float,
        drawableW: Float,
        drawableH: Float,
    ): PlanetTransform {
        if (hostW <= 0f || hostH <= 0f || drawableW <= 0f || drawableH <= 0f) {
            return PlanetTransform(1f, 0f, 0f, 1f, 0f, 0f, 0f, 0, 0)
        }
        val diameter = HomePlanetBackdropGeometry.diameter(hostW, hostH)
        val scale = diameter / drawableW
        val cx = hostW * HomePlanetBackdropGeometry.CENTER_X
        val cy = hostH * HomePlanetBackdropGeometry.CENTER_Y
        val left = cx - diameter / 2f
        val top = cy - drawableH * scale / 2f
        val layout = diameter.toInt().coerceAtLeast(1)
        return PlanetTransform(
            scale = scale,
            translateX = left,
            translateY = top,
            alpha = HomePlanetBackdropGeometry.ALPHA,
            diameter = diameter,
            centerX = cx,
            centerY = cy,
            left = left.toInt(),
            top = top.toInt(),
            layoutWidth = layout,
            layoutHeight = layout,
        )
    }

    fun fullscreenGlow(hostW: Float, hostH: Float): GlowLayout {
        val planet = fullscreenPlanet(hostW, hostH, 1024f, 1024f)
        val diameter = (planet.diameter * 1.04f).toInt().coerceAtLeast(1)
        return GlowLayout(
            diameter = diameter,
            left = (planet.centerX - diameter / 2f).toInt(),
            top = (planet.centerY - diameter / 2f).toInt(),
            alpha = HomePlanetBackdropGeometry.GLOW_ALPHA,
        )
    }

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
            return FoxLayout(1, 1, 0, 0, 0f, 0f, HomeHeroGeometry.FOX_WIDTH)
        }
        val spec = spec(mode)
        val aspect = if (drawableH > 0f) drawableW / drawableH else FOX_ASPECT
        var widthFrac = spec.foxWidthFrac
        var width = hostW * widthFrac
        var height = width / aspect
        var cx = hostW * spec.foxCenterXFrac
        var cy = hostH * spec.foxCenterYFrac
        if (isHome(mode)) {
            val maxH = hostH * HomeHeroGeometry.FOX_MAX_HEIGHT
            if (height > maxH && maxH > 1f) {
                height = maxH
                width = height * aspect
                widthFrac = width / hostW
            }
            val minTop = hostH * HomeHeroGeometry.FOX_MIN_TOP_FRAC
            val maxBottom = hostH * HomeHeroGeometry.FOX_SAFE_BOTTOM_FRAC
            val avail = maxBottom - minTop
            if (avail > 1f && height > avail) {
                height = avail
                width = height * aspect
                widthFrac = width / hostW
            }
            var top = cy - height / 2f
            var bottom = cy + height / 2f
            if (bottom > maxBottom) {
                val shift = bottom - maxBottom
                top -= shift
                bottom -= shift
                cy -= shift
            }
            if (top < minTop) {
                val shift = minTop - top
                top += shift
                bottom += shift
                cy += shift
                if (bottom > maxBottom) {
                    bottom = maxBottom
                    top = (bottom - height).coerceAtLeast(minTop)
                    height = bottom - top
                    width = height * aspect
                    widthFrac = width / hostW
                    cy = (top + bottom) / 2f
                }
            }
            val left = (cx - width / 2f).toInt()
            return FoxLayout(
                widthPx = width.toInt().coerceAtLeast(1),
                heightPx = height.toInt().coerceAtLeast(1),
                left = left,
                top = top.toInt(),
                centerX = cx,
                centerY = cy,
                widthFrac = widthFrac,
            )
        }
        // Splash/Connect: fox stays in the measured canvas and between title/CTA
        // bands. Planet is a separate full-screen layer and must not be boxed.
        val minTop = hostH * spec.foxTopMinFrac
        val maxBottom = hostH * spec.foxBottomMaxFrac
        val avail = maxBottom - minTop
        if (avail > 1f && height > avail) {
            height = avail
            width = height * aspect
            widthFrac = width / hostW
        }
        if (width > hostW && hostW > 1f) {
            width = hostW
            height = width / aspect
            widthFrac = 1f
            if (avail > 1f && height > avail) {
                height = avail
                width = height * aspect
                widthFrac = width / hostW
            }
        }
        var top = cy - height / 2f
        var bottom = cy + height / 2f
        if (bottom > maxBottom) {
            val shift = bottom - maxBottom
            top -= shift
            bottom -= shift
            cy -= shift
        }
        if (top < minTop) {
            val shift = minTop - top
            top += shift
            bottom += shift
            cy += shift
            if (bottom > maxBottom) {
                bottom = maxBottom
                top = (bottom - height).coerceAtLeast(minTop)
                height = bottom - top
                width = height * aspect
                widthFrac = width / hostW
                cy = (top + bottom) / 2f
            }
        }
        val left = (cx - width / 2f).toInt()
        return FoxLayout(
            widthPx = width.toInt().coerceAtLeast(1),
            heightPx = height.toInt().coerceAtLeast(1),
            left = left,
            top = top.toInt(),
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
        val diameter = (planet.diameter * 1.04f).toInt().coerceAtLeast(1)
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
        val spec = spec(mode)
        val bottomLimit = if (isHome(mode)) {
            hostH * HomeHeroGeometry.FOX_SAFE_BOTTOM_FRAC + 1f
        } else {
            hostH * spec.foxBottomMaxFrac + 1f
        }
        val topLimit = if (isHome(mode)) {
            hostH * HomeHeroGeometry.FOX_MIN_TOP_FRAC - 1f
        } else {
            hostH * spec.foxTopMinFrac - 1f
        }
        return layout.left >= -1 &&
            layout.left + layout.widthPx <= hostW + 1f &&
            layout.top.toFloat() >= topLimit &&
            layout.bottom <= bottomLimit + 1f
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
        val width = planet.layoutWidth.takeIf { it > 0 } ?: planet.diameter.toInt()
        val height = planet.layoutHeight.takeIf { it > 0 } ?: planet.diameter.toInt()
        return planet.left < 0 || planet.top < 0 ||
            planet.left + width > hostW || planet.top + height > hostH
    }

    fun planetOverflowsHost(
        hostW: Float,
        hostH: Float,
        drawableW: Float,
        drawableH: Float,
        variant: Variant,
    ): Boolean = planetOverflowsHost(hostW, hostH, drawableW, drawableH, variant.toMode())

    fun homeStatesShareGeometry(hostW: Float, hostH: Float): Boolean {
        val a = fox(hostW, hostH, HotFoxHeroMode.HOME_DISCONNECTED)
        val b = fox(hostW, hostH, HotFoxHeroMode.HOME_CONNECTING)
        val c = fox(hostW, hostH, HotFoxHeroMode.HOME_CONNECTED)
        val pa = planet(hostW, hostH, 1024f, 1024f, HotFoxHeroMode.HOME_DISCONNECTED)
        val pb = planet(hostW, hostH, 1024f, 1024f, HotFoxHeroMode.HOME_CONNECTING)
        val pc = planet(hostW, hostH, 1024f, 1024f, HotFoxHeroMode.HOME_CONNECTED)
        return a.left == b.left && a.left == c.left &&
            a.top == b.top && a.top == c.top &&
            a.widthPx == b.widthPx && a.widthPx == c.widthPx &&
            a.heightPx == b.heightPx && a.heightPx == c.heightPx &&
            abs(pa.centerX - pb.centerX) < 0.01f && abs(pa.centerX - pc.centerX) < 0.01f &&
            abs(pa.centerY - pb.centerY) < 0.01f && abs(pa.centerY - pc.centerY) < 0.01f &&
            abs(pa.diameter - pb.diameter) < 0.01f && abs(pa.diameter - pc.diameter) < 0.01f
    }
}
