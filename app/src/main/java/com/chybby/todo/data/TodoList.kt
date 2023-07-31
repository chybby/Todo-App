package com.chybby.todo.data

data class TodoList(
    val name: String = "",
    val position: Int = 0,
    val id: Long = 0,
)

data class TodoItem(
    val summary: String = "",
    val isCompleted: Boolean = false,
    val position: Int = 0,
    val id: Long = 0,
)
