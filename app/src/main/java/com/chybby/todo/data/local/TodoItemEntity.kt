package com.chybby.todo.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "todo_item",
    foreignKeys = [ForeignKey(
        entity = TodoListEntity::class,
        parentColumns = arrayOf("id"),
        childColumns = arrayOf("list_id"),
        onDelete = ForeignKey.CASCADE,
        onUpdate = ForeignKey.CASCADE,
    )]
)
data class TodoItemEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val summary: String,
    @ColumnInfo(name = "is_completed")
    val isCompleted: Boolean,
    // TODO: index
    @ColumnInfo(name = "list_id")
    val listId: Long,
    // The position of the item in the containing list.
    val position: Int,
)
