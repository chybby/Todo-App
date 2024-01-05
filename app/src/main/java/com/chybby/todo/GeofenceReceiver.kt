package com.chybby.todo

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.chybby.todo.data.workers.SendReminderNotificationsWorker
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingEvent
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class GeofenceReceiver : BroadcastReceiver() {

    @Inject
    lateinit var workManager: WorkManager

    override fun onReceive(context: Context, intent: Intent) {
        Timber.d("Received broadcast.")

        val geofencingEvent = GeofencingEvent.fromIntent(intent) ?: return

        if (geofencingEvent.geofenceTransition == Geofence.GEOFENCE_TRANSITION_EXIT) {
            return
        }

        for (geofence in geofencingEvent.triggeringGeofences ?: listOf()) {
            val listId = geofence.requestId.toLongOrNull() ?: continue
            Timber.d("Received intent for sending notifications for listId $listId.")
            val request = OneTimeWorkRequestBuilder<SendReminderNotificationsWorker>()
                .setInputData(workDataOf(SendReminderNotificationsWorker.KEY_LIST_ID to listId))
                .build()

            workManager.enqueue(request)
        }
    }
}