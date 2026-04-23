package com.zhenci.app.data.database

import androidx.room.*
import com.zhenci.app.data.entity.Task
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {
    @Query("SELECT * FROM tasks ORDER BY hour, minute")
    fun getAllTasks(): Flow<List<Task>>

    @Query("SELECT * FROM tasks ORDER BY hour, minute")
    suspend fun getAllTasksSync(): List<Task>

    @Query("SELECT * FROM tasks WHERE templateId = :templateId ORDER BY hour, minute")
    fun getTasksByTemplate(templateId: Long): Flow<List<Task>>

    @Query("SELECT * FROM tasks WHERE isEnabled = 1 ORDER BY hour, minute")
    fun getEnabledTasks(): Flow<List<Task>>

    @Query("SELECT * FROM tasks WHERE isCompleted = 0 ORDER BY hour, minute")
    fun getIncompleteTasks(): Flow<List<Task>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: Task): Long

    @Update
    suspend fun updateTask(task: Task)

    @Delete
    suspend fun deleteTask(task: Task)

    @Query("DELETE FROM tasks WHERE templateId = :templateId")
    suspend fun deleteTasksByTemplate(templateId: Long)

    @Query("SELECT * FROM tasks WHERE hour = :hour AND minute = :minute AND isEnabled = 1")
    suspend fun getTasksAtTime(hour: Int, minute: Int): List<Task>

    @Query("UPDATE tasks SET isCompleted = :completed WHERE id = :taskId")
    suspend fun updateTaskCompletion(taskId: Long, completed: Boolean)

    @Query("UPDATE tasks SET isCompleted = 0")
    suspend fun resetAllTasksCompletion()
}