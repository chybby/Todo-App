package com.chybby.todo.data.workers

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.content.edit
import androidx.hilt.work.HiltWorker
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequest
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.chybby.todo.data.Reminder
import com.chybby.todo.data.TodoListRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import timber.log.Timber
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.time.Duration.Companion.milliseconds

// A single self-rescheduling unique work shared by every list with a WifiReminder.
// The network constraint makes Android start it whenever a Wi-Fi network is connected.
// Each run reads the connected SSID, sends notifications for every matching list not
// yet notified for this connection, and re-enqueues itself for the next one. Armed and cancelled
// via ReminderRepository.updateWifiWatch, so the work exists only while a WifiReminder does.
@HiltWorker
class CheckWifiRemindersWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val todoListRepository: TodoListRepository,
    private val workManager: WorkManager,
) : CoroutineWorker(appContext, workerParams) {

    @RequiresApi(Build.VERSION_CODES.R)
    override suspend fun doWork(): Result {
        try {
            val wifiLists = todoListRepository.getTodoLists()
                .filter { it.reminder is Reminder.WifiReminder }
            if (wifiLists.isEmpty()) {
                // The last WifiReminder was deleted after this run was enqueued.
                Timber.d("No Wi-Fi reminders remain.")
                return Result.success()
            }

            val connectivityManager = applicationContext
                .getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            if (connectivityManager == null) {
                Timber.e("ConnectivityManager is null")
                rescheduleSelf(connectedToWifi = true)
                return Result.success()
            }

            val wifiNetwork = currentWifiNetwork(connectivityManager)
            if (wifiNetwork == null) {
                // Usually Wi-Fi disconnected between the constraint being satisfied and this
                // run, in which case the network constraint alone gates the next run. But if the
                // default network still reports the WI-FI transport (e.g. a VPN over Wi-Fi hiding
                // the physical network), the constraint is still satisfied, so wait out the
                // delay to avoid a hot loop.
                Timber.d("No connected Wi-Fi network found.")
                rescheduleSelf(connectedToWifi = isDefaultNetworkWifi(connectivityManager))
                return Result.success()
            }

            val ssid = wifiNetwork.ssid
            if (ssid == null) {
                // The SSID is redacted (Location toggle off, or a permission revoked since the
                // reminder was saved). Keep watching so notifications resume once readable.
                Timber.w("Connected SSID is unreadable (missing permission?).")
                rescheduleSelf(connectedToWifi = true)
                return Result.success()
            }

            // The constraint stays satisfied while connected, so later runs see a connection
            // that was already handled. Remember which lists were notified for this network. A
            // new connection has a new network handle, which resets the bookkeeping and lets
            // the reminder fire again.
            val prefs = applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val isNewConnection =
                prefs.getLong(KEY_HANDLED_NETWORK_HANDLE, -1L) != wifiNetwork.network.networkHandle
            val notifiedListIds = if (isNewConnection) {
                mutableSetOf()
            } else {
                prefs.getStringSet(KEY_NOTIFIED_LIST_IDS, null).orEmpty().toMutableSet()
            }

            for (todoList in wifiLists) {
                val reminder = todoList.reminder
                if (reminder !is Reminder.WifiReminder) continue
                if (reminder.ssid != ssid) continue
                if (todoList.id.toString() in notifiedListIds) continue

                Timber.d("Connected to $ssid, sending notifications for listId ${todoList.id}.")
                workManager.enqueue(
                    OneTimeWorkRequestBuilder<SendReminderNotificationsWorker>()
                        .setInputData(
                            workDataOf(SendReminderNotificationsWorker.KEY_LIST_ID to todoList.id)
                        )
                        .build()
                )
                notifiedListIds.add(todoList.id.toString())
            }

            prefs.edit {
                putLong(KEY_HANDLED_NETWORK_HANDLE, wifiNetwork.network.networkHandle)
                    .putStringSet(KEY_NOTIFIED_LIST_IDS, notifiedListIds)
            }

            // Must be the last statement: replacing the unique work cancels this running
            // instance, so WorkManager records this run as CANCELLED and ignores the returned
            // Result.
            rescheduleSelf(connectedToWifi = true)
            return Result.success()
        } catch (e: CancellationException) {
            // Includes the cancellation from the self-replace above. Rethrow so it isn't
            // converted into a retry of a WorkSpec that no longer exists.
            throw e
        } catch (e: Exception) {
            // Retry keeps this same WorkSpec enqueued, with backoff and the network constraint
            // intact, so the watch survives unexpected failures instead of silently dying.
            Timber.e(e)
            return Result.retry()
        }
    }

    // While connected, a zero delay would re-run immediately against the still-satisfied
    // constraint, so wait out the delay first. While disconnected the constraint alone gates
    // the next run, so a fresh connection fires promptly.
    private fun rescheduleSelf(connectedToWifi: Boolean) {
        val delayMinutes = if (connectedToWifi) RESCHEDULE_DELAY_MINUTES else 0L
        workManager.enqueueUniqueWork(WORK_NAME, ExistingWorkPolicy.REPLACE, request(delayMinutes))
    }

    private fun isDefaultNetworkWifi(connectivityManager: ConnectivityManager): Boolean {
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
    }

    private data class WifiNetworkInfo(val network: Network, val ssid: String?)

    // Finds the connected Wi-Fi network. Returns null when there is none, and a WifiNetworkInfo
    // with a null ssid when connected but the SSID is redacted. A callback registration is used
    // to locate the network (activeNetwork can be cellular while Wi-Fi is up) and, on API 31+,
    // because a callback created with FLAG_INCLUDE_LOCATION_INFO is the only way to read an
    // unredacted SSID: synchronous calls always redact location-sensitive fields there.
    @RequiresApi(Build.VERSION_CODES.R)
    private suspend fun currentWifiNetwork(
        connectivityManager: ConnectivityManager,
    ): WifiNetworkInfo? {
        val request = NetworkRequest.Builder()
            // No VALIDATED/INTERNET capability: captive-portal networks awaiting sign-in
            // should still match.
            .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
            .build()

        var callback: ConnectivityManager.NetworkCallback? = null
        val result = try {
            withTimeoutOrNull(WIFI_CALLBACK_TIMEOUT_MS.milliseconds) {
                suspendCancellableCoroutine<Pair<Network, NetworkCapabilities>> { continuation ->
                    fun onChanged(network: Network, capabilities: NetworkCapabilities) {
                        if (continuation.isActive) {
                            continuation.resume(network to capabilities)
                        }
                    }

                    val networkCallback =
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            object : ConnectivityManager.NetworkCallback(
                                FLAG_INCLUDE_LOCATION_INFO
                            ) {
                                override fun onCapabilitiesChanged(
                                    network: Network,
                                    networkCapabilities: NetworkCapabilities,
                                ) = onChanged(network, networkCapabilities)
                            }
                        } else {
                            object : ConnectivityManager.NetworkCallback() {
                                override fun onCapabilitiesChanged(
                                    network: Network,
                                    networkCapabilities: NetworkCapabilities,
                                ) = onChanged(network, networkCapabilities)
                            }
                        }
                    callback = networkCallback
                    connectivityManager.registerNetworkCallback(request, networkCallback)
                }
            }
        } finally {
            try {
                callback?.let { connectivityManager.unregisterNetworkCallback(it) }
            } catch (_: Exception) {
            }
        } ?: return null

        val (network, capabilities) = result
        val rawSsid =
            (capabilities.transportInfo as? WifiInfo)?.ssid
        val ssid = rawSsid
            ?.takeIf { it != WifiManager.UNKNOWN_SSID }
            ?.removeSurrounding("\"")
        return WifiNetworkInfo(network, ssid)
    }

    companion object {
        const val WORK_NAME = "WifiWatch"

        // Bounds both the worst-case notification delay when disconnecting and reconnecting in
        // quick succession, and how often the watch re-runs while staying connected.
        private const val RESCHEDULE_DELAY_MINUTES = 5L

        private const val WIFI_CALLBACK_TIMEOUT_MS = 10_000L

        private const val PREFS_NAME = "wifi_reminders"
        private const val KEY_HANDLED_NETWORK_HANDLE = "KEY_HANDLED_NETWORK_HANDLE"
        private const val KEY_NOTIFIED_LIST_IDS = "KEY_NOTIFIED_LIST_IDS"

        private fun constraints(): Constraints =
            Constraints.Builder()
                .setRequiredNetworkRequest(
                    NetworkRequest.Builder()
                        // No VALIDATED/INTERNET capability: captive-portal networks awaiting
                        // sign-in should still fire.
                        .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                        // The builder adds NOT_VPN by default. JobScheduler evaluates the
                        // constraint against this app's default network, which under a VPN is
                        // the VPN network itself: it lacks NOT_VPN but still advertises the
                        // WI-FI transport while the VPN runs over Wi-Fi.
                        .removeCapability(NetworkCapabilities.NET_CAPABILITY_NOT_VPN)
                        .build(),
                    // Used instead of the request on API 24 to 27, where UNMETERED approximates
                    // Wi-Fi: metered hotspots are missed and ethernet triggers a harmless
                    // no-match run.
                    NetworkType.UNMETERED,
                )
                .build()

        private fun request(initialDelayMinutes: Long): OneTimeWorkRequest =
            OneTimeWorkRequestBuilder<CheckWifiRemindersWorker>()
                .setConstraints(constraints())
                .setInitialDelay(initialDelayMinutes, TimeUnit.MINUTES)
                .build()

        // Arms the watch, replacing any pending run so a just-created reminder is evaluated
        // immediately when its network is already connected.
        fun armNow(workManager: WorkManager) {
            workManager.enqueueUniqueWork(WORK_NAME, ExistingWorkPolicy.REPLACE, request(0))
        }

        fun cancel(workManager: WorkManager) {
            workManager.cancelUniqueWork(WORK_NAME)
        }

        // Network handles don't survive a reboot and could collide with a fresh boot's handles.
        fun clearHandledNetwork(context: Context) {
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit {
                    clear()
                }
        }

        // Editing a list's reminder makes it eligible to be notified again on the current
        // connection. Without this, deleting and re-creating a reminder while connected to its
        // network would never fire until the next reconnect.
        fun clearNotifiedList(context: Context, listId: Long) {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val notified = prefs.getStringSet(KEY_NOTIFIED_LIST_IDS, null).orEmpty()
            if (listId.toString() in notified) {
                prefs.edit {
                    putStringSet(KEY_NOTIFIED_LIST_IDS, notified - listId.toString())
                }
            }
        }
    }
}