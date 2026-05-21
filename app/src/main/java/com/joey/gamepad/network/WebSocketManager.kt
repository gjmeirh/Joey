package com.joey.gamepad.network

import android.os.Handler
import android.os.Looper
import com.google.gson.Gson
import com.joey.gamepad.controller.ControllerType
import com.joey.gamepad.model.GamepadState
import okhttp3.*
import java.util.concurrent.TimeUnit

class WebSocketManager {

    enum class State { DISCONNECTED, CONNECTING, CONNECTED }

    private var webSocket: WebSocket? = null
    private val client = OkHttpClient.Builder()
        .pingInterval(5, TimeUnit.SECONDS)
        .connectTimeout(10, TimeUnit.SECONDS)
        .build()
    private val gson = Gson()
    private val mainHandler = Handler(Looper.getMainLooper())

    var state: State = State.DISCONNECTED
        private set

    var onStateChanged: ((State) -> Unit)? = null
    var onError: ((String) -> Unit)? = null

    fun connect(ipAddress: String, port: Int = 8765) {
        if (state != State.DISCONNECTED) return
        updateState(State.CONNECTING)

        val request = Request.Builder().url("ws://$ipAddress:$port").build()
        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(ws: WebSocket, response: Response) =
                mainHandler.post { updateState(State.CONNECTED) }.let {}

            override fun onFailure(ws: WebSocket, t: Throwable, response: Response?) =
                mainHandler.post {
                    updateState(State.DISCONNECTED)
                    onError?.invoke(t.message ?: "Connection failed")
                }.let {}

            override fun onClosed(ws: WebSocket, code: Int, reason: String) =
                mainHandler.post { updateState(State.DISCONNECTED) }.let {}
        })
    }

    fun sendGamepadState(state: GamepadState, controllerType: ControllerType) {
        if (this.state != State.CONNECTED) return
        val msg = buildMessage(state, controllerType)
        webSocket?.send(msg)
    }

    fun disconnect() {
        webSocket?.close(1000, "User disconnect")
        webSocket = null
        updateState(State.DISCONNECTED)
    }

    private fun updateState(newState: State) {
        state = newState
        onStateChanged?.invoke(newState)
    }

    private fun buildMessage(s: GamepadState, ct: ControllerType): String {
        val map = mapOf(
            "type" to "gamepad",
            "controller" to ct.name.lowercase(),
            "axes" to mapOf(
                "left_x" to s.leftStick.x,
                "left_y" to s.leftStick.y,
                "right_x" to s.rightStick.x,
                "right_y" to s.rightStick.y,
                "left_trigger" to s.leftTrigger,
                "right_trigger" to s.rightTrigger
            ),
            "buttons" to GamepadState.run {
                listOf(BTN_A, BTN_B, BTN_X, BTN_Y,
                    BTN_L1, BTN_R1, BTN_L2, BTN_R2, BTN_L3, BTN_R3,
                    BTN_DPAD_UP, BTN_DPAD_DOWN, BTN_DPAD_LEFT, BTN_DPAD_RIGHT,
                    BTN_START, BTN_SELECT, BTN_HOME
                ).associateWith { s.pressedButtons.contains(it) }
            }
        )
        return gson.toJson(map)
    }
}
