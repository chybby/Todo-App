package com.chybby.todo.data.di

import android.content.Context
import androidx.room.Room
import com.chybby.todo.data.local.TodoDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideTodoDatabase(
        @ApplicationContext context: Context,
    ) = Room.databaseBuilder(
        context,
        TodoDatabase::class.java,
        "todo_database"
    )
        .fallbackToDestructiveMigration()
        .build()

    @Provides
    @Singleton
    fun provideTodoListDao(db: TodoDatabase) = db.todoListDao()
}