package com.zhenci.app.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.zhenci.app.data.database.AppDatabase
import com.zhenci.app.data.entity.Task
import com.zhenci.app.data.entity.UserStats
import com.zhenci.app.service.AlarmScheduler
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class TaskViewModel(application: Application) : AndroidViewModel(application) {
    
    private val database = AppDatabase.getDatabase(application)
    private val taskDao = database.taskDao()
    private val userStatsDao = database.userStatsDao()
    
    // 任务列表 - 只获取今日任务（templateId = 0），排除模板任务
    val tasks: StateFlow<List<Task>> = taskDao.getTodayTasks()
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
            refreshData()
        }
    }
    
    /**
     * 强制刷新数据
     */
    fun refreshData() {
        viewModelScope.launch {
            android.util.Log.d("TaskViewModel", "refreshData: 开始刷新数据")
            // 检查是否需要重置每日数据
            checkAndResetDailyStats()
            // 初始化默认任务（如果数据库为空）
            initializeDefaultTasks()
            // 重新为所有启用的任务注册闹钟（修复应用重装/重启后闹钟丢失的问题）
            rescheduleAllAlarms()
            // 强制刷新任务列表 - 触发 Flow 重新发射
            refreshTasks()
            android.util.Log.d("TaskViewModel", "refreshData: 数据刷新完成")
        }
    }
    
    /**
     * 强制刷新任务列表
     * 通过查询数据库并手动触发 Flow 更新
     */
    private suspend fun refreshTasks() {
        android.util.Log.d("TaskViewModel", "refreshTasks: 强制刷新任务列表")
        // 查询最新任务数据，Flow 会自动发射新值
        val latestTasks = taskDao.getAllTasksSync()
        android.util.Log.d("TaskViewModel", "refreshTasks: 获取到 ${latestTasks.size} 个任务")
        // 检查是否有已完成的任务
        val completedCount = latestTasks.count { it.isCompleted }
        android.util.Log.d("TaskViewModel", "refreshTasks: 已完成任务数: $completedCount")
    }

    /**
     * 重新为所有启用的任务注册闹钟
     * 解决应用重装、系统重启后闹钟丢失的问题
     */
    private suspend fun rescheduleAllAlarms() {
        val alarmScheduler = AlarmScheduler(getApplication())
        val enabledTasks = taskDao.getAllTasksSync().filter { it.isEnabled && it.templateId == 0L }
        enabledTasks.forEach { task ->
            alarmScheduler.scheduleTask(task)
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
    
    // 添加任务，返回新任务id
    fun addTask(task: Task, onTaskAdded: (Long) -> Unit = {}) {
        viewModelScope.launch {
            val newId = taskDao.insertTask(task)
            android.util.Log.d("TaskViewModel", "addTask: 新任务已添加，id=$newId")
            onTaskAdded(newId)
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
    
    // 执行任务（+1分）- 返回 Deferred 以便调用者可以等待完成
    fun executeTask(taskId: Long): kotlinx.coroutines.Deferred<Unit> {
        return viewModelScope.async {
            android.util.Log.d("TaskViewModel", "executeTask: 开始执行任务 taskId=$taskId")
            // 标记任务完成
            taskDao.updateTaskCompletion(taskId, true)
            android.util.Log.d("TaskViewModel", "executeTask: 任务已标记为完成 taskId=$taskId")
            // 增加积分
            userStatsDao.addScore(1)
            userStatsDao.incrementExecuted()
            android.util.Log.d("TaskViewModel", "executeTask: 积分已增加 taskId=$taskId")
        }
    }
    
    // 关闭任务（不加分，但标记为完成，只记录关闭次数）- 返回 Deferred 以便调用者可以等待完成
    fun closeTask(taskId: Long): kotlinx.coroutines.Deferred<Unit> {
        return viewModelScope.async {
            android.util.Log.d("TaskViewModel", "closeTask: 开始关闭任务 taskId=$taskId")
            // 标记任务完成（有划线显示）
            taskDao.updateTaskCompletion(taskId, true)
            android.util.Log.d("TaskViewModel", "closeTask: 任务已标记为完成 taskId=$taskId")
            // 增加关闭计数
            userStatsDao.incrementClosed()
            android.util.Log.d("TaskViewModel", "closeTask: 关闭计数已增加 taskId=$taskId")
        }
    }
    
    // 获取未完成的任务
    fun getIncompleteTasks(): Flow<List<Task>> {
        return taskDao.getIncompleteTasks()
    }
}
