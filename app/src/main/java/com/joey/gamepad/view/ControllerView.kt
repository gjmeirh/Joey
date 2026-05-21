package com.joey.gamepad.view

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.*
import android.os.Handler
import android.os.Looper
import android.view.MotionEvent
import android.view.View
import com.joey.gamepad.controller.*
import com.joey.gamepad.model.AxisState
import com.joey.gamepad.model.GamepadState
import com.joey.gamepad.model.GamepadState.Companion as B
import com.joey.gamepad.network.WebSocketManager

@SuppressLint("ClickableViewAccessibility")
class ControllerView(context: Context) : View(context) {

    private val wsManager = WebSocketManager()
    private var controllerType = ControllerType.PLAYSTATION
    private var renderer: ControllerRenderer = PSControllerRenderer()
    private var gamepadState = GamepadState()

    var lastIp = ""
    val connectionState get() = wsManager.state
    var onConnectionChanged: ((WebSocketManager.State) -> Unit)? = null

    // track which pointer is touching what
    private data class PointerInfo(val hit: HitResult?)
    private val pointers = mutableMapOf<Int, PointerInfo>()

    // Handler for 60fps state send
    private val handler = Handler(Looper.getMainLooper())
    private val sendLoop = object : Runnable {
        override fun run() {
            wsManager.sendGamepadState(gamepadState, controllerType)
            handler.postDelayed(this, 16L)
        }
    }

    init {
        setLayerType(LAYER_TYPE_SOFTWARE, null)  // required for BlurMaskFilter
        isFocusable = true
        isFocusableInTouchMode = true

        wsManager.onStateChanged = { state ->
            onConnectionChanged?.invoke(state)
            invalidate()
        }
        handler.postDelayed(sendLoop, 16L)
    }

    // ── Public API ─────────────────────────────────────────────────────────

    fun connect(ip: String, port: Int = 8765) {
        lastIp = ip
        wsManager.connect(ip, port)
    }

    fun disconnect() {
        wsManager.disconnect()
    }

    fun setControllerType(type: ControllerType) {
        controllerType = type
        renderer = when (type) {
            ControllerType.PLAYSTATION    -> PSControllerRenderer()
            ControllerType.NINTENDO       -> NintendoControllerRenderer()
            ControllerType.RC_TRANSMITTER -> RCControllerRenderer()
        }
        gamepadState = GamepadState()
        pointers.clear()
        invalidate()
    }

    // ── Drawing ────────────────────────────────────────────────────────────

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawColor(Color.parseColor("#08080F"))
        renderer.draw(canvas, width.toFloat(), height.toFloat(), gamepadState)
        drawConnectionStatus(canvas)
    }

    private val statusPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 28f; typeface = Typeface.MONOSPACE }

    private fun drawConnectionStatus(canvas: Canvas) {
        val (text, color) = when (wsManager.state) {
            WebSocketManager.State.CONNECTED    -> "● CONNECTED" to 0xFF00FF88.toInt()
            WebSocketManager.State.CONNECTING   -> "◌ CONNECTING…" to 0xFFFFAA00.toInt()
            WebSocketManager.State.DISCONNECTED -> "○ OFFLINE" to 0xFF666688.toInt()
        }
        statusPaint.color = color
        canvas.drawText(text, 20f, height - 16f, statusPaint)
    }

    // ── Touch handling ─────────────────────────────────────────────────────

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN -> {
                val idx = event.actionIndex
                val pid = event.getPointerId(idx)
                val x = event.getX(idx); val y = event.getY(idx)
                val hit = renderer.hitTest(x, y, width.toFloat(), height.toFloat())
                pointers[pid] = PointerInfo(hit)
                applyTouchDown(hit, x, y)
            }
            MotionEvent.ACTION_MOVE -> {
                for (i in 0 until event.pointerCount) {
                    val pid = event.getPointerId(i)
                    val x = event.getX(i); val y = event.getY(i)
                    pointers[pid]?.hit?.let { applyTouchMove(it, x, y) }
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP -> {
                val idx = event.actionIndex
                val pid = event.getPointerId(idx)
                pointers[pid]?.hit?.let { applyTouchUp(it) }
                pointers.remove(pid)
            }
            MotionEvent.ACTION_CANCEL -> {
                pointers.values.forEach { it.hit?.let { h -> applyTouchUp(h) } }
                pointers.clear()
            }
        }
        invalidate()
        return true
    }

    private fun applyTouchDown(hit: HitResult?, x: Float, y: Float) {
        when (hit) {
            is HitResult.Stick   -> updateStick(hit.id, x, y)
            is HitResult.Button  -> gamepadState = gamepadState.copy(
                pressedButtons = gamepadState.pressedButtons + hit.id
            )
            is HitResult.Trigger -> updateTrigger(hit.id, x, y)
            null -> Unit
        }
    }

    private fun applyTouchMove(hit: HitResult, x: Float, y: Float) {
        when (hit) {
            is HitResult.Stick   -> updateStick(hit.id, x, y)
            is HitResult.Trigger -> updateTrigger(hit.id, x, y)
            else -> Unit
        }
    }

    private fun applyTouchUp(hit: HitResult) {
        when (hit) {
            is HitResult.Stick -> {
                gamepadState = when (hit.id) {
                    B.STICK_LEFT  -> gamepadState.copy(leftStick = AxisState())
                    B.STICK_RIGHT -> gamepadState.copy(rightStick = AxisState())
                    else          -> gamepadState
                }
            }
            is HitResult.Button  -> gamepadState = gamepadState.copy(
                pressedButtons = gamepadState.pressedButtons - hit.id
            )
            is HitResult.Trigger -> gamepadState = when (hit.id) {
                B.TRIGGER_LEFT  -> gamepadState.copy(leftTrigger = 0f)
                B.TRIGGER_RIGHT -> gamepadState.copy(rightTrigger = 0f)
                else            -> gamepadState
            }
        }
    }

    private fun updateStick(id: String, x: Float, y: Float) {
        val w = width.toFloat(); val h = height.toFloat()
        val center = renderer.stickCenter(id, w, h)
        val maxR = renderer.stickMaxRadius(w, h)
        var dx = x - center.x; var dy = y - center.y
        val dist = Math.hypot(dx.toDouble(), dy.toDouble()).toFloat()
        if (dist > maxR) { dx = dx / dist * maxR; dy = dy / dist * maxR }
        val nx = (dx / maxR).coerceIn(-1f, 1f)
        val ny = (dy / maxR).coerceIn(-1f, 1f)
        gamepadState = when (id) {
            B.STICK_LEFT  -> gamepadState.copy(leftStick = AxisState(nx, ny))
            B.STICK_RIGHT -> gamepadState.copy(rightStick = AxisState(nx, ny))
            else          -> gamepadState
        }
    }

    private fun updateTrigger(id: String, x: Float, y: Float) {
        // Use vertical position within the trigger rect for analog value
        val h = height.toFloat()
        val value = (1f - (y / (h * 0.10f))).coerceIn(0f, 1f)
        gamepadState = when (id) {
            B.TRIGGER_LEFT  -> gamepadState.copy(
                leftTrigger = value,
                pressedButtons = if (value > 0.5f) gamepadState.pressedButtons + B.BTN_L2
                                 else gamepadState.pressedButtons - B.BTN_L2
            )
            B.TRIGGER_RIGHT -> gamepadState.copy(
                rightTrigger = value,
                pressedButtons = if (value > 0.5f) gamepadState.pressedButtons + B.BTN_R2
                                 else gamepadState.pressedButtons - B.BTN_R2
            )
            else -> gamepadState
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        handler.removeCallbacks(sendLoop)
        wsManager.disconnect()
    }
}
