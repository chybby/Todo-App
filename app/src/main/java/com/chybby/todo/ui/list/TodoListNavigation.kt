package com.chybby.todo.ui.list

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
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
    composable(
        route = "$TodoListRoute/{$todoListIdArg}",
        enterTransition = {
            slideIntoContainer(
                towards = AnimatedContentTransitionScope.SlideDirection.Companion.Start,
                animationSpec = tween(500)
            )
        },
        popEnterTransition = {
            EnterTransition.None
        },
        exitTransition = {
            ExitTransition.None
        },
        popExitTransition = {
            slideOutOfContainer(
                towards = AnimatedContentTransitionScope.SlideDirection.Companion.End,
                animationSpec = tween(500)
            )
        }
    ) {
        val viewModel: TodoListScreenViewModel = hiltViewModel()
        val uiState by viewModel.uiState.collectAsStateWithLifecycle()
        TodoListScreen(
            uiState,
            onNameChanged = viewModel::editName,
            onTodoItemAdded = viewModel::addTodoItem,
            onSummaryChanged = viewModel::editSummary,
            onCompleted = viewModel::editCompleted,
            onMoveTodoItem = viewModel::moveTodoItem,
            onDelete = viewModel::deleteTodoItem,
            onDeleteCompleted = viewModel::deleteCompleted,
            onOpenReminderMenu = viewModel::openReminderMenu,
            onNavigateBack = onNavigateBack
        )
    }
}

fun NavController.navigateToTodoList(todoListId: Long, navOptions: NavOptions? = null) {
    this.navigate("$TodoListRoute/$todoListId", navOptions)
}