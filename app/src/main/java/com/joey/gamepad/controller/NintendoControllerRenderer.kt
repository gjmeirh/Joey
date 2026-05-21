package com.joey.gamepad.controller

import android.graphics.*
import com.joey.gamepad.model.GamepadState
import com.joey.gamepad.model.GamepadState.Companion as B

class NintendoControllerRenderer : ControllerRenderer() {

    private val C_BODY  = 0xFF00FF88.toInt()
    private val C_BTN_A = 0xFFFF4444.toInt()
    private val C_BTN_B = 0xFFFFDD00.toInt()
    private val C_BTN_X = 0xFF44AAFF.toInt()
    private val C_BTN_Y = 0xFF44FF88.toInt()
    private val C_DPAD  = 0xFF00FFCC.toInt()
    private val C_SHLD  = 0xFFFF8800.toInt()
    private val C_MISC  = 0xFF888899.toInt()
    private val C_STICK = 0xFF00FFCC.toInt()
    private val C_HOME  = 0xFFFFFFAA.toInt()

    private fun lsCenter(w: Float, h: Float)  = PointF(w * 0.250f, h * 0.380f)
    private fun rsCenter(w: Float, h: Float)  = PointF(w * 0.620f, h * 0.620f)
    private fun dpadCenter(w: Float, h: Float) = PointF(w * 0.180f, h * 0.640f)
    private fun faceCenter(w: Float, h: Float) = PointF(w * 0.720f, h * 0.380f)
    private fun fbRadius(w: Float) = w * 0.032f
    private fun fbOffset(w: Float) = w * 0.055f

    override fun stickCenter(id: String, w: Float, h: Float) =
        if (id == B.STICK_LEFT) lsCenter(w, h) else rsCenter(w, h)

    override fun stickMaxRadius(w: Float, h: Float) = w * 0.072f

    private fun bodyPath(w: Float, h: Float): Path {
        val p = Path()
        // Nintendo Switch Pro has a more rectangular oval with circular left/right lobes
        val cx = w * 0.5f

        // Main oval body
        p.addRoundRect(RectF(w * 0.080f, h * 0.080f, w * 0.920f, h * 0.920f), w * 0.130f, w * 0.130f, Path.Direction.CW)

        // Left circular lobe
        val leftLobe = Path()
        leftLobe.addCircle(w * 0.145f, h * 0.500f, w * 0.110f, Path.Direction.CW)
        p.op(leftLobe, Path.Op.UNION)

        // Right circular lobe
        val rightLobe = Path()
        rightLobe.addCircle(w * 0.855f, h * 0.500f, w * 0.110f, Path.Direction.CW)
        p.op(rightLobe, Path.Op.UNION)

        return p
    }

    override fun draw(canvas: Canvas, w: Float, h: Float, state: GamepadState) {
        val pb = state.pressedButtons

        // body fill + outline
        fillPaint.color = Color.argb(110, 0, 20, 10)
        canvas.drawPath(bodyPath(w, h), fillPaint)
        neonPath(canvas, bodyPath(w, h), C_BODY, 2f)

        drawShoulders(canvas, w, h, pb)
        drawTriggers(canvas, w, h, state)

        // Left stick
        lsCenter(w, h).let {
            drawStick(canvas, it.x, it.y, w * 0.072f, w * 0.028f,
                state.leftStick.x, state.leftStick.y, C_STICK, C_STICK, pb.contains(B.BTN_L3))
        }
        // Right stick
        rsCenter(w, h).let {
            drawStick(canvas, it.x, it.y, w * 0.072f, w * 0.028f,
                state.rightStick.x, state.rightStick.y, C_STICK, C_STICK, pb.contains(B.BTN_R3))
        }

        drawDpadSection(canvas, w, h, pb)
        drawFaceButtons(canvas, w, h, pb)
        drawCenterButtons(canvas, w, h, pb)
    }

    private fun drawShoulders(canvas: Canvas, w: Float, h: Float, pb: Set<String>) {
        val r = 8f
        neonRect(canvas, RectF(w * 0.095f, h * 0.100f, w * 0.310f, h * 0.175f), r, buttonColor(B.BTN_L1, C_SHLD, pb))
        neonRect(canvas, RectF(w * 0.690f, h * 0.100f, w * 0.905f, h * 0.175f), r, buttonColor(B.BTN_R1, C_SHLD, pb))
        neonText(canvas, "L", w * 0.202f, h * 0.135f, h * 0.065f, buttonColor(B.BTN_L1, C_SHLD, pb))
        neonText(canvas, "R", w * 0.798f, h * 0.135f, h * 0.065f, buttonColor(B.BTN_R1, C_SHLD, pb))
    }

    private fun drawTriggers(canvas: Canvas, w: Float, h: Float, state: GamepadState) {
        val pb = state.pressedButtons
        val lt = state.leftTrigger
        val rt = state.rightTrigger
        val ltRect = RectF(w * 0.095f, h * 0.025f, w * 0.310f, h * 0.105f)
        val rtRect = RectF(w * 0.690f, h * 0.025f, w * 0.905f, h * 0.105f)
        val r = 8f

        if (lt > 0.01f) { fillPaint.color = Color.argb((lt*100).toInt(),255,136,0); canvas.drawRoundRect(ltRect,r,r,fillPaint) }
        if (rt > 0.01f) { fillPaint.color = Color.argb((rt*100).toInt(),255,136,0); canvas.drawRoundRect(rtRect,r,r,fillPaint) }

        neonRect(canvas, ltRect, r, buttonColor(B.BTN_L2, C_SHLD, pb))
        neonRect(canvas, rtRect, r, buttonColor(B.BTN_R2, C_SHLD, pb))
        neonText(canvas, "ZL", w * 0.202f, h * 0.063f, h * 0.062f, buttonColor(B.BTN_L2, C_SHLD, pb))
        neonText(canvas, "ZR", w * 0.798f, h * 0.063f, h * 0.062f, buttonColor(B.BTN_R2, C_SHLD, pb))
    }

    private fun drawDpadSection(canvas: Canvas, w: Float, h: Float, pb: Set<String>) {
        dpadCenter(w, h).let { dc ->
            drawDpad(canvas, dc.x, dc.y, w * 0.042f, w * 0.020f,
                pb.contains(B.BTN_DPAD_UP), pb.contains(B.BTN_DPAD_DOWN),
                pb.contains(B.BTN_DPAD_LEFT), pb.contains(B.BTN_DPAD_RIGHT),
                C_DPAD)
        }
    }

    private fun drawFaceButtons(canvas: Canvas, w: Float, h: Float, pb: Set<String>) {
        val fc = faceCenter(w, h)
        val r = fbRadius(w)
        val off = fbOffset(w)
        // X top, A right, B bottom, Y left
        neonCircle(canvas, fc.x, fc.y - off, r, buttonColor(B.BTN_Y, C_BTN_X, pb))
        neonText(canvas, "X", fc.x, fc.y - off, r * 1.3f, buttonColor(B.BTN_Y, C_BTN_X, pb))

        neonCircle(canvas, fc.x + off, fc.y, r, buttonColor(B.BTN_B, C_BTN_A, pb))
        neonText(canvas, "A", fc.x + off, fc.y, r * 1.3f, buttonColor(B.BTN_B, C_BTN_A, pb))

        neonCircle(canvas, fc.x, fc.y + off, r, buttonColor(B.BTN_A, C_BTN_B, pb))
        neonText(canvas, "B", fc.x, fc.y + off, r * 1.3f, buttonColor(B.BTN_A, C_BTN_B, pb))

        neonCircle(canvas, fc.x - off, fc.y, r, buttonColor(B.BTN_X, C_BTN_Y, pb))
        neonText(canvas, "Y", fc.x - off, fc.y, r * 1.3f, buttonColor(B.BTN_X, C_BTN_Y, pb))
    }

    private fun drawCenterButtons(canvas: Canvas, w: Float, h: Float, pb: Set<String>) {
        val r = w * 0.018f
        // − select
        neonCircle(canvas, w * 0.415f, h * 0.420f, r, buttonColor(B.BTN_SELECT, C_MISC, pb))
        neonText(canvas, "−", w * 0.415f, h * 0.420f, h * 0.045f, buttonColor(B.BTN_SELECT, C_MISC, pb))
        // + start
        neonCircle(canvas, w * 0.585f, h * 0.420f, r, buttonColor(B.BTN_START, C_MISC, pb))
        neonText(canvas, "+", w * 0.585f, h * 0.420f, h * 0.045f, buttonColor(B.BTN_START, C_MISC, pb))
        // Home
        neonCircle(canvas, w * 0.585f, h * 0.575f, r, buttonColor(B.BTN_HOME, C_HOME, pb))
        neonText(canvas, "⌂", w * 0.585f, h * 0.570f, h * 0.040f, buttonColor(B.BTN_HOME, C_HOME, pb))
        // Screenshot
        neonRect(canvas, RectF(w*0.400f, h*0.545f, w*0.430f, h*0.605f), 4f, buttonColor("screenshot", C_MISC, pb))
    }

    override fun hitTest(x: Float, y: Float, w: Float, h: Float): HitResult? {
        fun dist(cx: Float, cy: Float) = Math.hypot((x-cx).toDouble(), (y-cy).toDouble()).toFloat()

        if (dist(lsCenter(w,h).x, lsCenter(w,h).y) <= w*0.082f) return HitResult.Stick(B.STICK_LEFT)
        if (dist(rsCenter(w,h).x, rsCenter(w,h).y) <= w*0.082f) return HitResult.Stick(B.STICK_RIGHT)

        faceCenter(w, h).let { fc ->
            val off = fbOffset(w); val r = fbRadius(w) * 1.5f
            if (dist(fc.x, fc.y - off) <= r) return HitResult.Button(B.BTN_Y)
            if (dist(fc.x + off, fc.y) <= r) return HitResult.Button(B.BTN_B)
            if (dist(fc.x, fc.y + off) <= r) return HitResult.Button(B.BTN_A)
            if (dist(fc.x - off, fc.y) <= r) return HitResult.Button(B.BTN_X)
        }

        dpadCenter(w, h).let { dc ->
            val arm = w * 0.042f; val hw = w * 0.020f
            if (x in (dc.x-hw)..(dc.x+hw) && y in (dc.y-arm)..(dc.y-hw)) return HitResult.Button(B.BTN_DPAD_UP)
            if (x in (dc.x-hw)..(dc.x+hw) && y in (dc.y+hw)..(dc.y+arm)) return HitResult.Button(B.BTN_DPAD_DOWN)
            if (x in (dc.x-arm)..(dc.x-hw) && y in (dc.y-hw)..(dc.y+hw)) return HitResult.Button(B.BTN_DPAD_LEFT)
            if (x in (dc.x+hw)..(dc.x+arm) && y in (dc.y-hw)..(dc.y+hw)) return HitResult.Button(B.BTN_DPAD_RIGHT)
        }

        if (x in w*0.095f..w*0.310f && y in h*0.100f..h*0.175f) return HitResult.Button(B.BTN_L1)
        if (x in w*0.690f..w*0.905f && y in h*0.100f..h*0.175f) return HitResult.Button(B.BTN_R1)
        if (x in w*0.095f..w*0.310f && y in h*0.025f..h*0.105f) return HitResult.Trigger(B.TRIGGER_LEFT)
        if (x in w*0.690f..w*0.905f && y in h*0.025f..h*0.105f) return HitResult.Trigger(B.TRIGGER_RIGHT)

        if (dist(w*0.415f, h*0.420f) <= w*0.025f) return HitResult.Button(B.BTN_SELECT)
        if (dist(w*0.585f, h*0.420f) <= w*0.025f) return HitResult.Button(B.BTN_START)
        if (dist(w*0.585f, h*0.575f) <= w*0.025f) return HitResult.Button(B.BTN_HOME)

        return null
    }
}
