package com.joey.gamepad.controller

import android.graphics.PointF

enum class ControllerType { PLAYSTATION, NINTENDO, RC_TRANSMITTER }

sealed class HitResult {
    data class Stick(val id: String) : HitResult()
    data class Button(val id: String) : HitResult()
    data class Trigger(val id: String) : HitResult()
}
