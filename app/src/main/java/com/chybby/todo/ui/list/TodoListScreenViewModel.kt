package com.chybby.todo.ui.list

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

    val uiState: StateFlow<TodoListScreenUiState> = combine(
        todoListRepository.getTodoListStreamById(_listId),
        todoListRepository.getTodoItemsStreamByListId(_listId),
    ) { todoList, todoItems ->
        TodoListScreenUiState(todoList, todoItems)
    }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            TodoListScreenUiState(TodoList(), listOf(), true),
        )

    fun editName(name: String) = viewModelScope.launch {
        todoListRepository.renameTodoList(_listId, name)
    }

    fun addTodoItem() = viewModelScope.launch {
        todoListRepository.addTodoItem(_listId)
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
}

data class TodoListScreenUiState(
    val todoList: TodoList,
    val todoItems: List<TodoItem>,
    val loading: Boolean = false,
)