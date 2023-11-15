package com.chybby.todo.data

interface ReminderRepository {
    fun createReminder(listId: Long, reminder: Reminder)

    fun deleteReminder(listId: Long)
}