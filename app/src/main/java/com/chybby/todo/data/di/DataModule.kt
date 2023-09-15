package com.chybby.todo.data.di

import com.chybby.todo.data.DefaultReminderRepository
import com.chybby.todo.data.OfflineTodoListRepository
import com.chybby.todo.data.ReminderRepository
import com.chybby.todo.data.TodoListRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class DataModule {

    @Binds
    @Singleton
    abstract fun bindTodoListRepository(
        offlineTodoListRepository: OfflineTodoListRepository,
    ): TodoListRepository

    @Binds
    @Singleton
    abstract fun bindReminderRepository(
        defaultReminderRepository: DefaultReminderRepository,
    ): ReminderRepository
}