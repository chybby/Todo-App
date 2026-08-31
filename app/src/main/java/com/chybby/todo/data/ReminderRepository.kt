package com.chybby.todo.data

interface ReminderRepository {
    suspend fun createReminder(listId: Long, reminder: Reminder): Result<Unit>

    // Deletes the OS-level trigger(s) for a list's reminder. Pass the reminder being deleted when
    // it is known so only its trigger is touched; with null, all trigger kinds are removed.
    suspend fun deleteReminder(listId: Long, reminder: Reminder? = null): Result<Unit>

    // The Wi-Fi watch is a single self-rescheduling WorkManager work shared by every list with a
    // WifiReminder, so it can only be cancelled once no such reminder remains in the database.
    // Callers invoke this after reminder writes with the current existence state. Redundant calls
    // are safe.
    fun updateWifiWatch(wifiRemindersExist: Boolean)
}
