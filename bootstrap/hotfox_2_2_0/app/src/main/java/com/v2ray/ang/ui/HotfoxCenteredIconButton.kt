package com.v2ray.ang.ui

import android.content.Context
import android.graphics.Canvas
import android.util.AttributeSet
import android.view.Gravity
import androidx.appcompat.widget.AppCompatButton

/**
 * Wide CTA whose start compound drawable and single-line label are centered as one group.
 * The background/ripple stays full width; only the content is shifted at draw time.
 */
class HotfoxCenteredIconButton @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = androidx.appcompat.R.attr.buttonStyle,
) : AppCompatButton(context, attrs, defStyleAttr) {

    init {
        gravity = Gravity.START or Gravity.CENTER_VERTICAL
        maxLines = 1
        isSingleLine = true
    }

    internal fun contentOffsetPx(): Float {
        val icon = compoundDrawablesRelative[0]
        val iconWidth = if (icon != null) icon.bounds.width() + compoundDrawablePadding else 0
        val groupWidth = iconWidth + paint.measureText(text?.toString().orEmpty())
        val freeWidth = width - paddingLeft - paddingRight - groupWidth
        if (freeWidth <= 0f) return 0f
        val desiredStart = paddingLeft + freeWidth / 2f
        val currentStart = if (layoutDirection == LAYOUT_DIRECTION_RTL) {
            width - paddingRight - groupWidth
        } else {
            paddingLeft.toFloat()
        }
        return desiredStart - currentStart
    }

    override fun onDraw(canvas: Canvas) {
        val offset = contentOffsetPx()
        if (offset == 0f) {
            super.onDraw(canvas)
            return
        }
        val save = canvas.save()
        canvas.translate(offset, 0f)
        super.onDraw(canvas)
        canvas.restoreToCount(save)
    }
}
