package com.example.quarter.android

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import kotlin.math.atan2
import kotlin.math.hypot

class DonutChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    data class Segment(val label: String, val amount: Double, val color: Int)

    private var segments: List<Segment> = emptyList()
    private var total: Double = 0.0

    // Углы сегментов для hit-test
    private var segmentAngles: List<Pair<Float, Float>> = emptyList() // (startAngle, endAngle)
    private var ringCx = 0f
    private var ringCy = 0f
    private var ringInnerRadius = 0f
    private var ringOuterRadius = 0f

    // Выбранный сегмент
    private var selectedIndex = -1

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

    private val tooltipBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xE6222222.toInt()
        style = Paint.Style.FILL
    }

    private val tooltipBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF444444.toInt()
        style = Paint.Style.STROKE
        strokeWidth = 1f
    }

    private val tooltipLabelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textAlign = Paint.Align.CENTER
    }

    private val tooltipAmountPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFFfea00a.toInt()
        textAlign = Paint.Align.CENTER
    }

    private val arcRect = RectF()

    fun setData(data: List<Segment>) {
        segments = data
        total = data.sumOf { it.amount }
        selectedIndex = -1
        requestLayout()
        invalidate()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val w = MeasureSpec.getSize(widthMeasureSpec)
        val dp = resources.displayMetrics.density
        val h = ((70 * 2 + 24 + 16) * dp).toInt()
        setMeasuredDimension(w, resolveSize(h, heightMeasureSpec))
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                val hit = hitTestRing(event.x, event.y)
                if (hit >= 0) {
                    selectedIndex = hit
                    invalidate()
                    return true
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                if (selectedIndex >= 0) {
                    selectedIndex = -1
                    invalidate()
                }
            }
        }
        return super.onTouchEvent(event)
    }

    private fun hitTestRing(x: Float, y: Float): Int {
        val dx = x - ringCx
        val dy = y - ringCy
        val dist = hypot(dx, dy)
        if (dist < ringInnerRadius || dist > ringOuterRadius) return -1

        // Угол в градусах, 0 = вправо, по часовой стрелке
        var angle = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat()
        // Нормализуем к 0..360 от -90 (верх)
        angle = (angle + 360f) % 360f

        for ((i, range) in segmentAngles.withIndex()) {
            val start = (range.first + 360f) % 360f
            val end = (range.second + 360f) % 360f
            if (start < end) {
                if (angle in start..end) return i
            } else {
                // Переход через 0°
                if (angle >= start || angle <= end) return i
            }
        }
        return -1
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (segments.isEmpty() || total == 0.0) return

        val dp = resources.displayMetrics.density
        val strokeW = 24 * dp
        val radius = 70 * dp
        val cx = width / 2f
        val cy = radius + strokeW / 2 + 8 * dp

        // Сохраняем параметры кольца для hit-test
        ringCx = cx
        ringCy = cy
        ringInnerRadius = radius - strokeW / 2
        ringOuterRadius = radius + strokeW / 2

        // Кольцо
        arcPaint.strokeWidth = strokeW
        arcRect.set(cx - radius, cy - radius, cx + radius, cy + radius)

        val gap = if (segments.size > 1) 2f else 0f
        var startAngle = -90f
        val angles = mutableListOf<Pair<Float, Float>>()

        for ((i, seg) in segments.withIndex()) {
            val rawSweep = (seg.amount / total * 360).toFloat()
            val sweep = if (i == segments.lastIndex) {
                360f - (startAngle + 90f) - gap / 2
            } else {
                rawSweep - gap
            }

            val drawStart = startAngle + gap / 2
            val drawSweep = sweep.coerceAtLeast(0.5f)

            // Выделение выбранного сегмента
            if (i == selectedIndex) {
                arcPaint.strokeWidth = strokeW + 6 * dp
            } else {
                arcPaint.strokeWidth = strokeW
            }
            arcPaint.color = seg.color
            canvas.drawArc(arcRect, drawStart, drawSweep, false, arcPaint)

            // Углы для hit-test (в системе Canvas: 0=вправо, по часовой)
            angles.add(Pair(drawStart, drawStart + drawSweep))

            startAngle += rawSweep
        }
        segmentAngles = angles
        arcPaint.strokeWidth = strokeW

        // Центр: категория+сумма если выбран, иначе общая сумма
        if (selectedIndex in segments.indices) {
            val seg = segments[selectedIndex]
            centerTextPaint.textSize = 18 * dp
            canvas.drawText(seg.label, cx, cy - 4 * dp, centerTextPaint)
            centerTextPaint.textSize = 14 * dp
            val pct = (seg.amount / total * 100).toInt()
            canvas.drawText("${formatAmount(seg.amount)} ₽ · $pct%", cx, cy + 14 * dp, centerTextPaint)
        } else {
            centerTextPaint.textSize = 16 * dp
            canvas.drawText("${formatAmount(total)} ₽", cx, cy + centerTextPaint.textSize / 3, centerTextPaint)
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
