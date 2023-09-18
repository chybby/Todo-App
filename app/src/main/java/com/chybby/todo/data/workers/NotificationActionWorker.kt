package com.chybby.todo.data.workers

import android.annotation.SuppressLint
import android.content.Context
import androidx.core.app.NotificationManagerCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.chybby.todo.data.TodoListRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import timber.log.Timber

@HiltWorker
class NotificationActionWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val todoListRepository: TodoListRepository,
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val itemId = inputData.getLong(KEY_ITEM_ID, -1)
        val markDone = inputData.getBoolean(KEY_MARK_DONE, false)

        return try {
            if (itemId == -1L) {
                Timber.e("Missing input item id.")
                throw IllegalArgumentException("Missing input item id")
            }

            if (markDone) {
                // Does nothing if the item was deleted since the notification was sent.
                todoListRepository.completeTodoItem(itemId, true)
            }

            clearNotificationForItem(itemId, todoListRepository, applicationContext)

            return Result.success()
        } catch (throwable: Throwable) {
            Timber.e("Error performing notification action.")
            throwable.printStackTrace()
            Result.failure()
        }
    }

    companion object {
        const val KEY_ITEM_ID = "KEY_ITEM_ID"
        const val KEY_MARK_DONE = "KEY_MARK_DONE"

        @SuppressLint("MissingPermission")
        suspend fun clearNotificationForItem(
            itemId: Long,
            todoListRepository: TodoListRepository,
            context: Context,
        ) {
            val notificationManager = NotificationManagerCompat.from(context)

            val todoItem = todoListRepository.getTodoItem(itemId)

            // Cancel the notification for the item.
            todoItem.notificationId?.also { notificationId ->
                notificationManager.cancel(notificationId)
                todoListRepository.clearNotificationId(notificationId)
            }

            // Cancel the notification for the list if all the notifications for the items have
            // been cancelled.
            val todoList = todoListRepository.getTodoListStreamById(todoItem.listId).first()
            val allItemNotificationsCleared =
                todoListRepository.getTodoItemsStreamByListId(todoItem.listId).first()
                    .all { it.notificationId == null }

            if (allItemNotificationsCleared) {
                todoList.notificationId?.also { notificationId ->
                    notificationManager.cancel(notificationId)
                    todoListRepository.clearNotificationId(notificationId)
                }
                return
            }

            // Update the summary notification if it hasn't been cleared.
            if (notificationManager.activeNotifications.any { it.id == todoList.notificationId }
                && todoList.notificationId != null) {
                val todoItems =
                    todoListRepository.getTodoItemsStreamByListId(todoItem.listId).first()
                        .filter { it.notificationId != null }
                val summaryNotification = SendReminderNotificationsWorker.buildSummaryNotification(
                    context,
                    todoList,
                    todoItems
                )

                notificationManager.notify(todoList.notificationId, summaryNotification)
            }
        }
    }
}