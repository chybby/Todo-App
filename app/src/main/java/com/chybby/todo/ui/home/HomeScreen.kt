package com.chybby.todo.ui.home

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement.spacedBy
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
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
import androidx.compose.material3.rememberDismissState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.chybby.todo.R
import com.chybby.todo.data.TodoList
import com.chybby.todo.ui.theme.TodoTheme

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    uiState: HomeScreenUiState,
    onAddTodoList: () -> Unit,
    onNavigateToTodoList: (todoListId: Long) -> Unit,
    onDeleteTodoList: (todoListId: Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val paddingSmall = dimensionResource(id = R.dimen.padding_small)

    LaunchedEffect(uiState.newTodoListId) {
        if (uiState.newTodoListId != null) {
            // Navigate to the newly created list.
            onNavigateToTodoList(uiState.newTodoListId)
        }
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
            contentPadding = contentPadding,
            verticalArrangement = spacedBy(paddingSmall),
            modifier = Modifier
                .fillMaxSize()
                .padding(PaddingValues(paddingSmall))
        ) {
            items(uiState.todoLists, key = { it.id }) { todoList ->
                TodoList(
                    todoList = todoList,
                    onClick = { onNavigateToTodoList(todoList.id) },
                    onDelete = { onDeleteTodoList(todoList.id) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .animateItemPlacement()
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodoList(todoList: TodoList, onClick: () -> Unit, onDelete: () -> Unit, modifier: Modifier = Modifier) {
    val paddingSmall = dimensionResource(id = R.dimen.padding_small)

    val dismissState = rememberDismissState(
        confirmValueChange = {
            if (it == DismissValue.Default) {
                false
            } else {
                onDelete()
                true
            }
        }
    )

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
                Text(
                    text = todoList.name,
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier
                        .padding(paddingSmall)
                )
            }
        }
    )
}

@Preview(device = "id:Nexus 5", showSystemUi = true)
@Composable
fun HomeScreenPreview() {
    TodoTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            HomeScreen(HomeScreenUiState(
                listOf(
                    TodoList(name = "Shopping", position = 0),
                    TodoList(name = "Clothes", position = 1),
                    TodoList(name = "Books to Read", position = 2),
                ),
                newTodoListId = null,
            ), {}, {}, {})
        }
    }
}