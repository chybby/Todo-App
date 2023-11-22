package com.chybby.todo.ui.list

import android.Manifest
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.app.ActivityCompat
import com.chybby.todo.R
import com.chybby.todo.data.Location
import com.chybby.todo.data.Reminder
import com.chybby.todo.rememberMultiplePermissionsStateSafe
import com.chybby.todo.rememberPermissionStateSafe
import com.chybby.todo.ui.theme.TodoTheme
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.MultiplePermissionsState
import com.google.accompanist.permissions.PermissionState
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.shouldShowRationale
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.AutocompletePrediction
import com.google.android.libraries.places.api.model.AutocompleteSessionToken
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.net.FetchPlaceRequest
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest
import com.google.maps.android.compose.Circle
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import kotlinx.coroutines.delay
import timber.log.Timber
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

const val MAX_PLACES_RESULTS: Int = 5
const val MAP_CAMERA_ZOOM: Float = 17f
const val MIN_GEOFENCE_RADIUS: Double = 100.0
const val MAX_GEOFENCE_RADIUS: Double = 500.0
const val DEFAULT_GEOFENCE_RADIUS: Double = 200.0
const val INITIALLY_SELECTED_HOUR: Int = 18

val PLACES_QUERY_TYPING_DELAY: Duration = 300.milliseconds

fun dateAndTimeToDateTime(date: Long, hour: Int, minute: Int): LocalDateTime {
    val localDate = LocalDate.ofInstant(Instant.ofEpochMilli(date), ZoneOffset.UTC)
    val localTime = LocalTime.of(hour, minute)
    return LocalDateTime.of(localDate, localTime)
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun ReminderDialog(
    todoListReminder: Reminder?,
    onConfirm: (Reminder) -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit,
) {
    val smallPadding = dimensionResource(R.dimen.padding_small)
    val mediumPadding = dimensionResource(R.dimen.padding_medium)

    val tabTitlesAndIcons = listOf(
        Pair("Time", ImageVector.vectorResource(R.drawable.time)),
        Pair("Location", Icons.Default.LocationOn)
    )
    val timeTabIndex = 0
    val locationTabIndex = 1
    var selectedTab by remember {
        mutableIntStateOf(
            if (todoListReminder is Reminder.LocationReminder) locationTabIndex else timeTabIndex
        )
    }

    var newTimeReminder by remember {
        mutableStateOf<Reminder.TimeReminder?>(
            todoListReminder as? Reminder.TimeReminder ?: Reminder.TimeReminder(
                LocalDate.now().atTime(INITIALLY_SELECTED_HOUR, 0)
            )
        )
    }
    var newLocationReminder by remember { mutableStateOf(todoListReminder as? Reminder.LocationReminder) }

    // Represent the local time as a UTC timestamp.
    var currentDateTime by remember { mutableStateOf(LocalDateTime.now()) }

    var backgroundLocationPermissionRationaleOpen by rememberSaveable { mutableStateOf(false) }

    val backgroundLocationPermissionState = if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.Q) {
        null
    } else {
        rememberPermissionStateSafe(
            Manifest.permission.ACCESS_BACKGROUND_LOCATION
        ) { granted ->
            if (granted) {
                // Background location permission was just granted.
                // Let's assume foreground location permission is granted.
                selectedTab = locationTabIndex
            }
        }
    }

    var foregroundLocationPermissionRationaleOpen by rememberSaveable { mutableStateOf(false) }

    val foregroundLocationPermissions = mutableListOf(
        Manifest.permission.ACCESS_COARSE_LOCATION,
        Manifest.permission.ACCESS_FINE_LOCATION,
    )

    if (Build.VERSION.SDK_INT == Build.VERSION_CODES.Q) {
        // On API level 29, the permission dialog includes an "Allow all the time" option.
        foregroundLocationPermissions.add(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
    }

    val foregroundLocationPermissionsState = rememberMultiplePermissionsStateSafe(
        foregroundLocationPermissions
    ) { granted ->
        if (granted.values.all { it }) {
            // Foreground location permissions were just granted.

            // Still need to check whether background location permission is granted.
            val allPermissionsGranted = requestLocationPermissions(
                // We know the foreground location permission is granted already.
                foregroundLocationPermissionState = null,
                onOpenForegroundLocationPermissionRationale = {},
                backgroundLocationPermissionState = backgroundLocationPermissionState,
                onOpenBackgroundLocationPermissionRationale = {
                    backgroundLocationPermissionRationaleOpen = true
                }
            )

            if (allPermissionsGranted) {
                selectedTab = locationTabIndex
            }
        } else {
            // Re-open the foreground location permission rationale if not all permissions were granted.
            foregroundLocationPermissionRationaleOpen = true
        }
    }

    if (foregroundLocationPermissionRationaleOpen) {
        ForegroundLocationPermissionRationaleDialog(
            onConfirm = {
                foregroundLocationPermissionRationaleOpen = false
                foregroundLocationPermissionsState.launchMultiplePermissionRequest()
            },
            onDismiss = { foregroundLocationPermissionRationaleOpen = false },
        )
    }

    if (backgroundLocationPermissionRationaleOpen && Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        BackgroundLocationPermissionRationaleDialog(
            onConfirm = {
                backgroundLocationPermissionRationaleOpen = false
                backgroundLocationPermissionState?.launchPermissionRequest()
            },
            onDismiss = { backgroundLocationPermissionRationaleOpen = false },
        )
    }

    // Recompose when the current time changes.
    LaunchedEffect(Unit) {
        while (true) {
            delay(1.seconds)
            currentDateTime = LocalDateTime.now()
        }
    }

    val saveButtonEnabled by remember {
        derivedStateOf {
            when (selectedTab) {
                timeTabIndex -> {
                    // Check if time is in the past.
                    (newTimeReminder != null) && (newTimeReminder!!.dateTime > currentDateTime)
                }

                locationTabIndex -> {
                    newLocationReminder != null
                }

                else -> {
                    false
                }
            }
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = MaterialTheme.shapes.extraLarge,
            tonalElevation = 8.dp,
        ) {
            Column(modifier = Modifier.padding(mediumPadding)) {
                val title = when (todoListReminder) {
                    null -> stringResource(R.string.add_reminder)
                    else -> stringResource(R.string.edit_reminder)
                }
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(smallPadding)
                )

                TabRow(selectedTabIndex = selectedTab) {
                    tabTitlesAndIcons.forEachIndexed { index, (title, icon) ->
                        Tab(
                            text = { Text(title) },
                            icon = { Icon(icon, null) },
                            selected = selectedTab == index,
                            onClick = {
                                if (index == locationTabIndex && !requestLocationPermissions(
                                        foregroundLocationPermissionState = foregroundLocationPermissionsState,
                                        onOpenForegroundLocationPermissionRationale = {
                                            foregroundLocationPermissionRationaleOpen = true
                                        },
                                        backgroundLocationPermissionState = backgroundLocationPermissionState,
                                        onOpenBackgroundLocationPermissionRationale = {
                                            backgroundLocationPermissionRationaleOpen = true
                                        }
                                    )
                                ) {
                                    return@Tab
                                }
                                selectedTab = index
                            }
                        )
                    }
                }

                when (selectedTab) {
                    timeTabIndex -> {
                        TimeReminder(
                            timeReminder = newTimeReminder,
                            onReminderUpdated = { reminder ->
                                newTimeReminder = reminder
                            }
                        )
                    }

                    locationTabIndex -> {
                        LocationReminder(
                            locationReminder = newLocationReminder,
                            onReminderUpdated = { reminder ->
                                newLocationReminder = reminder
                            }
                        )
                    }
                }

                Spacer(Modifier.height(smallPadding))

                Row(
                    modifier = Modifier
                        .align(Alignment.End)
                ) {
                    if (todoListReminder != null) {
                        TextButton(onClick = onDelete) {
                            Text(stringResource(R.string.delete))
                        }
                    }
                    TextButton(onClick = onDismiss) {
                        Text(stringResource(R.string.cancel))
                    }
                    Button(
                        onClick = {
                            when (selectedTab) {
                                timeTabIndex -> onConfirm(newTimeReminder!!)
                                locationTabIndex -> onConfirm(newLocationReminder!!)
                            }
                        },
                        enabled = saveButtonEnabled
                    ) {
                        Text(stringResource(R.string.save))
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimeReminder(
    timeReminder: Reminder.TimeReminder?,
    onReminderUpdated: (Reminder.TimeReminder?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val smallPadding = dimensionResource(R.dimen.padding_small)

    var isDatePickerOpen by remember { mutableStateOf(false) }
    var isTimePickerOpen by remember { mutableStateOf(false) }

    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis =
        timeReminder?.dateTime?.toEpochSecond(ZoneOffset.UTC)?.seconds?.inWholeMilliseconds ?:
        // The instant representing the start of the local day in UTC.
        LocalDate.now().atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli()
    )

    // TODO: setting the time to 11:XX PM or 12:XX PM is buggy. Should be fixed in material3 1.2.0
    // 11:XX PM -> 12:XX PM
    // 12:XX PM -> 12:XX AM
    val timePickerState = rememberTimePickerState(
        initialHour = timeReminder?.dateTime?.hour ?: INITIALLY_SELECTED_HOUR,
        initialMinute = timeReminder?.dateTime?.minute ?: 0
    )

    val selectedDateTime = datePickerState.selectedDateMillis?.let {
        dateAndTimeToDateTime(it, timePickerState.hour, timePickerState.minute)
    }

    val time = LocalTime.of(timePickerState.hour, timePickerState.minute)
    val formattedTime = DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).format(time)

    val date = datePickerState.selectedDateMillis?.let {
        LocalDate.ofInstant(Instant.ofEpochMilli(it), ZoneOffset.UTC)
    }
    val formattedDate = date?.let { DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL).format(it) }

    Column(
        modifier = modifier,
    ) {
        TextButton(
            onClick = { isTimePickerOpen = true }
        ) {
            Icon(
                painterResource(R.drawable.time),
                contentDescription = stringResource(R.string.time)
            )
            Spacer(Modifier.width(smallPadding))
            Text(text = formattedTime, style = MaterialTheme.typography.bodyMedium)
        }
        TextButton(
            onClick = { isDatePickerOpen = true }
        ) {
            Icon(
                Icons.Default.DateRange,
                contentDescription = stringResource(R.string.date)
            )
            Spacer(Modifier.width(smallPadding))
            Text(
                text = formattedDate ?: stringResource(R.string.choose_a_date),
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }

    if (isDatePickerOpen) {
        DatePickerDialog(
            onDismissRequest = { isDatePickerOpen = false },
            confirmButton = {
                Button(onClick = {
                    isDatePickerOpen = false
                    onReminderUpdated(selectedDateTime?.let { Reminder.TimeReminder(it) })
                }) {
                    Text(stringResource(R.string.done))
                }
            },
        ) {
            DatePicker(datePickerState)
        }
    }

    if (isTimePickerOpen) {
        TimePickerDialog(
            onDismissRequest = { isTimePickerOpen = false },
            confirmButton = {
                Button(
                    onClick = {
                        isTimePickerOpen = false
                        onReminderUpdated(selectedDateTime?.let { Reminder.TimeReminder(it) })
                    },
                ) {
                    Text(stringResource(R.string.done))
                }
            }
        ) {
            TimePicker(timePickerState)
        }
    }
}

@Composable
fun TimePickerDialog(
    onDismissRequest: () -> Unit,
    confirmButton: @Composable () -> Unit,
    content: @Composable (ColumnScope.() -> Unit),
) {
    Dialog(
        onDismissRequest = onDismissRequest,
    ) {
        Surface(
            shape = MaterialTheme.shapes.extraLarge,
            tonalElevation = 8.dp,
        ) {
            Column(modifier = Modifier.padding(dimensionResource(R.dimen.padding_medium))) {
                content()

                Box(Modifier.align(Alignment.End)) {
                    confirmButton()
                }
            }
        }
    }
}

@Composable
fun LocationReminder(
    locationReminder: Reminder.LocationReminder?,
    onReminderUpdated: (Reminder.LocationReminder?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val smallPadding = dimensionResource(R.dimen.padding_small)

    var isLocationPickerOpen by remember { mutableStateOf(false) }

    var location by remember { mutableStateOf(locationReminder?.location) }

    Column(
        modifier = modifier,
    ) {
        TextButton(
            onClick = { isLocationPickerOpen = true },
        ) {
            Icon(
                Icons.Default.LocationOn,
                contentDescription = stringResource(R.string.location)
            )
            Spacer(Modifier.width(smallPadding))
            Text(
                text = location?.description ?: stringResource(R.string.choose_a_location),
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }

    if (isLocationPickerOpen) {
        LocationPickerDialog(
            location = location,
            onLocationSelected = { newLocation ->
                isLocationPickerOpen = false
                location = newLocation
                onReminderUpdated(Reminder.LocationReminder(newLocation))
            },
            onDismiss = { isLocationPickerOpen = false }
        )
    }
}

fun Address?.getDescription(latLng: LatLng): String {
    return if (this != null && this.maxAddressLineIndex >= 0) {
        val addressLines =
            (0..this.maxAddressLineIndex).map { index ->
                this.getAddressLine(index)
            }
        addressLines.joinToString()
    } else {
        "${latLng.latitude}, ${latLng.longitude}"
    }
}

@Composable
fun LocationPickerDialog(
    location: Location?,
    onLocationSelected: (Location) -> Unit,
    onDismiss: () -> Unit,
) {
    val smallPadding = dimensionResource(R.dimen.padding_small)

    var selectedLocation by remember { mutableStateOf(location) }

    Dialog(
        onDismissRequest = onDismiss,
        DialogProperties()
    ) {
        Surface(
            shape = MaterialTheme.shapes.extraLarge,
            tonalElevation = 8.dp,
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(smallPadding),
                modifier = Modifier
                    .padding(dimensionResource(R.dimen.padding_medium))
            ) {
                LocationSearchBox(
                    selectedLocation = selectedLocation,
                    onLocationSelected = { newLocation: Location ->
                        selectedLocation = newLocation
                    }
                )

                LocationPickerMap(
                    selectedLocation = selectedLocation,
                    onLocationSelected = { newLocation: Location ->
                        selectedLocation = newLocation
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(400.dp)
                )

                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier
                        .fillMaxWidth()
                ) {
                    Slider(
                        value = (selectedLocation?.radius ?: DEFAULT_GEOFENCE_RADIUS).toFloat(),
                        onValueChange = { newRadius ->
                            selectedLocation = selectedLocation?.copy(radius = newRadius.toDouble())
                        },
                        valueRange = MIN_GEOFENCE_RADIUS.toFloat()..MAX_GEOFENCE_RADIUS.toFloat(),
                        modifier = Modifier
                            .weight(1.0f)
                    )
                    Row {
                        TextButton(
                            onClick = onDismiss
                        ) {
                            Text(stringResource(R.string.cancel))
                        }
                        Button(
                            onClick = { onLocationSelected(selectedLocation!!) },
                            enabled = selectedLocation != null,
                        ) {
                            Text(stringResource(R.string.done))
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocationSearchBox(
    selectedLocation: Location?,
    onLocationSelected: (location: Location) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current

    var query by remember(selectedLocation) {
        mutableStateOf(TextFieldValue(selectedLocation?.description ?: ""))
    }
    var keepWholeSelection by remember { mutableStateOf(false) }
    if (keepWholeSelection) {
        // In case onValueChange was not called immediately after onFocusChanged.
        SideEffect {
            keepWholeSelection = false
        }
    }

    val placesClient = if (LocalInspectionMode.current) {
        // Don't create a places client if only previewing.
        null
    } else {
        remember { Places.createClient(context) }
    }
    val token = remember { AutocompleteSessionToken.newInstance() }

    var results by remember { mutableStateOf<List<AutocompletePrediction>>(listOf()) }
    var expanded by remember { mutableStateOf(false) }
    var isSearchBoxFocused by remember { mutableStateOf(false) }

    // Run a places query on new input after a short delay.
    LaunchedEffect(query.text) {
        delay(PLACES_QUERY_TYPING_DELAY)

        val request = FindAutocompletePredictionsRequest.builder()
            .setSessionToken(token)
            .setQuery(query.text)
            .build()

        placesClient!!.findAutocompletePredictions(request)
            .addOnSuccessListener { response ->
                results = response.autocompletePredictions.take(MAX_PLACES_RESULTS)
                if (isSearchBoxFocused) {
                    expanded = true
                }
            }
            .addOnFailureListener { exception ->
                Timber.w("Error when autocompleting place.")
                exception.printStackTrace()
            }
    }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { },
        modifier = modifier,
    ) {
        OutlinedTextField(
            value = query,
            onValueChange = {
                if (keepWholeSelection) {
                    keepWholeSelection = false
                    query = it.copy(selection = TextRange(0, query.text.length))
                } else {
                    query = it
                }
            },
            label = { Text("Search") },
            leadingIcon = { Icon(Icons.Default.Search, "Search") },
            colors = ExposedDropdownMenuDefaults.textFieldColors(),
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor()
                .onFocusChanged { focusState ->
                    isSearchBoxFocused = focusState.isFocused
                    if (focusState.isFocused) {
                        keepWholeSelection = true
                        query = query.copy(selection = TextRange(0, query.text.length))
                    }
                },
        )

        if (results.isNotEmpty()) {
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                results.forEach { prediction ->
                    val text = prediction.getFullText(null).toString()
                    DropdownMenuItem(
                        text = { Text(text) },
                        onClick = {
                            query = TextFieldValue(text = text)
                            focusManager.clearFocus(true)
                            expanded = false

                            val placeFields = listOf(Place.Field.LAT_LNG)

                            val request = FetchPlaceRequest.newInstance(
                                prediction.placeId,
                                placeFields
                            )

                            placesClient!!.fetchPlace(request)
                                .addOnSuccessListener { response ->
                                    response.place.latLng?.let {
                                        onLocationSelected(
                                            Location(
                                                it,
                                                selectedLocation?.radius ?: DEFAULT_GEOFENCE_RADIUS,
                                                text
                                            )
                                        )
                                    }
                                }
                                .addOnFailureListener { exception ->
                                    Timber.w("Error when getting place details.")
                                    exception.printStackTrace()
                                }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun LocationPickerMap(
    selectedLocation: Location?,
    onLocationSelected: (location: Location) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current

    val geocoder = if (LocalInspectionMode.current) {
        // Don't create a geocoder if only previewing.
        null
    } else {
        remember { Geocoder(context) }
    }

    // Make sure the closures below always see the updated value.
    val updatedSelectedLocation by rememberUpdatedState(selectedLocation)

    val cameraPositionState = rememberCameraPositionState {
        selectedLocation?.let { selectedLocation ->
            position = CameraPosition.fromLatLngZoom(
                selectedLocation.latLng, MAP_CAMERA_ZOOM
            )
        }
    }

    LaunchedEffect(selectedLocation) {
        if (selectedLocation != null) {
            // Move the camera position to the new selected location.
            cameraPositionState.move(
                CameraUpdateFactory.newLatLngZoom(
                    selectedLocation.latLng,
                    MAP_CAMERA_ZOOM
                )
            )
        } else {
            // Move the camera position to the current gps location if a location isn't already set.
            if (ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return@LaunchedEffect
            }

            LocationServices.getFusedLocationProviderClient(context).lastLocation.addOnSuccessListener { location ->
                if (location != null) {
                    cameraPositionState.move(
                        CameraUpdateFactory.newLatLngZoom(
                            LatLng(
                                location.latitude,
                                location.longitude
                            ),
                            MAP_CAMERA_ZOOM
                        )
                    )
                }
            }
        }
    }

    GoogleMap(
        properties = MapProperties(isMyLocationEnabled = true),
        uiSettings = MapUiSettings(
            mapToolbarEnabled = true,
            myLocationButtonEnabled = true
        ),
        cameraPositionState = cameraPositionState,
        onMapLongClick = { location ->
            focusManager.clearFocus()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                geocoder!!.getFromLocation(
                    location.latitude,
                    location.longitude,
                    1
                ) { addresses ->
                    val address = addresses.getOrNull(0)
                    onLocationSelected(
                        Location(
                            location,
                            updatedSelectedLocation?.radius ?: DEFAULT_GEOFENCE_RADIUS,
                            address.getDescription(location)
                        )
                    )
                }
            } else {
                // This deprecated function is only used on older Android versions.
                @Suppress("DEPRECATION")
                val address = geocoder!!.getFromLocation(
                    location.latitude,
                    location.longitude,
                    1
                )?.getOrNull(0)

                onLocationSelected(
                    Location(
                        location,
                        updatedSelectedLocation?.radius ?: DEFAULT_GEOFENCE_RADIUS,
                        address.getDescription(location)
                    )
                )
            }
        },
        modifier = modifier
    ) {
        selectedLocation?.let { selectedLocation ->
            Circle(
                center = selectedLocation.latLng,
                radius = selectedLocation.radius,
                strokeColor = MaterialTheme.colorScheme.primary,
                fillColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
            )

            Marker(
                state = MarkerState(
                    position = selectedLocation.latLng
                )
            )
        }
    }
}

@OptIn(ExperimentalPermissionsApi::class)
fun requestLocationPermissions(
    foregroundLocationPermissionState: MultiplePermissionsState?,
    onOpenForegroundLocationPermissionRationale: () -> Unit,
    backgroundLocationPermissionState: PermissionState?,
    onOpenBackgroundLocationPermissionRationale: () -> Unit,
): Boolean {
    if (foregroundLocationPermissionState?.allPermissionsGranted == false) {
        // Foreground location permission isn't granted, go through permission grant flow.
        val allForegroundPermissionsRevoked =
            foregroundLocationPermissionState.permissions.size == foregroundLocationPermissionState.revokedPermissions.size
        if (!allForegroundPermissionsRevoked) {
            // User gave COARSE location permission but not FINE.
            foregroundLocationPermissionState.launchMultiplePermissionRequest()
        } else if (foregroundLocationPermissionState.shouldShowRationale) {
            onOpenForegroundLocationPermissionRationale()
        } else {
            // No permissions granted.
            foregroundLocationPermissionState.launchMultiplePermissionRequest()
        }
        return false
    }

    if (backgroundLocationPermissionState?.status?.isGranted == false) {
        // Background location permission isn't granted, go through permission grant flow.
        if (backgroundLocationPermissionState.status.shouldShowRationale) {
            onOpenBackgroundLocationPermissionRationale()
        } else {
            backgroundLocationPermissionState.launchPermissionRequest()
        }
        return false
    }

    return true
}

@Composable
fun ForegroundLocationPermissionRationaleDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(stringResource(R.string.foreground_location_permission_rationale_title))
        },
        text = {
            Text(stringResource(R.string.foreground_location_permission_rationale_description))
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(stringResource(R.string.yes))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.no))
            }
        },
        modifier = modifier,
    )
}

@RequiresApi(Build.VERSION_CODES.R)
@Composable
fun BackgroundLocationPermissionRationaleDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(stringResource(R.string.background_location_permission_rationale_title))
        },
        text = {
            Text(
                stringResource(
                    R.string.background_location_permission_rationale_description,
                    LocalContext.current.packageManager.backgroundPermissionOptionLabel
                )
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(stringResource(R.string.ok))
            }
        },
        modifier = modifier,
    )
}


@Preview(device = "id:Nexus 5", showSystemUi = true)
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

@Preview(device = "id:Nexus 5", showSystemUi = true)
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

@Preview(device = "id:Nexus 5", showSystemUi = true)
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

@Preview(device = "id:Nexus 5", showSystemUi = true, apiLevel = 33)
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