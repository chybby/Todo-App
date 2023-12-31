package com.chybby.todo.data


import android.content.Context
import com.chybby.todo.data.local.TodoItemEntity
import com.chybby.todo.data.local.TodoListDao
import com.chybby.todo.data.local.TodoListEntity
import com.chybby.todo.data.workers.NotificationActionWorker
import com.chybby.todo.di.DefaultDispatcher
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject
import kotlin.reflect.KClass

class OfflineTodoListRepository @Inject constructor(
    private val todoListDao: TodoListDao,
    private val reminderRepository: ReminderRepository,
    @ApplicationContext private val context: Context,
    @DefaultDispatcher private val dispatcher: CoroutineDispatcher,
) : TodoListRepository {

    private suspend fun clearNotificationForItem(itemId: Long) {
        NotificationActionWorker.clearNotificationForItem(itemId, this, context)
    }

    // Streams.

    override val todoListsStream: Flow<List<TodoList>> =
        todoListDao.observeTodoLists().map { todoLists ->
            // Use the dispatcher to map potentially many items.
            withContext(dispatcher) {
                todoLists.toExternal()
            }
        }

    override suspend fun getTodoLists(): List<TodoList> =
        todoListDao.getTodoLists().map { todoLists ->
            // Use the dispatcher to map potentially many items.
            withContext(dispatcher) {
                todoLists.toExternal()
            }
        }

    override fun getTodoListStreamById(id: Long): Flow<TodoList> =
        todoListDao.observeTodoListById(id).map { it.toExternal() }

    override fun getTodoItemsStreamByListId(listId: Long): Flow<List<TodoItem>> =
        todoListDao.observeTodoItemsByListId(listId).map { todoItems ->
            // Use the dispatcher to map potentially many items.
            withContext(dispatcher) {
                todoItems.toExternal()
            }
        }

    override suspend fun getTodoItem(id: Long): TodoItem =
        todoListDao.getTodoItemById(id).toExternal()

    // TodoList.

    override suspend fun addTodoList(): Long {
        return todoListDao.insertTodoListLast(
            TodoListEntity(
                name = "",
                position = 0,
                reminderDateTime = null,
                reminderLocationLatitude = null,
                reminderLocationLongitude = null,
                reminderLocationDescription = null,
                reminderLocationRadius = null,
                notificationId = null,
            )
        )
    }

    override suspend fun moveTodoList(id: Long, afterPosition: Int) =
        todoListDao.moveTodoList(id, afterPosition + 1)

    override suspend fun renameTodoList(id: Long, name: String) =
        todoListDao.updateTodoListName(id, name)

    override suspend fun deleteTodoList(id: Long) {
        val result = reminderRepository.deleteReminder(id)
        if (result.isFailure) {
            Timber.e("Failed to delete reminder.")
            Timber.e(result.exceptionOrNull())
            return
        }
        todoListDao.observeTodoItemsByListId(id).first().map { todoItem ->
            clearNotificationForItem(todoItem.id)
        }
        todoListDao.deleteTodoList(id)
    }

    // Completed items shouldn't have a notification so no need to clear.
    override suspend fun deleteCompleted(id: Long) = todoListDao.deleteCompletedTodoItems(id)

    override suspend fun editTodoListReminder(id: Long, reminder: Reminder?) {
        // Try to create/delete the reminder first as it can fail.
        val result = if (reminder != null) {
            reminderRepository.createReminder(id, reminder)
        } else {
            reminderRepository.deleteReminder(id)
        }

        if (result.isSuccess) {
            todoListDao.updateTodoListReminder(id, reminder)
        }
    }

    override suspend fun scheduleExistingReminders(type: KClass<*>) {
        getTodoLists().forEach { todoList ->
            // If the reminder is time-based and in the past, the alarm will be triggered
            // immediately.
            if (todoList.reminder != null && todoList.reminder::class == type) {
                val result = reminderRepository.createReminder(todoList.id, todoList.reminder)
                if (result.isFailure) {
                    Timber.e("Failed to reschedule reminder.")
                    Timber.e(result.exceptionOrNull())
                }
            }
        }
    }

    override suspend fun allocateTodoListNotificationId(id: Long): Int =
        todoListDao.allocateTodoListNotificationId(id)

    // TodoItem.

    override suspend fun addTodoItem(listId: Long, afterPosition: Int?): Long {
        val todoItem = TodoItemEntity(
            summary = "",
            isCompleted = false,
            listId = listId,
            position = afterPosition?.plus(1) ?: 0,
            notificationId = null,
        )

        return if (afterPosition == null) {
            todoListDao.insertTodoItemLast(todoItem)
        } else {
            todoListDao.insertTodoItemInPosition(todoItem)
        }
    }

    override suspend fun editTodoItemSummary(id: Long, summary: String) =
        todoListDao.updateTodoItemSummary(id, summary)

    override suspend fun completeTodoItem(id: Long, completed: Boolean) {
        todoListDao.updateTodoItemCompleted(id, completed)
        if (completed) {
            clearNotificationForItem(id)
        }
    }


    override suspend fun moveTodoItem(id: Long, afterPosition: Int) =
        todoListDao.moveTodoItem(id, afterPosition + 1)

    override suspend fun deleteTodoItem(id: Long) {
        clearNotificationForItem(id)
        todoListDao.deleteTodoItem(id)
    }

    override suspend fun allocateTodoItemNotificationId(id: Long): Int =
        todoListDao.allocateTodoItemNotificationId(id)

    // Notification

    override suspend fun clearNotificationId(id: Int) =
        todoListDao.deleteNotification(id)

    override suspend fun clearAllNotifications() =
        todoListDao.deleteAllNotifications()
}