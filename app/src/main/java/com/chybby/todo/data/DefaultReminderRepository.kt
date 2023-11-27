package com.chybby.todo.data

import android.Manifest
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import com.chybby.todo.AlarmReceiver
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
    @ApplicationContext val context: Context,
) : ReminderRepository {

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
    private val geofencingClient = LocationServices.getGeofencingClient(context)

    private fun createPendingIntent(
        listId: Long,
        requestCode: Int,
        mutable: Boolean = false,
    ): PendingIntent {
        val intent = Intent(context, AlarmReceiver::class.java)
        intent.data = Uri.Builder()
            .appendQueryParameter(AlarmReceiver.LIST_ID_PARAMETER, listId.toString())
            .build()

        return PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            if (mutable) PendingIntent.FLAG_MUTABLE else PendingIntent.FLAG_IMMUTABLE
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
            createPendingIntent(listId, 0)
        )

        return Result.success(Unit)
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
            geofencingClient.addGeofences(request, createPendingIntent(listId, 1, mutable = true))
                .await()
        } catch (e: Exception) {
            Timber.e("Failed to add geofence.")
            Timber.e(e)
            return Result.failure(e)
        }

        Timber.d("Added geofence for listId $listId at $latLng")
        return Result.success(Unit)
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
        }
    }

    private fun deleteAlarm(listId: Long) {
        alarmManager?.cancel(createPendingIntent(listId, 0))
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

    override suspend fun deleteReminder(listId: Long): Result<Unit> {
        deleteAlarm(listId)
        return deleteGeofence(listId)
    }
}