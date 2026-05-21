package com.joey.gamepad.controller

import android.graphics.*
import com.joey.gamepad.model.GamepadState
import com.joey.gamepad.model.GamepadState.Companion as B

// T8L M2 style RC transmitter: two large gimbals, toggle switches, screen
class RCControllerRenderer : ControllerRenderer() {

    private val C_BODY    = 0xFFFF00FF.toInt()  // magenta
    private val C_GIMBAL  = 0xFF00FFFF.toInt()
    private val C_SWITCH  = 0xFF00FF88.toInt()
    private val C_SCREEN  = 0xFF4488FF.toInt()
    private val C_TRIM    = 0xFFFFAA00.toInt()
    private val C_BTN     = 0xFFFF88FF.toInt()
    private val C_SHLD    = 0xFFFFCC00.toInt()
    private val C_MISC    = 0xFF888899.toInt()

    private fun lsCenter(w: Float, h: Float) = PointF(w * 0.220f, h * 0.620f)
    private fun rsCenter(w: Float, h: Float) = PointF(w * 0.780f, h * 0.620f)

    override fun stickCenter(id: String, w: Float, h: Float) =
        if (id == B.STICK_LEFT) lsCenter(w, h) else rsCenter(w, h)

    override fun stickMaxRadius(w: Float, h: Float) = w * 0.100f

    override fun draw(canvas: Canvas, w: Float, h: Float, state: GamepadState) {
        val pb = state.pressedButtons
        drawBody(canvas, w, h)
        drawScreen(canvas, w, h)
        drawToggles(canvas, w, h, pb)
        drawShoulders(canvas, w, h, pb)
        drawGimbals(canvas, w, h, state)
        drawTrimButtons(canvas, w, h, pb)
        drawCenterButtons(canvas, w, h, pb)
    }

    private fun drawBody(canvas: Canvas, w: Float, h: Float) {
        val body = RectF(w * 0.040f, h * 0.050f, w * 0.960f, h * 0.950f)
        fillPaint.color = Color.argb(110, 15, 0, 20)
        canvas.drawRoundRect(body, 30f, 30f, fillPaint)
        neonRect(canvas, body, 30f, C_BODY, 2.5f)

        // Antenna left
        neonLine(canvas, w*0.100f, h*0.050f, w*0.080f, h*0.000f, C_BODY, 2f)
        // Antenna right
        neonLine(canvas, w*0.900f, h*0.050f, w*0.920f, h*0.000f, C_BODY, 2f)
    }

    private fun drawScreen(canvas: Canvas, w: Float, h: Float) {
        val screen = RectF(w * 0.350f, h * 0.080f, w * 0.650f, h * 0.420f)
        fillPaint.color = Color.argb(80, 0, 30, 80)
        canvas.drawRoundRect(screen, 10f, 10f, fillPaint)
        neonRect(canvas, screen, 10f, C_SCREEN, 1.5f)

        // Decorative scan lines
        paint.color = Color.argb(30, 0, 100, 255)
        paint.strokeWidth = 1f
        paint.maskFilter = null
        var sy = screen.top + 12f
        while (sy < screen.bottom - 6f) {
            canvas.drawLine(screen.left + 6f, sy, screen.right - 6f, sy, paint)
            sy += 10f
        }
        neonText(canvas, "JOEY RC", w * 0.500f, h * 0.230f, h * 0.060f, C_SCREEN)

        // Signal bars
        for (i in 0..4) {
            val bx = screen.left + 14f + i * 12f
            val bh = 6f + i * 5f
            val by = screen.top + 20f
            fillPaint.color = Color.argb(200, 0, 136, 255)
            canvas.drawRect(bx, by + (25f - bh), bx + 8f, by + 25f, fillPaint)
        }
    }

    private fun drawToggles(canvas: Canvas, w: Float, h: Float, pb: Set<String>) {
        // 3 toggle switches at top between screen and sides
        val positions = listOf(w * 0.130f, w * 0.250f, w * 0.750f, w * 0.870f)
        val toggleIds = listOf("sw1", "sw2", "sw3", "sw4")
        for (i in positions.indices) {
            val tx = positions[i]
            val ty = h * 0.200f
            val pressed = pb.contains(toggleIds[i])
            val c = if (pressed) Color.WHITE else C_SWITCH
            // Switch body
            neonRect(canvas, RectF(tx - 10f, ty - 24f, tx + 10f, ty + 24f), 6f, c, 1.5f)
            // Lever position
            val leverY = if (pressed) ty - 10f else ty + 10f
            fillPaint.color = c
            canvas.drawRoundRect(RectF(tx - 7f, leverY - 8f, tx + 7f, leverY + 8f), 4f, 4f, fillPaint)
            neonText(canvas, "CH${i + 1}", tx, ty + 36f, h * 0.038f, C_SWITCH)
        }
    }

    private fun drawShoulders(canvas: Canvas, w: Float, h: Float, pb: Set<String>) {
        val r = 8f
        neonRect(canvas, RectF(w * 0.050f, h * 0.060f, w * 0.200f, h * 0.135f), r, buttonColor(B.BTN_L1, C_SHLD, pb))
        neonRect(canvas, RectF(w * 0.800f, h * 0.060f, w * 0.950f, h * 0.135f), r, buttonColor(B.BTN_R1, C_SHLD, pb))
        neonText(canvas, "SWA", w * 0.125f, h * 0.095f, h * 0.050f, buttonColor(B.BTN_L1, C_SHLD, pb))
        neonText(canvas, "SWD", w * 0.875f, h * 0.095f, h * 0.050f, buttonColor(B.BTN_R1, C_SHLD, pb))
    }

    private fun drawGimbals(canvas: Canvas, w: Float, h: Float, state: GamepadState) {
        val pb = state.pressedButtons
        val gimbalR = w * 0.100f
        val knobR = w * 0.030f

        lsCenter(w, h).let { sc ->
            // Outer housing
            neonCircle(canvas, sc.x, sc.y, gimbalR + 15f, C_BODY, 1.5f)
            // Corner brackets
            drawGimbalBrackets(canvas, sc.x, sc.y, gimbalR + 15f)
            drawStick(canvas, sc.x, sc.y, gimbalR, knobR,
                state.leftStick.x, state.leftStick.y, C_GIMBAL, C_GIMBAL, pb.contains(B.BTN_L3))
            neonText(canvas, "LEFT", sc.x, sc.y + gimbalR + 28f, h * 0.040f, C_GIMBAL)
        }
        rsCenter(w, h).let { sc ->
            neonCircle(canvas, sc.x, sc.y, gimbalR + 15f, C_BODY, 1.5f)
            drawGimbalBrackets(canvas, sc.x, sc.y, gimbalR + 15f)
            drawStick(canvas, sc.x, sc.y, gimbalR, knobR,
                state.rightStick.x, state.rightStick.y, C_GIMBAL, C_GIMBAL, pb.contains(B.BTN_R3))
            neonText(canvas, "RIGHT", sc.x, sc.y + gimbalR + 28f, h * 0.040f, C_GIMBAL)
        }
    }

    private fun drawGimbalBrackets(canvas: Canvas, cx: Float, cy: Float, r: Float) {
        val bLen = 18f; val bOff = r + 6f
        val corners = listOf(
            Pair(cx - bOff, cy - bOff) to Pair(bLen, bLen),
            Pair(cx + bOff, cy - bOff) to Pair(-bLen, bLen),
            Pair(cx - bOff, cy + bOff) to Pair(bLen, -bLen),
            Pair(cx + bOff, cy + bOff) to Pair(-bLen, -bLen)
        )
        paint.color = C_BODY; paint.strokeWidth = 2f; paint.maskFilter = null; paint.alpha = 180
        for ((pos, dirs) in corners) {
            canvas.drawLine(pos.first, pos.second, pos.first + dirs.first, pos.second, paint)
            canvas.drawLine(pos.first, pos.second, pos.first, pos.second + dirs.second, paint)
        }
        paint.alpha = 255
    }

    private fun drawTrimButtons(canvas: Canvas, w: Float, h: Float, pb: Set<String>) {
        // Small trim buttons arranged around each gimbal
        val trimIds = listOf(
            Triple(B.BTN_DPAD_UP, lsCenter(w,h).x, lsCenter(w,h).y - w*0.130f),
            Triple(B.BTN_DPAD_DOWN, lsCenter(w,h).x, lsCenter(w,h).y + w*0.130f),
            Triple(B.BTN_DPAD_LEFT, lsCenter(w,h).x - w*0.130f, lsCenter(w,h).y),
            Triple(B.BTN_DPAD_RIGHT, lsCenter(w,h).x + w*0.130f, lsCenter(w,h).y)
        )
        val labels = listOf("▲", "▼", "◀", "▶")
        for ((i, trim) in trimIds.withIndex()) {
            val c = buttonColor(trim.first, C_TRIM, pb)
            neonRect(canvas, RectF(trim.second - 10f, trim.third - 8f, trim.second + 10f, trim.third + 8f), 4f, c, 1.5f)
            neonText(canvas, labels[i], trim.second, trim.third, h * 0.035f, c)
        }

        // Right side trim (face buttons)
        val rTrimIds = listOf(
            Triple(B.BTN_Y, rsCenter(w,h).x, rsCenter(w,h).y - w*0.130f),
            Triple(B.BTN_A, rsCenter(w,h).x, rsCenter(w,h).y + w*0.130f),
            Triple(B.BTN_X, rsCenter(w,h).x - w*0.130f, rsCenter(w,h).y),
            Triple(B.BTN_B, rsCenter(w,h).x + w*0.130f, rsCenter(w,h).y)
        )
        val rLabels = listOf("Y", "A", "X", "B")
        for ((i, trim) in rTrimIds.withIndex()) {
            val c = buttonColor(trim.first, C_BTN, pb)
            neonRect(canvas, RectF(trim.second - 10f, trim.third - 8f, trim.second + 10f, trim.third + 8f), 4f, c, 1.5f)
            neonText(canvas, rLabels[i], trim.second, trim.third, h * 0.035f, c)
        }
    }

    private fun drawCenterButtons(canvas: Canvas, w: Float, h: Float, pb: Set<String>) {
        // Center cluster
        val cx = w * 0.500f
        neonRect(canvas, RectF(cx - 25f, h * 0.460f, cx - 5f, h * 0.530f), 6f, buttonColor(B.BTN_SELECT, C_MISC, pb))
        neonRect(canvas, RectF(cx + 5f, h * 0.460f, cx + 25f, h * 0.530f), 6f, buttonColor(B.BTN_START, C_MISC, pb))
        neonText(canvas, "RTN", cx - 15f, h * 0.495f, h * 0.038f, buttonColor(B.BTN_SELECT, C_MISC, pb))
        neonText(canvas, "PWR", cx + 15f, h * 0.495f, h * 0.038f, buttonColor(B.BTN_START, C_MISC, pb))
    }

    override fun hitTest(x: Float, y: Float, w: Float, h: Float): HitResult? {
        fun dist(cx: Float, cy: Float) = Math.hypot((x-cx).toDouble(), (y-cy).toDouble()).toFloat()

        if (dist(lsCenter(w,h).x, lsCenter(w,h).y) <= w*0.115f) return HitResult.Stick(B.STICK_LEFT)
        if (dist(rsCenter(w,h).x, rsCenter(w,h).y) <= w*0.115f) return HitResult.Stick(B.STICK_RIGHT)

        // Left trim (dpad)
        lsCenter(w, h).let { sc ->
            if (dist(sc.x, sc.y - w*0.130f) <= 16f) return HitResult.Button(B.BTN_DPAD_UP)
            if (dist(sc.x, sc.y + w*0.130f) <= 16f) return HitResult.Button(B.BTN_DPAD_DOWN)
            if (dist(sc.x - w*0.130f, sc.y) <= 16f) return HitResult.Button(B.BTN_DPAD_LEFT)
            if (dist(sc.x + w*0.130f, sc.y) <= 16f) return HitResult.Button(B.BTN_DPAD_RIGHT)
        }
        // Right trim (face)
        rsCenter(w, h).let { sc ->
            if (dist(sc.x, sc.y - w*0.130f) <= 16f) return HitResult.Button(B.BTN_Y)
            if (dist(sc.x, sc.y + w*0.130f) <= 16f) return HitResult.Button(B.BTN_A)
            if (dist(sc.x - w*0.130f, sc.y) <= 16f) return HitResult.Button(B.BTN_X)
            if (dist(sc.x + w*0.130f, sc.y) <= 16f) return HitResult.Button(B.BTN_B)
        }

        if (x in w*0.050f..w*0.200f && y in h*0.060f..h*0.135f) return HitResult.Button(B.BTN_L1)
        if (x in w*0.800f..w*0.950f && y in h*0.060f..h*0.135f) return HitResult.Button(B.BTN_R1)

        val cx = w * 0.5f
        if (x in cx-25f..cx-5f && y in h*0.460f..h*0.530f) return HitResult.Button(B.BTN_SELECT)
        if (x in cx+5f..cx+25f && y in h*0.460f..h*0.530f) return HitResult.Button(B.BTN_START)

        return null
    }
}
