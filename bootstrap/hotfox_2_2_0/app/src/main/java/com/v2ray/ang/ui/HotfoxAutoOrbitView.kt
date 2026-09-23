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
 * Intentionally drawn from vector geometry instead of the old raster
 * hf_auto_orbits + hf_globe_orange pair so it stays crisp at every density.
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

        drawOrbit(canvas, cx, cy, group * 0.50f, alpha = 34, strokeDp = 0.95f)
        drawOrbit(canvas, cx, cy, group * 0.36f, alpha = 28, strokeDp = 0.85f)
        drawOrbit(canvas, cx, cy, group * 0.23f, alpha = 23, strokeDp = 0.8f)

        drawNode(canvas, cx, cy, group * 0.50f, 214f, group)
        drawNode(canvas, cx, cy, group * 0.50f, 24f, group)
        drawNode(canvas, cx, cy, group * 0.36f, 142f, group)
        drawNode(canvas, cx, cy, group * 0.36f, 332f, group)

        val globeRadius = (group * 0.145f)
            .coerceIn(dp(39f), dp(56f))
        drawGlobe(canvas, cx, cy, globeRadius)
    }

    private fun drawOrbit(
        canvas: Canvas,
        cx: Float,
        cy: Float,
        radius: Float,
        alpha: Int,
        strokeDp: Float,
    ) {
        orbitPaint.color = withAlpha(orangeWarm, alpha)
        orbitPaint.strokeWidth = dp(strokeDp)
        canvas.drawCircle(cx, cy, radius, orbitPaint)
    }

    private fun drawNode(
        canvas: Canvas,
        cx: Float,
        cy: Float,
        radius: Float,
        angleDegrees: Float,
        group: Float,
    ) {
        val radians = Math.toRadians(angleDegrees.toDouble())
        val x = cx + cos(radians).toFloat() * radius
        val y = cy + sin(radians).toFloat() * radius

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
        globeGlowPaint.color = withAlpha(orange, 34)
        globeGlowPaint.strokeWidth = dp(8f)
        canvas.drawCircle(cx, cy, r + dp(1.5f), globeGlowPaint)

        globePaint.color = withAlpha(orange, 244)
        globePaint.strokeWidth = dp(3.1f)
        canvas.drawCircle(cx, cy, r, globePaint)

        globeClip.reset()
        globeClip.addCircle(cx, cy, r - dp(1.5f), Path.Direction.CW)

        val save = canvas.save()
        canvas.clipPath(globeClip)

        globePaint.strokeWidth = dp(2.35f)
        globePaint.color = withAlpha(orange, 224)

        rect.set(cx - r * 0.54f, cy - r, cx + r * 0.54f, cy + r)
        canvas.drawOval(rect, globePaint)

        rect.set(cx - r, cy - r * 0.47f, cx + r, cy + r * 0.47f)
        canvas.drawOval(rect, globePaint)

        canvas.drawLine(cx - r, cy, cx + r, cy, globePaint)
        canvas.restoreToCount(save)

        // Reassert the outer contour after clipping internal curves.
        globePaint.strokeWidth = dp(3.1f)
        globePaint.color = withAlpha(orange, 250)
        canvas.drawCircle(cx, cy, r, globePaint)
    }

    private fun dp(value: Float): Float = value * density

    private fun withAlpha(color: Int, alpha: Int): Int {
        return (color and 0x00FFFFFF) or (alpha.coerceIn(0, 255) shl 24)
    }
}
