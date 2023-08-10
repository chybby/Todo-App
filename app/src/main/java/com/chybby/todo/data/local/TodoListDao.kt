package com.chybby.todo.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface TodoListDao {

    // Get

    @Query("SELECT * FROM todo_list ORDER BY position")
    fun observeTodoLists(): Flow<List<TodoListEntity>>

    @Query("SELECT * FROM todo_list WHERE id = :id")
    fun observeTodoListById(id: Long): Flow<TodoListEntity>

    @Query("SELECT * FROM todo_item WHERE list_id = :listId ORDER BY position")
    fun observeTodoItemsByListId(listId: Long): Flow<List<TodoItemEntity>>

    @Query("SELECT position FROM todo_item WHERE list_id = :listId ORDER BY position DESC LIMIT 1")
    suspend fun getLastPositionInList(listId: Long): Int

    // Insert

    @Insert
    suspend fun insertTodoList(todoItem: TodoListEntity): Long

    @Insert
    suspend fun insertTodoItem(todoItem: TodoItemEntity): Long

    @Transaction
    suspend fun insertTodoItemLast(todoItem: TodoItemEntity): Long {
        val position = getLastPositionInList(todoItem.listId)
        val todoItemWithPosition = todoItem.copy(position = position)
        return insertTodoItem(todoItemWithPosition)
    }

    @Transaction
    suspend fun insertTodoItemInPosition(todoItem: TodoItemEntity): Long {
        prepareForTodoItemInsertion(todoItem.listId, todoItem.position)
        return insertTodoItem(todoItem)
    }

    // Modify

    @Query("UPDATE todo_list SET name = :name WHERE id = :id")
    suspend fun updateTodoListName(id: Long, name: String)

    @Query("UPDATE todo_item SET summary = :summary WHERE id = :id")
    suspend fun updateTodoItemSummary(id: Long, summary: String)

    @Query("UPDATE todo_item SET is_completed = :completed WHERE id = :id")
    suspend fun updateTodoItemCompleted(id: Long, completed: Boolean)

    @Query("UPDATE todo_item SET position = position + 1 WHERE list_id = :listId AND position >= :position")
    suspend fun prepareForTodoItemInsertion(listId: Long, position: Int)

    // Delete

    @Query("DELETE from todo_list WHERE id = :id")
    suspend fun deleteTodoList(id: Long)

    @Query("DELETE from todo_item WHERE id = :id")
    suspend fun deleteTodoItem(id: Long)

    @Query("DELETE from todo_item WHERE list_id = :listId AND is_completed")
    suspend fun deleteCompleted(listId: Long)
}