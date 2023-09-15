package com.chybby.todo.data

import java.time.LocalDateTime

interface ReminderRepository {
    fun createReminder(listId: Long, reminderDateTime: LocalDateTime)

    fun deleteReminder(listId: Long)
}