package com.chybby.todo.ui.list

import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Build
import android.provider.Settings
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.chybby.todo.R
import com.chybby.todo.data.Location
import com.chybby.todo.data.Reminder
import com.chybby.todo.data.TodoItem
import com.chybby.todo.data.TodoList
import com.chybby.todo.rememberPermissionStateSafe
import com.chybby.todo.ui.ReminderDialog
import com.chybby.todo.ui.ReminderInfo
import com.chybby.todo.ui.isItemWithIndexVisible
import com.chybby.todo.ui.theme.TodoTheme
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.PermissionState
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.shouldShowRationale
import com.google.android.gms.maps.model.LatLng
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import sh.calvin.reorderable.ReorderableCollectionItemScope
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.ReorderableLazyListState
import sh.calvin.reorderable.rememberReorderableLazyListState
import timber.log.Timber
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

val PERSIST_TEXT_FIELD_TYPING_DELAY: Duration = 300.milliseconds

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun TodoListScreen(
    uiState: TodoListScreenUiState,
    onNameChanged: (String) -> Unit,
    onTodoItemAdded: (afterPosition: Int?) -> Unit,
    onSummaryChanged: (Long, String) -> Unit,
    onCompleted: (Long, Boolean) -> Unit,
    onMoveTodoItem: (Long, Int) -> Unit,
    onDelete: (Long) -> Unit,
    onDeleteCompleted: () -> Unit,
    onOpenReminderMenu: (Boolean) -> Unit,
    onReminderUpdated: (Reminder?) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (uiState.loading) {
        return
    }

    // Permissions
    var notificationPermissionRationaleOpen by rememberSaveable { mutableStateOf(false) }
    var alarmPermissionRationaleOpen by rememberSaveable { mutableStateOf(false) }

    val context = LocalContext.current
    val alarmManager = remember { context.getSystemService(AlarmManager::class.java) }

    val notificationPermissionState = if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
        null
    } else {
        rememberPermissionStateSafe(
            permission = android.Manifest.permission.POST_NOTIFICATIONS
        ) { granted ->
            if (granted) {
                // Notification permission was just granted.

                // Still need to check whether other permissions are granted.
                val allPermissionsGranted = requestNotificationAndAlarmPermissions(
                    // We know the notification permission is granted already.
                    notificationPermissionState = null,
                    onOpenNotificationPermissionRationale = {},
                    alarmManager = alarmManager,
                    onOpenAlarmPermissionRationale = { alarmPermissionRationaleOpen = true }
                )

                if (allPermissionsGranted) {
                    onOpenReminderMenu(true)
                }
            }
        }
    }

    if (notificationPermissionRationaleOpen) {
        NotificationPermissionRationaleDialog(
            onConfirm = {
                notificationPermissionRationaleOpen = false
                notificationPermissionState?.launchPermissionRequest()
            },
            onDismiss = { notificationPermissionRationaleOpen = false },
        )
    }

    if (alarmPermissionRationaleOpen) {
        AlarmPermissionRationaleDialog(
            onConfirm = {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    requestAlarmPermissions(context)
                }
            },
            onDismiss = { alarmPermissionRationaleOpen = false },
        )
    }

    // When the app is resumed, check if the alarm permission was granted.
    val lifecycle = LocalLifecycleOwner.current
    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                if (alarmPermissionRationaleOpen) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && alarmManager.canScheduleExactAlarms()) {
                        alarmPermissionRationaleOpen = false
                        // Still need to check whether other permissions are granted.
                        val allPermissionsGranted = requestNotificationAndAlarmPermissions(
                            notificationPermissionState = notificationPermissionState,
                            onOpenNotificationPermissionRationale = {
                                notificationPermissionRationaleOpen = true
                            },
                            alarmManager = alarmManager,
                            onOpenAlarmPermissionRationale = {
                                alarmPermissionRationaleOpen = true
                            }
                        )

                        if (allPermissionsGranted) {
                            onOpenReminderMenu(true)
                        }
                    }
                }
            }
        }
        lifecycle.lifecycle.addObserver(observer)

        onDispose {
            lifecycle.lifecycle.removeObserver(observer)
        }
    }

    var indexToFocus by remember { mutableStateOf<Int?>(null) }

    Scaffold(
        topBar = {
            TodoListScreenTopBar(
                name = uiState.todoList.name,
                onNameChanged = onNameChanged,
                onNext = {
                    onTodoItemAdded(null)
                    indexToFocus = 0
                },
                imeAction = if (uiState.todoItems.isEmpty()) ImeAction.Next else ImeAction.Done,
                hasReminder = uiState.todoList.reminder != null,
                onOpenReminderMenu = { open ->
                    if (open) {
                        val allPermissionsGranted = requestNotificationAndAlarmPermissions(
                            notificationPermissionState = notificationPermissionState,
                            onOpenNotificationPermissionRationale = {
                                notificationPermissionRationaleOpen = true
                            },
                            alarmManager = alarmManager,
                            onOpenAlarmPermissionRationale = {
                                alarmPermissionRationaleOpen = true
                            }
                        )

                        if (!allPermissionsGranted) {
                            return@TodoListScreenTopBar
                        }
                    }

                    // Permissions granted.
                    onOpenReminderMenu(open)
                },
                onNavigateBack = onNavigateBack,
            )
        },
        modifier = modifier.imePadding()
    ) { contentPadding ->
        TodoListScreenContent(
            uiState = uiState,
            indexToFocus = indexToFocus,
            onIndexToFocusChanged = { newIndex -> indexToFocus = newIndex },
            onTodoItemAdded = onTodoItemAdded,
            onSummaryChanged = onSummaryChanged,
            onCompleted = onCompleted,
            onMoveTodoItem = onMoveTodoItem,
            onDelete = onDelete,
            onDeleteCompleted = onDeleteCompleted,
            onOpenReminderMenu = onOpenReminderMenu,
            modifier = Modifier
                .padding(contentPadding)
        )
    }

    if (uiState.reminderMenuOpen) {
        ReminderDialog(
            todoListReminder = uiState.todoList.reminder,
            onConfirm = { reminder ->
                onReminderUpdated(reminder)
                onOpenReminderMenu(false)
            },
            onDelete = {
                onReminderUpdated(null)
                onOpenReminderMenu(false)
            },
            onDismiss = { onOpenReminderMenu(false) }
        )
    }
}

@OptIn(ExperimentalPermissionsApi::class)
fun requestNotificationAndAlarmPermissions(
    notificationPermissionState: PermissionState?,
    onOpenNotificationPermissionRationale: () -> Unit,
    alarmManager: AlarmManager,
    onOpenAlarmPermissionRationale: () -> Unit,
): Boolean {
    if (notificationPermissionState?.status?.isGranted == false) {
        // Notification permission isn't granted, go through permission grant flow.
        if (notificationPermissionState.status.shouldShowRationale) {
            onOpenNotificationPermissionRationale()
        } else {
            notificationPermissionState.launchPermissionRequest()
        }
        return false
    }

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
        // Alarm permission isn't granted, go through permission grant flow.
        onOpenAlarmPermissionRationale()
        return false
    }

    return true
}

@RequiresApi(Build.VERSION_CODES.S)
fun requestAlarmPermissions(context: Context) {
    Timber.d("Requesting SCHEDULE_EXACT_ALARM permission")
    Intent().also { intent ->
        intent.action = Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM
        intent.data = ("package:" + context.packageName).toUri()
        context.startActivity(intent)
    }
}

@Composable
fun TodoListScreenContent(
    uiState: TodoListScreenUiState,
    indexToFocus: Int?,
    onIndexToFocusChanged: (Int?) -> Unit,
    onTodoItemAdded: (afterPosition: Int?) -> Unit,
    onSummaryChanged: (Long, String) -> Unit,
    onCompleted: (Long, Boolean) -> Unit,
    onMoveTodoItem: (Long, Int) -> Unit,
    onDelete: (Long) -> Unit,
    onDeleteCompleted: () -> Unit,
    onOpenReminderMenu: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val smallPadding = dimensionResource(R.dimen.padding_small)

    Column(
        modifier = modifier
    ) {
        if (uiState.todoList.reminder != null) {
            Box(
                contentAlignment = Alignment.CenterEnd,
                modifier = Modifier
                    .fillMaxWidth(0.75f)
                    .align(Alignment.End)
            ) {
                ReminderInfo(
                    reminder = uiState.todoList.reminder,
                    onClick = { onOpenReminderMenu(true) },
                    modifier = Modifier
                        .padding(horizontal = smallPadding)
                )
            }
        }

        TodoItemsColumn(
            todoItems = uiState.todoItems,
            indexToFocus = indexToFocus,
            onIndexToFocusChanged = onIndexToFocusChanged,
            onTodoItemAdded = onTodoItemAdded,
            onSummaryChanged = onSummaryChanged,
            onCompleted = onCompleted,
            onMoveTodoItem = onMoveTodoItem,
            onDelete = onDelete,
            onDeleteCompleted = onDeleteCompleted,
            modifier = Modifier
                .fillMaxSize()
        )
    }
}

@Composable
fun TodoItemsColumn(
    todoItems: ImmutableList<TodoItem>,
    indexToFocus: Int?,
    onIndexToFocusChanged: (Int?) -> Unit,
    onTodoItemAdded: (afterPosition: Int?) -> Unit,
    onSummaryChanged: (Long, String) -> Unit,
    onCompleted: (Long, Boolean) -> Unit,
    onMoveTodoItem: (Long, Int) -> Unit,
    onDelete: (Long) -> Unit,
    onDeleteCompleted: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val smallPadding = dimensionResource(R.dimen.padding_small)

    // Store a mutable version of the list locally so it is updated quickly while dragging.
    val uncompletedTodoItems = remember {
        todoItems.filter { !it.isCompleted }.toMutableStateList()
    }

    val lazyListState = rememberLazyListState()
    val reorderableLazyListState = rememberReorderableLazyListState(
        lazyListState,
        onMove = { from, to ->
            val fromIndex = uncompletedTodoItems.indexOfFirst { it.id == from.key as Long }
            val toIndex = uncompletedTodoItems.indexOfFirst { it.id == to.key as Long }

            if (fromIndex != -1 && toIndex != -1) {
                uncompletedTodoItems.apply {
                    add(toIndex, removeAt(fromIndex))
                }
                onMoveTodoItem(from.key as Long, toIndex)
            }
        },
    )

    val isDragging = reorderableLazyListState.isAnyItemDragging

    // Sync with todoItems when it changes, but skip it while dragging to avoid jumbled items.
    if (!isDragging) {
        val filtered = todoItems.filter { !it.isCompleted }
        val currentIds = uncompletedTodoItems.map { it.id }
        val newIds = filtered.map { it.id }

        if (currentIds != newIds) {
            uncompletedTodoItems.clear()
            uncompletedTodoItems.addAll(filtered)
        } else {
            filtered.forEachIndexed { index, item ->
                if (uncompletedTodoItems[index] != item) {
                    uncompletedTodoItems[index] = item
                }
            }
        }
    }

    // Ensure completedTodoItems never contains anything that is currently in uncompletedTodoItems
    // to avoid duplicate key crashes during section transitions.
    val completedTodoItems = remember(todoItems, uncompletedTodoItems.size) {
        val currentUncompletedIds = uncompletedTodoItems.map { it.id }.toSet()
        todoItems.filter { it.isCompleted && it.id !in currentUncompletedIds }
    }

    var completedItemsShown by rememberSaveable { mutableStateOf(false) }

    LazyColumn(
        state = lazyListState,
        modifier = modifier
    ) {
        uncompletedSection(
            items = uncompletedTodoItems,
            reorderableState = reorderableLazyListState,
            todoItemsSize = todoItems.size,
            indexToFocus = indexToFocus,
            onIndexToFocusChanged = onIndexToFocusChanged,
            onTodoItemAdded = onTodoItemAdded,
            onSummaryChanged = onSummaryChanged,
            onCompleted = onCompleted,
            onDelete = onDelete,
        )

        item(key = "add_todo_item_button") {
            TextButton(
                onClick = {
                    onIndexToFocusChanged(uncompletedTodoItems.size)
                    onTodoItemAdded(null)
                }
            ) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.add_item))
                Spacer(Modifier.width(smallPadding))
                Text(
                    text = stringResource(R.string.add_item),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        completedHeaderSection(
            completedItemsShown = completedItemsShown,
            onToggleCompleted = { completedItemsShown = !completedItemsShown },
            onDeleteCompleted = onDeleteCompleted
        )

        if (completedItemsShown) {
            completedSection(
                items = completedTodoItems,
                onCompleted = onCompleted,
                onSummaryChanged = onSummaryChanged,
                onDelete = onDelete,
            )
        }
    }

    // Scroll to focused item.
    LaunchedEffect(todoItems.size) {
        indexToFocus?.let { index ->
            if (!lazyListState.isItemWithIndexVisible(index)) {
                lazyListState.animateScrollToItem(index)
            }
        }
    }
}

fun LazyListScope.uncompletedSection(
    items: List<TodoItem>,
    reorderableState: ReorderableLazyListState,
    todoItemsSize: Int,
    indexToFocus: Int?,
    onIndexToFocusChanged: (Int?) -> Unit,
    onTodoItemAdded: (afterPosition: Int?) -> Unit,
    onSummaryChanged: (Long, String) -> Unit,
    onCompleted: (Long, Boolean) -> Unit,
    onDelete: (Long) -> Unit,
) {
    itemsIndexed(items, key = { _, item -> item.id }) { index, todoItem ->
        val focusRequester = remember { FocusRequester() }

        ReorderableItem(reorderableState, key = todoItem.id) { isDragging ->
            TodoItem(
                todoItem = todoItem,
                onCompleted = { onCompleted(todoItem.id, it) },
                onSummaryChanged = { onSummaryChanged(todoItem.id, it) },
                onDelete = { focusPreviousItem ->
                    if (focusPreviousItem) {
                        onIndexToFocusChanged(if (index >= 1) index - 1 else null)
                    }
                    onDelete(todoItem.id)
                },
                onNext = {
                    onIndexToFocusChanged(index + 1)
                    onTodoItemAdded(todoItem.position)
                },
                reorderableCollectionItemScope = this,
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(elevation = if (isDragging) 4.dp else 0.dp),
                focusRequester = focusRequester,
            )
        }

        LaunchedEffect(todoItemsSize) {
            if (index == indexToFocus) {
                focusRequester.requestFocus()
                onIndexToFocusChanged(null)
            }
        }
    }
}

fun LazyListScope.completedHeaderSection(
    completedItemsShown: Boolean,
    onToggleCompleted: () -> Unit,
    onDeleteCompleted: () -> Unit,
) {
    item {
        val smallPadding = dimensionResource(R.dimen.padding_small)
        Box(
            modifier = Modifier
                .fillMaxWidth()
        ) {
            TextButton(
                onClick = onToggleCompleted
            ) {
                val icon = if (completedItemsShown) {
                    Icons.Default.KeyboardArrowDown
                } else {
                    Icons.Default.KeyboardArrowUp
                }

                val contentDescription = if (completedItemsShown) {
                    stringResource(R.string.hide_completed_items)
                } else {
                    stringResource(R.string.show_completed_items)
                }

                Icon(icon, contentDescription)
                Spacer(Modifier.width(smallPadding))
                Text(text = "Completed items", style = MaterialTheme.typography.bodyMedium)
            }
            IconButton(
                onClick = onDeleteCompleted,
                modifier = Modifier
                    .align(Alignment.CenterEnd)
            ) {
                Icon(
                    Icons.Default.Delete,
                    stringResource(R.string.delete_all_completed_items),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

fun LazyListScope.completedSection(
    items: List<TodoItem>,
    onCompleted: (Long, Boolean) -> Unit,
    onSummaryChanged: (Long, String) -> Unit,
    onDelete: (Long) -> Unit,
) {
    items(
        items,
        key = { "completed_${it.id}" }
    ) { todoItem ->
        TodoItem(
            todoItem = todoItem,
            onCompleted = { onCompleted(todoItem.id, it) },
            onSummaryChanged = { onSummaryChanged(todoItem.id, it) },
            onDelete = { onDelete(todoItem.id) },
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TodoTextField(
    value: String,
    onValueChanged: (String) -> Unit,
    textStyle: TextStyle,
    modifier: Modifier = Modifier,
    focusRequester: FocusRequester = FocusRequester.Default,
    imeAction: ImeAction = ImeAction.Default,
    onNext: () -> Unit = {},
    onDelete: () -> Unit = {},
) {
    // Keep the current value of the textbox as UI state. Update the database once the user has
    // stopped typing for a short period.
    var text by rememberSaveable(stateSaver = TextFieldValue.Saver) {
        // Start the cursor at the end of the existing text.
        mutableStateOf(TextFieldValue(text = value, selection = TextRange(value.length)))
    }

    val focusManager = LocalFocusManager.current
    val isImeVisible = WindowInsets.isImeVisible

    LaunchedEffect(isImeVisible) {
        if (!isImeVisible) {
            focusManager.clearFocus()
        }
    }

    LaunchedEffect(text.text) {
        delay(PERSIST_TEXT_FIELD_TYPING_DELAY)
        // TODO: Changes are lost if the user navigates back very quickly after typing.
        onValueChanged(text.text)
    }

    BasicTextField(
        value = text,
        onValueChange = {
            text = it
        },
        keyboardActions = KeyboardActions(
            onDone = {
                onValueChanged(text.text)
                defaultKeyboardAction(ImeAction.Done)
            },
            onNext = {
                onValueChanged(text.text)
                onNext()
            },
        ),
        keyboardOptions = KeyboardOptions(
            imeAction = imeAction,
            capitalization = KeyboardCapitalization.Sentences
        ),
        singleLine = true,
        textStyle = textStyle.copy(color = MaterialTheme.colorScheme.onBackground),
        cursorBrush = SolidColor(MaterialTheme.colorScheme.onBackground),
        modifier = modifier
            .focusRequester(focusRequester)
            // Move the cursor to the end of the existing text when focus changes.
            .onFocusChanged { text = text.copy(selection = TextRange(text.text.length)) }
            // Delete item if backspace on empty summary.
            .onKeyEvent {
                if (it.key == Key.Backspace && text.text.isEmpty()) {
                    onDelete()
                }
                false
            }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodoListScreenTopBar(
    name: String,
    onNameChanged: (String) -> Unit,
    onNext: () -> Unit,
    imeAction: ImeAction,
    hasReminder: Boolean,
    onOpenReminderMenu: (Boolean) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
) {

    val titleFocusRequester = remember { FocusRequester() }

    TopAppBar(
        title = {
            TodoTextField(
                value = name,
                onValueChanged = onNameChanged,
                textStyle = MaterialTheme.typography.headlineMedium,
                focusRequester = titleFocusRequester,
                imeAction = imeAction,
                onNext = onNext,
            )
        },
        navigationIcon = {
            IconButton(onClick = onNavigateBack) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.back)
                )
            }
        },
        actions = {
            IconButton(onClick = { onOpenReminderMenu(true) }) {
                val reminderIcon = when (hasReminder) {
                    true -> Icons.Filled.Notifications
                    false -> Icons.Outlined.Notifications
                }

                Icon(
                    reminderIcon,
                    contentDescription = stringResource(R.string.add_reminder),
                    modifier = Modifier
                        .padding(dimensionResource(R.dimen.padding_small))
                )
            }
        },
        modifier = modifier,
    )

    // Focus the title if it is empty.
    LaunchedEffect(name) {
        if (name.isEmpty()) {
            titleFocusRequester.requestFocus()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodoItem(
    todoItem: TodoItem,
    onCompleted: (Boolean) -> Unit,
    onSummaryChanged: (String) -> Unit,
    onDelete: (focusPreviousItem: Boolean) -> Unit,
    modifier: Modifier = Modifier,
    reorderableCollectionItemScope: ReorderableCollectionItemScope? = null,
    focusRequester: FocusRequester = FocusRequester.Default,
    onNext: () -> Unit = {},
) {
    val dismissState = rememberSwipeToDismissBoxState(
        positionalThreshold = { distance -> distance * 0.33f }
    )
    val scope = rememberCoroutineScope()

    SwipeToDismissBox(
        state = dismissState,
        modifier = modifier,
        onDismiss = { direction ->
            if (direction == SwipeToDismissBoxValue.EndToStart || direction == SwipeToDismissBoxValue.StartToEnd) {
                onDelete(false)
            } else {
                scope.launch { dismissState.reset() }
            }
        },
        backgroundContent = {},
        content = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .alpha(if (dismissState.progress == 1f) 1f else 1f - dismissState.progress),
            ) {
                Checkbox(checked = todoItem.isCompleted, onCheckedChange = onCompleted)
                TodoTextField(
                    value = todoItem.summary,
                    onValueChanged = onSummaryChanged,
                    textStyle = MaterialTheme.typography.bodyMedium,
                    focusRequester = focusRequester,
                    imeAction = if (todoItem.isCompleted) ImeAction.Done else ImeAction.Next,
                    onNext = onNext,
                    onDelete = { onDelete(true) },
                    modifier = Modifier
                        .weight(1f)
                )
                if (!todoItem.isCompleted && reorderableCollectionItemScope != null) {
                    IconButton(
                        modifier = with(reorderableCollectionItemScope) { Modifier.draggableHandle() },
                        onClick = {},
                    ) {
                        Icon(
                            painterResource(R.drawable.drag_indicator),
                            contentDescription = "Reorder",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .padding(dimensionResource(R.dimen.padding_small))
                        )
                    }
                }
            }
        }
    )
}

@Composable
fun NotificationPermissionRationaleDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(stringResource(R.string.notification_permission_rationale_title))
        },
        text = {
            Text(stringResource(R.string.notification_permission_rationale_description))
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

@Composable
fun AlarmPermissionRationaleDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(stringResource(R.string.alarm_permission_rationale_title))
        },
        text = {
            Text(stringResource(R.string.alarm_permission_rationale_description))
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

@Preview(device = "id:pixel_9", showSystemUi = true, apiLevel = 36)
@Preview(
    device = "id:pixel_9", showSystemUi = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL, apiLevel = 36
)
@Composable
fun TodoListScreenPreview() {
    TodoTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            TodoListScreen(
                uiState = TodoListScreenUiState(
                    todoList = TodoList(
                        id = 0,
                        name = "Shopping",
                        position = 0,
                        reminder = Reminder.LocationReminder(
                            Location(
                                LatLng(0.0, 0.0),
                                0.0,
                                "Local Supermarket"
                            )
                        )
                    ),
                    todoItems = persistentListOf(
                        TodoItem(
                            id = 0,
                            summary = "Eggs",
                            listId = 0,
                            isCompleted = false,
                            position = 0
                        ),
                        TodoItem(
                            id = 1,
                            summary = "Bread",
                            listId = 0,
                            isCompleted = false,
                            position = 1
                        ),
                        TodoItem(
                            id = 2,
                            summary = "Bag of chips",
                            listId = 0,
                            isCompleted = false,
                            position = 2
                        ),
                        TodoItem(
                            id = 3,
                            summary = "Toothpaste",
                            listId = 0,
                            isCompleted = true,
                            position = 3
                        ),
                        TodoItem(
                            id = 4,
                            summary = "Deodorant",
                            listId = 0,
                            isCompleted = true,
                            position = 4
                        ),
                    )
                ),
                onNameChanged = {},
                onTodoItemAdded = {},
                onSummaryChanged = { _, _ -> },
                onCompleted = { _, _ -> },
                onMoveTodoItem = { _, _ -> },
                onDelete = {},
                onDeleteCompleted = {},
                onOpenReminderMenu = {},
                onReminderUpdated = {},
                onNavigateBack = {},
            )
        }
    }
}