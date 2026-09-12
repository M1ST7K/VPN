package com.v2ray.ang.ui

import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Matrix
import android.util.AttributeSet
import android.view.Gravity
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.FrameLayout
import android.widget.ImageView
import com.v2ray.ang.R
import com.v2ray.ang.vpn.HotfoxMotion

/**
 * Full-bleed fox + planet hero. Not a round card: the planet sphere is
 * allowed to overflow, while the fox is fit-centered and never cropped.
 */
class HotfoxHeroView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : FrameLayout(context, attrs, defStyleAttr) {

    enum class AnimationMode { NONE, BREATHING }

    private val planet: ImageView
    private val fox: ImageView
    private val planetMatrix = Matrix()
    private var variant = HotfoxHeroComposition.Variant.PAGE
    private var animationMode = AnimationMode.NONE
    private var breath: ObjectAnimator? = null
    private var applying = false

    init {
        clipChildren = false
        clipToPadding = false
        clipToOutline = false
        importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO
        inflate(context, R.layout.view_hotfox_hero, this)
        planet = findViewById(R.id.img_art_planet)
        fox = findViewById(R.id.img_art_bust)
        planet.scaleType = ImageView.ScaleType.MATRIX
        fox.scaleType = ImageView.ScaleType.FIT_CENTER
        fox.adjustViewBounds = true
        if (attrs != null) {
            val ta = context.obtainStyledAttributes(attrs, R.styleable.HotfoxHeroView, defStyleAttr, 0)
            variant = when (ta.getInt(R.styleable.HotfoxHeroView_hfHeroVariant, 1)) {
                0 -> HotfoxHeroComposition.Variant.SPLASH
                2 -> HotfoxHeroComposition.Variant.SUPPORT
                else -> HotfoxHeroComposition.Variant.PAGE
            }
            fox.visibility = if (ta.getBoolean(R.styleable.HotfoxHeroView_hfHeroShowFox, true)) {
                VISIBLE
            } else {
                GONE
            }
            planet.visibility = if (ta.getBoolean(R.styleable.HotfoxHeroView_hfHeroShowPlanet, true)) {
                VISIBLE
            } else {
                GONE
            }
            if (ta.getBoolean(R.styleable.HotfoxHeroView_hfHeroAnimate, variant != HotfoxHeroComposition.Variant.SUPPORT)) {
                animationMode = AnimationMode.BREATHING
            }
            ta.recycle()
        }
        planet.setImageResource(R.drawable.hf_native_planet_sphere)
        fox.setImageResource(R.drawable.hf_fox_bust_transparent)
    }

    fun setVariant(next: HotfoxHeroComposition.Variant) {
        if (variant == next) return
        variant = next
        applyComposition()
    }

    fun setShowFox(show: Boolean) {
        fox.visibility = if (show) VISIBLE else GONE
    }

    fun setShowPlanet(show: Boolean) {
        planet.visibility = if (show) VISIBLE else GONE
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
        applyComposition()
        restartBreath()
    }

    override fun onDetachedFromWindow() {
        breath?.cancel()
        breath = null
        fox.translationY = 0f
        super.onDetachedFromWindow()
    }

    private fun applyComposition() {
        val w = width.toFloat()
        val h = height.toFloat()
        if (w < 2f || h < 2f || applying) return
        val drawable = planet.drawable ?: return
        val dw = drawable.intrinsicWidth.toFloat()
        val dh = drawable.intrinsicHeight.toFloat()
        if (dw <= 0f || dh <= 0f) return
        applying = true
        val transform = HotfoxHeroComposition.planet(w, h, dw, dh, variant)
        planetMatrix.reset()
        planetMatrix.setScale(transform.scale, transform.scale)
        planetMatrix.postTranslate(transform.translateX, transform.translateY)
        planet.scaleType = ImageView.ScaleType.MATRIX
        planet.imageMatrix = planetMatrix
        planet.alpha = transform.alpha

        val foxLayout = HotfoxHeroComposition.fox(w, h, variant)
        val lp = fox.layoutParams as LayoutParams
        val gravity = if (foxLayout.centerVertically) {
            Gravity.CENTER
        } else {
            Gravity.CENTER_HORIZONTAL or Gravity.BOTTOM
        }
        if (lp.width != foxLayout.sizePx ||
            lp.height != foxLayout.sizePx ||
            lp.bottomMargin != foxLayout.bottomMarginPx ||
            lp.gravity != gravity
        ) {
            lp.width = foxLayout.sizePx
            lp.height = foxLayout.sizePx
            lp.bottomMargin = foxLayout.bottomMarginPx
            lp.gravity = gravity
            fox.layoutParams = lp
        }
        fox.scaleType = ImageView.ScaleType.FIT_CENTER
        applying = false
    }

    private fun restartBreath() {
        breath?.cancel()
        breath = null
        fox.translationY = 0f
        if (animationMode != AnimationMode.BREATHING) return
        if (isInEditMode) return
        if (HotfoxMotion.reducedMotion(context)) return
        breath = ObjectAnimator.ofFloat(fox, "translationY", 0f, -3.5f).apply {
            duration = 3200L
            repeatMode = ValueAnimator.REVERSE
            repeatCount = ValueAnimator.INFINITE
            interpolator = AccelerateDecelerateInterpolator()
            start()
        }
    }
}
