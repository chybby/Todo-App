package com.chybby.todo.data

import android.Manifest
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.work.WorkManager
import com.chybby.todo.AlarmReceiver
import com.chybby.todo.GeofenceReceiver
import com.chybby.todo.data.workers.CheckWifiRemindersWorker
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.model.LatLng
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.tasks.await
import timber.log.Timber
import java.time.LocalDateTime
import java.time.ZoneId
import javax.inject.Inject

class DefaultReminderRepository @Inject constructor(
    @param:ApplicationContext val context: Context,
    private val workManager: WorkManager,
) : ReminderRepository {

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
    private val geofencingClient = LocationServices.getGeofencingClient(context)

    private fun createAlarmPendingIntent(
        listId: Long,
    ): PendingIntent {
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

    private fun createAlarm(listId: Long, dateTime: LocalDateTime): Result<Unit> {
        if (alarmManager == null) {
            Timber.e("AlarmManager is null")
            return Result.failure(IllegalStateException())
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
            Timber.w("SCHEDULE_EXACT_ALARM permission missing")
            // When the permission is granted, AlarmReceiver will attempt to set alarms for all
            // saved reminders, so this will run again then.
            return Result.failure(IllegalStateException())
        }

        Timber.d("Setting alarm for listId $listId at $dateTime")
        // This cancels any already existing alarm for the same PendingIntent.
        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            dateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(),
            createAlarmPendingIntent(listId)
        )

        return Result.success(Unit)
    }

    private fun createGeofencePendingIntent(): PendingIntent {
        val intent = Intent(context, GeofenceReceiver::class.java)

        return PendingIntent.getBroadcast(
            context,
            0,
            intent,
            PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    private suspend fun createGeofence(listId: Long, latLng: LatLng, radius: Float): Result<Unit> {
        if (context.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Timber.w("ACCESS_FINE_LOCATION permission missing")
            return Result.failure(IllegalStateException())
        }

        if (context.checkSelfPermission(Manifest.permission.ACCESS_BACKGROUND_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Timber.w("ACCESS_BACKGROUND_LOCATION permission missing")
            return Result.failure(IllegalStateException())
        }

        val geofence = Geofence.Builder()
            // Each TodoList has zero or one geofence identified by its id.
            .setRequestId(listId.toString())
            .setExpirationDuration(Geofence.NEVER_EXPIRE)
            .setCircularRegion(latLng.latitude, latLng.longitude, radius)
            .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER)
            .build()

        val request = GeofencingRequest.Builder()
            .addGeofence(geofence)
            .build()

        try {
            geofencingClient.addGeofences(request, createGeofencePendingIntent())
                .await()
        } catch (e: Exception) {
            Timber.e("Failed to add geofence.")
            Timber.e(e)
            return Result.failure(e)
        }

        Timber.d("Added geofence for listId $listId at $latLng")
        return Result.success(Unit)
    }

    // Without these permissions the worker can't read the connected SSID, so fail at save time
    // rather than silently never firing. SSIDs count as location data: reading them is gated
    // behind the location permissions on every API level (NEARBY_WIFI_DEVICES only covers the
    // connect/manage Wi-Fi APIs and the picker's scans, not SSID reads).
    private fun checkWifiReminderPermissions(): Result<Unit> {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (context.checkSelfPermission(Manifest.permission.NEARBY_WIFI_DEVICES) != PackageManager.PERMISSION_GRANTED) {
                Timber.w("NEARBY_WIFI_DEVICES permission missing")
                return Result.failure(IllegalStateException())
            }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            if (context.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                Timber.w("ACCESS_FINE_LOCATION permission missing")
                return Result.failure(IllegalStateException())
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
                context.checkSelfPermission(Manifest.permission.ACCESS_BACKGROUND_LOCATION) != PackageManager.PERMISSION_GRANTED
            ) {
                Timber.w("ACCESS_BACKGROUND_LOCATION permission missing")
                return Result.failure(IllegalStateException())
            }
        }

        return Result.success(Unit)
    }

    override fun updateWifiWatch(wifiRemindersExist: Boolean) {
        if (wifiRemindersExist) {
            CheckWifiRemindersWorker.armNow(workManager)
        } else {
            CheckWifiRemindersWorker.cancel(workManager)
        }
    }

    override suspend fun createReminder(listId: Long, reminder: Reminder): Result<Unit> {
        when (reminder) {
            is Reminder.TimeReminder -> {
                var result = createAlarm(listId, reminder.dateTime)
                if (result.isFailure) {
                    return result
                }
                result = deleteGeofence(listId)
                if (result.isFailure) {
                    deleteAlarm(listId)
                    return result
                }
                return result
            }

            is Reminder.LocationReminder -> {
                val result = createGeofence(
                    listId,
                    reminder.location.latLng,
                    reminder.location.radius.toFloat()
                )
                if (result.isFailure) {
                    return result
                }
                deleteAlarm(listId)
                return Result.success(Unit)
            }

            is Reminder.WifiReminder -> {
                var result = checkWifiReminderPermissions()
                if (result.isFailure) {
                    return result
                }
                // This runs before the reminder is written to the database, so the immediate
                // run may not see it yet. editTodoListReminder syncs the watch again after the
                // write, and that replacement run does.
                CheckWifiRemindersWorker.armNow(workManager)
                result = deleteGeofence(listId)
                if (result.isFailure) {
                    // Leave the watch armed: it may be serving other lists' Wi-Fi reminders.
                    // updateWifiWatch cancels it when none remain.
                    return result
                }
                deleteAlarm(listId)
                return Result.success(Unit)
            }
        }
    }

    private fun deleteAlarm(listId: Long) {
        alarmManager?.cancel(createAlarmPendingIntent(listId))
    }

    private suspend fun deleteGeofence(listId: Long): Result<Unit> {
        try {
            geofencingClient.removeGeofences(listOf(listId.toString())).await()
        } catch (e: Exception) {
            Timber.e("Failed to remove geofence.")
            Timber.e(e)
            return Result.failure(e)
        }

        return Result.success(Unit)
    }

    override suspend fun deleteReminder(listId: Long, reminder: Reminder?): Result<Unit> {
        return when (reminder) {
            is Reminder.TimeReminder -> {
                // Deleting an alarm can't fail, and skipping the geofence round-trip avoids
                // failing (and keeping a stale reminder around) when Play Services is unavailable.
                deleteAlarm(listId)
                Result.success(Unit)
            }

            is Reminder.LocationReminder -> deleteGeofence(listId)

            is Reminder.WifiReminder -> {
                // The Wi-Fi watch work is shared by all lists, so there is no per-list trigger
                // to delete. updateWifiWatch cancels it once no Wi-Fi reminder remains in the
                // database.
                Result.success(Unit)
            }

            null -> {
                deleteAlarm(listId)
                deleteGeofence(listId)
            }
        }
    }
}