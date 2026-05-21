package com.joey.gamepad.controller

import android.graphics.*
import com.joey.gamepad.model.GamepadState

abstract class ControllerRenderer {

    protected val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }
    protected val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }
    protected val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
        textAlign = Paint.Align.CENTER
    }

    abstract fun draw(canvas: Canvas, w: Float, h: Float, state: GamepadState)
    abstract fun hitTest(x: Float, y: Float, w: Float, h: Float): HitResult?
    abstract fun stickCenter(id: String, w: Float, h: Float): PointF
    abstract fun stickMaxRadius(w: Float, h: Float): Float

    // ── Neon rendering helpers ──────────────────────────────────────────────

    protected fun neon(canvas: Canvas, color: Int, coreStroke: Float, block: (Paint) -> Unit) {
        paint.color = color
        // outer glow
        paint.strokeWidth = coreStroke + 14f
        paint.maskFilter = BlurMaskFilter(22f, BlurMaskFilter.Blur.NORMAL)
        paint.alpha = 45
        block(paint)
        // mid glow
        paint.strokeWidth = coreStroke + 6f
        paint.maskFilter = BlurMaskFilter(10f, BlurMaskFilter.Blur.NORMAL)
        paint.alpha = 100
        block(paint)
        // core
        paint.strokeWidth = coreStroke
        paint.maskFilter = null
        paint.alpha = 255
        block(paint)
    }

    protected fun neonCircle(canvas: Canvas, cx: Float, cy: Float, r: Float, color: Int, stroke: Float = 2f) =
        neon(canvas, color, stroke) { canvas.drawCircle(cx, cy, r, it) }

    protected fun neonRect(canvas: Canvas, rect: RectF, rx: Float, color: Int, stroke: Float = 2f) =
        neon(canvas, color, stroke) { canvas.drawRoundRect(rect, rx, rx, it) }

    protected fun neonLine(canvas: Canvas, x1: Float, y1: Float, x2: Float, y2: Float, color: Int, stroke: Float = 2f) =
        neon(canvas, color, stroke) { canvas.drawLine(x1, y1, x2, y2, it) }

    protected fun neonPath(canvas: Canvas, path: Path, color: Int, stroke: Float = 2f) =
        neon(canvas, color, stroke) { canvas.drawPath(path, it) }

    protected fun neonText(canvas: Canvas, text: String, cx: Float, cy: Float, size: Float, color: Int) {
        textPaint.textSize = size
        textPaint.color = color
        textPaint.maskFilter = BlurMaskFilter(size * 0.5f, BlurMaskFilter.Blur.NORMAL)
        textPaint.alpha = 90
        canvas.drawText(text, cx, cy + size * 0.35f, textPaint)
        textPaint.maskFilter = null
        textPaint.alpha = 255
        canvas.drawText(text, cx, cy + size * 0.35f, textPaint)
    }

    // Dim overlay when pressed to simulate a "lit up" flash
    protected fun buttonColor(id: String, base: Int, pressed: Set<String>): Int =
        if (pressed.contains(id)) Color.WHITE else base

    // Draw an analog stick: outer ring + crosshair + moving knob
    protected fun drawStick(
        canvas: Canvas,
        cx: Float, cy: Float,
        maxR: Float,
        knobR: Float,
        nx: Float, ny: Float,        // normalized -1..1
        ringColor: Int,
        knobColor: Int,
        pressed: Boolean
    ) {
        val kx = cx + nx * maxR
        val ky = cy + ny * maxR

        // outer ring
        neonCircle(canvas, cx, cy, maxR, ringColor, 1.5f)
        // subtle crosshair
        val dim = Color.argb(60, Color.red(ringColor), Color.green(ringColor), Color.blue(ringColor))
        paint.color = dim; paint.strokeWidth = 1f; paint.maskFilter = null; paint.alpha = 60
        canvas.drawLine(cx - maxR, cy, cx + maxR, cy, paint)
        canvas.drawLine(cx, cy - maxR, cx, cy + maxR, paint)

        // direction line from center to knob
        if (nx != 0f || ny != 0f) neonLine(canvas, cx, cy, kx, ky, ringColor, 1f)

        // knob
        val kc = if (pressed) Color.WHITE else knobColor
        neonCircle(canvas, kx, ky, knobR, kc, 2f)
        fillPaint.color = Color.argb(180, 0, 0, 0)
        canvas.drawCircle(kx, ky, knobR - 2f, fillPaint)
    }

    // Draw a D-pad cross at cx,cy with arm length and arm width
    protected fun drawDpad(
        canvas: Canvas,
        cx: Float, cy: Float,
        arm: Float, hw: Float,
        up: Boolean, down: Boolean, left: Boolean, right: Boolean,
        color: Int
    ) {
        fun armColor(pressed: Boolean) = if (pressed) Color.WHITE else color
        val r = 6f
        // up
        neonRect(canvas, RectF(cx - hw, cy - arm, cx + hw, cy - hw), r, armColor(up))
        // down
        neonRect(canvas, RectF(cx - hw, cy + hw, cx + hw, cy + arm), r, armColor(down))
        // left
        neonRect(canvas, RectF(cx - arm, cy - hw, cx - hw, cy + hw), r, armColor(left))
        // right
        neonRect(canvas, RectF(cx + hw, cy - hw, cx + arm, cy + hw), r, armColor(right))
        // center
        neonRect(canvas, RectF(cx - hw, cy - hw, cx + hw, cy + hw), r, color)
    }
}
