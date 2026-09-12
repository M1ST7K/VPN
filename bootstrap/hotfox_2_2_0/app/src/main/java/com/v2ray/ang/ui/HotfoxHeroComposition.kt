package com.v2ray.ang.ui

import kotlin.math.abs

/**
 * Full-viewport fox + planet composition.
 *
 * HOME states share one locked geometry. Splash / subscription keep their own.
 * Fox uses FIT and is never cropped. Planet may overflow the viewport.
 */
data class HomeHeroGeometry(
    val foxWidthFraction: Float = 0.62f,
    val foxCenterXFraction: Float = 0.54f,
    val foxCenterYFraction: Float = 0.49f,
    val planetWidthFraction: Float = 1.18f,
    val planetCenterXFraction: Float = 0.43f,
    val planetCenterYFraction: Float = 0.43f,
    val planetAlpha: Float = 1f,
    val glowAlpha: Float = 0.28f,
)

object HotfoxHeroComposition {
    /** Owner-imported master fox after white-key, unresized: 1063 x 1186. */
    const val FOX_INTRINSIC_WIDTH = 1063f
    const val FOX_INTRINSIC_HEIGHT = 1186f
    const val FOX_ASPECT = FOX_INTRINSIC_WIDTH / FOX_INTRINSIC_HEIGHT

    val HOME = HomeHeroGeometry()

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
            foxWidthFrac = HOME.foxWidthFraction,
            foxCenterXFrac = HOME.foxCenterXFraction,
            foxCenterYFrac = HOME.foxCenterYFraction,
            planetOverflow = HOME.planetWidthFraction,
            planetCenterXFrac = HOME.planetCenterXFraction,
            planetCenterYFrac = HOME.planetCenterYFraction,
            planetAlpha = HOME.planetAlpha,
            glowAlpha = HOME.glowAlpha,
            useLockedHome = true,
        )
    } else when (mode) {
        HotFoxHeroMode.SPLASH -> ModeSpec(
            foxWidthFrac = 0.72f,
            foxCenterXFrac = 0.54f,
            foxCenterYFrac = 0.56f,
            planetOverflow = 1.22f,
            planetCenterXFrac = 0.45f,
            planetCenterYFrac = 0.46f,
            planetAlpha = 1f,
            glowAlpha = 0.36f,
        )
        HotFoxHeroMode.SUBSCRIPTION -> ModeSpec(
            foxWidthFrac = 0.64f,
            foxCenterXFrac = 0.54f,
            foxCenterYFrac = 0.54f,
            planetOverflow = 1.20f,
            planetCenterXFrac = 0.44f,
            planetCenterYFrac = 0.46f,
            planetAlpha = 1f,
            glowAlpha = 0.30f,
        )
        HotFoxHeroMode.SUBSCRIPTION_INPUT -> ModeSpec(
            foxWidthFrac = 0.62f,
            foxCenterXFrac = 0.54f,
            foxCenterYFrac = 0.62f,
            planetOverflow = 1.18f,
            planetCenterXFrac = 0.43f,
            planetCenterYFrac = 0.58f,
            planetAlpha = 0.94f,
            glowAlpha = 0.24f,
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
        val overflow = spec.planetOverflow
        val diameter = hostW * overflow
        val scale = diameter / drawableW
        val cx = hostW * spec.planetCenterXFrac
        val cy = hostH * spec.planetCenterYFrac
        val left = cx - diameter / 2f
        val top = cy - drawableH * scale / 2f
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
            return FoxLayout(1, 1, 0, 0, 0f, 0f, HOME.foxWidthFraction)
        }
        val spec = spec(mode)
        val aspect = if (drawableH > 0f) drawableW / drawableH else FOX_ASPECT
        val widthFrac = spec.foxWidthFrac
        val width = hostW * widthFrac
        val height = width / aspect
        val cx = hostW * spec.foxCenterXFrac
        val cy = hostH * spec.foxCenterYFrac
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
        val bottomLimit = if (isHome(mode)) hostH * 0.70f else hostH + 1f
        return layout.left >= -1 &&
            layout.left + layout.widthPx <= hostW + 1f &&
            layout.top >= -1 &&
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
        return planet.diameter > hostW
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
