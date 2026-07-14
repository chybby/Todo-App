package com.chybby.todo.ui.home

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Arrangement.spacedBy
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import com.chybby.todo.R
import com.chybby.todo.data.Location
import com.chybby.todo.data.Reminder
import com.chybby.todo.data.TodoList
import com.chybby.todo.ui.ReminderDialog
import com.chybby.todo.ui.ReminderInfo
import com.chybby.todo.ui.syncWith
import com.chybby.todo.ui.theme.TodoTheme
import com.google.android.gms.maps.model.LatLng
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.launch
import sh.calvin.reorderable.ReorderableCollectionItemScope
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

@Composable
fun HomeScreen(
    uiState: HomeScreenUiState,
    onAddTodoList: () -> Unit,
    onMoveTodoList: (todoListId: Long, to: Int) -> Unit,
    onNavigateToTodoList: (todoListId: Long) -> Unit,
    onDeleteTodoList: (todoListId: Long) -> Unit,
    onOpenReminderMenu: (Long?) -> Unit,
    onReminderUpdated: (Long, Reminder?) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (uiState.loading) {
        return
    }

    val smallPadding = dimensionResource(id = R.dimen.padding_small)

    LaunchedEffect(uiState.newTodoListId) {
        if (uiState.newTodoListId != null) {
            // Navigate to the newly created list.
            onNavigateToTodoList(uiState.newTodoListId)
        }
    }

    // Store a mutable version of the list locally so it is updated quickly while dragging.
    val todoLists = remember {
        uiState.todoLists.toMutableStateList()
    }

    val lazyListState = rememberLazyListState()
    val reorderableLazyListState = rememberReorderableLazyListState(
        lazyListState,
        onMove = { from, to ->
            val fromIndex = todoLists.indexOfFirst { it.id == from.key as Long }
            val toIndex = todoLists.indexOfFirst { it.id == to.key as Long }

            if (fromIndex != -1 && toIndex != -1) {
                todoLists.apply {
                    add(toIndex, removeAt(fromIndex))
                }
                onMoveTodoList(from.key as Long, toIndex)
            }
        }
    )

    // Sync with uiState.todoLists when it changes, but skip it while dragging to avoid jumbled items.
    val isDragging = reorderableLazyListState.isAnyItemDragging
    if (!isDragging) {
        todoLists.syncWith(uiState.todoLists) { it.id }
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = onAddTodoList) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.new_todo_list))
            }
        },
        modifier = modifier,
    ) { contentPadding ->
        LazyColumn(
            state = lazyListState,
            contentPadding = contentPadding,
            verticalArrangement = spacedBy(smallPadding),
            modifier = Modifier
                .fillMaxSize()
                .padding(smallPadding)
        ) {
            items(todoLists, key = { it.id }) { todoList ->
                ReorderableItem(reorderableLazyListState, key = todoList.id) { isDragging ->
                    TodoList(
                        todoList = todoList,
                        onClick = { onNavigateToTodoList(todoList.id) },
                        onDelete = { onDeleteTodoList(todoList.id) },
                        onOpenReminderMenu = onOpenReminderMenu,
                        reorderableCollectionItemScope = this,
                        modifier = Modifier
                            .fillMaxWidth()
                            .shadow(if (isDragging) 4.dp else 0.dp)
                    )
                }
            }
        }
    }

    if (uiState.reminderMenuOpenForListId != null) {
        // TODO: slow for a large number of todo lists but probably fine.
        val reminderMenuTodoList =
            uiState.todoLists.find { it.id == uiState.reminderMenuOpenForListId }

        if (reminderMenuTodoList == null) {
            // The list no longer exists (e.g. it was deleted before state was restored).
            LaunchedEffect(uiState.reminderMenuOpenForListId) {
                onOpenReminderMenu(null)
            }
        } else {
            ReminderDialog(
                todoListReminder = reminderMenuTodoList.reminder,
                onConfirm = { reminder ->
                    onReminderUpdated(
                        uiState.reminderMenuOpenForListId,
                        reminder
                    )
                    onOpenReminderMenu(null)
                },
                onDelete = {
                    onReminderUpdated(uiState.reminderMenuOpenForListId, null)
                    onOpenReminderMenu(null)
                },
                onDismiss = { onOpenReminderMenu(null) }
            )
        }
    }
}

@Composable
fun ConfirmDeleteDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(stringResource(R.string.delete_list_question))
        },
        text = {
            Text(stringResource(R.string.are_you_sure_you_want_to_delete_this_list))
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
fun TodoList(
    todoList: TodoList,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    onOpenReminderMenu: (Long?) -> Unit,
    reorderableCollectionItemScope: ReorderableCollectionItemScope,
    modifier: Modifier = Modifier,
) {
    val smallPadding = dimensionResource(id = R.dimen.padding_small)

    var deleteDialogOpen by remember { mutableStateOf(false) }
    var itemWidth by remember { mutableFloatStateOf(1f) }

    val dismissState = rememberSwipeToDismissBoxState(
        positionalThreshold = { distance -> distance / 3f }
    )
    val linearProgress by remember {
        derivedStateOf {
            val offset = runCatching { dismissState.requireOffset() }.getOrDefault(0f)
            (kotlin.math.abs(offset) / itemWidth).coerceIn(0f, 1f)
        }
    }

    val coroutineScope = rememberCoroutineScope()

    // Make sure the SwipeToDismiss doesn't get stuck in the swiped state.
    LaunchedEffect(deleteDialogOpen) {
        if (!deleteDialogOpen) {
            dismissState.reset()
        }
    }

    if (deleteDialogOpen) {
        ConfirmDeleteDialog(
            onConfirm = {
                deleteDialogOpen = false
                onDelete()
            },
            onDismiss = {
                deleteDialogOpen = false
                coroutineScope.launch {
                    dismissState.reset()
                }
            }
        )
    }

    SwipeToDismissBox(
        state = dismissState,
        modifier = modifier.onSizeChanged { itemWidth = it.width.toFloat() },
        onDismiss = { direction ->
            if (direction == SwipeToDismissBoxValue.EndToStart || direction == SwipeToDismissBoxValue.StartToEnd) {
                deleteDialogOpen = true
            } else {
                coroutineScope.launch { dismissState.reset() }
            }
        },
        backgroundContent = {
            val direction = dismissState.dismissDirection
            val alignment = when (direction) {
                SwipeToDismissBoxValue.StartToEnd -> Alignment.CenterStart
                SwipeToDismissBoxValue.EndToStart -> Alignment.CenterEnd
                else -> Alignment.Center
            }

            val icon = Icons.Default.Delete

            Card {
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(
                            lerp(
                                Color.LightGray,
                                Color.Red,
                                lerp(0.5f, 1.0f, linearProgress * 3.0f).coerceAtMost(1.0f)
                            )
                        )
                        .padding(horizontal = 10.dp),
                    contentAlignment = alignment
                ) {
                    Icon(
                        icon,
                        contentDescription = stringResource(R.string.delete_list),
                        modifier = Modifier
                            .size(40.dp)
                            .scale(
                                lerp(
                                    0.5f,
                                    1.0f,
                                    linearProgress * 3.0f
                                ).coerceAtMost(1.0f)
                            ),
                        tint = Color.White
                    )
                }
            }
        },
        content = {
            Card(
                onClick = onClick,
                modifier = Modifier
                    .fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(smallPadding)
                ) {
                    Column(
                        verticalArrangement = spacedBy(smallPadding)
                    ) {
                        Text(
                            text = todoList.name,
                            style = MaterialTheme.typography.headlineSmall,
                        )

                        if (todoList.reminder != null) {
                            ReminderInfo(
                                reminder = todoList.reminder,
                                onClick = { onOpenReminderMenu(todoList.id) },
                                modifier = Modifier
                                    .fillMaxWidth(0.75f)
                            )
                        }
                    }

                    IconButton(
                        modifier = with(reorderableCollectionItemScope) { Modifier.draggableHandle() },
                        onClick = {},
                    ) {
                        Icon(
                            painterResource(R.drawable.drag_indicator),
                            contentDescription = "Reorder",
                        )
                    }
                }
            }
        }
    )
}

@Preview(device = "id:pixel_9", showSystemUi = true, apiLevel = 36)
@Preview(
    device = "id:pixel_9", showSystemUi = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL, apiLevel = 36
)
@Composable
fun HomeScreenPreview() {
    TodoTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            HomeScreen(
                uiState = HomeScreenUiState(
                    persistentListOf(
                        TodoList(id = 0, name = "Shopping", position = 0),
                        TodoList(
                            id = 1, name = "Clothes", position = 1,
                            reminder = Reminder.LocationReminder(
                                Location(
                                    LatLng(0.0, 0.0),
                                    0.0,
                                    "Fancy Dress Store",
                                )
                            )
                        ),
                        TodoList(id = 2, name = "Books to Read", position = 2),
                    ),
                    reminderMenuOpenForListId = null,
                    newTodoListId = null,
                ),
                onAddTodoList = {},
                onMoveTodoList = { _, _ -> },
                onNavigateToTodoList = {},
                onDeleteTodoList = {},
                onOpenReminderMenu = {},
                onReminderUpdated = { _, _ -> }
            )
        }
    }
}