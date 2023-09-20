package com.chybby.todo.data

import androidx.compose.runtime.Immutable
import java.time.LocalDateTime

sealed interface Reminder {
    // A date and time with no associated timezone.
    data class TimeReminder(val dateTime: LocalDateTime) : Reminder

    // A latitude and longitude.
    data class LocationReminder(val location: Pair<Float, Float>) : Reminder
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
