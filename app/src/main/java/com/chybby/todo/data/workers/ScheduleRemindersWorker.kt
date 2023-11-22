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

        if (scheduleTimeReminders) {
            Timber.d("Scheduling existing time-based reminders.")
            todoListRepository.scheduleExistingReminders(Reminder.TimeReminder::class)
        }

        if (scheduleLocationReminders) {
            Timber.d("Scheduling existing location-based reminders.")
            todoListRepository.scheduleExistingReminders(Reminder.LocationReminder::class)
        }

        return Result.success()
    }

    companion object {
        const val KEY_SCHEDULE_TIME_REMINDERS = "KEY_SCHEDULE_TIME_REMINDERS"
        const val KEY_SCHEDULE_LOCATION_REMINDERS = "KEY_SCHEDULE_LOCATION_REMINDERS"
    }
}