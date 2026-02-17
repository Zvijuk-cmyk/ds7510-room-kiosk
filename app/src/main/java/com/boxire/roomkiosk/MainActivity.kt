package com.boxire.roomkiosk

import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.get
import com.boxire.roomkiosk.data.CalendarProvider
import com.boxire.roomkiosk.data.GoogleCalendarProvider
import com.boxire.roomkiosk.data.MockCalendarProvider
import com.boxire.roomkiosk.hardware.LedController
import com.boxire.roomkiosk.hardware.NoOpLedController
import com.boxire.roomkiosk.hardware.VendorLedController
import com.boxire.roomkiosk.model.RoomEvent
import com.boxire.roomkiosk.ui.RoomUiState
import com.boxire.roomkiosk.ui.RoomViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var viewModel: RoomViewModel
    
    // UI References
    private lateinit var layoutStatusContainer: ConstraintLayout
    private lateinit var textRoomName: TextView
    private lateinit var textStatus: TextView
    private lateinit var textStatusDetail: TextView
    private lateinit var layoutActionsAvailable: LinearLayout
    private lateinit var layoutActionsBusy: LinearLayout
    private lateinit var btnBook15: Button
    private lateinit var btnBook30: Button
    private lateinit var btnBook60: Button
    private lateinit var btnEndMeeting: Button
    private lateinit var containerMeetings: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Hardware Logic (Dependency Injection simplified)
        val calendarProvider: CalendarProvider = try {
            MockCalendarProvider() // Defaulting to Mock for stability as requested
            // To use Google: if (hasCredentials) GoogleCalendarProvider(this) else MockCalendarProvider()
        } catch (e: Exception) {
            MockCalendarProvider()
        }

        val ledController: LedController = try {
            // Check for vendor specific flag or property? For now default to NoOp or VendorStub
            VendorLedController() 
        } catch (e: Exception) {
            NoOpLedController()
        }

        // factory for ViewModel not essential for simple app, but good practice. 
        // We'll just construct it here for simplicity or use a factory if needed.
        // Since ViewModelProvider default factory doesn't validly create with args without a factory,
        // we'll create a simple factory or just hold the VM instance if we don't care about config changes (Kiosk doesn't rotate).
        // For correctness, let's use a factory.
        
        viewModel = getViewModel(calendarProvider, ledController)

        bindViews()
        setupClickListeners()
        setupObservers()
        setupKioskMode()
    }

    private fun getViewModel(calendar: CalendarProvider, led: LedController): RoomViewModel {
        return object : androidx.lifecycle.ViewModelProvider.Factory {
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                return RoomViewModel(calendar, led) as T
            }
        }.create(RoomViewModel::class.java)
    }

    private fun bindViews() {
        layoutStatusContainer = findViewById(R.id.layoutStatusContainer)
        textRoomName = findViewById(R.id.textRoomName)
        textStatus = findViewById(R.id.textStatus)
        textStatusDetail = findViewById(R.id.textStatusDetail)
        layoutActionsAvailable = findViewById(R.id.layoutActionsAvailable)
        layoutActionsBusy = findViewById(R.id.layoutActionsBusy)
        btnBook15 = findViewById(R.id.btnBook15)
        btnBook30 = findViewById(R.id.btnBook30)
        btnBook60 = findViewById(R.id.btnBook60)
        btnEndMeeting = findViewById(R.id.btnEndMeeting)
        containerMeetings = findViewById(R.id.containerMeetings)
    }

    private fun setupClickListeners() {
        btnBook15.setOnClickListener { viewModel.bookMeeting(15) }
        btnBook30.setOnClickListener { viewModel.bookMeeting(30) }
        btnBook60.setOnClickListener { viewModel.bookMeeting(60) }
        btnEndMeeting.setOnClickListener { viewModel.endMeeting() }
    }

    private fun setupObservers() {
        viewModel.uiState.observe(this) { state ->
            renderState(state)
        }
    }

    private fun renderState(state: RoomUiState) {
        textRoomName.text = state.roomName

        if (state.isBusy) {
            // BUSY STATE
            setBusyUI(state)
        } else {
            // AVAILABLE STATE
            setAvailableUI(state)
        }

        renderMeetingList(state.upcomingMeetings)
    }

    private fun setBusyUI(state: RoomUiState) {
        // Red Background
        val bg = layoutStatusContainer.background as? GradientDrawable
        val redColor = getColor(R.color.status_busy_bg)
        bg?.setColor(redColor)

        textStatus.text = "Busy"
        
        // Detail: "Ends in X min"
        val minLeft = state.timeUntilNextState / 60000
        textStatusDetail.text = "Ends in $minLeft min"

        layoutActionsAvailable.visibility = View.GONE
        layoutActionsBusy.visibility = View.VISIBLE
    }

    private fun setAvailableUI(state: RoomUiState) {
        // Green Background
        val bg = layoutStatusContainer.background as? GradientDrawable
        val greenColor = getColor(R.color.status_available_bg)
        bg?.setColor(greenColor)

        textStatus.text = "Available"
        
        // Detail: "For X min"
        if (state.upcomingMeetings.isNotEmpty()) {
             val minUntil = state.timeUntilNextState / 60000
             textStatusDetail.text = "For $minUntil min"
        } else {
             textStatusDetail.text = "All day"
        }

        layoutActionsAvailable.visibility = View.VISIBLE
        layoutActionsBusy.visibility = View.GONE
    }

    private fun renderMeetingList(meetings: List<RoomEvent>) {
        containerMeetings.removeAllViews()
        val inflater = LayoutInflater.from(this)
        val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())

        for (meeting in meetings) {
            val view = inflater.inflate(R.layout.item_meeting, containerMeetings, false)
            val titleView = view.findViewById<TextView>(R.id.textTitle)
            val timeView = view.findViewById<TextView>(R.id.textTime)

            titleView.text = meeting.title
            val startStr = timeFormat.format(Date(meeting.startTime))
            val endStr = timeFormat.format(Date(meeting.endTime))
            timeView.text = "$startStr - $endStr"

            containerMeetings.addView(view)
        }
    }

    private fun setupKioskMode() {
        // Keep screen on
        window.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        // Immersive sticky
        enableKioskFullscreen()
        
        // Disable back button
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                // Do nothing
            }
        })
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

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) enableKioskFullscreen()
    }
}
