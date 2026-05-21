package com.joey.gamepad.controller

import android.graphics.*
import com.joey.gamepad.model.GamepadState
import com.joey.gamepad.model.GamepadState.Companion as B

class PSControllerRenderer : ControllerRenderer() {

    // ── Palette ────────────────────────────────────────────────────────────
    private val C_BODY   = 0xFF00FFFF.toInt()   // cyan body outline
    private val C_CROSS  = 0xFF5599FF.toInt()   // blue  ✕
    private val C_CIRCLE = 0xFFFF4444.toInt()   // red   ○
    private val C_SQUARE = 0xFFFF88FF.toInt()   // pink  □
    private val C_TRI    = 0xFF44FF99.toInt()   // green △
    private val C_DPAD   = 0xFF00DDFF.toInt()
    private val C_SHLD   = 0xFFFFAA00.toInt()   // orange L/R
    private val C_MISC   = 0xFF888899.toInt()
    private val C_STICK  = 0xFF00FFCC.toInt()

    // ── Layout (all as fraction of w / h) ──────────────────────────────────
    private fun lsCenter(w: Float, h: Float)  = PointF(w * 0.265f, h * 0.52f)
    private fun rsCenter(w: Float, h: Float)  = PointF(w * 0.605f, h * 0.68f)
    private fun dpadCenter(w: Float, h: Float) = PointF(w * 0.195f, h * 0.68f)
    private fun faceCenter(w: Float, h: Float) = PointF(w * 0.735f, h * 0.52f)
    private fun fbRadius(w: Float)  = w * 0.034f
    private fun fbOffset(w: Float)  = w * 0.058f

    override fun stickCenter(id: String, w: Float, h: Float) =
        if (id == B.STICK_LEFT) lsCenter(w, h) else rsCenter(w, h)

    override fun stickMaxRadius(w: Float, h: Float) = w * 0.075f

    // ── Body path ──────────────────────────────────────────────────────────
    private fun bodyPath(w: Float, h: Float): Path {
        val p = Path()
        p.moveTo(w * 0.185f, h * 0.155f)
        // top arch
        p.cubicTo(w * 0.210f, h * 0.075f, w * 0.380f, h * 0.060f, w * 0.500f, h * 0.060f)
        p.cubicTo(w * 0.620f, h * 0.060f, w * 0.790f, h * 0.075f, w * 0.815f, h * 0.155f)
        // right shoulder
        p.cubicTo(w * 0.920f, h * 0.155f, w * 0.935f, h * 0.320f, w * 0.905f, h * 0.460f)
        // right outer
        p.cubicTo(w * 0.900f, h * 0.580f, w * 0.875f, h * 0.700f, w * 0.840f, h * 0.750f)
        // right handle
        p.cubicTo(w * 0.820f, h * 0.800f, w * 0.800f, h * 0.895f, w * 0.755f, h * 0.930f)
        p.cubicTo(w * 0.710f, h * 0.955f, w * 0.660f, h * 0.935f, w * 0.630f, h * 0.870f)
        // bottom between handles
        p.cubicTo(w * 0.600f, h * 0.800f, w * 0.560f, h * 0.785f, w * 0.500f, h * 0.785f)
        p.cubicTo(w * 0.440f, h * 0.785f, w * 0.400f, h * 0.800f, w * 0.370f, h * 0.870f)
        // left handle
        p.cubicTo(w * 0.340f, h * 0.935f, w * 0.290f, h * 0.955f, w * 0.245f, h * 0.930f)
        p.cubicTo(w * 0.200f, h * 0.895f, w * 0.180f, h * 0.800f, w * 0.160f, h * 0.750f)
        // left outer
        p.cubicTo(w * 0.125f, h * 0.700f, w * 0.100f, h * 0.580f, w * 0.095f, h * 0.460f)
        // left shoulder
        p.cubicTo(w * 0.065f, h * 0.320f, w * 0.080f, h * 0.155f, w * 0.185f, h * 0.155f)
        p.close()
        return p
    }

    // ── Draw ───────────────────────────────────────────────────────────────
    override fun draw(canvas: Canvas, w: Float, h: Float, state: GamepadState) {
        val pb = state.pressedButtons

        drawBodyShell(canvas, w, h)
        drawShoulderButtons(canvas, w, h, pb)
        drawTriggers(canvas, w, h, state)
        drawTouchpad(canvas, w, h)
        drawShareOptions(canvas, w, h, pb)
        drawPsButton(canvas, w, h, pb)

        // Left stick
        lsCenter(w, h).let { sc ->
            drawStick(canvas, sc.x, sc.y, w * 0.075f, w * 0.030f,
                state.leftStick.x, state.leftStick.y,
                C_STICK, C_STICK, pb.contains(B.BTN_L3))
        }
        // Right stick
        rsCenter(w, h).let { sc ->
            drawStick(canvas, sc.x, sc.y, w * 0.075f, w * 0.030f,
                state.rightStick.x, state.rightStick.y,
                C_STICK, C_STICK, pb.contains(B.BTN_R3))
        }

        drawDpadSection(canvas, w, h, pb)
        drawFaceButtons(canvas, w, h, pb)
    }

    private fun drawBodyShell(canvas: Canvas, w: Float, h: Float) {
        // Fill body dark
        fillPaint.color = Color.argb(120, 5, 5, 20)
        val path = bodyPath(w, h)
        canvas.drawPath(path, fillPaint)
        // Neon outline
        neonPath(canvas, path, C_BODY, 2f)
    }

    private fun drawShoulderButtons(canvas: Canvas, w: Float, h: Float, pb: Set<String>) {
        val r = 8f
        neonRect(canvas, RectF(w * 0.090f, h * 0.090f, w * 0.270f, h * 0.170f), r, buttonColor(B.BTN_L1, C_SHLD, pb))
        neonRect(canvas, RectF(w * 0.730f, h * 0.090f, w * 0.910f, h * 0.170f), r, buttonColor(B.BTN_R1, C_SHLD, pb))
        neonText(canvas, "L1", w * 0.180f, h * 0.127f, h * 0.065f, buttonColor(B.BTN_L1, C_SHLD, pb))
        neonText(canvas, "R1", w * 0.820f, h * 0.127f, h * 0.065f, buttonColor(B.BTN_R1, C_SHLD, pb))
    }

    private fun drawTriggers(canvas: Canvas, w: Float, h: Float, state: GamepadState) {
        val pb = state.pressedButtons
        val lt = state.leftTrigger
        val rt = state.rightTrigger

        val ltRect = RectF(w * 0.090f, h * 0.010f, w * 0.260f, h * 0.090f)
        val rtRect = RectF(w * 0.740f, h * 0.010f, w * 0.910f, h * 0.090f)
        val r = 8f

        // Fill based on trigger value
        if (lt > 0.01f) {
            fillPaint.color = Color.argb((lt * 100).toInt(), 255, 170, 0)
            canvas.drawRoundRect(ltRect, r, r, fillPaint)
        }
        if (rt > 0.01f) {
            fillPaint.color = Color.argb((rt * 100).toInt(), 255, 170, 0)
            canvas.drawRoundRect(rtRect, r, r, fillPaint)
        }

        neonRect(canvas, ltRect, r, buttonColor(B.BTN_L2, C_SHLD, pb))
        neonRect(canvas, rtRect, r, buttonColor(B.BTN_R2, C_SHLD, pb))
        neonText(canvas, "L2", w * 0.175f, h * 0.047f, h * 0.065f, buttonColor(B.BTN_L2, C_SHLD, pb))
        neonText(canvas, "R2", w * 0.825f, h * 0.047f, h * 0.065f, buttonColor(B.BTN_R2, C_SHLD, pb))
    }

    private fun drawTouchpad(canvas: Canvas, w: Float, h: Float) {
        val rect = RectF(w * 0.395f, h * 0.235f, w * 0.605f, h * 0.420f)
        fillPaint.color = Color.argb(50, 0, 200, 255)
        canvas.drawRoundRect(rect, 12f, 12f, fillPaint)
        neonRect(canvas, rect, 12f, C_MISC, 1.5f)
        neonText(canvas, "TOUCH", w * 0.500f, h * 0.318f, h * 0.040f, C_MISC)
    }

    private fun drawShareOptions(canvas: Canvas, w: Float, h: Float, pb: Set<String>) {
        val r = w * 0.018f
        neonCircle(canvas, w * 0.420f, h * 0.460f, r, buttonColor(B.BTN_SELECT, C_MISC, pb))
        neonText(canvas, "⋯", w * 0.420f, h * 0.456f, h * 0.040f, buttonColor(B.BTN_SELECT, C_MISC, pb))
        neonCircle(canvas, w * 0.580f, h * 0.460f, r, buttonColor(B.BTN_START, C_MISC, pb))
        neonText(canvas, "≡", w * 0.580f, h * 0.456f, h * 0.040f, buttonColor(B.BTN_START, C_MISC, pb))
    }

    private fun drawPsButton(canvas: Canvas, w: Float, h: Float, pb: Set<String>) {
        neonCircle(canvas, w * 0.500f, h * 0.730f, w * 0.022f, buttonColor(B.BTN_HOME, C_BODY, pb), 2f)
        neonText(canvas, "PS", w * 0.500f, h * 0.725f, h * 0.038f, buttonColor(B.BTN_HOME, C_BODY, pb))
    }

    private fun drawDpadSection(canvas: Canvas, w: Float, h: Float, pb: Set<String>) {
        dpadCenter(w, h).let { dc ->
            drawDpad(canvas, dc.x, dc.y, w * 0.045f, w * 0.022f,
                pb.contains(B.BTN_DPAD_UP), pb.contains(B.BTN_DPAD_DOWN),
                pb.contains(B.BTN_DPAD_LEFT), pb.contains(B.BTN_DPAD_RIGHT),
                C_DPAD)
        }
    }

    private fun drawFaceButtons(canvas: Canvas, w: Float, h: Float, pb: Set<String>) {
        val fc = faceCenter(w, h)
        val r = fbRadius(w)
        val off = fbOffset(w)

        // △ top
        neonCircle(canvas, fc.x, fc.y - off, r, buttonColor(B.BTN_Y, C_TRI, pb))
        neonText(canvas, "△", fc.x, fc.y - off, r * 1.4f, buttonColor(B.BTN_Y, C_TRI, pb))
        // ○ right
        neonCircle(canvas, fc.x + off, fc.y, r, buttonColor(B.BTN_B, C_CIRCLE, pb))
        neonText(canvas, "○", fc.x + off, fc.y, r * 1.4f, buttonColor(B.BTN_B, C_CIRCLE, pb))
        // ✕ bottom
        neonCircle(canvas, fc.x, fc.y + off, r, buttonColor(B.BTN_A, C_CROSS, pb))
        neonText(canvas, "✕", fc.x, fc.y + off, r * 1.4f, buttonColor(B.BTN_A, C_CROSS, pb))
        // □ left
        neonCircle(canvas, fc.x - off, fc.y, r, buttonColor(B.BTN_X, C_SQUARE, pb))
        neonText(canvas, "□", fc.x - off, fc.y, r * 1.4f, buttonColor(B.BTN_X, C_SQUARE, pb))
    }

    // ── Hit test ───────────────────────────────────────────────────────────
    override fun hitTest(x: Float, y: Float, w: Float, h: Float): HitResult? {
        fun dist(cx: Float, cy: Float) = Math.hypot((x - cx).toDouble(), (y - cy).toDouble()).toFloat()

        // Left stick
        lsCenter(w, h).let { if (dist(it.x, it.y) <= w * 0.085f) return HitResult.Stick(B.STICK_LEFT) }
        // Right stick
        rsCenter(w, h).let { if (dist(it.x, it.y) <= w * 0.085f) return HitResult.Stick(B.STICK_RIGHT) }

        // Face buttons
        faceCenter(w, h).let { fc ->
            val off = fbOffset(w)
            val r = fbRadius(w) * 1.5f
            if (dist(fc.x, fc.y - off) <= r) return HitResult.Button(B.BTN_Y)
            if (dist(fc.x + off, fc.y) <= r) return HitResult.Button(B.BTN_B)
            if (dist(fc.x, fc.y + off) <= r) return HitResult.Button(B.BTN_A)
            if (dist(fc.x - off, fc.y) <= r) return HitResult.Button(B.BTN_X)
        }

        // D-pad
        dpadCenter(w, h).let { dc ->
            val arm = w * 0.045f
            val hw = w * 0.022f
            if (x in (dc.x - hw)..(dc.x + hw) && y in (dc.y - arm)..(dc.y - hw)) return HitResult.Button(B.BTN_DPAD_UP)
            if (x in (dc.x - hw)..(dc.x + hw) && y in (dc.y + hw)..(dc.y + arm)) return HitResult.Button(B.BTN_DPAD_DOWN)
            if (x in (dc.x - arm)..(dc.x - hw) && y in (dc.y - hw)..(dc.y + hw)) return HitResult.Button(B.BTN_DPAD_LEFT)
            if (x in (dc.x + hw)..(dc.x + arm) && y in (dc.y - hw)..(dc.y + hw)) return HitResult.Button(B.BTN_DPAD_RIGHT)
        }

        // Shoulder / triggers
        if (x in w*0.09f..w*0.27f && y in h*0.09f..h*0.17f) return HitResult.Button(B.BTN_L1)
        if (x in w*0.73f..w*0.91f && y in h*0.09f..h*0.17f) return HitResult.Button(B.BTN_R1)
        if (x in w*0.09f..w*0.26f && y in h*0.01f..h*0.09f) return HitResult.Trigger(B.TRIGGER_LEFT)
        if (x in w*0.74f..w*0.91f && y in h*0.01f..h*0.09f) return HitResult.Trigger(B.TRIGGER_RIGHT)

        // Share / Options
        if (dist(w * 0.420f, h * 0.460f) <= w * 0.025f) return HitResult.Button(B.BTN_SELECT)
        if (dist(w * 0.580f, h * 0.460f) <= w * 0.025f) return HitResult.Button(B.BTN_START)

        // PS button
        if (dist(w * 0.500f, h * 0.730f) <= w * 0.030f) return HitResult.Button(B.BTN_HOME)

        return null
    }
}
