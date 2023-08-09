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
import javax.inject.Inject

@HiltViewModel
class TodoListScreenViewModel @Inject constructor(
    private val todoListRepository: TodoListRepository,
    savedStateHandle: SavedStateHandle
): ViewModel() {
    private val _todoListArgs = TodoListArgs(savedStateHandle)
    private val _listId = _todoListArgs.todoListId

    private var _newTodoItemId: Long? by mutableStateOf(null)

    val uiState: StateFlow<TodoListScreenUiState> = combine(
        snapshotFlow { _newTodoItemId },
        todoListRepository.getTodoListStreamById(_listId),
        todoListRepository.getTodoItemsStreamByListId(_listId),
    ) { newTodoItemId, todoList, todoItems ->
        TodoListScreenUiState(todoList, todoItems, newTodoItemId)
    }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            TodoListScreenUiState(TodoList(), listOf(), null, true),
        )

    fun editName(name: String) = viewModelScope.launch {
        todoListRepository.renameTodoList(_listId, name)
    }

    fun addTodoItem(afterPosition: Int) = viewModelScope.launch {
        // TODO: position new item in correct position.
        _newTodoItemId = todoListRepository.addTodoItem(_listId)
    }

    fun ackNewTodoItem() {
        _newTodoItemId = null
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
    val newTodoItemId: Long? = null,
    val loading: Boolean = false,
)