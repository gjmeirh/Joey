package com.joey.gamepad

import android.graphics.*
import android.os.Bundle
import android.view.*
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.snackbar.Snackbar
import com.joey.gamepad.controller.ControllerType
import com.joey.gamepad.network.WebSocketManager
import com.joey.gamepad.view.ControllerView

class MainActivity : AppCompatActivity() {

    private lateinit var controllerView: ControllerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enterFullscreen()

        val root = FrameLayout(this).apply {
            setBackgroundColor(Color.parseColor("#08080F"))
        }

        controllerView = ControllerView(this)
        controllerView.onConnectionChanged = { state ->
            if (state == WebSocketManager.State.DISCONNECTED &&
                controllerView.lastIp.isNotEmpty()) {
                showSnack("Disconnected from ${controllerView.lastIp}")
            }
        }
        root.addView(controllerView, FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT)

        // Hamburger menu button — top-right corner
        val menuBtn = HamburgerButton(this)
        menuBtn.setOnClickListener { showMenu(it) }
        val lp = FrameLayout.LayoutParams(96, 96).apply {
            gravity = Gravity.TOP or Gravity.END
            setMargins(0, 28, 28, 0)
        }
        root.addView(menuBtn, lp)

        setContentView(root)
    }

    // ── Menu ───────────────────────────────────────────────────────────────

    private fun showMenu(anchor: View) {
        val popup = PopupMenu(this, anchor)
        popup.menu.add(0, 1, 0, getString(R.string.menu_connect))
        popup.menu.add(0, 2, 0, getString(R.string.menu_disconnect))
        popup.menu.add(0, 3, 0, getString(R.string.menu_gamepad))
        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                1 -> showConnectDialog()
                2 -> { controllerView.disconnect(); showSnack("Disconnected") }
                3 -> showGamepadMenu()
            }
            true
        }
        popup.show()
    }

    private fun showConnectDialog() {
        val input = EditText(this).apply {
            hint = getString(R.string.connect_hint)
            inputType = android.text.InputType.TYPE_CLASS_TEXT
            setText(controllerView.lastIp)
            setTextColor(Color.WHITE)
            setHintTextColor(Color.GRAY)
            setPadding(32, 24, 32, 24)
        }
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.connect_title))
            .setView(input)
            .setPositiveButton("Connect") { _, _ ->
                val ip = input.text.toString().trim()
                if (ip.isNotEmpty()) {
                    controllerView.connect(ip)
                    showSnack("Connecting to $ip…")
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showGamepadMenu() {
        val options = arrayOf(
            getString(R.string.ctrl_ps4),
            getString(R.string.ctrl_nintendo),
            getString(R.string.ctrl_rc)
        )
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.controller_select))
            .setItems(options) { _, which ->
                val type = when (which) {
                    0 -> ControllerType.PLAYSTATION
                    1 -> ControllerType.NINTENDO
                    2 -> ControllerType.RC_TRANSMITTER
                    else -> ControllerType.PLAYSTATION
                }
                controllerView.setControllerType(type)
                showSnack("Controller: ${options[which]}")
            }
            .show()
    }

    // ── Fullscreen helpers ─────────────────────────────────────────────────

    private fun enterFullscreen() {
        @Suppress("DEPRECATION")
        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )
        @Suppress("DEPRECATION")
        window.decorView.systemUiVisibility = (
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
            or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
            or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
            or View.SYSTEM_UI_FLAG_FULLSCREEN
            or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        )
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) enterFullscreen()
    }

    private fun showSnack(msg: String) =
        Snackbar.make(controllerView, msg, Snackbar.LENGTH_SHORT).show()
}

// ── Hamburger button drawn purely with Canvas ──────────────────────────────

private class HamburgerButton(context: android.content.Context) : View(context) {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF00FFFF.toInt()
        strokeWidth = 4f
        strokeCap = Paint.Cap.ROUND
        maskFilter = BlurMaskFilter(6f, BlurMaskFilter.Blur.NORMAL)
        style = Paint.Style.STROKE
    }
    private val paintCore = Paint(paint).apply { maskFilter = null }

    init { setLayerType(LAYER_TYPE_SOFTWARE, null) }

    override fun onDraw(canvas: Canvas) {
        val cx = width / 2f
        val w2 = width * 0.32f
        val gap = height * 0.22f
        val cy = height / 2f
        for (dy in listOf(-gap, 0f, gap)) {
            canvas.drawLine(cx - w2, cy + dy, cx + w2, cy + dy, paint)
            canvas.drawLine(cx - w2, cy + dy, cx + w2, cy + dy, paintCore)
        }
    }
}
