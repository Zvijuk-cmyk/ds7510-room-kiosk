package com.boxire.roomkiosk

import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {

    private lateinit var layoutRoot: ConstraintLayout
    private lateinit var txtStatus: TextView
    private lateinit var btn15: Button
    private lateinit var btn30: Button
    private lateinit var btn60: Button
    private lateinit var groupButtons: LinearLayout

    private var adminTapCount = 0
    private var lastTapTime = 0L
    private val handler = Handler(Looper.getMainLooper())
    // Runnable to check status periodically
    private val statusChecker = object : Runnable {
        override fun run() {
            refreshState()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Hide system bars for immersive mode
        val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
        windowInsetsController.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())

        layoutRoot = findViewById(R.id.layoutRoot)
        txtStatus = findViewById(R.id.txtStatus)
        btn15 = findViewById(R.id.btn15)
        btn30 = findViewById(R.id.btn30)
        btn60 = findViewById(R.id.btn60)
        groupButtons = findViewById(R.id.groupButtons)

        setupButtons()
        setupAdminGesture()
        refreshState()
    }

    private fun setupButtons() {
        val clickListener = View.OnClickListener { v ->
            val minutes = when (v.id) {
                R.id.btn15 -> 15
                R.id.btn30 -> 30
                R.id.btn60 -> 60
                else -> 0
            }
            showConfirmation(minutes)
        }
        btn15.setOnClickListener(clickListener)
        btn30.setOnClickListener(clickListener)
        btn60.setOnClickListener(clickListener)
    }

    private fun showConfirmation(minutes: Int) {
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.confirm_title))
            .setMessage(getString(R.string.confirm_message, minutes))
            .setPositiveButton(getString(R.string.confirm_ok)) { _, _ ->
                bookRoom(minutes)
            }
            .setNegativeButton(getString(R.string.confirm_cancel), null)
            .show()
    }

    private fun bookRoom(minutes: Int) {
        val endTime = System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(minutes.toLong())
        getSharedPreferences("kiosk_prefs", Context.MODE_PRIVATE)
            .edit()
            .putLong("end_time", endTime)
            .apply()
        refreshState()
    }

    private fun setupAdminGesture() {
        txtStatus.setOnClickListener {
            val now = System.currentTimeMillis()
            if (now - lastTapTime < 500) {
                adminTapCount++
            } else {
                adminTapCount = 1
            }
            lastTapTime = now

            if (adminTapCount >= 5) {
                toggleForceFree()
                adminTapCount = 0
            }
        }
    }

    private fun toggleForceFree() {
        val prefs = getSharedPreferences("kiosk_prefs", Context.MODE_PRIVATE)
        val endTime = prefs.getLong("end_time", 0)
        
        if (System.currentTimeMillis() < endTime) {
            // Force Free
            prefs.edit().remove("end_time").apply()
        } else {
            // Force Busy (for testing)
            bookRoom(15)
        }
        refreshState()
    }

    private fun refreshState() {
        val prefs = getSharedPreferences("kiosk_prefs", Context.MODE_PRIVATE)
        val endTime = prefs.getLong("end_time", 0)
        val currentTime = System.currentTimeMillis()
        val isBusy = currentTime < endTime

        // Remove any pending callbacks
        handler.removeCallbacks(statusChecker)

        if (isBusy) {
            layoutRoot.setBackgroundColor(ContextCompat.getColor(this, R.color.bg_busy))
            txtStatus.text = getString(R.string.status_occupied)
            groupButtons.visibility = View.GONE
            
            // Re-check when time is up
            val delay = endTime - currentTime
            if (delay > 0) {
                handler.postDelayed(statusChecker, delay)
            } else {
                // If delay is somehow negative/zero but state logic said busy, fix it next tick
                handler.postDelayed(statusChecker, 1000)
            }
        } else {
            layoutRoot.setBackgroundColor(ContextCompat.getColor(this, R.color.bg_free))
            txtStatus.text = getString(R.string.status_free)
            groupButtons.visibility = View.VISIBLE
        }
    }
}
