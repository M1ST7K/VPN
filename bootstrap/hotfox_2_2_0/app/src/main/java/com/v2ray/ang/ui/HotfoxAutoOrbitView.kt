package com.v2ray.ang.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import com.v2ray.ang.R
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/**
 * Native scalable artwork for onboarding AUTO.
 *
 * Intentionally drawn from Canvas geometry instead of a fixed-size raster
 * orbit/globe pair so it stays crisp at every density.
 * Decorative only: no product state or server data is encoded here.
 */
class HotfoxAutoOrbitView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : View(context, attrs, defStyleAttr) {

    private val density = resources.displayMetrics.density
    private val orange = ContextCompat.getColor(context, R.color.hf_v13_orange)
    private val orangeWarm = ContextCompat.getColor(context, R.color.hf_v13_orange_start)

    private val orbitPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }

    private val nodeRingPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = dp(1.1f)
    }

    private val nodeFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val globePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }

    private val globeGlowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }

    private val globeFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val auraPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val globeClip = Path()
    private val rect = RectF()

    init {
        importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO
        isClickable = false
        isFocusable = false
        setWillNotDraw(false)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (width <= 0 || height <= 0) return

        val cx = width * 0.5f
        val cy = height * 0.50f
        val available = min(width.toFloat(), height.toFloat())
        val minGroup = dp(220f)
        val maxGroup = dp(340f)
        val group = (available * 0.86f)
            .coerceAtMost(maxGroup)
            .let { if (available >= minGroup) it.coerceAtLeast(minGroup) else available * 0.94f }

        auraPaint.color = withAlpha(orange, 16)
        canvas.drawCircle(cx, cy, group * 0.22f, auraPaint)

        drawOrbit(canvas, cx, cy, group * 0.50f, yScale = 0.86f, alpha = 25, strokeDp = 0.85f)
        drawOrbit(canvas, cx, cy, group * 0.39f, yScale = 0.92f, alpha = 30, strokeDp = 0.9f)
        drawOrbit(canvas, cx, cy, group * 0.28f, yScale = 0.97f, alpha = 36, strokeDp = 0.95f)

        drawNode(canvas, cx, cy, group * 0.50f, 0.86f, 214f, group)
        drawNode(canvas, cx, cy, group * 0.50f, 0.86f, 28f, group)
        drawNode(canvas, cx, cy, group * 0.39f, 0.92f, 138f, group)
        drawNode(canvas, cx, cy, group * 0.39f, 0.92f, 328f, group)

        val globeRadius = (group * 0.155f)
            .coerceIn(dp(41f), dp(54f))
        drawGlobe(canvas, cx, cy, globeRadius)
    }

    private fun drawOrbit(
        canvas: Canvas,
        cx: Float,
        cy: Float,
        radius: Float,
        yScale: Float,
        alpha: Int,
        strokeDp: Float,
    ) {
        orbitPaint.color = withAlpha(orangeWarm, alpha)
        orbitPaint.strokeWidth = dp(strokeDp)
        rect.set(cx - radius, cy - radius * yScale, cx + radius, cy + radius * yScale)
        canvas.drawOval(rect, orbitPaint)
    }

    private fun drawNode(
        canvas: Canvas,
        cx: Float,
        cy: Float,
        radius: Float,
        yScale: Float,
        angleDegrees: Float,
        group: Float,
    ) {
        val radians = Math.toRadians(angleDegrees.toDouble())
        val x = cx + cos(radians).toFloat() * radius
        val y = cy + sin(radians).toFloat() * radius * yScale

        val halo = (group * 0.044f).coerceIn(dp(10f), dp(17f))
        val core = (group * 0.018f).coerceIn(dp(4f), dp(7f))

        nodeFillPaint.color = withAlpha(orangeWarm, 13)
        canvas.drawCircle(x, y, halo, nodeFillPaint)

        nodeRingPaint.color = withAlpha(orangeWarm, 52)
        canvas.drawCircle(x, y, halo * 0.76f, nodeRingPaint)

        nodeFillPaint.color = withAlpha(orange, 184)
        canvas.drawCircle(x, y, core, nodeFillPaint)
    }

    private fun drawGlobe(canvas: Canvas, cx: Float, cy: Float, r: Float) {
        globeGlowPaint.color = withAlpha(orange, 36)
        globeGlowPaint.strokeWidth = dp(7.5f)
        canvas.drawCircle(cx, cy, r + dp(1.2f), globeGlowPaint)

        globeFillPaint.color = withAlpha(orange, 22)
        canvas.drawCircle(cx, cy, r, globeFillPaint)

        globePaint.color = withAlpha(orange, 250)
        globePaint.strokeWidth = dp(3.2f)
        canvas.drawCircle(cx, cy, r, globePaint)

        globeClip.reset()
        globeClip.addCircle(cx, cy, r - dp(1.2f), Path.Direction.CW)
        val save = canvas.save()
        canvas.clipPath(globeClip)

        globePaint.strokeWidth = dp(1.85f)
        globePaint.color = withAlpha(orangeWarm, 190)
        canvas.drawLine(cx, cy - r, cx, cy + r, globePaint)

        rect.set(cx - r * 0.58f, cy - r, cx + r * 0.58f, cy + r)
        canvas.drawOval(rect, globePaint)

        globePaint.strokeWidth = dp(2.1f)
        globePaint.color = withAlpha(orange, 220)
        canvas.drawLine(cx - r, cy, cx + r, cy, globePaint)
        globePaint.strokeWidth = dp(1.75f)
        globePaint.color = withAlpha(orangeWarm, 176)
        val lat = r * 0.42f
        rect.set(cx - r, cy - lat - r * 0.08f, cx + r, cy - lat + r * 0.08f)
        canvas.drawOval(rect, globePaint)
        rect.set(cx - r, cy + lat - r * 0.08f, cx + r, cy + lat + r * 0.08f)
        canvas.drawOval(rect, globePaint)

        canvas.restoreToCount(save)

        globePaint.strokeWidth = dp(3.2f)
        globePaint.color = withAlpha(orange, 255)
        canvas.drawCircle(cx, cy, r, globePaint)
    }

    private fun dp(value: Float): Float = value * density

    private fun withAlpha(color: Int, alpha: Int): Int {
        return (color and 0x00FFFFFF) or (alpha.coerceIn(0, 255) shl 24)
    }
}
