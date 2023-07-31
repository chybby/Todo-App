package com.chybby.todo.data


import com.chybby.todo.data.local.TodoItemEntity
import com.chybby.todo.data.local.TodoListDao
import com.chybby.todo.data.local.TodoListEntity
import com.chybby.todo.di.DefaultDispatcher
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject

class OfflineTodoListRepository @Inject constructor(
    private val todoListDao: TodoListDao,
    @DefaultDispatcher private val dispatcher: CoroutineDispatcher,
): TodoListRepository {

    // Streams.

    override val todoListsStream: Flow<List<TodoList>> = todoListDao.observeTodoLists().map {todoLists ->
        // Use the dispatcher to map potentially many items.
        withContext(dispatcher) {
            todoLists.toExternal()
        }
    }

    override fun getTodoListStreamById(id: Long): Flow<TodoList> = todoListDao.observeTodoListById(id).map { it.toExternal() }

    override fun getTodoItemsStreamByListId(listId: Long): Flow<List<TodoItem>> = todoListDao.observeTodoItemsByListId(listId).map {todoItems ->
        // Use the dispatcher to map potentially many items.
        withContext(dispatcher) {
            todoItems.toExternal()
        }
    }

    // TodoList.

    override suspend fun addTodoList(): Long {
        return todoListDao.upsertTodoList(TodoListEntity(name = "", position = 0)) // TODO: position at the end.
    }

    override suspend fun renameTodoList(id: Long, name: String) = todoListDao.updateTodoListName(id, name)

    // TODO: Delete operations

    // TodoItem.

    override suspend fun addTodoItem(listId: Long): Long {
        return todoListDao.upsertTodoItem(TodoItemEntity(
            summary = "",
            isCompleted = false,
            listId = listId,
            position = 0, // TODO: position at the end.
        ))
    }

    override suspend fun editTodoItemSummary(id: Long, summary: String) = todoListDao.updateTodoItemSummary(id, summary)

    override suspend fun completeTodoItem(id: Long, completed: Boolean) = todoListDao.updateTodoItemCompleted(id, completed)

    // TODO: Delete operations

}