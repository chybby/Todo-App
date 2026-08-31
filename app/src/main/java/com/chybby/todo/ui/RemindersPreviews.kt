package com.chybby.todo.ui

import android.content.res.Configuration
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.chybby.todo.data.Location
import com.chybby.todo.data.Reminder
import com.chybby.todo.ui.theme.TodoTheme
import com.google.android.gms.maps.model.LatLng
import java.time.LocalDateTime

// These previews live in their own file because the preview renderer reflects over every method
// of the file class containing a @Preview. Reminders.kt has method signatures referencing
// framework classes the preview classloader can't load (Geocoder, WifiManager), which makes that
// reflection throw NoClassDefFoundError before the preview even runs.

@Preview(device = "id:pixel_9", showSystemUi = true)
@Preview(
    device = "id:pixel_9", showSystemUi = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL
)
@Composable
fun ReminderDialogPreview() {
    TodoTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            ReminderDialog(
                todoListReminder = null,
                onConfirm = {},
                onDelete = {},
                onDismiss = {},
            )
        }
    }
}

@Preview(device = "id:pixel_9", showSystemUi = true)
@Preview(
    device = "id:pixel_9", showSystemUi = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL
)
@Composable
fun ReminderDialogTimePreview() {
    TodoTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            ReminderDialog(
                todoListReminder = Reminder.TimeReminder(
                    dateTime = LocalDateTime.of(
                        /* year = */ 2023,
                        /* month = */ 1,
                        /* dayOfMonth = */ 30,
                        /* hour = */ 15,
                        /* minute = */ 35
                    )
                ),
                onConfirm = {},
                onDelete = {},
                onDismiss = {},
            )
        }
    }
}

@Preview(device = "id:pixel_9", showSystemUi = true)
@Preview(
    device = "id:pixel_9", showSystemUi = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL
)
@Composable
fun ReminderDialogLocationPreview() {
    TodoTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            ReminderDialog(
                todoListReminder = Reminder.LocationReminder(
                    Location(
                        LatLng(0.0, 0.0),
                        DEFAULT_GEOFENCE_RADIUS,
                        "The Whitehouse"
                    )
                ),
                onConfirm = {},
                onDelete = {},
                onDismiss = {},
            )
        }
    }
}

@Preview(device = "id:pixel_9", showSystemUi = true)
@Preview(
    device = "id:pixel_9", showSystemUi = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL
)
@Composable
fun ReminderDialogWifiPreview() {
    TodoTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            ReminderDialog(
                todoListReminder = Reminder.WifiReminder("MyHomeNetwork"),
                onConfirm = {},
                onDelete = {},
                onDismiss = {},
            )
        }
    }
}

@Preview(device = "id:pixel_9", showSystemUi = true)
@Preview(
    device = "id:pixel_9", showSystemUi = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL
)
@Composable
fun WifiPickerDialogPreview() {
    TodoTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            WifiPickerDialog(
                ssid = "MyHomeNetwork",
                onSsidSelected = {},
                onDismiss = {},
            )
        }
    }
}

@Preview(device = "id:pixel_9", showSystemUi = true)
@Preview(
    device = "id:pixel_9", showSystemUi = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL
)
@Composable
fun LocationPickerDialogPreview() {
    TodoTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            LocationPickerDialog(
                location = null,
                onLocationSelected = {},
                onDismiss = {},
            )
        }
    }
}

@Preview(device = "id:pixel_9")
@Preview(
    device = "id:pixel_9",
    uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL
)
@Composable
fun TimeReminderInfoPreview() {
    TodoTheme {
        Surface(
            color = MaterialTheme.colorScheme.background
        ) {
            ReminderInfo(
                Reminder.TimeReminder(LocalDateTime.of(2023, 12, 12, 10, 0)),
                onClick = {},
            )
        }
    }
}

@Preview(device = "id:pixel_9")
@Preview(
    device = "id:pixel_9",
    uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL
)
@Composable
fun LocationReminderInfoPreview() {
    TodoTheme {
        Surface(
            color = MaterialTheme.colorScheme.background
        ) {
            ReminderInfo(
                Reminder.LocationReminder(
                    Location(
                        LatLng(0.0, 0.0),
                        100.0,
                        "Sydney, Australia"
                    )
                ),
                onClick = {},
            )
        }
    }
}

@Preview(device = "id:pixel_9")
@Preview(
    device = "id:pixel_9",
    uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL
)
@Composable
fun WifiReminderInfoPreview() {
    TodoTheme {
        Surface(
            color = MaterialTheme.colorScheme.background
        ) {
            ReminderInfo(
                Reminder.WifiReminder("MyHomeNetwork"),
                onClick = {},
            )
        }
    }
}
