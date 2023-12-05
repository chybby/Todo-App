package com.chybby.todo.ui.home

import android.content.res.Configuration
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Arrangement.spacedBy
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.DismissDirection
import androidx.compose.material3.DismissValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismiss
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDismissState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.chybby.todo.R
import com.chybby.todo.data.Location
import com.chybby.todo.data.Reminder
import com.chybby.todo.data.TodoList
import com.chybby.todo.ui.ReminderDialog
import com.chybby.todo.ui.ReminderInfo
import com.chybby.todo.ui.theme.TodoTheme
import com.google.android.gms.maps.model.LatLng
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.launch
import org.burnoutcrew.reorderable.ReorderableItem
import org.burnoutcrew.reorderable.ReorderableLazyListState
import org.burnoutcrew.reorderable.detectReorder
import org.burnoutcrew.reorderable.rememberReorderableLazyListState
import org.burnoutcrew.reorderable.reorderable

@Composable
fun HomeScreen(
    uiState: HomeScreenUiState,
    onAddTodoList: () -> Unit,
    onMoveTodoList: (from: Int, to: Int) -> Unit,
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
    val todoLists by rememberUpdatedState(uiState.todoLists.toMutableStateList())

    val state = rememberReorderableLazyListState(
        onMove = { from, to ->
            // While dragging, update the list stored in the composition.
            todoLists.apply {
                add(to.index, removeAt(from.index))
            }
        },
        onDragEnd = { from, to ->
            // When drag ends, update the database.
            onMoveTodoList(from, to)
        }
    )

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = onAddTodoList) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.new_todo_list))
            }
        },
        modifier = modifier,
    ) { contentPadding ->

        LazyColumn(
            state = state.listState,
            contentPadding = contentPadding,
            verticalArrangement = spacedBy(smallPadding),
            modifier = Modifier
                .fillMaxSize()
                .padding(smallPadding)
                .reorderable(state)
        ) {
            items(todoLists, key = { it.id }) { todoList ->
                ReorderableItem(reorderableState = state, key = todoList.id) { isDragging ->
                    TodoList(
                        todoList = todoList,
                        onClick = { onNavigateToTodoList(todoList.id) },
                        onDelete = { onDeleteTodoList(todoList.id) },
                        onOpenReminderMenu = onOpenReminderMenu,
                        reorderableLazyListState = state,
                        modifier = Modifier
                            .fillMaxWidth()
                            .shadow(if (isDragging) 4.dp else 0.dp)
                    )
                }
            }
        }
    }

    if (uiState.reminderMenuOpenForListId != null) {
        ReminderDialog(
            // TODO: slow for a large number of todo lists but probably fine.
            todoListReminder = uiState.todoLists.find { it.id == uiState.reminderMenuOpenForListId }!!.reminder,
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodoList(
    todoList: TodoList,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    onOpenReminderMenu: (Long?) -> Unit,
    reorderableLazyListState: ReorderableLazyListState,
    modifier: Modifier = Modifier,
) {
    val smallPadding = dimensionResource(id = R.dimen.padding_small)

    var deleteDialogOpen by remember { mutableStateOf(false) }

    val dismissState = rememberDismissState(
        confirmValueChange = {
            if (it != DismissValue.Default) {
                deleteDialogOpen = true
                true
            } else {
                false
            }
        }
    )

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

    SwipeToDismiss(
        state = dismissState,
        modifier = modifier,
        directions = setOf(DismissDirection.StartToEnd, DismissDirection.EndToStart),
        background = {
            val direction = dismissState.dismissDirection ?: return@SwipeToDismiss
            val color by animateColorAsState(
                when (dismissState.targetValue) {
                    DismissValue.Default -> MaterialTheme.colorScheme.tertiaryContainer
                    else -> MaterialTheme.colorScheme.errorContainer
                }, label = "Animate background color on dismiss"
            )
            val alignment = when (direction) {
                DismissDirection.StartToEnd -> Alignment.CenterStart
                DismissDirection.EndToStart -> Alignment.CenterEnd
            }

            val icon = Icons.Default.Delete

            val scale by animateFloatAsState(
                if (dismissState.targetValue == DismissValue.Default) 0.75f else 1f,
                label = "Animate scale on dismiss"
            )

            Card {
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(color)
                        .padding(horizontal = 20.dp),
                    contentAlignment = alignment
                ) {
                    Icon(
                        icon,
                        contentDescription = stringResource(R.string.delete_list),
                        modifier = Modifier.scale(scale)
                    )
                }
            }
        },
        dismissContent = {
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

                    Icon(
                        painterResource(R.drawable.drag_indicator),
                        contentDescription = null,
                        modifier = Modifier
                            .detectReorder(reorderableLazyListState)
                    )
                }
            }
        }
    )
}

@Preview(device = "id:Nexus 5", showSystemUi = true)
@Preview(
    device = "id:Nexus 5", showSystemUi = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL
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