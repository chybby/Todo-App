package com.chybby.todo.data.workers

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.chybby.todo.data.Reminder
import com.chybby.todo.data.TodoListRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import timber.log.Timber

@HiltWorker
class ScheduleRemindersWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val todoListRepository: TodoListRepository,
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val scheduleTimeReminders = inputData.getBoolean(KEY_SCHEDULE_TIME_REMINDERS, false)
        val scheduleLocationReminders =
            inputData.getBoolean(KEY_SCHEDULE_LOCATION_REMINDERS, false)
        val scheduleWifiReminders = inputData.getBoolean(KEY_SCHEDULE_WIFI_REMINDERS, false)

        if (scheduleTimeReminders) {
            Timber.d("Scheduling existing time-based reminders.")
            todoListRepository.scheduleExistingReminders(Reminder.TimeReminder::class)
        }

        if (scheduleLocationReminders) {
            Timber.d("Scheduling existing location-based reminders.")
            todoListRepository.scheduleExistingReminders(Reminder.LocationReminder::class)
        }

        if (scheduleWifiReminders) {
            Timber.d("Resyncing the Wi-Fi watch.")
            // Network handles from before a reboot could collide with the new boot's handles.
            // Clear them before arming so the immediate run sees clean state and can notify
            // again for the network connected at boot.
            CheckWifiRemindersWorker.clearHandledNetwork(applicationContext)
            todoListRepository.syncWifiWatch()
        }

        return Result.success()
    }

    companion object {
        const val KEY_SCHEDULE_TIME_REMINDERS = "KEY_SCHEDULE_TIME_REMINDERS"
        const val KEY_SCHEDULE_LOCATION_REMINDERS = "KEY_SCHEDULE_LOCATION_REMINDERS"
        const val KEY_SCHEDULE_WIFI_REMINDERS = "KEY_SCHEDULE_WIFI_REMINDERS"
    }
}