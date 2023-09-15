package com.chybby.todo.data.workers

import android.annotation.SuppressLint
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.chybby.todo.R
import com.chybby.todo.TodoApplication
import com.chybby.todo.data.TodoItem
import com.chybby.todo.data.TodoList
import com.chybby.todo.data.TodoListRepository
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

    @SuppressLint("MissingPermission")
    fun createNotifications(
        notificationManager: NotificationManagerCompat,
        todoList: TodoList,
        todoItems: List<TodoItem>,
    ) {
        //TODO: Create a notification group for each todo list.
        //TODO: Tapping the notification should open the app to the appropriate list.
        //TODO: Have a look at https://developer.android.com/develop/ui/views/notifications/custom-notification/.
        //TODO: Update notification icon.

        for (todoItem in todoItems) {
            //TODO: Add actions for completing todo items from the notification.
            //TODO: Create NotificationActionReceiver for completing notification actions.
//                val doneIntent = Intent(applicationContext, AlarmReceiver::class.java).apply {
//                    action = "TODO"
//                    putExtra(EXTRA_NOTIFICATION_ID, 0)
//                }
//                val donePendingIntent: PendingIntent =
//                    PendingIntent.getBroadcast(applicationContext, 0, doneIntent,
//                        PendingIntent.FLAG_IMMUTABLE)

            val builder =
                NotificationCompat.Builder(applicationContext, TodoApplication.REMINDER_CHANNEL_ID)
                    .setSmallIcon(R.drawable.notification_icon)
                    .setContentTitle(todoItem.summary)
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            //.addAction(R.drawable.done, applicationContext.getString(R.string.done), donePendingIntent)

            notificationManager.notify(todoItem.id.toInt(), builder.build())
        }
    }

    override suspend fun doWork(): Result {

        val listId = inputData.getLong(KEY_LIST_ID, -1)

        return try {
            if (listId == -1L) {
                Timber.e("No list id given to SendReminderNotificationsWorker")
                throw IllegalArgumentException("Missing input list id")
            }

            // Delete the reminder from the database. Even if sending the notifications fails,
            // there's no point keeping a reminder for a time in the past around.
            todoListRepository.editTodoListReminder(listId, null)

            val notificationManager = NotificationManagerCompat.from(applicationContext)

            if (!notificationManager.areNotificationsEnabled()) {
                Timber.e("Notifications are not enabled")
                throw IllegalStateException("Notifications are not enabled")
            }

            Timber.d("Sending notifications for list with id $listId")

            val todoList = todoListRepository.getTodoListStreamById(listId).first()
            val todoItems = todoListRepository.getTodoItemsStreamByListId(listId).first()
                .filter { !it.isCompleted }

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
        // const val GROUP_KEY_REMINDER = "com.chybby.todo.REMINDER."
    }
}