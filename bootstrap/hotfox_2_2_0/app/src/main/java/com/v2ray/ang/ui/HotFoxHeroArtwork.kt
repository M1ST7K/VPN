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
            // Clip is never allowed on the fox layer, even if XML still passes true.
            ta.getBoolean(R.styleable.HotFoxHeroArtwork_hfHeroClip, false)
            ta.recycle()
        }
        clipChildren = false
        clipToPadding = false
        planet.setImageResource(R.drawable.hf_native_planet_sphere)
        fox.setImageResource(R.drawable.hotfox_fox_master)
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
        fox.visibility = if (show) VISIBLE else GONE
    }

    fun setShowPlanet(show: Boolean) {
        val vis = if (show) VISIBLE else GONE
        planet.visibility = vis
        glow.visibility = vis
        // Fade/nav scrims stay up so controls and navigation never sit on raw artwork.
    }

    fun setHeroLayers(showPlanet: Boolean, showFox: Boolean) {
        setShowPlanet(showPlanet)
        setShowFox(showFox)
    }

    fun setAnimationMode(mode: AnimationMode) {
        animationMode = mode
        if (isAttachedToWindow) restartBreath()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        requestLayout()
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
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
        val wf = w.toFloat()
        val hf = h.toFloat()
        val loc = IntArray(2)
        getLocationOnScreen(loc)
        val screenW = resources.displayMetrics.widthPixels.toFloat().coerceAtLeast(wf)
        val screenH = resources.displayMetrics.heightPixels.toFloat().coerceAtLeast(hf)
        val planetDrawable = planet.drawable
        val dw = planetDrawable?.intrinsicWidth?.toFloat()?.takeIf { it > 0f } ?: 1024f
        val dh = planetDrawable?.intrinsicHeight?.toFloat()?.takeIf { it > 0f } ?: 1024f
        val planetLayout = HotfoxHeroComposition.planet(screenW, screenH, dw, dh, mode)
        planet.alpha = planetLayout.alpha
        planet.scaleType = ImageView.ScaleType.FIT_CENTER
        val pd = planetLayout.diameter.toInt().coerceAtLeast(1)
        place(planet, planetLayout.left - loc[0], planetLayout.top - loc[1], pd, pd)

        val glowLayout = HotfoxHeroComposition.glow(screenW, screenH, mode)
        glow.alpha = glowLayout.alpha
        place(
            glow,
            glowLayout.left - loc[0],
            glowLayout.top - loc[1],
            glowLayout.diameter,
            glowLayout.diameter,
        )

        val foxDrawable = fox.drawable
        val foxDw = foxDrawable?.intrinsicWidth?.toFloat()?.takeIf { it > 0f }
            ?: HotfoxHeroComposition.FOX_INTRINSIC_WIDTH
        val foxDh = foxDrawable?.intrinsicHeight?.toFloat()?.takeIf { it > 0f }
            ?: HotfoxHeroComposition.FOX_INTRINSIC_HEIGHT
        val foxLayout = HotfoxHeroComposition.fox(screenW, screenH, mode, drawableW = foxDw, drawableH = foxDh)
        fox.setPadding(0, 0, 0, 0)
        fox.scaleType = ImageView.ScaleType.FIT_CENTER
        fox.adjustViewBounds = false
        place(
            fox,
            foxLayout.left - loc[0],
            foxLayout.top - loc[1],
            foxLayout.widthPx,
            foxLayout.heightPx,
        )

        val blendH = if (HotfoxHeroComposition.isHome(mode)) {
            (h * 0.48f).toInt().coerceAtLeast(1)
        } else {
            (h * 0.42f).toInt().coerceAtLeast(1)
        }
        place(blendBottom, 0, h - blendH, w, blendH)
        val navH = (h * 0.12f).toInt().coerceAtLeast(1)
        place(navScrim, 0, h - navH, w, navH)
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
