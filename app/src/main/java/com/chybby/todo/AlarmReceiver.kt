package com.chybby.todo

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.chybby.todo.data.workers.SendReminderNotificationsWorker
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class AlarmReceiver : BroadcastReceiver() {

    @Inject
    lateinit var workManager: WorkManager

    override fun onReceive(context: Context, intent: Intent) {
        Timber.d("Received broadcast.")

        intent.data?.getQueryParameter(LIST_ID_PARAMETER)?.toLongOrNull()?.let { listId ->
            Timber.d("Received intent for sending notifications for listId $listId.")
            val request = OneTimeWorkRequestBuilder<SendReminderNotificationsWorker>()
                .setInputData(workDataOf(SendReminderNotificationsWorker.KEY_LIST_ID to listId))
                .build()

            workManager.enqueue(request)
        }
    }

    companion object {
        const val LIST_ID_PARAMETER = "LIST_ID_PARAMETER"
    }
}