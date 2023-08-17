package com.chybby.todo.ui.list

import android.content.res.Configuration
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
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
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.chybby.todo.R
import com.chybby.todo.data.TodoItem
import com.chybby.todo.data.TodoList
import com.chybby.todo.ui.isItemWithIndexVisible
import com.chybby.todo.ui.theme.TodoTheme
import kotlinx.coroutines.delay
import org.burnoutcrew.reorderable.ReorderableItem
import org.burnoutcrew.reorderable.ReorderableLazyListState
import org.burnoutcrew.reorderable.detectReorder
import org.burnoutcrew.reorderable.rememberReorderableLazyListState
import org.burnoutcrew.reorderable.reorderable

const val TYPING_PERSIST_DELAY_MS: Long = 300

@Composable
fun TodoListScreen(
    uiState: TodoListScreenUiState,
    onNameChanged: (String) -> Unit,
    onTodoItemAdded: (afterPosition: Int?) -> Unit,
    onSummaryChanged: (Long, String) -> Unit,
    onCompleted: (Long, Boolean) -> Unit,
    onMoveTodoItem: (Int, Int) -> Unit,
    onDelete: (Long) -> Unit,
    onDeleteCompleted: () -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
) {

    if (uiState.loading) {
        return
    }

    val smallPadding = dimensionResource(R.dimen.padding_small)

    val todoItemsByCompleted = uiState.todoItems.groupBy { it.isCompleted }

    val state = rememberReorderableLazyListState(
        onMove = { from, to ->
            onMoveTodoItem(from.index, to.index)
        },
        canDragOver = { draggedOver, _ ->
            draggedOver.index < (todoItemsByCompleted[false]?.size ?: 0)
        }
    )

    var indexToFocus by remember { mutableStateOf<Int?>(null) }

    Scaffold (
        topBar = {
            TodoListScreenTopBar(
                name = uiState.todoList.name,
                onNameChanged = onNameChanged,
                onNavigateBack = onNavigateBack,
            )
        },
        modifier = modifier
    ) { contentPadding ->
        var completedItemsShown by rememberSaveable { mutableStateOf(false) }

        LazyColumn(
            state = state.listState,
            contentPadding = contentPadding,
            modifier = Modifier
                .fillMaxSize()
                .reorderable(state)
        ) {

            // Uncompleted items.
            val uncompletedItems = todoItemsByCompleted.getOrDefault(false, listOf())
            itemsIndexed(uncompletedItems, key = { _, item -> item.id }) {index, todoItem ->

                val focusRequester = remember { FocusRequester() }

                ReorderableItem(state, key = todoItem.id) {isDragging ->
                    TodoItem(
                        todoItem = todoItem,
                        onCompleted = { onCompleted(todoItem.id, it) },
                        onSummaryChanged = { onSummaryChanged(todoItem.id, it) },
                        onDelete = { focusPreviousItem ->
                            if (focusPreviousItem) {
                                indexToFocus = if (index >= 1) index - 1 else null
                            }
                            onDelete(todoItem.id)
                        },
                        onNext = {
                            indexToFocus = index + 1
                            onTodoItemAdded(todoItem.position)
                        },
                        reorderableLazyListState = state,
                        modifier = Modifier
                            .shadow(elevation = if (isDragging) 4.dp else 0.dp),
                        focusRequester = focusRequester,
                    )
                }

                LaunchedEffect(uiState.todoItems.size) {
                    if (index == indexToFocus) {
                        focusRequester.requestFocus()
                        indexToFocus = null
                    }
                }
            }

            // Add new item button.
            item {
                TextButton(
                    onClick = {
                        indexToFocus = uncompletedItems.size
                        onTodoItemAdded(null)
                    }
                ) {
                    Icon(Icons.Default.Add, contentDescription = stringResource(R.string.add_item))
                    Spacer(Modifier.width(smallPadding))
                    Text(text = stringResource(R.string.add_item), style = MaterialTheme.typography.bodyMedium)
                }
            }

            // Completed items.
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                ) {
                    TextButton(
                        onClick = { completedItemsShown = !completedItemsShown }
                    ) {
                        val icon = when(completedItemsShown) {
                            true -> { Icons.Default.KeyboardArrowDown }
                            false -> { Icons.Default.KeyboardArrowUp }
                        }

                        val contentDescription = when(completedItemsShown) {
                            true -> { stringResource(R.string.hide_completed_items) }
                            false -> { stringResource(R.string.show_completed_items) }
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
                        Icon(Icons.Default.Delete, stringResource(R.string.delete_all_completed_items))
                    }
                }
            }

            if (completedItemsShown) {
                items(
                    todoItemsByCompleted.getOrDefault(true, listOf()),
                    key = { it.id }) { todoItem ->
                    TodoItem(
                        todoItem = todoItem,
                        onCompleted = { onCompleted(todoItem.id, it) },
                        onSummaryChanged = { onSummaryChanged(todoItem.id, it) },
                        onDelete = { onDelete(todoItem.id) },
                    )
                }
            }
        }
    }

    // Scroll to focused item.
    LaunchedEffect(uiState.todoItems.size) {
        indexToFocus?.let { index ->
            if (!state.listState.isItemWithIndexVisible(index)) {
                state.listState.animateScrollToItem(index)
            }
        }
    }
}

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

    LaunchedEffect(text.text) {
        delay(TYPING_PERSIST_DELAY_MS)
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
        keyboardOptions = KeyboardOptions(imeAction = imeAction),
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
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier) {

    val titleFocusRequester = remember { FocusRequester() }

    TopAppBar(
        title = {
            TodoTextField(
                value = name,
                onValueChanged = onNameChanged,
                textStyle = MaterialTheme.typography.headlineMedium,
                focusRequester = titleFocusRequester,
            )
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
    reorderableLazyListState: ReorderableLazyListState? = null,
    focusRequester: FocusRequester = FocusRequester.Default,
    onNext: () -> Unit = {},
) {
    val dismissState = rememberDismissState(
        positionalThreshold = { distance -> distance * 0.33f },
        confirmValueChange = {
            if (it != DismissValue.Default) {
                onDelete(false)
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
                if (!todoItem.isCompleted && reorderableLazyListState != null) {
                    Icon(
                        painterResource(R.drawable.drag_indicator),
                        contentDescription = null,
                        modifier = Modifier
                            .detectReorder(reorderableLazyListState)
                            .padding(dimensionResource(R.dimen.padding_small))
                    )
                }
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
                onMoveTodoItem = {_, _ -> },
                onDelete = {},
                onDeleteCompleted = {},
                onNavigateBack = {},
            )
        }
    }
}