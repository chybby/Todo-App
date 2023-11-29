package com.chybby.todo.ui.home

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.compose.composable

const val HomeRoute = "home"

fun NavGraphBuilder.homeScreen(
    onNavigateToTodoList: (todoListId: Long) -> Unit,
) {
    composable(
        route = HomeRoute,
        enterTransition = {
            EnterTransition.None
        },
        popEnterTransition = {
            EnterTransition.None
        },
        exitTransition = {
            ExitTransition.None
        },
        popExitTransition = {
            ExitTransition.None
        }
    ) {
        val viewModel: HomeScreenViewModel = hiltViewModel()
        val uiState by viewModel.uiState.collectAsStateWithLifecycle()
        HomeScreen(
            uiState = uiState,
            onAddTodoList = viewModel::addTodoList,
            onMoveTodoList = viewModel::moveTodoList,
            onNavigateToTodoList = {
                onNavigateToTodoList(it)
                viewModel.ackNewTodoList()
            },
            onDeleteTodoList = viewModel::deleteTodoList,
            onOpenReminderMenu = viewModel::openReminderMenu,
            onReminderUpdated = viewModel::editReminder,
        )
    }
}

fun NavController.navigateToHome(navOptions: NavOptions? = null) {
    this.navigate(HomeRoute, navOptions)
}