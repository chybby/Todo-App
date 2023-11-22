package com.chybby.todo.data

import androidx.compose.runtime.Immutable
import com.google.android.gms.maps.model.LatLng
import java.time.LocalDateTime

data class Location(
    val latLng: LatLng,
    val radius: Double,
    val description: String,
)

sealed interface Reminder {
    // A date and time with no associated timezone.
    data class TimeReminder(val dateTime: LocalDateTime) : Reminder

    // A latitude, longitude and radius.
    data class LocationReminder(val location: Location) : Reminder
}

@Immutable
data class TodoList(
    val name: String = "",
    // Where this TodoList is positioned relative to other TodoLists.
    val position: Int = 0,
    val reminder: Reminder? = null,
    val notificationId: Int? = null,
    val id: Long = 0,
)

@Immutable
data class TodoItem(
    val summary: String = "",
    val isCompleted: Boolean = false,
    val listId: Long,
    // Where this TodoItem is positioned relative to other TodoItems in the same TodoList.
    val position: Int = 0,
    val notificationId: Int? = null,
    val id: Long = 0,
)
