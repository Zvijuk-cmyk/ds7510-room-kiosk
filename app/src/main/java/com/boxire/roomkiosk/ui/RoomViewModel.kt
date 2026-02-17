package com.boxire.roomkiosk.ui

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.boxire.roomkiosk.data.CalendarProvider
import com.boxire.roomkiosk.hardware.LedController
import com.boxire.roomkiosk.model.RoomEvent
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.Collections

data class RoomUiState(
    val roomName: String = "Pixel Parkview",
    val isBusy: Boolean = false,
    val currentMeeting: RoomEvent? = null,
    val upcomingMeetings: List<RoomEvent> = emptyList(),
    val timeUntilNextState: Long = 0L, // ms
    val currentTimeParams: String = "" // e.g., "10:00 AM"
)

class RoomViewModel(
    private val calendarProvider: CalendarProvider,
    private val ledController: LedController
) : ViewModel() {

    private val _uiState = MutableLiveData(RoomUiState())
    val uiState: LiveData<RoomUiState> = _uiState

    private var clockJob: Job? = null
    private var refreshJob: Job? = null
    
    // Configurable room ID
    private val roomId = "pixel-parkview-1"

    init {
        startClock()
        startDataRefresh()
    }

    private fun startClock() {
        clockJob = viewModelScope.launch {
            while (isActive) {
                updateTimeAndStatus()
                delay(1000)
            }
        }
    }

    private fun startDataRefresh() {
        refreshJob = viewModelScope.launch {
            while (isActive) {
                fetchEvents()
                delay(30_000) // Refresh every 30s
            }
        }
    }

    private fun updateTimeAndStatus() {
        val now = System.currentTimeMillis()
        val currentEvents = _uiState.value?.upcomingMeetings ?: emptyList() // This might be stale, but fetchEvents updates it
        
        // Re-evaluate busy status every second based on cached events
        // (Actually, fetchEvents updates the list, but we need to check if we just crossed into a meeting time)
        // For simplicity, we rely on fetchEvents to organize the "current" vs "upcoming".
        // But for precise countdowns, we recalculate here.
        
        val state = _uiState.value ?: RoomUiState()
        
        // Update Time Display (not strictly needed in state if UI handles clock, but good for consistency)
        // We'll leave formatting to the UI or binding adapter.

        // Recalculate countdown
        var timeLeft = 0L
        if (state.isBusy && state.currentMeeting != null) {
            timeLeft = state.currentMeeting.endTime - now
        } else if (!state.isBusy && state.upcomingMeetings.isNotEmpty()) {
            timeLeft = state.upcomingMeetings[0].startTime - now
        }
        
        if (timeLeft < 0) timeLeft = 0
        
        _uiState.postValue(state.copy(timeUntilNextState = timeLeft))
    }

    private suspend fun fetchEvents() {
        val events = calendarProvider.getTodaysEvents(roomId)
        val now = System.currentTimeMillis()
        
        // Determine status
        val current = events.find { now >= it.startTime && now < it.endTime }
        val isBusy = current != null
        
        // Filter upcoming
        val upcoming = events.filter { it.startTime > now }.sortedBy { it.startTime }

        val newState = _uiState.value?.copy(
            isBusy = isBusy,
            currentMeeting = current,
            upcomingMeetings = upcoming
        ) ?: RoomUiState(isBusy = isBusy, currentMeeting = current, upcomingMeetings = upcoming)

        _uiState.postValue(newState)
        
        // Update LED
        if (isBusy) ledController.setBusy() else ledController.setAvailable()
    }

    fun bookMeeting(durationMinutes: Int) {
        viewModelScope.launch {
            val success = calendarProvider.createQuickBooking(roomId, durationMinutes)
            if (success) fetchEvents()
        }
    }

    fun endMeeting() {
        val current = _uiState.value?.currentMeeting ?: return
        viewModelScope.launch {
            // If it has an ID, use it, otherwise best effort (mock)
            val eventId = current.eventId ?: "current"
            val success = calendarProvider.endCurrentMeeting(roomId, eventId)
            if (success) fetchEvents()
        }
    }
    
    override fun onCleared() {
        super.onCleared()
        ledController.setOff()
    }
}
