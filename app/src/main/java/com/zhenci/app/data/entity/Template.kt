package com.zhenci.app.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

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
                val gson = Gson()
                val type = object : TypeToken<List<Task>>() {}.type
                gson.fromJson(tasksJson, type) ?: emptyList()
            } catch (e: Exception) {
                emptyList()
            }
        } else {
            emptyList()
        }
    }
}