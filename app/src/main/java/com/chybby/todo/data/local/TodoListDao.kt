package com.chybby.todo.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface TodoListDao {

    // Get

    @Query("SELECT * FROM todo_list ORDER BY position")
    fun observeTodoLists(): Flow<List<TodoListEntity>>

    @Query("SELECT * FROM todo_list where id = :id")
    fun observeTodoListById(id: Long): Flow<TodoListEntity>

    @Query("SELECT * FROM todo_item WHERE list_id = :listId ORDER BY position")
    fun observeTodoItemsByListId(listId: Long): Flow<List<TodoItemEntity>>

    // Upsert

    @Upsert
    suspend fun upsertTodoList(todoItem: TodoListEntity): Long

    @Upsert
    suspend fun upsertTodoItem(todoItem: TodoItemEntity): Long

    // Modify

    @Query("UPDATE todo_list SET name = :name WHERE id = :id")
    suspend fun updateTodoListName(id: Long, name: String)

    @Query("UPDATE todo_item SET summary = :summary WHERE id = :id")
    suspend fun updateTodoItemSummary(id: Long, summary: String)

    @Query("UPDATE todo_item SET is_completed = :completed WHERE id = :id")
    suspend fun updateTodoItemCompleted(id: Long, completed: Boolean)

    // Delete

    @Query("DELETE from todo_list where id = :id")
    suspend fun deleteTodoList(id: Long)
}