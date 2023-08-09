package com.chybby.todo.ui.list

import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.compose.composable

const val TodoListRoute = "todoList"

private const val todoListIdArg = "todoListId"

class TodoListArgs(val todoListId: Long) {
    constructor(savedStateHandle: SavedStateHandle) :
            this(checkNotNull(savedStateHandle[todoListIdArg]).toString().toLong())
}

fun NavGraphBuilder.todoListScreen(
    onNavigateBack: () -> Unit,
) {
    composable("$TodoListRoute/{$todoListIdArg}") {
        val viewModel: TodoListScreenViewModel = hiltViewModel()
        val uiState by viewModel.uiState.collectAsStateWithLifecycle()
        TodoListScreen(
            uiState,
            onNameChanged = viewModel::editName,
            onTodoItemAdded = viewModel::addTodoItem,
            onAckNewTodoItem = viewModel::ackNewTodoItem,
            onSummaryChanged = viewModel::editSummary,
            onCompleted = viewModel::editCompleted,
            onDelete = viewModel::deleteTodoItem,
            onDeleteCompleted = viewModel::deleteCompleted,
            onNavigateBack = onNavigateBack
        )
    }
}

fun NavController.navigateToTodoList(todoListId: Long, navOptions: NavOptions? = null) {
    this.navigate("$TodoListRoute/$todoListId", navOptions)
}