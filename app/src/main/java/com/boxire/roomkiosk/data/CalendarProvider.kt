package com.boxire.roomkiosk.data

import com.boxire.roomkiosk.model.RoomEvent
import java.util.Calendar

interface CalendarProvider {
    suspend fun getTodaysEvents(roomId: String): List<RoomEvent>
    suspend fun createQuickBooking(roomId: String, durationMinutes: Int): Boolean
    suspend fun endCurrentMeeting(roomId: String, eventId: String): Boolean
}

class MockCalendarProvider : CalendarProvider {
    // In-memory fake storage
    private val events = mutableListOf<RoomEvent>()

    init {
        // Seed with some dummy data for today
        val now = System.currentTimeMillis()
        val cal = Calendar.getInstance()
        
        // 1. Meeting just finished
        cal.add(Calendar.MINUTE, -60)
        events.add(RoomEvent("Standup", cal.timeInMillis, cal.timeInMillis + 30 * 60000))
        
        // 2. Upcoming later today
        cal.timeInMillis = now
        cal.add(Calendar.HOUR, 1) // in 1 hour
        cal.set(Calendar.MINUTE, 0)
        val start2 = cal.timeInMillis
        val end2 = start2 + 60 * 60000
        events.add(RoomEvent("Product Review", start2, end2))
    }

    override suspend fun getTodaysEvents(roomId: String): List<RoomEvent> {
        return events.sortedBy { it.startTime }
    }

    override suspend fun createQuickBooking(roomId: String, durationMinutes: Int): Boolean {
        val now = System.currentTimeMillis()
        val end = now + durationMinutes * 60000
        events.add(RoomEvent("Kiosk Booking", now, end, true, "kiosk-${System.currentTimeMillis()}"))
        return true
    }

    override suspend fun endCurrentMeeting(roomId: String, eventId: String): Boolean {
        val event = events.find { it.eventId == eventId } ?: return false
        // In a real app we might shorten the event, here we just remove it for visual confirmation
        events.remove(event)
        return true
    }
}
