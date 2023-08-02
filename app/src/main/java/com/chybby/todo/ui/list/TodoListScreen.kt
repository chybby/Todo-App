package com.chybby.todo.ui.list

import android.content.res.Configuration
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DismissDirection
import androidx.compose.material3.DismissValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismiss
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDismissState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import com.chybby.todo.R
import com.chybby.todo.data.TodoItem
import com.chybby.todo.data.TodoList
import com.chybby.todo.ui.theme.TodoTheme
import kotlinx.coroutines.delay

const val TYPING_PERSIST_DELAY_MS: Long = 300

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TodoListScreen(
    uiState: TodoListScreenUiState,
    onNameChanged: (String) -> Unit,
    onTodoItemAdded: () -> Unit,
    onSummaryChanged: (Long, String) -> Unit,
    onCompleted: (Long, Boolean) -> Unit,
    onDelete: (Long) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold (
        topBar = {
            TodoListScreenTopBar(
                name = uiState.todoList.name,
                loading = uiState.loading,
                onNameChanged = onNameChanged,
                onNavigateBack = onNavigateBack,
            )
        },
        modifier = modifier
    ) { contentPadding ->
        // TODO: bottom of list gets covered by keyboard.
        LazyColumn(
            contentPadding = contentPadding,
            modifier = Modifier
                .fillMaxSize()
        ) {

            val todoItemsByCompleted = uiState.todoItems.groupBy { it.isCompleted }

            // Uncompleted items.
            items(todoItemsByCompleted.getOrDefault(false, listOf()), key = { it.id }) {todoItem ->
                TodoItem(
                    todoItem = todoItem,
                    onCompleted = { onCompleted(todoItem.id, it) },
                    onSummaryChanged = { onSummaryChanged(todoItem.id, it) },
                    onDelete = { onDelete(todoItem.id) },
                    modifier = Modifier
                        .animateItemPlacement()
                )
            }

            // Add new item button.
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onTodoItemAdded ) {
                        Icon(Icons.Default.Add, contentDescription = stringResource(R.string.add_item))
                    }
                    Text(text = stringResource(R.string.add_item), style = MaterialTheme.typography.bodyMedium)
                }
            }

            // Completed items.
            // TODO: put completed items in a collapsible dropdown.
            items(todoItemsByCompleted.getOrDefault(true, listOf()), key = { it.id }) {todoItem ->
                // TODO: allow summary changes on completed items?
                TodoItem(
                    todoItem = todoItem,
                    onCompleted = { onCompleted(todoItem.id, it) },
                    onSummaryChanged = { onSummaryChanged(todoItem.id, it) },
                    onDelete = { onDelete(todoItem.id) },
                    modifier = Modifier
                        .animateItemPlacement()
                )
            }
        }
    }
}

@Composable
fun TodoTextField(value: String, onValueChanged: (String) -> Unit, textStyle: TextStyle, modifier: Modifier = Modifier) {
    // Keep the current value of the textbox as UI state. Update the database once the user has
    // stopped typing for a short period.
    var newValue by rememberSaveable(value) { mutableStateOf(value) }
    LaunchedEffect(newValue) {
        delay(TYPING_PERSIST_DELAY_MS)
        // TODO: Changes are lost if the user navigates back very quickly after typing.
        onValueChanged(newValue)
    }

    val focusRequester = remember { FocusRequester() }

    BasicTextField(
        value = newValue,
        onValueChange = {
            newValue = it
        },
        keyboardActions = KeyboardActions(onDone = {
            onValueChanged(newValue)
            defaultKeyboardAction(ImeAction.Done)
        }),
        singleLine = true,
        textStyle = textStyle.copy(color = MaterialTheme.colorScheme.onBackground),
        cursorBrush = SolidColor(MaterialTheme.colorScheme.onBackground),
        modifier = modifier
            .fillMaxWidth()
            .focusRequester(focusRequester)
    )

    LaunchedEffect(value) {
        if (value.isEmpty()) {
            // Automatically focus a text field if it is empty.
            // This will focus the list name on navigating to this screen if empty.
            // It will also focus the item summary when a new item is added.
            focusRequester.requestFocus()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodoListScreenTopBar(
    name: String,
    loading: Boolean,
    onNameChanged: (String) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier) {

    TopAppBar(
        title = {
            // Don't show the text field while loading so it isn't automatically focused.
            if (!loading) {
                TodoTextField(
                    value = name,
                    onValueChanged = onNameChanged,
                    textStyle = MaterialTheme.typography.headlineMedium
                )
            }
        },
        navigationIcon = {
            IconButton(onClick = onNavigateBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = stringResource(R.string.back))
            }
        },
        actions = {
            // TODO: notifications.
            // Icon(Icons.Default.Notifications, contentDescription = stringResource(R.string.add_reminder))
        },
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodoItem(todoItem: TodoItem, onCompleted: (Boolean) -> Unit, onSummaryChanged: (String) -> Unit, onDelete: () -> Unit, modifier: Modifier = Modifier) {

    val dismissState = rememberDismissState(
        positionalThreshold = { distance -> distance * 0.33f },
        confirmValueChange = {
            if (it != DismissValue.Default) {
                onDelete()
                true
            } else {
                false
            }
        }
    )

    SwipeToDismiss(
        state = dismissState,
        modifier = modifier,
        directions = setOf(DismissDirection.StartToEnd, DismissDirection.EndToStart),
        background = {},
        dismissContent = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = modifier
                    .alpha(if (dismissState.progress == 1f) 1f else 1f - dismissState.progress),
            ) {
                Checkbox(checked = todoItem.isCompleted, onCheckedChange = onCompleted )
                // TODO: add a new item on enter?
                TodoTextField(
                    value = todoItem.summary,
                    onValueChanged = onSummaryChanged,
                    textStyle = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    )
}

@Preview(device = "id:Nexus 5", showSystemUi = true)
@Preview(device = "id:Nexus 5", showSystemUi = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL
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
                    todoList = TodoList(name = "Shopping", position = 0),
                    todoItems = listOf(
                        TodoItem(
                            id = 0,
                            summary = "Eggs",
                            isCompleted = false,
                            position = 0
                        ),
                        TodoItem(
                            id = 1,
                            summary = "Bread",
                            isCompleted = false,
                            position = 1
                        ),
                        TodoItem(
                            id = 2,
                            summary = "Bag of chips",
                            isCompleted = false,
                            position = 2
                        ),
                        TodoItem(
                            id = 3,
                            summary = "Toothpaste",
                            isCompleted = true,
                            position = 3
                        ),
                        TodoItem(
                            id = 4,
                            summary = "Deodorant",
                            isCompleted = true,
                            position = 4
                        ),
                    )
                ),
                onNameChanged = {},
                onTodoItemAdded = {},
                onSummaryChanged = {_, _ -> },
                onCompleted = {_, _ -> },
                onDelete = {},
                onNavigateBack = {},
            )
        }
    }
}