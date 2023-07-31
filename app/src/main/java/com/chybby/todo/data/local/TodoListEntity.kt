package com.chybby.todo.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "todo_list")
data class TodoListEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    // The position of the list on the home screen.
    val position: Int,

    // TODO: remind based on time or location.
)

//data class TodoListAndItems(
//    @Embedded
//    val todoList: TodoListEntity,
//    @Relation(
//        parentColumn = "id",
//        entityColumn = "list_id"
//    )
//    val todoItems: List<TodoItemEntity>
//)