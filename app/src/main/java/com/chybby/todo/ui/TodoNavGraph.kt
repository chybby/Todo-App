package com.chybby.todo.ui

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import com.chybby.todo.ui.home.HomeRoute
import com.chybby.todo.ui.home.homeScreen
import com.chybby.todo.ui.list.navigateToTodoList
import com.chybby.todo.ui.list.todoListScreen

@Composable
fun TodoNavGraph(modifier: Modifier = Modifier) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = HomeRoute,
        modifier = modifier,
        enterTransition = { EnterTransition.None },
        popEnterTransition = { EnterTransition.None },
        exitTransition = { ExitTransition.None },
        popExitTransition = { ExitTransition.None },
    ) {
        homeScreen(
            onNavigateToTodoList = { todoListId ->
                navController.navigateToTodoList(todoListId)
            }
        )

        todoListScreen(
            onNavigateBack = { navController.popBackStack(HomeRoute, inclusive = false) }
        )
    }
}