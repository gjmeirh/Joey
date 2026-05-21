package com.joey.gamepad.model

data class AxisState(val x: Float = 0f, val y: Float = 0f)

data class GamepadState(
    val leftStick: AxisState = AxisState(),
    val rightStick: AxisState = AxisState(),
    val leftTrigger: Float = 0f,
    val rightTrigger: Float = 0f,
    val pressedButtons: Set<String> = emptySet()
) {
    companion object {
        const val BTN_A = "a"
        const val BTN_B = "b"
        const val BTN_X = "x"
        const val BTN_Y = "y"
        const val BTN_L1 = "l1"
        const val BTN_R1 = "r1"
        const val BTN_L2 = "l2"
        const val BTN_R2 = "r2"
        const val BTN_L3 = "l3"
        const val BTN_R3 = "r3"
        const val BTN_DPAD_UP = "dpad_up"
        const val BTN_DPAD_DOWN = "dpad_down"
        const val BTN_DPAD_LEFT = "dpad_left"
        const val BTN_DPAD_RIGHT = "dpad_right"
        const val BTN_START = "start"
        const val BTN_SELECT = "select"
        const val BTN_HOME = "home"
        const val STICK_LEFT = "left"
        const val STICK_RIGHT = "right"
        const val TRIGGER_LEFT = "lt"
        const val TRIGGER_RIGHT = "rt"
    }
}
