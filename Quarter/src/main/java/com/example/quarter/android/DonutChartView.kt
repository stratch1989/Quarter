package com.example.quarter.android

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.util.AttributeSet
import android.view.View

class DonutChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    data class Segment(val label: String, val amount: Double, val color: Int)

    private var segments: List<Segment> = emptyList()
    private var total: Double = 0.0

    private val arcPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.BUTT
    }

    private val centerTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }

    private val legendLabelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
    }

    private val legendAmountPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFFfea00a.toInt()
        textAlign = Paint.Align.RIGHT
    }

    private val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val arcRect = RectF()

    fun setData(data: List<Segment>) {
        segments = data
        total = data.sumOf { it.amount }
        requestLayout()
        invalidate()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val w = MeasureSpec.getSize(widthMeasureSpec)
        val dp = resources.displayMetrics.density
        val ringArea = (50 * 2 + 20 + 16) * dp
        val legendArea = segments.size * 24 * dp + 20 * dp
        val h = (ringArea + legendArea).toInt()
        setMeasuredDimension(w, resolveSize(h, heightMeasureSpec))
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (segments.isEmpty() || total == 0.0) return

        val dp = resources.displayMetrics.density
        val strokeW = 20 * dp
        val radius = 50 * dp
        val cx = width / 2f
        val cy = radius + strokeW / 2 + 8 * dp

        // Кольцо
        arcPaint.strokeWidth = strokeW
        arcRect.set(cx - radius, cy - radius, cx + radius, cy + radius)

        val gap = if (segments.size > 1) 2f else 0f
        var startAngle = -90f
        for ((i, seg) in segments.withIndex()) {
            val rawSweep = (seg.amount / total * 360).toFloat()
            val sweep = if (i == segments.lastIndex) {
                360f - (startAngle + 90f) - gap / 2
            } else {
                rawSweep - gap
            }
            arcPaint.color = seg.color
            canvas.drawArc(arcRect, startAngle + gap / 2, sweep.coerceAtLeast(0.5f), false, arcPaint)
            startAngle += rawSweep
        }

        // Сумма в центре
        centerTextPaint.textSize = 16 * dp
        val centerText = "${formatAmount(total)} ₽"
        canvas.drawText(centerText, cx, cy + centerTextPaint.textSize / 3, centerTextPaint)

        // Легенда
        val legendTop = cy + radius + strokeW / 2 + 24 * dp
        val lineH = 24 * dp
        val dotR = 5 * dp
        legendLabelPaint.textSize = 13.5f * dp
        legendAmountPaint.textSize = 13.5f * dp
        val leftPad = 4 * dp
        val rightPad = width - 4 * dp

        for ((i, seg) in segments.withIndex()) {
            val y = legendTop + i * lineH
            dotPaint.color = seg.color
            canvas.drawCircle(leftPad + dotR, y, dotR, dotPaint)
            canvas.drawText(seg.label, leftPad + dotR * 3 + 2 * dp, y + legendLabelPaint.textSize / 3, legendLabelPaint)
            canvas.drawText("${formatAmount(seg.amount)} ₽", rightPad, y + legendAmountPaint.textSize / 3, legendAmountPaint)
        }
    }

    private fun formatAmount(amount: Double): String {
        return if (amount == amount.toLong().toDouble()) amount.toLong().toString()
        else amount.toString()
    }

    companion object {
        val COLORS = intArrayOf(
            0xFFFF6B6B.toInt(),
            0xFF4ECDC4.toInt(),
            0xFFFFE66D.toInt(),
            0xFF45B7D1.toInt(),
            0xFFAA96DA.toInt(),
            0xFFFCBF49.toInt(),
            0xFF80ED99.toInt(),
            0xFFFF8C94.toInt(),
            0xFFA8D8EA.toInt(),
            0xFFC9CBA3.toInt(),
        )
        const val NO_CATEGORY_COLOR = 0xFF666666.toInt()
    }
}
