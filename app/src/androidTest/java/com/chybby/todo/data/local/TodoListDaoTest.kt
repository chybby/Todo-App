package com.chybby.todo.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TodoListDaoTest {

    private lateinit var database: TodoDatabase
    private lateinit var dao: TodoListDao

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, TodoDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = database.todoListDao()
    }

    @After
    fun closeDb() {
        database.close()
    }

    private fun todoList(name: String = "list") = TodoListEntity(
        name = name,
        position = 0,
        reminderDateTime = null,
        reminderLocationLatitude = null,
        reminderLocationLongitude = null,
        reminderLocationRadius = null,
        reminderLocationDescription = null,
        notificationId = null,
    )

    private fun todoItem(
        listId: Long,
        summary: String,
        completed: Boolean = false,
        position: Int = 0,
    ) = TodoItemEntity(
        summary = summary,
        isCompleted = completed,
        listId = listId,
        position = position,
        notificationId = null,
    )

    private suspend fun insertItems(listId: Long, vararg summaries: String): Map<String, Long> =
        summaries.associateWith { summary ->
            dao.insertTodoItemLast(todoItem(listId, summary))
        }

    private suspend fun itemsInOrder(listId: Long): List<TodoItemEntity> =
        dao.observeTodoItemsByListId(listId).first()

    private suspend fun summariesInOrder(listId: Long): List<String> =
        itemsInOrder(listId).map { it.summary }

    @Test
    fun insertTodoItemLast_assignsIncreasingPositions() = runBlocking {
        val listId = dao.insertTodoListLast(todoList())

        insertItems(listId, "A", "B", "C")

        val items = itemsInOrder(listId)
        assertEquals(listOf("A", "B", "C"), items.map { it.summary })
        assertEquals(listOf(0, 1, 2), items.map { it.position })
    }

    @Test
    fun insertTodoItemInPosition_shiftsLaterItems() = runBlocking {
        val listId = dao.insertTodoListLast(todoList())
        insertItems(listId, "A", "B", "C")

        dao.insertTodoItemInPosition(todoItem(listId, "D", position = 1))

        assertEquals(listOf("A", "D", "B", "C"), summariesInOrder(listId))
    }

    @Test
    fun moveTodoItem_movesItemAndShiftsOthers() = runBlocking {
        val listId = dao.insertTodoListLast(todoList())
        val ids = insertItems(listId, "A", "B", "C")

        dao.moveTodoItem(ids.getValue("C"), 0)

        assertEquals(listOf("C", "A", "B"), summariesInOrder(listId))
    }

    @Test
    fun moveTodoItem_missingItem_isNoOp() = runBlocking {
        val listId = dao.insertTodoListLast(todoList())
        insertItems(listId, "A", "B")

        dao.moveTodoItem(id = 12345, position = 0)

        assertEquals(listOf("A", "B"), summariesInOrder(listId))
    }

    @Test
    fun completeTodoItem_movesToTopOfCompletedSection() = runBlocking {
        val listId = dao.insertTodoListLast(todoList())
        val ids = insertItems(listId, "A", "B", "C")

        dao.completeTodoItem(ids.getValue("B"), true)
        dao.completeTodoItem(ids.getValue("A"), true)

        val items = itemsInOrder(listId)
        // The most recently completed item goes to the top of the completed section.
        assertEquals(listOf("C", "A", "B"), items.map { it.summary })
        assertEquals(listOf(false, true, true), items.map { it.isCompleted })
    }

    @Test
    fun uncompleteTodoItem_movesToBottomOfUncompletedSection() = runBlocking {
        val listId = dao.insertTodoListLast(todoList())
        val ids = insertItems(listId, "A", "B", "C")
        dao.completeTodoItem(ids.getValue("B"), true)
        dao.completeTodoItem(ids.getValue("A"), true)

        dao.completeTodoItem(ids.getValue("A"), false)

        val items = itemsInOrder(listId)
        assertEquals(listOf("C", "A", "B"), items.map { it.summary })
        assertEquals(listOf(false, false, true), items.map { it.isCompleted })
    }

    @Test
    fun completeTodoItem_missingItem_isNoOp() = runBlocking {
        val listId = dao.insertTodoListLast(todoList())
        insertItems(listId, "A")

        dao.completeTodoItem(id = 12345, completed = true)

        assertEquals(listOf("A"), summariesInOrder(listId))
    }

    @Test
    fun completeTodoItem_onlyItemInList_staysInList() = runBlocking {
        val listId = dao.insertTodoListLast(todoList())
        val ids = insertItems(listId, "A")

        dao.completeTodoItem(ids.getValue("A"), true)

        val items = itemsInOrder(listId)
        assertEquals(listOf("A"), items.map { it.summary })
        assertEquals(listOf(true), items.map { it.isCompleted })
    }

    @Test
    fun moveTodoList_movesListAndShiftsOthers() = runBlocking {
        val idA = dao.insertTodoListLast(todoList("A"))
        val idB = dao.insertTodoListLast(todoList("B"))
        val idC = dao.insertTodoListLast(todoList("C"))

        dao.moveTodoList(idC, 0)

        assertEquals(listOf("C", "A", "B"), dao.getTodoLists().map { it.name })
        assertEquals(listOf(idC, idA, idB), dao.getTodoLists().map { it.id })
    }

    @Test
    fun getTodoItemById_missingItem_returnsNull() = runBlocking {
        assertNull(dao.getTodoItemById(12345))
    }
}
