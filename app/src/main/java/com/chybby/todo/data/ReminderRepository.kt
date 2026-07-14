package com.chybby.todo.data

interface ReminderRepository {
    suspend fun createReminder(listId: Long, reminder: Reminder): Result<Unit>

    // Deletes the OS-level trigger(s) for a list's reminder. Pass the reminder being deleted when
    // it is known so only its trigger is touched; with null, all trigger kinds are removed.
    suspend fun deleteReminder(listId: Long, reminder: Reminder? = null): Result<Unit>
}
