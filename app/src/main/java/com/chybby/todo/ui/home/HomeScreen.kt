package com.chybby.todo.ui.home

import androidx.compose.foundation.layout.Arrangement.spacedBy
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.chybby.todo.R
import com.chybby.todo.data.TodoList
import com.chybby.todo.ui.theme.TodoTheme

@Composable
fun HomeScreen(
    uiState: HomeScreenUiState,
    onAddTodoList: () -> Unit,
    onNavigateToTodoList: (todoListId: Long) -> Unit,
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
            items(uiState.todoLists) { todoList ->
                TodoList(
                    todoList = todoList,
                    onTodoListClick = { onNavigateToTodoList(todoList.id) },
                    modifier = Modifier
                        .fillMaxWidth()
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodoList(todoList: TodoList, onTodoListClick: () -> Unit, modifier: Modifier = Modifier) {
    val paddingSmall = dimensionResource(id = R.dimen.padding_small)

    Card(
        onClick = onTodoListClick,
        modifier = modifier
    ) {
        Text(
            text = todoList.name,
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier
                .padding(paddingSmall)
        )
    }
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
            ), {}, {})
        }
    }
}