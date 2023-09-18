package com.chybby.todo.data

import androidx.compose.runtime.Immutable
import java.time.LocalDateTime

//TODO: reminder can either be time or location based.
//sealed interface Reminder {
//    data class TimeReminder(val dateTime: LocalDateTime): Reminder
//    // data class LocationReminder(val location: ??): Reminder
//}

@Immutable
data class TodoList(
    val name: String = "",
    // Where this TodoList is positioned relative to other TodoLists.
    val position: Int = 0,
    // A date and time with no associated timezone.
    val reminderDateTime: LocalDateTime? = null,
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
