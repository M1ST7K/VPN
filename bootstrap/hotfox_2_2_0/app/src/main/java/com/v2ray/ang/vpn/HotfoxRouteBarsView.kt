package com.v2ray.ang.vpn

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import com.v2ray.ang.R

/**
 * Editorial route visualization: thin vertical bars, taller orange accent at VPN.
 * State-aware; does not invent telemetry.
 */
class HotfoxRouteBarsView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : View(context, attrs, defStyleAttr) {

    enum class Visual { IDLE, CONNECTING, CONNECTED, ERROR }

    private val cream = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val orange = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private var visual = Visual.IDLE
    private var pulse = 0.55f
    private var pulseRising = true

    init {
        cream.color = ContextCompat.getColor(context, R.color.hotfox_editorial_text)
        orange.color = ContextCompat.getColor(context, R.color.hotfox_orange)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        if (!isInEditMode) post(pulseRunnable)
    }

    fun setVisual(next: Visual) {
        if (visual == next) return
        visual = next
        invalidate()
    }

    override fun onDetachedFromWindow() {
        removeCallbacks(pulseRunnable)
        super.onDetachedFromWindow()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 0f || h <= 0f) return
        val count = 28
        val gap = w / count
        val barWidth = (gap * 0.22f).coerceAtLeast(1.5f)
        val mid = count / 2
        val creamAlpha = when (visual) {
            Visual.CONNECTED -> 200
            Visual.CONNECTING -> (90 + 90 * pulse).toInt()
            Visual.ERROR -> 70
            Visual.IDLE -> 120
        }
        cream.alpha = creamAlpha
        orange.alpha = when (visual) {
            Visual.CONNECTED -> 255
            Visual.CONNECTING -> (140 + 100 * pulse).toInt()
            Visual.ERROR -> 80
            Visual.IDLE -> 160
        }
        for (i in 0 until count) {
            val cx = gap * i + gap / 2f
            val dist = kotlin.math.abs(i - mid).toFloat() / mid
            val base = if (i == mid) 0.92f else (0.22f + 0.28f * (1f - dist))
            val barH = h * base
            val top = (h - barH) / 2f
            val paint = if (i == mid) orange else cream
            canvas.drawRoundRect(
                cx - barWidth / 2f,
                top,
                cx + barWidth / 2f,
                top + barH,
                barWidth,
                barWidth,
                paint,
            )
        }
    }

    private val pulseRunnable = object : Runnable {
        override fun run() {
            if (visual == Visual.CONNECTING || visual == Visual.CONNECTED) {
                pulse += if (pulseRising) 0.04f else -0.04f
                if (pulse >= 1f) {
                    pulse = 1f
                    pulseRising = false
                } else if (pulse <= 0.35f) {
                    pulse = 0.35f
                    pulseRising = true
                }
                invalidate()
            }
            postDelayed(this, 40L)
        }
    }
}
