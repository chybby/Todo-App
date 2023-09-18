package com.chybby.todo.data

import com.chybby.todo.data.local.TodoItemEntity
import com.chybby.todo.data.local.TodoListEntity

// There are two models:
//  - External: the model exposed from the data layer to the rest of the application.
//  - Local: the model used by the database.

// External to local.

fun TodoList.toLocal() = TodoListEntity(
    id = id,
    name = name,
    position = position,
    reminderDateTime = reminderDateTime,
    notificationId = notificationId,
)

@JvmName("todoListExternalToLocal")
fun List<TodoList>.toLocal() = map(TodoList::toLocal)

fun TodoItem.toLocal() = TodoItemEntity(
    id = id,
    summary = summary,
    isCompleted = isCompleted,
    listId = listId,
    position = position,
    notificationId = notificationId,
)

@JvmName("todoItemExternalToLocal")
fun List<TodoItem>.toLocal() = map { it.toLocal() }

// Local to external.

fun TodoItemEntity.toExternal() = TodoItem(
    summary = summary,
    isCompleted = isCompleted,
    listId = listId,
    position = position,
    notificationId = notificationId,
    id = id,
)

@JvmName("todoItemLocalToExternal")
fun List<TodoItemEntity>.toExternal() = map(TodoItemEntity::toExternal)

fun TodoListEntity.toExternal() = TodoList(
    name = name,
    position = position,
    reminderDateTime = reminderDateTime,
    notificationId = notificationId,
    id = id,
)

@JvmName("todoListLocalToExternal")
fun List<TodoListEntity>.toExternal() = map(TodoListEntity::toExternal)