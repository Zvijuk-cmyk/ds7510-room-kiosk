package com.boxire.roomkiosk

import android.graphics.Color
import android.os.Bundle
import android.os.CountDownTimer
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var root: LinearLayout
    private lateinit var txtStatus: TextView
    private lateinit var txtTimer: TextView
    private lateinit var btn15: Button
    private lateinit var btn30: Button
    private lateinit var btn60: Button
    private lateinit var btnEnd: Button

    private var busy: Boolean = false
    private var endAtMs: Long = 0L
    private var timer: CountDownTimer? = null

    // admin toggle: tap status 5 times
    private var tapCount = 0
    private var lastTapMs = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        root = findViewById(R.id.rootLayout)
        txtStatus = findViewById(R.id.txtStatus)
        txtTimer = findViewById(R.id.txtTimer)

        btn15 = findViewById(R.id.btn15)
        btn30 = findViewById(R.id.btn30)
        btn60 = findViewById(R.id.btn60)
        btnEnd = findViewById(R.id.btnEnd)

        btn15.setOnClickListener { startMeeting(15) }
        btn30.setOnClickListener { startMeeting(30) }
        btn60.setOnClickListener { startMeeting(60) }
        btnEnd.setOnClickListener { endMeeting() }

        txtStatus.setOnClickListener { adminToggleGesture() }

        // load state
        val prefs = getSharedPreferences("roomkiosk", MODE_PRIVATE)
        busy = prefs.getBoolean("busy", false)
        endAtMs = prefs.getLong("endAtMs", 0L)

        applyState()
        enableKioskFullscreen()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) enableKioskFullscreen()
    }

    private fun enableKioskFullscreen() {
        // Immersive fullscreen (Android 11+)
        window.decorView.systemUiVisibility =
            (View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_FULLSCREEN)

        // New API (best effort)
        val controller = window.insetsController
        controller?.hide(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
        controller?.systemBarsBehavior =
            WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
    }

    private fun adminToggleGesture() {
        val now = System.currentTimeMillis()
        if (now - lastTapMs > 1500) tapCount = 0
        lastTapMs = now
        tapCount++

        if (tapCount >= 5) {
            tapCount = 0
            if (busy) endMeeting() else startMeeting(15) // quick test default
        }
    }

    private fun startMeeting(minutes: Int) {
        busy = true
        endAtMs = System.currentTimeMillis() + minutes * 60_000L
        persist()
        applyState()
    }

    private fun endMeeting() {
        busy = false
        endAtMs = 0L
        timer?.cancel()
        timer = null
        persist()
        applyState()
    }

    private fun persist() {
        val prefs = getSharedPreferences("roomkiosk", MODE_PRIVATE)
        prefs.edit()
            .putBoolean("busy", busy)
            .putLong("endAtMs", endAtMs)
            .apply()
    }

    private fun applyState() {
        if (!busy) {
            setFreeUI()
            return
        }

        // if time already expired -> free
        val remaining = endAtMs - System.currentTimeMillis()
        if (remaining <= 0) {
            endMeeting()
            return
        }

        setBusyUI()
        startOrRestartTimer(remaining)
    }

    private fun startOrRestartTimer(remainingMs: Long) {
        timer?.cancel()
        timer = object : CountDownTimer(remainingMs, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                txtTimer.text = "Time left: " + formatMs(millisUntilFinished)
            }

            override fun onFinish() {
                endMeeting()
            }
        }.start()
    }

    private fun setFreeUI() {
        root.setBackgroundColor(Color.parseColor("#113A1F")) // discreet green
        txtStatus.text = "FREE"
        txtTimer.text = ""
        btn15.isEnabled = true
        btn30.isEnabled = true
        btn60.isEnabled = true
        btnEnd.isEnabled = false
        btnEnd.alpha = 0.5f
    }

    private fun setBusyUI() {
        root.setBackgroundColor(Color.parseColor("#3A1111")) // discreet red
        txtStatus.text = "BUSY"
        btn15.isEnabled = false
        btn30.isEnabled = false
        btn60.isEnabled = false
        btnEnd.isEnabled = true
        btnEnd.alpha = 1.0f
    }

    private fun formatMs(ms: Long): String {
        val totalSeconds = (ms / 1000).toInt()
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return String.format("%02d:%02d", minutes, seconds)
    }
}
