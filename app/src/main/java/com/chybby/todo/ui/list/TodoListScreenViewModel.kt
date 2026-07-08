package com.chybby.todo.ui.list

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chybby.todo.data.Reminder
import com.chybby.todo.data.TodoItem
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
class TodoListScreenViewModel @Inject constructor(
    private val todoListRepository: TodoListRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {
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
            todoItems = todoItems.toImmutableList(),
            reminderMenuOpen = reminderMenuOpen,
            loading = false
        )
    }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            TodoListScreenUiState(
                todoList = TodoList(),
                todoItems = persistentListOf(),
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

    fun moveTodoItem(itemId: Long, to: Int) = viewModelScope.launch {
        val uncompletedItems = uiState.value.todoItems.filter { !it.isCompleted }

        // Use the ID to find the correct item, in case the list has shifted.
        val actualFrom = uncompletedItems.indexOfFirst { it.id == itemId }
        if (actualFrom == -1 || to >= uncompletedItems.size) {
            return@launch
        }

        var afterPosition = -1
        if (to > 0) {
            // Find the item that will be BEFORE the moved item in the new order.
            val targetIndexInOldList = if (actualFrom < to) to else to - 1
            afterPosition = uncompletedItems[targetIndexInOldList].position
        }
        todoListRepository.moveTodoItem(itemId, afterPosition)
    }

    fun openReminderMenu(open: Boolean) {
        _reminderMenuOpen = open
    }

    fun editReminder(reminder: Reminder?) = viewModelScope.launch {
        todoListRepository.editTodoListReminder(_listId, reminder)
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

@Immutable
data class TodoListScreenUiState(
    val todoList: TodoList,
    val todoItems: ImmutableList<TodoItem>,
    val reminderMenuOpen: Boolean = false,
    val loading: Boolean = false,
)