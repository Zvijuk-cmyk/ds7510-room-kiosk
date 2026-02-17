package com.boxire.roomkiosk.model

data class RoomEvent(
    val title: String,
    val startTime: Long,
    val endTime: Long,
    val isOrganizerEditable: Boolean = false,
    val eventId: String? = null
)
