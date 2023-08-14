package com.chybby.todo.ui.home

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chybby.todo.data.TodoList
import com.chybby.todo.data.TodoListRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeScreenViewModel @Inject constructor(
    private val todoListRepository: TodoListRepository
): ViewModel() {

    private var _newTodoListId: Long? by mutableStateOf(null)

    val uiState: StateFlow<HomeScreenUiState> = combine(
        snapshotFlow { _newTodoListId },
        todoListRepository.todoListsStream,
    ) { newTodoListId, todoLists ->
        HomeScreenUiState(todoLists, newTodoListId)
    }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            HomeScreenUiState(listOf(), null),
        )

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
    val todoLists: List<TodoList>,
    val newTodoListId: Long?,
)