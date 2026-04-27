package com.zhenci.app.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "templates")
data class Template(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val description: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val isDefault: Boolean = false,
    val tasksJson: String? = null // 存储模板中的任务列表 JSON
) {
    // 获取模板中的任务列表（从 JSON 解析）
    fun getTasks(): List<Task> {
        return if (tasksJson != null) {
            try {
                val gson = com.google.gson.Gson()
                val type = object : com.google.gson.reflect.TypeToken<List<Task>>() {}.type
                gson.fromJson(tasksJson, type) ?: emptyList()
            } catch (e: Exception) {
                emptyList()
            }
        } else {
            emptyList()
        }
    }
}