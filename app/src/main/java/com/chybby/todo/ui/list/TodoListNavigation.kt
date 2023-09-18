package com.chybby.todo.ui.list

import android.app.PendingIntent
import android.app.TaskStackBuilder
import android.content.Context
import android.content.Intent
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.runtime.getValue
import androidx.core.net.toUri
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.compose.composable
import androidx.navigation.navDeepLink
import com.chybby.todo.ui.MainActivity

const val TodoListRoute = "todoList"

const val uri = "todolist://$TodoListRoute"

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
        deepLinks = listOf(navDeepLink { uriPattern = "$uri/{$todoListIdArg}" }),
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
            onReminderUpdated = viewModel::editReminder,
            onNavigateBack = onNavigateBack
        )
    }
}

fun NavController.navigateToTodoList(todoListId: Long, navOptions: NavOptions? = null) {
    this.navigate("$TodoListRoute/$todoListId", navOptions)
}

fun Context.pendingIntentForTodoList(todoListId: Long): PendingIntent {
    val deepLinkIntent = Intent(
        Intent.ACTION_VIEW,
        "$uri/$todoListId".toUri(),
        applicationContext,
        MainActivity::class.java
    )

    return TaskStackBuilder.create(applicationContext).run {
        addNextIntentWithParentStack(deepLinkIntent)
        getPendingIntent(0, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
    }
}