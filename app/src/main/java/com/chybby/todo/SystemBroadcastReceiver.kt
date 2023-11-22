package com.chybby.todo

import android.app.AlarmManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import com.chybby.todo.data.workers.ClearNotificationsWorker
import com.chybby.todo.data.workers.ScheduleAlarmsWorker
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
                workManager
                    // Clear notifications from the database. Notifications disappear after a reboot.
                    .beginWith(OneTimeWorkRequest.from(ClearNotificationsWorker::class.java))
                    .then(OneTimeWorkRequest.from(ScheduleAlarmsWorker::class.java))
                    .enqueue()
            }

            AlarmManager.ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED -> {
                Timber.d("Received intent with action ${intent.action}")
                // TODO: this should only schedule time-based reminders.
                workManager.enqueue(OneTimeWorkRequest.from(ScheduleAlarmsWorker::class.java))
            }
        }
    }
}