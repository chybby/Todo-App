package com.chybby.todo.data.workers

import android.annotation.SuppressLint
import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.chybby.todo.NotificationActionReceiver
import com.chybby.todo.R
import com.chybby.todo.TodoApplication
import com.chybby.todo.data.Reminder
import com.chybby.todo.data.TodoItem
import com.chybby.todo.data.TodoList
import com.chybby.todo.data.TodoListRepository
import com.chybby.todo.ui.list.pendingIntentForTodoList
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import timber.log.Timber

@HiltWorker
class SendReminderNotificationsWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val todoListRepository: TodoListRepository,
) : CoroutineWorker(appContext, workerParams) {

    private fun buildItemNotification(todoItem: TodoItem, todoList: TodoList): Notification {

        // Each TodoList has its own notification group.
        val groupKey = GROUP_KEY_REMINDER + todoList.id.toString()

        val doneIntent = Intent(applicationContext, NotificationActionReceiver::class.java)
        doneIntent.action = NotificationActionReceiver.DONE_ACTION
        doneIntent.data = Uri.Builder()
            .appendQueryParameter(
                NotificationActionReceiver.ITEM_ID_PARAMETER,
                todoItem.id.toString()
            )
            .build()
        val donePendingIntent: PendingIntent =
            PendingIntent.getBroadcast(
                /* context = */ applicationContext,
                /* requestCode = */ 0,
                /* intent = */ doneIntent,
                /* flags = */ PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )

        val deleteIntent = Intent(applicationContext, NotificationActionReceiver::class.java)
        deleteIntent.action = NotificationActionReceiver.CLEARED_ACTION
        deleteIntent.data = Uri.Builder()
            .appendQueryParameter(
                NotificationActionReceiver.ITEM_ID_PARAMETER,
                todoItem.id.toString()
            )
            .build()
        val deletePendingIntent: PendingIntent =
            PendingIntent.getBroadcast(
                /* context = */ applicationContext,
                /* requestCode = */ 0,
                /* intent = */ deleteIntent,
                /* flags = */ PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )

        return NotificationCompat.Builder(applicationContext, TodoApplication.REMINDER_CHANNEL_ID)
            .setSmallIcon(R.drawable.todo_notification)
            .setContentTitle(todoItem.summary)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setGroup(groupKey)
            .setGroupAlertBehavior(NotificationCompat.GROUP_ALERT_SUMMARY)
            // Avoid sorting integers lexicographically instead of numerically.
            .setSortKey(todoItem.position.toString().padStart(10, '0'))
            .setContentIntent(applicationContext.pendingIntentForTodoList(todoList.id))
            .addAction(
                R.drawable.done,
                applicationContext.getString(R.string.done),
                donePendingIntent
            )
            .setDeleteIntent(deletePendingIntent)
            .build()
    }

    @SuppressLint("MissingPermission")
    private suspend fun createNotifications(
        notificationManager: NotificationManagerCompat,
        todoList: TodoList,
        todoItems: List<TodoItem>,
    ) {
        // TODO: Have a look at https://developer.android.com/develop/ui/views/notifications/custom-notification/.
        // TODO: Update notification icon.

        // Create a notification for each uncompleted TodoItem.
        for (todoItem in todoItems) {
            val notification = buildItemNotification(todoItem, todoList)

            val itemNotificationId = todoListRepository.allocateTodoItemNotificationId(todoItem.id)

            notificationManager.notify(itemNotificationId, notification)
        }

        // Create a summary notification.
        val summaryNotification = buildSummaryNotification(applicationContext, todoList, todoItems)

        val listNotificationId = todoListRepository.allocateTodoListNotificationId(todoList.id)

        notificationManager.notify(listNotificationId, summaryNotification)
    }

    override suspend fun doWork(): Result {

        val listId = inputData.getLong(KEY_LIST_ID, -1)

        return try {
            if (listId == -1L) {
                Timber.e("Missing input list id.")
                throw IllegalArgumentException("Missing input list id")
            }

            val todoList = todoListRepository.getTodoListStreamById(listId).first()

            if (todoList.reminder is Reminder.TimeReminder) {
                // Delete the reminder from the database. Even if sending the notifications fails,
                // there's no point keeping a reminder for a time in the past around.
                todoListRepository.editTodoListReminder(listId, null)
            }

            val notificationManager = NotificationManagerCompat.from(applicationContext)

            if (!notificationManager.areNotificationsEnabled()) {
                Timber.e("Notifications are not enabled")
                throw IllegalStateException("Notifications are not enabled")
            }

            Timber.d("Sending notifications for list with id $listId")

            val todoItems = todoListRepository.getTodoItemsStreamByListId(listId).first()
                .filter { !it.isCompleted }

            if (todoItems.isEmpty()) {
                // No items to send notifications for.
                return Result.success()
            }

            createNotifications(notificationManager, todoList, todoItems)

            return Result.success()
        } catch (throwable: Throwable) {
            Timber.e("Error sending notifications")
            throwable.printStackTrace()
            Result.failure()
        }
    }

    companion object {
        const val KEY_LIST_ID = "KEY_LIST_ID"
        const val GROUP_KEY_REMINDER = "com.chybby.todo.REMINDER."

        fun buildSummaryNotification(
            context: Context,
            todoList: TodoList,
            todoItems: List<TodoItem>,
        ): Notification {

            // Each TodoList has its own notification group.
            val groupKey = GROUP_KEY_REMINDER + todoList.id.toString()

            val inboxStyle = NotificationCompat.InboxStyle()
            for (todoItem in todoItems) {
                // Up to 6 of these lines are displayed in the summary.
                inboxStyle.addLine(todoItem.summary)
            }

            return NotificationCompat.Builder(context, TodoApplication.REMINDER_CHANNEL_ID)
                .setSmallIcon(R.drawable.todo_notification)
                .setContentTitle(todoList.name)
                .setStyle(inboxStyle)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setOnlyAlertOnce(true)
                .setGroup(groupKey)
                .setGroupAlertBehavior(NotificationCompat.GROUP_ALERT_SUMMARY)
                .setGroupSummary(true)
                .setContentIntent(context.pendingIntentForTodoList(todoList.id))
                .build()
        }
    }
}