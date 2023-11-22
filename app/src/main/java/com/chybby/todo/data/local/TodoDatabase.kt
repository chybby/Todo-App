package com.chybby.todo.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import kotlin.time.Duration.Companion.seconds

class Converters {
    @TypeConverter
    fun fromTimestamp(value: Long?): LocalDateTime? {
        return value?.let { LocalDateTime.ofInstant(Instant.ofEpochMilli(it), ZoneOffset.UTC) }
    }

    @TypeConverter
    fun dateToTimestamp(dateTime: LocalDateTime?): Long? {
        return dateTime?.toEpochSecond(ZoneOffset.UTC)?.seconds?.inWholeMilliseconds
    }
}

@Database(
    entities = [TodoItemEntity::class, TodoListEntity::class, NotificationEntity::class],
    version = 8,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class TodoDatabase : RoomDatabase() {
    abstract fun todoListDao(): TodoListDao
}