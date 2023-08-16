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

    @Query("SELECT position FROM todo_list ORDER BY position DESC LIMIT 1")
    suspend fun getLastListPosition(): Int

    @Query("SELECT position FROM todo_item WHERE list_id = :listId ORDER BY position DESC LIMIT 1")
    suspend fun getLastItemPositionInList(listId: Long): Int

    @Query("SELECT * FROM todo_item WHERE id = :id")
    suspend fun getTodoItemById(id: Long): TodoItemEntity

    // Insert

    @Insert
    suspend fun insertTodoList(todoList: TodoListEntity): Long

    @Transaction
    suspend fun insertTodoListLast(todoList: TodoListEntity): Long {
        val position = getLastListPosition() + 1
        val todoListWithPosition = todoList.copy(position = position)
        return insertTodoList(todoListWithPosition)
    }

    @Insert
    suspend fun insertTodoItem(todoItem: TodoItemEntity): Long

    @Transaction
    suspend fun insertTodoItemLast(todoItem: TodoItemEntity): Long {
        val position = getLastItemPositionInList(todoItem.listId) + 1
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

    @Query("UPDATE todo_list SET position = position + 1 WHERE position >= :position")
    suspend fun prepareForTodoListInsertion(position: Int)

    @Query("UPDATE todo_list SET position = :position WHERE id = :id")
    suspend fun updateTodoListPosition(id: Long, position: Int)

    @Transaction
    suspend fun moveTodoList(id: Long, position: Int) {
        prepareForTodoListInsertion(position)
        updateTodoListPosition(id, position)
    }

    @Query("UPDATE todo_item SET summary = :summary WHERE id = :id")
    suspend fun updateTodoItemSummary(id: Long, summary: String)

    @Query("UPDATE todo_item SET is_completed = :completed WHERE id = :id")
    suspend fun updateTodoItemCompleted(id: Long, completed: Boolean)

    @Query("UPDATE todo_item SET position = position + 1 WHERE list_id = :listId AND position >= :position")
    suspend fun prepareForTodoItemInsertion(listId: Long, position: Int)

    @Query("UPDATE todo_item SET position = :position WHERE id = :id")
    suspend fun updateTodoItemPosition(id: Long, position: Int)

    @Transaction
    suspend fun moveTodoItem(id: Long, position: Int) {
        val todoItem = getTodoItemById(id)
        prepareForTodoItemInsertion(todoItem.listId, position)
        updateTodoItemPosition(id, position)
    }

    // Delete

    @Query("DELETE from todo_list WHERE id = :id")
    suspend fun deleteTodoList(id: Long)

    @Query("DELETE from todo_item WHERE id = :id")
    suspend fun deleteTodoItem(id: Long)

    @Query("DELETE from todo_item WHERE list_id = :listId AND is_completed")
    suspend fun deleteCompleted(listId: Long)
}