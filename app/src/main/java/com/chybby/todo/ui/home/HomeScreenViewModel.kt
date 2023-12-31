package com.chybby.todo.ui.home

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chybby.todo.data.Reminder
import com.chybby.todo.data.TodoList
import com.chybby.todo.data.TodoListRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeScreenViewModel @Inject constructor(
    private val todoListRepository: TodoListRepository,
) : ViewModel() {

    private var _newTodoListId: Long? by mutableStateOf(null)
    private var _reminderMenuOpenForListId: Long? by mutableStateOf(null)

    val uiState: StateFlow<HomeScreenUiState> = combine(
        snapshotFlow { _newTodoListId },
        snapshotFlow { _reminderMenuOpenForListId },
        todoListRepository.todoListsStream,
    ) { newTodoListId, reminderMenuOpenForListId, todoLists ->
        HomeScreenUiState(todoLists.toImmutableList(), newTodoListId, reminderMenuOpenForListId)
    }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            HomeScreenUiState(persistentListOf(), null, null, true),
        )

    fun openReminderMenu(listId: Long?) {
        _reminderMenuOpenForListId = listId
    }

    fun editReminder(listId: Long, reminder: Reminder?) = viewModelScope.launch {
        todoListRepository.editTodoListReminder(listId, reminder)
    }

    fun addTodoList() = viewModelScope.launch {
        _newTodoListId = todoListRepository.addTodoList()
    }

    fun ackNewTodoList() {
        _newTodoListId = null
    }

    fun moveTodoList(from: Int, to: Int) = viewModelScope.launch {
        var afterPosition = -1
        if (to > 0) {
            afterPosition = uiState.value.todoLists[to - (if (from < to) 0 else 1)].position
        }
        val todoListToMove = uiState.value.todoLists[from]
        todoListRepository.moveTodoList(todoListToMove.id, afterPosition)
    }

    fun deleteTodoList(id: Long) = viewModelScope.launch {
        todoListRepository.deleteTodoList(id)
    }
}

data class HomeScreenUiState(
    val todoLists: ImmutableList<TodoList>,
    val newTodoListId: Long?,
    val reminderMenuOpenForListId: Long?,
    val loading: Boolean = false,
)