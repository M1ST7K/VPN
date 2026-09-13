package com.v2ray.ang.ui

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.v2ray.ang.R
import com.v2ray.ang.vpn.HotfoxMotion

enum class HotFoxHeroMode {
    SPLASH,
    HOME_DISCONNECTED,
    HOME_CONNECTING,
    HOME_CONNECTED,
    SUBSCRIPTION,
    SUBSCRIPTION_INPUT,
}

/**
 * Full-screen fox + planet hero layer.
 *
 * Not a banner, card, circle, or Column row. Screens overlay UI on top of this view.
 */
open class HotFoxHeroArtwork @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : FrameLayout(context, attrs, defStyleAttr) {

    enum class AnimationMode { NONE, BREATHING }

    private val glow: ImageView
    private val planet: ImageView
    private val fox: ImageView
    private val blendBottom: View
    private val navScrim: View
    private var mode = HotFoxHeroMode.HOME_DISCONNECTED
    private var animationMode = AnimationMode.NONE
    private var backdrop = false
    private var breath: AnimatorSet? = null
    private var insetTopPx = 0
    private var insetBottomPx = 0

    init {
        clipChildren = false
        clipToPadding = false
        clipToOutline = false
        importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO
        isClickable = false
        isFocusable = false
        inflate(context, R.layout.view_hotfox_hero, this)
        glow = findViewById(R.id.img_art_glow)
        planet = findViewById(R.id.img_art_planet)
        fox = findViewById(R.id.img_art_bust)
        blendBottom = findViewById(R.id.hero_blend_bottom)
        navScrim = findViewById(R.id.hero_nav_scrim)
        planet.scaleType = ImageView.ScaleType.FIT_CENTER
        fox.scaleType = ImageView.ScaleType.FIT_CENTER
        fox.adjustViewBounds = false
        fox.cropToPadding = false
        fox.setPadding(0, 0, 0, 0)
        if (attrs != null) {
            val ta = context.obtainStyledAttributes(attrs, R.styleable.HotFoxHeroArtwork, defStyleAttr, 0)
            mode = modeFromAttr(ta.getInt(R.styleable.HotFoxHeroArtwork_hfHeroVariant, 3))
            fox.visibility = if (ta.getBoolean(R.styleable.HotFoxHeroArtwork_hfHeroShowFox, true)) {
                VISIBLE
            } else {
                GONE
            }
            planet.visibility = if (ta.getBoolean(R.styleable.HotFoxHeroArtwork_hfHeroShowPlanet, true)) {
                VISIBLE
            } else {
                GONE
            }
            glow.visibility = planet.visibility
            blendBottom.visibility = VISIBLE
            navScrim.visibility = VISIBLE
            if (ta.getBoolean(R.styleable.HotFoxHeroArtwork_hfHeroAnimate, false)) {
                animationMode = AnimationMode.BREATHING
            }
            backdrop = ta.getBoolean(R.styleable.HotFoxHeroArtwork_hfHeroBackdrop, false)
            if (backdrop) {
                fox.visibility = GONE
                planet.visibility = VISIBLE
                glow.visibility = VISIBLE
                blendBottom.visibility = GONE
                navScrim.visibility = GONE
                clipChildren = false
                clipToPadding = false
            } else {
                clipChildren = ta.getBoolean(R.styleable.HotFoxHeroArtwork_hfHeroClip, false)
                clipToPadding = clipChildren
            }
            ta.recycle()
        }
        ensureArtworkLoaded()
        ViewCompat.setOnApplyWindowInsetsListener(this) { _, insets ->
            val bars = insets.getInsets(
                WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout(),
            )
            insetTopPx = bars.top
            insetBottomPx = bars.bottom
            requestLayout()
            insets
        }
    }

    fun setMode(next: HotFoxHeroMode) {
        if (mode == next) return
        mode = next
        requestLayout()
    }

    fun setVariant(next: HotfoxHeroComposition.Variant) {
        setMode(next.toMode())
    }

    fun setShowFox(show: Boolean) {
        if (backdrop) {
            fox.visibility = GONE
            return
        }
        fox.visibility = if (show) VISIBLE else GONE
        ensureArtworkLoaded()
    }

    fun setShowPlanet(show: Boolean) {
        val vis = if (show) VISIBLE else GONE
        planet.visibility = vis
        glow.visibility = vis
        ensureArtworkLoaded()
        // Fade/nav scrims stay up so controls and navigation never sit on raw artwork.
    }

    fun setHeroLayers(showPlanet: Boolean, showFox: Boolean) {
        setShowPlanet(showPlanet)
        setShowFox(showFox)
    }

    private fun ensureArtworkLoaded() {
        if (planet.visibility == VISIBLE && planet.drawable == null) {
            planet.setImageResource(R.drawable.hf_native_planet_sphere)
        }
        if (!backdrop && fox.visibility == VISIBLE && fox.drawable == null) {
            fox.setImageResource(R.drawable.hotfox_fox_master)
        }
    }

    fun setAnimationMode(mode: AnimationMode) {
        animationMode = mode
        if (isAttachedToWindow) restartBreath()
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        ensureArtworkLoaded()
        restartBreath()
        requestApplyInsets()
        requestLayout()
    }

    override fun onDetachedFromWindow() {
        breath?.cancel()
        breath = null
        fox.translationY = 0f
        fox.scaleX = 1f
        fox.scaleY = 1f
        super.onDetachedFromWindow()
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        layoutHero(right - left, bottom - top)
    }

    private fun layoutHero(w: Int, h: Int) {
        if (w < 2 || h < 2) return
        ensureArtworkLoaded()
        if (backdrop) {
            layoutBackdrop(w, h)
            return
        }
        val home = HotfoxHeroComposition.isHome(mode)
        clipChildren = false
        clipToPadding = false
        clipToOutline = false
        val wf = w.toFloat()
        val hf = h.toFloat()
        val loc = IntArray(2)
        getLocationOnScreen(loc)
        val hostW: Float
        val hostH: Float
        val originX: Int
        val originY: Int
        if (home) {
            // Compose the bust against the phone canvas so it fills the
            // headline→CTA band like the 05/06/07 references, not the short HeroSlot.
            hostW = resources.displayMetrics.widthPixels.toFloat().coerceAtLeast(wf)
            hostH = resources.displayMetrics.heightPixels.toFloat().coerceAtLeast(hf)
            originX = loc[0]
            originY = loc[1]
            navScrim.visibility = GONE
        } else {
            hostW = resources.displayMetrics.widthPixels.toFloat().coerceAtLeast(wf)
            hostH = resources.displayMetrics.heightPixels.toFloat().coerceAtLeast(hf)
            originX = loc[0]
            originY = loc[1]
            navScrim.visibility = VISIBLE
        }
        if (planet.visibility == VISIBLE) {
            val planetDrawable = planet.drawable
            val dw = planetDrawable?.intrinsicWidth?.toFloat()?.takeIf { it > 0f } ?: 1024f
            val dh = planetDrawable?.intrinsicHeight?.toFloat()?.takeIf { it > 0f } ?: 1024f
            val planetLayout = HotfoxHeroComposition.planet(hostW, hostH, dw, dh, mode)
            planet.alpha = planetLayout.alpha
            planet.scaleType = ImageView.ScaleType.FIT_CENTER
            val pd = planetLayout.diameter.toInt().coerceAtLeast(1)
            place(planet, planetLayout.left - originX, planetLayout.top - originY, pd, pd)

            val glowLayout = HotfoxHeroComposition.glow(hostW, hostH, mode)
            glow.alpha = glowLayout.alpha
            place(
                glow,
                glowLayout.left - originX,
                glowLayout.top - originY,
                glowLayout.diameter,
                glowLayout.diameter,
            )
        } else {
            place(planet, 0, 0, 1, 1)
            place(glow, 0, 0, 1, 1)
        }

        val foxDrawable = fox.drawable
        val foxDw = foxDrawable?.intrinsicWidth?.toFloat()?.takeIf { it > 0f }
            ?: HotfoxHeroComposition.FOX_INTRINSIC_WIDTH
        val foxDh = foxDrawable?.intrinsicHeight?.toFloat()?.takeIf { it > 0f }
            ?: HotfoxHeroComposition.FOX_INTRINSIC_HEIGHT
        val foxLayout = HotfoxHeroComposition.fox(hostW, hostH, mode, drawableW = foxDw, drawableH = foxDh)
        fox.setPadding(0, 0, 0, 0)
        fox.scaleType = ImageView.ScaleType.FIT_CENTER
        fox.adjustViewBounds = false
        place(
            fox,
            foxLayout.left - originX,
            foxLayout.top - originY,
            foxLayout.widthPx,
            foxLayout.heightPx,
        )

        val blendH = if (home) {
            1
        } else {
            (h * 0.42f).toInt().coerceAtLeast(1)
        }
        if (home) {
            blendBottom.visibility = GONE
            place(blendBottom, 0, h, w, 1)
        } else {
            blendBottom.visibility = VISIBLE
            place(blendBottom, 0, h - blendH, w, blendH)
        }
        if (!home) {
            val navH = (h * 0.12f).toInt().coerceAtLeast(1)
            place(navScrim, 0, h - navH, w, navH)
        } else {
            place(navScrim, 0, h, w, 1)
        }
    }

    private fun layoutBackdrop(w: Int, h: Int) {
        clipChildren = false
        clipToPadding = false
        fox.visibility = GONE
        planet.visibility = VISIBLE
        glow.visibility = VISIBLE
        blendBottom.visibility = GONE
        navScrim.visibility = GONE
        val planetDrawable = planet.drawable
        val dw = planetDrawable?.intrinsicWidth?.toFloat()?.takeIf { it > 0f } ?: 1024f
        val dh = planetDrawable?.intrinsicHeight?.toFloat()?.takeIf { it > 0f } ?: 1024f
        val planetLayout = HotfoxHeroComposition.fullscreenPlanet(w.toFloat(), h.toFloat(), dw, dh)
        planet.alpha = planetLayout.alpha
        planet.scaleType = ImageView.ScaleType.FIT_CENTER
        val pd = planetLayout.diameter.toInt().coerceAtLeast(1)
        place(planet, planetLayout.left, planetLayout.top, pd, pd)
        val glowLayout = HotfoxHeroComposition.fullscreenGlow(w.toFloat(), h.toFloat())
        glow.alpha = glowLayout.alpha
        place(glow, glowLayout.left, glowLayout.top, glowLayout.diameter, glowLayout.diameter)
        place(blendBottom, 0, h, w, 1)
        place(navScrim, 0, h, w, 1)
    }

    private fun place(view: View, x: Int, y: Int, widthPx: Int, heightPx: Int) {
        val vw = widthPx.coerceAtLeast(1)
        val vh = heightPx.coerceAtLeast(1)
        view.measure(
            MeasureSpec.makeMeasureSpec(vw, MeasureSpec.EXACTLY),
            MeasureSpec.makeMeasureSpec(vh, MeasureSpec.EXACTLY),
        )
        view.layout(x, y, x + vw, y + vh)
        view.translationX = 0f
        view.translationY = 0f
    }

    private fun restartBreath() {
        breath?.cancel()
        breath = null
        fox.translationY = 0f
        fox.scaleX = 1f
        fox.scaleY = 1f
        if (animationMode != AnimationMode.BREATHING) return
        if (HotfoxHeroComposition.isHome(mode)) return
        if (isInEditMode) return
        if (HotfoxMotion.reducedMotion(context)) return
        val drift = resources.displayMetrics.density * 8f
        val move = ObjectAnimator.ofFloat(fox, "translationY", 0f, -drift).apply {
            duration = 2400L
            repeatMode = ValueAnimator.REVERSE
            repeatCount = ValueAnimator.INFINITE
            interpolator = AccelerateDecelerateInterpolator()
        }
        val sx = ObjectAnimator.ofFloat(fox, "scaleX", 1f, 1.015f).apply {
            duration = 2400L
            repeatMode = ValueAnimator.REVERSE
            repeatCount = ValueAnimator.INFINITE
            interpolator = AccelerateDecelerateInterpolator()
        }
        val sy = ObjectAnimator.ofFloat(fox, "scaleY", 1f, 1.015f).apply {
            duration = 2400L
            repeatMode = ValueAnimator.REVERSE
            repeatCount = ValueAnimator.INFINITE
            interpolator = AccelerateDecelerateInterpolator()
        }
        breath = AnimatorSet().apply {
            playTogether(move, sx, sy)
            start()
        }
    }

    companion object {
        fun modeFromAttr(value: Int): HotFoxHeroMode = when (value) {
            0 -> HotFoxHeroMode.SPLASH
            2, 7 -> HotFoxHeroMode.SUBSCRIPTION_INPUT
            4 -> HotFoxHeroMode.HOME_CONNECTING
            5 -> HotFoxHeroMode.HOME_CONNECTED
            6 -> HotFoxHeroMode.SUBSCRIPTION
            else -> HotFoxHeroMode.HOME_DISCONNECTED
        }
    }
}
