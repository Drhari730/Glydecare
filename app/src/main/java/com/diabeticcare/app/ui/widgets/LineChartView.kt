package com.diabeticcare.app.ui.widgets

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import com.diabeticcare.app.R

class LineChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.chart_grid)
        strokeWidth = 1.2f
    }
    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.health_teal)
        strokeWidth = 5f
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }
    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(35, 0, 90, 96)
        style = Paint.Style.FILL
    }
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.text_hint)
        textSize = 26f
        textAlign = Paint.Align.CENTER
    }

    private var points: List<Float> = emptyList()
    private var emptyLabel = "No trend yet"

    fun setPoints(values: List<Float>, label: String = "No trend yet") {
        points = values.takeLast(14)
        emptyLabel = label
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()
        val left = 20f
        val right = w - 20f
        val top = 18f
        val bottom = h - 24f

        repeat(4) { i ->
            val y = top + ((bottom - top) / 3f) * i
            canvas.drawLine(left, y, right, y, gridPaint)
        }

        if (points.size < 2) {
            canvas.drawText(emptyLabel, w / 2f, h / 2f + 8f, textPaint)
            return
        }

        val min = points.minOrNull() ?: 0f
        val max = points.maxOrNull() ?: 1f
        val range = (max - min).takeIf { it > 0f } ?: 1f
        val stepX = (right - left) / (points.size - 1)
        val path = Path()
        val fill = Path()

        points.forEachIndexed { index, value ->
            val x = left + stepX * index
            val y = bottom - ((value - min) / range) * (bottom - top)
            if (index == 0) {
                path.moveTo(x, y)
                fill.moveTo(x, bottom)
                fill.lineTo(x, y)
            } else {
                path.lineTo(x, y)
                fill.lineTo(x, y)
            }
        }
        fill.lineTo(left + stepX * (points.size - 1), bottom)
        fill.close()
        canvas.drawPath(fill, fillPaint)
        canvas.drawPath(path, linePaint)
    }
}
