package com.diabeticcare.app.ui.widgets

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import com.diabeticcare.app.R

class RingProgressView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.surface_variant)
        style = Paint.Style.STROKE
        strokeWidth = 18f
        strokeCap = Paint.Cap.ROUND
    }
    private val progressPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.health_teal)
        style = Paint.Style.STROKE
        strokeWidth = 18f
        strokeCap = Paint.Cap.ROUND
    }
    private val valuePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.text_primary)
        textSize = 34f
        textAlign = Paint.Align.CENTER
        isFakeBoldText = true
    }
    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.text_secondary)
        textSize = 22f
        textAlign = Paint.Align.CENTER
    }

    private var progress = 0.65f
    private var centerText = "65%"
    private var labelText = "Progress"

    fun setRing(progressFraction: Float, value: String, label: String) {
        progress = progressFraction.coerceIn(0f, 1f)
        centerText = value
        labelText = label
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val pad = 18f
        val rect = RectF(pad, pad, width - pad, height - pad)
        canvas.drawArc(rect, -90f, 360f, false, trackPaint)
        canvas.drawArc(rect, -90f, 360f * progress, false, progressPaint)
        val cx = width / 2f
        val cy = height / 2f
        canvas.drawText(centerText, cx, cy - 2f, valuePaint)
        canvas.drawText(labelText, cx, cy + 34f, labelPaint)
    }
}
