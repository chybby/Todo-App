package com.chybby.todo.data

interface ReminderRepository {
    suspend fun createReminder(listId: Long, reminder: Reminder): Result<Unit>

    suspend fun deleteReminder(listId: Long): Result<Unit>
}