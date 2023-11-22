package com.chybby.todo

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.chybby.todo.data.workers.NotificationActionWorker
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class NotificationActionReceiver : BroadcastReceiver() {

    @Inject
    lateinit var workManager: WorkManager

    override fun onReceive(context: Context, intent: Intent) {
        Timber.d("Received broadcast.")
        if (intent.action == DONE_ACTION) {
            intent.data?.getQueryParameter(ITEM_ID_PARAMETER)?.toLongOrNull()?.let { itemId ->
                Timber.d("Received intent for marking item with id $itemId complete.")
                val request = OneTimeWorkRequestBuilder<NotificationActionWorker>()
                    .setInputData(
                        workDataOf(
                            NotificationActionWorker.KEY_ITEM_ID to itemId,
                            NotificationActionWorker.KEY_MARK_DONE to true
                        )
                    )
                    .build()

                workManager.enqueue(request)
            }
        } else if (intent.action == CLEARED_ACTION) {
            intent.data?.getQueryParameter(ITEM_ID_PARAMETER)?.toLongOrNull()?.let { itemId ->
                Timber.d("Received intent for clearing notification for item with id $itemId.")
                val request = OneTimeWorkRequestBuilder<NotificationActionWorker>()
                    .setInputData(
                        workDataOf(
                            NotificationActionWorker.KEY_ITEM_ID to itemId,
                            NotificationActionWorker.KEY_MARK_DONE to false
                        )
                    )
                    .build()

                // Wait for any existing clear work to complete.
                workManager.beginUniqueWork(
                    CLEAR_UNIQUE_WORK_ID,
                    ExistingWorkPolicy.APPEND_OR_REPLACE,
                    request
                ).enqueue()
            }
        }
    }

    companion object {
        const val ITEM_ID_PARAMETER = "ITEM_ID_PARAMETER"
        const val DONE_ACTION = "custom.actions.intent.DONE"
        const val CLEARED_ACTION = "custom.actions.intent.CLEARED"
        const val CLEAR_UNIQUE_WORK_ID = "CLEAR_UNIQUE_WORK_ID"
    }
}