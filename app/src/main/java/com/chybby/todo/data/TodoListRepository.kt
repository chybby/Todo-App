package com.chybby.todo.data

import kotlinx.coroutines.flow.Flow
import java.time.LocalDateTime

interface TodoListRepository {
    // Streams.

    val todoListsStream: Flow<List<TodoList>>

    suspend fun getTodoLists(): List<TodoList>

    fun getTodoListStreamById(id: Long): Flow<TodoList>

    fun getTodoItemsStreamByListId(listId: Long): Flow<List<TodoItem>>

    // TodoList.

    suspend fun addTodoList(): Long

    suspend fun renameTodoList(id: Long, name: String)

    suspend fun moveTodoList(id: Long, afterPosition: Int)

    suspend fun deleteTodoList(id: Long)

    suspend fun deleteCompleted(id: Long)

    suspend fun editTodoListReminder(id: Long, dateTime: LocalDateTime?)

    suspend fun scheduleExistingReminders()

    // TodoItem.

    suspend fun addTodoItem(listId: Long, afterPosition: Int? = null): Long

    suspend fun editTodoItemSummary(id: Long, summary: String)

    suspend fun completeTodoItem(id: Long, completed: Boolean = true)

    suspend fun moveTodoItem(id: Long, afterPosition: Int)

    suspend fun deleteTodoItem(id: Long)
}