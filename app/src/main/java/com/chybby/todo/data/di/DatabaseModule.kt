package com.chybby.todo.data.di

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.chybby.todo.data.local.TodoDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

val MIGRATION_8_9 = object : Migration(8, 9) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("CREATE INDEX index_todo_list_notification_id ON todo_list(notification_id)")
        db.execSQL("CREATE INDEX index_todo_item_list_id ON todo_item(list_id)")
        db.execSQL("CREATE INDEX index_todo_item_notification_id ON todo_item(notification_id)")
    }
}

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
        .addMigrations(MIGRATION_8_9)
        .fallbackToDestructiveMigration()
        .build()

    @Provides
    @Singleton
    fun provideTodoListDao(db: TodoDatabase) = db.todoListDao()
}