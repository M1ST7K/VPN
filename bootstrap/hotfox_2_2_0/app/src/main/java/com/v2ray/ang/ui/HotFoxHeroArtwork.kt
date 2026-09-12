package com.v2ray.ang.ui

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
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
    private var mode = HotFoxHeroMode.HOME_DISCONNECTED
    private var animationMode = AnimationMode.NONE
    private var breath: AnimatorSet? = null
    private var insetTopPx = 0
    private var insetBottomPx = 0
    private var lastW = 0
    private var lastH = 0
    private var lastMode: HotFoxHeroMode? = null
    private var lastInsetTop = -1
    private var lastInsetBottom = -1

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
            blendBottom.visibility = planet.visibility
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
            applyComposition(force = true)
            insets
        }
    }

    fun setMode(next: HotFoxHeroMode) {
        if (mode == next) return
        mode = next
        applyComposition(force = true)
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
        blendBottom.visibility = vis
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
        applyComposition()
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        applyComposition(force = true)
        restartBreath()
        requestApplyInsets()
    }

    override fun onDetachedFromWindow() {
        breath?.cancel()
        breath = null
        fox.translationY = 0f
        fox.scaleX = 1f
        fox.scaleY = 1f
        super.onDetachedFromWindow()
    }

    private fun applyComposition(force: Boolean = false) {
        val w = width
        val h = height
        if (w < 2 || h < 2) return
        val insetTop = insetTopPx.toFloat()
        val insetBottom = paddingBottom.toFloat()
        if (
            !force &&
            w == lastW &&
            h == lastH &&
            mode == lastMode &&
            insetTopPx == lastInsetTop &&
            insetBottomPx == lastInsetBottom
        ) {
            return
        }
        val wf = w.toFloat()
        val hf = h.toFloat()
        val planetDrawable = planet.drawable
        val dw = planetDrawable?.intrinsicWidth?.toFloat()?.takeIf { it > 0f } ?: 1024f
        val dh = planetDrawable?.intrinsicHeight?.toFloat()?.takeIf { it > 0f } ?: 1024f
        val planetLayout = HotfoxHeroComposition.planet(
            wf, hf, dw, dh, mode, insetTop, insetBottom,
        )
        layoutLayer(
            planet,
            planetLayout.diameter.toInt().coerceAtLeast(1),
            planetLayout.diameter.toInt().coerceAtLeast(1),
            planetLayout.left,
            planetLayout.top,
        )
        planet.alpha = planetLayout.alpha
        planet.scaleType = ImageView.ScaleType.FIT_CENTER

        val glowLayout = HotfoxHeroComposition.glow(wf, hf, mode, insetTop, insetBottom)
        layoutLayer(glow, glowLayout.diameter, glowLayout.diameter, glowLayout.left, glowLayout.top)
        glow.alpha = glowLayout.alpha

        val foxDrawable = fox.drawable
        val foxDw = foxDrawable?.intrinsicWidth?.toFloat()?.takeIf { it > 0f }
            ?: HotfoxHeroComposition.FOX_INTRINSIC_WIDTH
        val foxDh = foxDrawable?.intrinsicHeight?.toFloat()?.takeIf { it > 0f }
            ?: HotfoxHeroComposition.FOX_INTRINSIC_HEIGHT
        val foxLayout = HotfoxHeroComposition.fox(
            wf, hf, mode, insetTop, insetBottom, foxDw, foxDh,
        )
        fox.setPadding(0, 0, 0, 0)
        fox.scaleType = ImageView.ScaleType.FIT_CENTER
        fox.adjustViewBounds = false
        layoutLayer(fox, foxLayout.widthPx, foxLayout.heightPx, foxLayout.left, foxLayout.top)
        fox.bringToFront()

        val blendH = (h * 0.38f).toInt().coerceAtLeast(1)
        val blendLp = blendBottom.layoutParams as LayoutParams
        if (blendLp.height != blendH || blendLp.gravity != Gravity.BOTTOM) {
            blendLp.width = LayoutParams.MATCH_PARENT
            blendLp.height = blendH
            blendLp.gravity = Gravity.BOTTOM
            blendBottom.layoutParams = blendLp
        }

        lastW = w
        lastH = h
        lastMode = mode
        lastInsetTop = insetTopPx
        lastInsetBottom = insetBottomPx
    }

    private fun layoutLayer(view: View, widthPx: Int, heightPx: Int, left: Int, top: Int) {
        val lp = view.layoutParams as LayoutParams
        lp.width = widthPx
        lp.height = heightPx
        lp.gravity = Gravity.TOP or Gravity.START
        lp.leftMargin = 0
        lp.rightMargin = 0
        lp.marginStart = left
        lp.marginEnd = 0
        lp.topMargin = top
        lp.bottomMargin = 0
        view.layoutParams = lp
    }

    private fun restartBreath() {
        breath?.cancel()
        breath = null
        fox.translationY = 0f
        fox.scaleX = 1f
        fox.scaleY = 1f
        if (animationMode != AnimationMode.BREATHING) return
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
