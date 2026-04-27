package com.zhenci.app.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.zhenci.app.data.database.AppDatabase
import com.zhenci.app.data.entity.Task
import com.zhenci.app.data.entity.Template
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class TemplateViewModel(application: Application) : AndroidViewModel(application) {
    
    private val database = AppDatabase.getDatabase(application)
    private val templateDao = database.templateDao()
    private val taskDao = database.taskDao()
    
    // 模板列表
    val templates: StateFlow<List<Template>> = templateDao.getAllTemplates()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    
    // 添加模板
    fun addTemplate(name: String, description: String, tasks: List<Task> = emptyList()) {
        viewModelScope.launch {
            val template = Template(
                name = name,
                description = description,
                createdAt = System.currentTimeMillis(),
                isDefault = false
            )
            val templateId = templateDao.insertTemplate(template)
            
            // 如果有任务，保存到模板任务表
            if (tasks.isNotEmpty()) {
                val gson = Gson()
                val tasksJson = gson.toJson(tasks)
                templateDao.updateTemplateTasks(templateId, tasksJson)
            }
        }
    }
    
    // 更新模板
    fun updateTemplate(template: Template) {
        viewModelScope.launch {
            templateDao.updateTemplate(template)
        }
    }
    
    // 删除模板
    fun deleteTemplate(template: Template) {
        viewModelScope.launch {
            templateDao.deleteTemplate(template)
        }
    }
    
    // 复制模板
    fun duplicateTemplate(template: Template) {
        viewModelScope.launch {
            val newTemplate = template.copy(
                id = 0,
                name = "${template.name} 副本",
                createdAt = System.currentTimeMillis(),
                isDefault = false
            )
            templateDao.insertTemplate(newTemplate)
        }
    }
    
    // 应用模板 - 将模板的任务添加到今日任务
    fun applyTemplate(template: Template, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            // 获取模板中的任务
            val templateWithTasks = templateDao.getTemplateWithTasks(template.id)
            val tasks = templateWithTasks?.tasks ?: emptyList()
            
            if (tasks.isNotEmpty()) {
                // 将模板任务添加到任务表
                tasks.forEach { task ->
                    val newTask = task.copy(
                        id = 0, // 新任务
                        isCompleted = false,
                        isEnabled = true
                    )
                    taskDao.insertTask(newTask)
                }
                onSuccess()
            }
        }
    }
    
    // 导出模板为 JSON
    fun exportTemplate(template: Template): String {
        val gson = Gson()
        return gson.toJson(template)
    }
    
    // 从 JSON 导入模板
    fun importTemplate(json: String, onSuccess: () -> Unit = {}, onError: (String) -> Unit = {}) {
        viewModelScope.launch {
            try {
                val gson = Gson()
                val template = gson.fromJson(json, Template::class.java)
                val newTemplate = template.copy(
                    id = 0, // 新模板
                    createdAt = System.currentTimeMillis(),
                    isDefault = false
                )
                templateDao.insertTemplate(newTemplate)
                onSuccess()
            } catch (e: Exception) {
                onError(e.message ?: "导入失败")
            }
        }
    }
    
    // 设置默认模板
    fun setDefaultTemplate(templateId: Long) {
        viewModelScope.launch {
            templateDao.clearDefaultTemplates()
            templateDao.setDefaultTemplate(templateId)
        }
    }
}
