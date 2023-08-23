package com.chybby.todo.ui.list

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chybby.todo.data.TodoItem
import com.chybby.todo.data.TodoList
import com.chybby.todo.data.TodoListRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import javax.inject.Inject

@HiltViewModel
class TodoListScreenViewModel @Inject constructor(
    private val todoListRepository: TodoListRepository,
    savedStateHandle: SavedStateHandle
): ViewModel() {
    private val _todoListArgs = TodoListArgs(savedStateHandle)
    private val _listId = _todoListArgs.todoListId

    private var _reminderMenuOpen: Boolean by mutableStateOf(false)

    val uiState: StateFlow<TodoListScreenUiState> = combine(
        snapshotFlow { _reminderMenuOpen },
        todoListRepository.getTodoListStreamById(_listId),
        todoListRepository.getTodoItemsStreamByListId(_listId),
    ) { reminderMenuOpen, todoList, todoItems ->
        TodoListScreenUiState(
            todoList = todoList,
            todoItems = todoItems,
            reminderMenuOpen = reminderMenuOpen,
            loading = false
        )
    }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            TodoListScreenUiState(
                todoList = TodoList(),
                todoItems = listOf(),
                reminderMenuOpen = false,
                loading = true
            ),
        )

    fun editName(name: String) = viewModelScope.launch {
        todoListRepository.renameTodoList(_listId, name)
    }

    fun addTodoItem(afterPosition: Int? = null) = viewModelScope.launch {
        todoListRepository.addTodoItem(_listId, afterPosition)
    }

    fun moveTodoItem(from: Int, to: Int) = viewModelScope.launch {
        val uncompletedItems = uiState.value.todoItems.filter { !it.isCompleted }

        if (to >= uncompletedItems.size || from >= uncompletedItems.size) {
            return@launch
        }

        var afterPosition = -1
        if (to > 0) {
            afterPosition = uncompletedItems[to - (if (from < to) 0 else 1)].position
        }
        val todoItemToMove = uncompletedItems[from]
        todoListRepository.moveTodoItem(todoItemToMove.id, afterPosition)
    }

    fun openReminderMenu(open: Boolean) {
        _reminderMenuOpen = open
    }

    fun editReminder(dateTime: LocalDateTime?) = viewModelScope.launch {
        todoListRepository.editTodoListReminder(_listId, dateTime)
    }

    fun editSummary(id: Long, summary: String) = viewModelScope.launch {
        todoListRepository.editTodoItemSummary(id, summary)
    }

    fun editCompleted(id: Long, completed: Boolean) = viewModelScope.launch {
        todoListRepository.completeTodoItem(id, completed)
    }

    fun deleteTodoItem(id: Long) = viewModelScope.launch {
        todoListRepository.deleteTodoItem(id)
    }

    fun deleteCompleted() = viewModelScope.launch {
        todoListRepository.deleteCompleted(_listId)
    }
}

data class TodoListScreenUiState(
    val todoList: TodoList,
    val todoItems: List<TodoItem>,
    val reminderMenuOpen: Boolean = false,
    val loading: Boolean = false,
)