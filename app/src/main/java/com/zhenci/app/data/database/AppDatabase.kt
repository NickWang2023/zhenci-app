package com.zhenci.app.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.zhenci.app.data.entity.Task
import com.zhenci.app.data.entity.Template
import com.zhenci.app.data.entity.UserStats

@Database(
    entities = [Task::class, Template::class, UserStats::class],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun taskDao(): TaskDao
    abstract fun templateDao(): TemplateDao
    abstract fun userStatsDao(): UserStatsDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "zhenci_database"
                )
                .fallbackToDestructiveMigration() // 数据库版本变化时重建数据库
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}