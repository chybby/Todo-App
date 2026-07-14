package com.chybby.todo

import android.app.AlarmManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.location.LocationManager
import androidx.core.location.LocationManagerCompat
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

    private fun scheduleRemindersRequest(
        time: Boolean,
        location: Boolean,
    ): OneTimeWorkRequest = OneTimeWorkRequestBuilder<ScheduleRemindersWorker>()
        .setInputData(
            workDataOf(
                ScheduleRemindersWorker.KEY_SCHEDULE_TIME_REMINDERS to time,
                ScheduleRemindersWorker.KEY_SCHEDULE_LOCATION_REMINDERS to location,
            )
        )
        .build()

    override fun onReceive(context: Context, intent: Intent) {
        Timber.d("Received system broadcast with action ${intent.action}")
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED -> {
                workManager
                    // Clear notifications from the database. Notifications disappear after a reboot.
                    .beginWith(OneTimeWorkRequest.from(ClearNotificationsWorker::class.java))
                    .then(scheduleRemindersRequest(time = true, location = true))
                    .enqueue()
            }

            // TODO: Geofences probably also need to be rescheduled when location permission is revoked and restored.
            AlarmManager.ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED -> {
                workManager.enqueue(scheduleRemindersRequest(time = true, location = false))
            }

            // Alarms are scheduled as a fixed instant computed from the reminder's local date and
            // time, so recompute them when the timezone or clock changes.
            Intent.ACTION_TIMEZONE_CHANGED, Intent.ACTION_TIME_CHANGED -> {
                workManager.enqueue(scheduleRemindersRequest(time = true, location = false))
            }

            // Play Services silently removes geofences when location services are disabled.
            // Re-register them when location is turned back on.
            LocationManager.PROVIDERS_CHANGED_ACTION -> {
                val locationManager =
                    context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
                if (locationManager != null && LocationManagerCompat.isLocationEnabled(
                        locationManager
                    )
                ) {
                    workManager.enqueue(scheduleRemindersRequest(time = false, location = true))
                }
            }
        }
    }
}
