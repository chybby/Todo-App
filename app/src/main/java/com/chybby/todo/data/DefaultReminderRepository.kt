package com.chybby.todo.data

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import com.chybby.todo.AlarmReceiver
import dagger.hilt.android.qualifiers.ApplicationContext
import timber.log.Timber
import java.time.LocalDateTime
import java.time.ZoneId
import javax.inject.Inject

class DefaultReminderRepository @Inject constructor(
    @ApplicationContext val context: Context,
) : ReminderRepository {

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager

    private fun createPendingIntent(listId: Long): PendingIntent {
        val intent = Intent(context, AlarmReceiver::class.java)
        intent.data = Uri.Builder()
            .appendQueryParameter(AlarmReceiver.LIST_ID_PARAMETER, listId.toString())
            .build()

        return PendingIntent.getBroadcast(
            context,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    override fun createReminder(listId: Long, reminderDateTime: LocalDateTime) {
        if (alarmManager == null) {
            Timber.e("AlarmManager is null")
            return
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
            Timber.w("SCHEDULE_EXACT_ALARM permission missing")
            // When the permission is granted, AlarmReceiver will attempt to set alarms for all
            // saved reminders, so this will run again then.
            return
        }

        Timber.d("Setting alarm for listId $listId at $reminderDateTime")
        // This cancels any already existing alarm for the same PendingIntent.
        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            reminderDateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(),
            createPendingIntent(listId)
        )
    }

    override fun deleteReminder(listId: Long) {
        alarmManager?.cancel(createPendingIntent(listId))
    }
}