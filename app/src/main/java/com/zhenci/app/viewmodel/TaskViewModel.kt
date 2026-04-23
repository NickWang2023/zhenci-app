package com.zhenci.app.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.zhenci.app.data.database.AppDatabase
import com.zhenci.app.data.entity.Task
import com.zhenci.app.data.entity.UserStats
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class TaskViewModel(application: Application) : AndroidViewModel(application) {
    
    private val database = AppDatabase.getDatabase(application)
    private val taskDao = database.taskDao()
    private val userStatsDao = database.userStatsDao()
    
    // 任务列表
    val tasks: StateFlow<List<Task>> = taskDao.getAllTasks()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    
    // 用户统计
    val userStats: StateFlow<UserStats> = userStatsDao.getUserStats()
        .map { it ?: UserStats() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UserStats())
    
    // 今日积分
    val todayScore: StateFlow<Int> = userStats.map { it.todayScore }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)
    
    // 总积分
    val totalScore: StateFlow<Int> = userStats.map { it.totalScore }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)
    
    init {
        viewModelScope.launch {
            // 检查是否需要重置每日数据
            checkAndResetDailyStats()
            // 初始化默认任务（如果数据库为空）
            initializeDefaultTasks()
        }
    }
    
    private suspend fun checkAndResetDailyStats() {
        val stats = userStatsDao.getUserStatsSync() ?: UserStats()
        val today = LocalDate.now().format(DateTimeFormatter.ISO_DATE)
        
        if (stats.lastActiveDate != today) {
            // 新的一天，重置今日数据
            val newStats = stats.copy(
                todayScore = 0,
                todayExecuted = 0,
                todayClosed = 0,
                lastActiveDate = today
            )
            userStatsDao.insertOrUpdate(newStats)
            
            // 重置所有任务的完成状态
            taskDao.resetAllTasksCompletion()
        }
    }
    
    private suspend fun initializeDefaultTasks() {
        val currentTasks = taskDao.getAllTasksSync()
        if (currentTasks.isEmpty()) {
            // 添加默认任务
            val defaultTasks = listOf(
                Task(1, "晨间阅读", 7, 0, com.zhenci.app.data.entity.TaskType.WORK, true, false),
                Task(2, "吃早饭", 8, 0, com.zhenci.app.data.entity.TaskType.LIFE, true, false),
                Task(3, "开始工作", 9, 0, com.zhenci.app.data.entity.TaskType.WORK, true, false),
                Task(4, "吃午饭", 12, 0, com.zhenci.app.data.entity.TaskType.LIFE, true, false),
                Task(5, "运动健身", 18, 0, com.zhenci.app.data.entity.TaskType.LIFE, true, false),
                Task(6, "复盘总结", 21, 0, com.zhenci.app.data.entity.TaskType.WORK, true, false)
            )
            defaultTasks.forEach { taskDao.insertTask(it) }
        }
    }
    
    // 添加任务
    fun addTask(task: Task) {
        viewModelScope.launch {
            taskDao.insertTask(task)
        }
    }
    
    // 更新任务
    fun updateTask(task: Task) {
        viewModelScope.launch {
            taskDao.updateTask(task)
        }
    }
    
    // 删除任务
    fun deleteTask(task: Task) {
        viewModelScope.launch {
            taskDao.deleteTask(task)
        }
    }
    
    // 切换任务完成状态
    fun toggleTaskCompletion(taskId: Long, completed: Boolean) {
        viewModelScope.launch {
            taskDao.updateTaskCompletion(taskId, completed)
        }
    }
    
    // 执行任务（+1分）
    fun executeTask(taskId: Long) {
        viewModelScope.launch {
            // 标记任务完成
            taskDao.updateTaskCompletion(taskId, true)
            // 增加积分
            userStatsDao.addScore(1)
            userStatsDao.incrementExecuted()
        }
    }
    
    // 关闭任务（不加分）
    fun closeTask(taskId: Long) {
        viewModelScope.launch {
            userStatsDao.incrementClosed()
        }
    }
    
    // 获取未完成的任务
    fun getIncompleteTasks(): Flow<List<Task>> {
        return taskDao.getIncompleteTasks()
    }
}
