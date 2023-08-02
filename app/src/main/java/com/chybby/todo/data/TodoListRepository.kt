package com.chybby.todo.data

import kotlinx.coroutines.flow.Flow

interface TodoListRepository {
    // Streams.

    val todoListsStream: Flow<List<TodoList>>

    fun getTodoListStreamById(id: Long): Flow<TodoList>

    fun getTodoItemsStreamByListId(listId: Long): Flow<List<TodoItem>>

    // TodoList.

    suspend fun addTodoList(): Long

    suspend fun renameTodoList(id: Long, name: String)

    suspend fun deleteTodoList(id: Long)

    suspend fun deleteCompleted(id: Long)

    // TodoItem.

    suspend fun addTodoItem(listId: Long): Long

    suspend fun editTodoItemSummary(id: Long, summary: String)

    suspend fun completeTodoItem(id: Long, completed: Boolean = true)

    suspend fun deleteTodoItem(id: Long)
}