package com.zhenci.app.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tasks")
data class Task(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val content: String,        // 任务内容，10字以内
    val hour: Int,
    val minute: Int,
    val type: TaskType = TaskType.OTHER,
    val isEnabled: Boolean = true,
    val isCompleted: Boolean = false,
    val templateId: Long = 0
)

enum class TaskType {
    WORK, LIFE, OTHER
}