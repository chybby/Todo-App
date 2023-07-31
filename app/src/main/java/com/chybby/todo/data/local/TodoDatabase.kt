package com.chybby.todo.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [TodoItemEntity::class, TodoListEntity::class], version = 1, exportSchema = false)
abstract class TodoDatabase: RoomDatabase() {
    abstract fun todoListDao(): TodoListDao
}