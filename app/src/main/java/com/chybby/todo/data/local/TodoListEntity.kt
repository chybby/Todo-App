package com.chybby.todo.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import java.time.LocalDateTime

@Entity(
    tableName = "todo_list",
    foreignKeys = [
        ForeignKey(
            entity = NotificationEntity::class,
            parentColumns = arrayOf("id"),
            childColumns = arrayOf("notification_id"),
            onDelete = ForeignKey.SET_NULL,
            onUpdate = ForeignKey.CASCADE,
        ),
    ]
)
data class TodoListEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    // Where this TodoList is positioned relative to other TodoLists.
    val position: Int,

    // A date and time with no associated timezone.
    @ColumnInfo(name = "reminder_date_time")
    val reminderDateTime: LocalDateTime?,

    // TODO: remind based on location.

    @ColumnInfo(name = "notification_id")
    val notificationId: Int?,
)