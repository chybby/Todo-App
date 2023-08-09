package com.chybby.todo.ui.home

import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.compose.composable

const val HomeRoute = "home"

fun NavGraphBuilder.homeScreen(
    onNavigateToTodoList: (todoListId: Long) -> Unit
) {
    composable(HomeRoute) {
        val viewModel: HomeScreenViewModel = hiltViewModel()
        val uiState by viewModel.uiState.collectAsStateWithLifecycle()
        HomeScreen(
            uiState = uiState,
            onAddTodoList = viewModel::addTodoList,
            onNavigateToTodoList = {
                onNavigateToTodoList(it)
                viewModel.ackNewTodoList()
            },
            onDeleteTodoList = viewModel::deleteTodoList,
        )
    }
}

fun NavController.navigateToHome(navOptions: NavOptions? = null) {
    this.navigate(HomeRoute, navOptions)
}