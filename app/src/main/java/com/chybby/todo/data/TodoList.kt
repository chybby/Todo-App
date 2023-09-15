package com.chybby.todo.data

import androidx.compose.runtime.Immutable
import java.time.LocalDateTime

@Immutable
data class TodoList(
    val name: String = "",
    // Where this TodoList is positioned relative to other TodoLists.
    val position: Int = 0,
    // A date and time with no associated timezone.
    val reminderDateTime: LocalDateTime? = null,
    val id: Long = 0,
)

@Immutable
data class TodoItem(
    val summary: String = "",
    val isCompleted: Boolean = false,
    // Where this TodoItem is positioned relative to other TodoItems in the same TodoList.
    val position: Int = 0,
    val id: Long = 0,
)
