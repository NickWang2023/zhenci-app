package com.zhenci.app.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.zhenci.app.data.database.AppDatabase
import com.zhenci.app.data.entity.Task
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * 模板详情页 ViewModel
 * 管理模板内的任务列表
 */
class TemplateDetailViewModel(
    application: Application,
    private val templateId: Long
) : AndroidViewModel(application) {

    private val database = AppDatabase.getDatabase(application)
    private val taskDao = database.taskDao()

    // 模板内的任务列表
    val templateTasks: StateFlow<List<Task>> = taskDao.getTasksByTemplate(templateId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /**
     * 添加任务到模板
     */
    fun addTask(task: Task) {
        viewModelScope.launch {
            taskDao.insertTask(task)
        }
    }

    /**
     * 更新模板任务
     */
    fun updateTask(task: Task) {
        viewModelScope.launch {
            taskDao.updateTask(task)
        }
    }

    /**
     * 删除模板任务
     */
    fun deleteTask(task: Task) {
        viewModelScope.launch {
            taskDao.deleteTask(task)
        }
    }

    /**
     * 获取模板任务数量
     */
    fun getTaskCount(): Int {
        return templateTasks.value.size
    }
}
