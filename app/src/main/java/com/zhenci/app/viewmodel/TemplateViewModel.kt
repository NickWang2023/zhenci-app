package com.zhenci.app.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.zhenci.app.data.database.AppDatabase
import com.zhenci.app.data.entity.Task
import com.zhenci.app.data.entity.Template
import com.zhenci.app.service.AlarmScheduler
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class TemplateViewModel(application: Application) : AndroidViewModel(application) {
    
    private val database = AppDatabase.getDatabase(application)
    private val templateDao = database.templateDao()
    private val taskDao = database.taskDao()
    
    // 防止 applyTemplate 被重复调用
    private var isApplyingTemplate = false
    
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
    fun applyTemplate(template: Template, clearExisting: Boolean = false, onSuccess: () -> Unit = {}) {
        // 防止重复调用
        if (isApplyingTemplate) {
            android.util.Log.d("TemplateViewModel", "applyTemplate 正在执行中，跳过重复调用")
            return
        }
        
        viewModelScope.launch {
            isApplyingTemplate = true
            try {
                val alarmScheduler = AlarmScheduler(getApplication())
                
                // 从数据库获取模板关联的任务（通过 templateId 关联）
                val allTemplateTasks = taskDao.getAllTasksSync().filter { it.templateId == template.id }
                
                // 去重：如果模板中有重复任务，只保留一个
                val templateTasks = allTemplateTasks.distinctBy { "${it.content}_${it.hour}_${it.minute}" }
                
                android.util.Log.d("TemplateViewModel", "应用模板: ${template.name}, 模板任务数: ${templateTasks.size}")
                
                if (templateTasks.isNotEmpty()) {
                    // 如果要求清空现有任务，先删除所有非模板任务并取消它们的闹钟
                    if (clearExisting) {
                        val existingTasks = taskDao.getAllTasksSync().filter { it.templateId == 0L }
                        existingTasks.forEach { task ->
                            // 取消旧任务的闹钟
                            alarmScheduler.cancelTask(task.id)
                            // 从数据库删除任务
                            taskDao.deleteTask(task)
                        }
                    }
                    
                    // 将模板任务复制到今日任务（templateId = 0 表示是今日任务）
                    templateTasks.forEach { task ->
                        // 检查是否已存在完全相同的任务（去重）
                        val existingTodayTasks = taskDao.getAllTasksSync().filter { it.templateId == 0L }
                        val isDuplicate = existingTodayTasks.any { existing ->
                            existing.content == task.content && 
                            existing.hour == task.hour && 
                            existing.minute == task.minute
                        }
                        
                        if (!isDuplicate) {
                            val newTask = task.copy(
                                id = 0, // 新任务，让数据库自动生成ID
                                templateId = 0, // 0 表示这是今日任务，不是模板任务
                                isCompleted = false,
                                isEnabled = true
                            )
                            val newTaskId = taskDao.insertTask(newTask)
                            // 为新任务设置闹钟
                            val insertedTask = newTask.copy(id = newTaskId)
                            alarmScheduler.scheduleTask(insertedTask)
                            android.util.Log.d("TemplateViewModel", "添加任务: ${task.content}")
                        } else {
                            android.util.Log.d("TemplateViewModel", "跳过重复任务: ${task.content}")
                        }
                    }
                    onSuccess()
                }
            } finally {
                isApplyingTemplate = false
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
