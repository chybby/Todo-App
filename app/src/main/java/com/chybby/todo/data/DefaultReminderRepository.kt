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

    private fun createAlarm(listId: Long, dateTime: LocalDateTime) {
        // TODO: this function should probably return an error value.

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

        Timber.d("Setting alarm for listId $listId at $dateTime")
        // This cancels any already existing alarm for the same PendingIntent.
        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            dateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(),
            createPendingIntent(listId, 0)
        )
    }

    private fun createGeofence(listId: Long, latLng: LatLng, radius: Float) {
        // TODO: this function should probably return an error value.

        if (context.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Timber.w("ACCESS_FINE_LOCATION permission missing")
            return
        }

        if (context.checkSelfPermission(Manifest.permission.ACCESS_BACKGROUND_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Timber.w("ACCESS_BACKGROUND_LOCATION permission missing")
            return
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

        geofencingClient.addGeofences(request, createPendingIntent(listId, 1, mutable = true))
            .addOnFailureListener { exception ->
                Timber.e("Failed to add geofence.")
                exception.printStackTrace()
            }
    }

    override fun createReminder(listId: Long, reminder: Reminder) {
        // TODO: this function should probably return an error value.

        when (reminder) {
            is Reminder.TimeReminder -> {
                createAlarm(listId, reminder.dateTime)
                deleteGeofence(listId)
            }

            is Reminder.LocationReminder -> {
                // TODO: custom radius
                createGeofence(listId, reminder.location.latLng, 50f)
                deleteAlarm(listId)
            }
        }
    }

    private fun deleteAlarm(listId: Long) {
        alarmManager?.cancel(createPendingIntent(listId, 0))
    }

    private fun deleteGeofence(listId: Long) {
        geofencingClient.removeGeofences(listOf(listId.toString()))
            .addOnFailureListener { exception ->
                Timber.e("Failed to remove geofence.")
                exception.printStackTrace()
            }
    }

    override fun deleteReminder(listId: Long) {
        deleteAlarm(listId)
        deleteGeofence(listId)
    }
}