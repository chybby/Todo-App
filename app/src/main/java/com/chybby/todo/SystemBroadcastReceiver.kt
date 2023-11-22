package com.chybby.todo

import android.app.AlarmManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.OneTimeWorkRequest
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.chybby.todo.data.workers.ClearNotificationsWorker
import com.chybby.todo.data.workers.ScheduleRemindersWorker
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class SystemBroadcastReceiver : BroadcastReceiver() {

    @Inject
    lateinit var workManager: WorkManager

    override fun onReceive(context: Context, intent: Intent) {
        Timber.d("Received system broadcast.")
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED -> {
                Timber.d("Received intent with action ${intent.action}")

                val scheduleRemindersRequest = OneTimeWorkRequestBuilder<ScheduleRemindersWorker>()
                    .setInputData(
                        workDataOf(
                            ScheduleRemindersWorker.KEY_SCHEDULE_TIME_REMINDERS to true,
                            ScheduleRemindersWorker.KEY_SCHEDULE_LOCATION_REMINDERS to true,
                        )
                    )
                    .build()

                workManager
                    // Clear notifications from the database. Notifications disappear after a reboot.
                    .beginWith(OneTimeWorkRequest.from(ClearNotificationsWorker::class.java))
                    .then(scheduleRemindersRequest)
                    .enqueue()
            }

            // TODO: geofences probably also need to be rescheduled when location permission is revoked and restored.
            AlarmManager.ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED -> {
                Timber.d("Received intent with action ${intent.action}")
                val request = OneTimeWorkRequestBuilder<ScheduleRemindersWorker>()
                    .setInputData(
                        workDataOf(
                            ScheduleRemindersWorker.KEY_SCHEDULE_TIME_REMINDERS to true,
                        )
                    )
                    .build()
                workManager.enqueue(request)
            }
        }
    }
}