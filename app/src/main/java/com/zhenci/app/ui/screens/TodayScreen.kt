package com.zhenci.app.ui.screens

import android.app.TimePickerDialog
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.zhenci.app.data.entity.Task
import com.zhenci.app.data.entity.TaskType
import com.zhenci.app.ui.theme.LifeTaskColor
import com.zhenci.app.ui.theme.OtherTaskColor
import com.zhenci.app.ui.theme.WorkTaskColor
import com.zhenci.app.ui.components.ReminderDialog
import com.zhenci.app.service.AlarmScheduler
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodayScreen() {
    val context = LocalContext.current
    
    // 任务列表状态（实际应用应从数据库读取）
    var tasks by remember {
        mutableStateOf(
            mutableListOf(
                Task(1, "晨间阅读", 7, 0, TaskType.WORK, true, false),
                Task(2, "吃早饭", 8, 0, TaskType.LIFE, true, false),
                Task(3, "开始工作", 9, 0, TaskType.WORK, true, false),
                Task(4, "吃午饭", 12, 0, TaskType.LIFE, true, false),
                Task(5, "运动健身", 18, 0, TaskType.LIFE, true, false),
                Task(6, "复盘总结", 21, 0, TaskType.WORK, true, false)
            )
        )
    }
    
    // 对话框状态
    var showAddDialog by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    var editingTask by remember { mutableStateOf<Task?>(null) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var deletingTask by remember { mutableStateOf<Task?>(null) }
    
    // 提醒弹窗状态
    var showReminderDialog by remember { mutableStateOf(false) }
    var reminderTask by remember { mutableStateOf<Task?>(null) }
    
    // 积分状态（实际应从数据库读取）
    var totalScore by remember { mutableStateOf(0) }
    var todayScore by remember { mutableStateOf(0) }
    
    // 闹钟调度器
    val alarmScheduler = remember { AlarmScheduler(context) }
    
    // 计算完成率
    val completedCount = tasks.count { it.isCompleted }
    val completionRate = if (tasks.isNotEmpty()) (completedCount * 100 / tasks.size) else 0
    
    // 模拟：检查是否有任务到达提醒时间（实际应由AlarmManager触发）
    LaunchedEffect(Unit) {
        // 这里模拟弹窗，实际应由AlarmReceiver触发
        // 为了演示，延迟3秒显示一个模拟弹窗
        kotlinx.coroutines.delay(3000)
        if (tasks.isNotEmpty()) {
            reminderTask = tasks.first()
            showReminderDialog = true
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Column {
                        Text("今日日程")
                        Text(
                            text = "完成 $completedCount/${tasks.size} | 积分 $todayScore",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "添加任务")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // 当前时间显示
            CurrentTimeHeader()
            
            // 任务列表
            if (tasks.isEmpty()) {
                EmptyTaskView()
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(
                        items = tasks.sortedBy { it.hour * 60 + it.minute },
                        key = { it.id }
                    ) { task ->
                        TaskCard(
                            task = task,
                            onToggleComplete = {
                                tasks = tasks.map { 
                                    if (it.id == task.id) it.copy(isCompleted = !it.isCompleted) else it 
                                }.toMutableList()
                            },
                            onToggleEnable = {
                                val newEnabledState = !task.isEnabled
                                tasks = tasks.map { 
                                    if (it.id == task.id) it.copy(isEnabled = newEnabledState) else it 
                                }.toMutableList()
                                // 更新闹钟状态
                                if (newEnabledState) {
                                    alarmScheduler.scheduleTask(task.copy(isEnabled = true))
                                } else {
                                    alarmScheduler.cancelTask(task.id)
                                }
                            },
                            onEdit = {
                                editingTask = task
                                showEditDialog = true
                            },
                            onDelete = {
                                deletingTask = task
                                showDeleteConfirm = true
                            }
                        )
                    }
                }
            }
        }
    }

    // 添加任务对话框
    if (showAddDialog) {
        TaskDialog(
            title = "添加任务",
            task = null,
            onDismiss = { showAddDialog = false },
            onConfirm = { newTask ->
                val newId = (tasks.maxOfOrNull { it.id } ?: 0) + 1
                val taskWithId = newTask.copy(id = newId)
                tasks.add(taskWithId)
                // 如果启用了提醒，设置闹钟
                if (taskWithId.isEnabled) {
                    alarmScheduler.scheduleTask(taskWithId)
                }
                showAddDialog = false
            }
        )
    }

    // 编辑任务对话框
    if (showEditDialog && editingTask != null) {
        TaskDialog(
            title = "编辑任务",
            task = editingTask,
            onDismiss = { 
                showEditDialog = false
                editingTask = null
            },
            onConfirm = { updatedTask ->
                tasks = tasks.map { 
                    if (it.id == updatedTask.id) updatedTask else it 
                }.toMutableList()
                // 重新设置闹钟
                alarmScheduler.cancelTask(updatedTask.id)
                if (updatedTask.isEnabled) {
                    alarmScheduler.scheduleTask(updatedTask)
                }
                showEditDialog = false
                editingTask = null
            }
        )
    }

    // 删除确认对话框
    if (showDeleteConfirm && deletingTask != null) {
        AlertDialog(
            onDismissRequest = { 
                showDeleteConfirm = false
                deletingTask = null
            },
            title = { Text("确认删除") },
            text = { Text("确定要删除任务「${deletingTask?.content}」吗？") },
            confirmButton = {
                TextButton(
                    onClick = {
                        deletingTask?.let { task ->
                            alarmScheduler.cancelTask(task.id)
                            tasks.removeAll { it.id == task.id }
                        }
                        showDeleteConfirm = false
                        deletingTask = null
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("删除")
                }
            },
            dismissButton = {
                TextButton(onClick = { 
                    showDeleteConfirm = false
                    deletingTask = null
                }) {
                    Text("取消")
                }
            }
        )
    }

    // 提醒弹窗
    if (showReminderDialog && reminderTask != null) {
        ReminderDialog(
            taskContent = reminderTask!!.content,
            taskTime = String.format("%02d:%02d", reminderTask!!.hour, reminderTask!!.minute),
            onExecute = {
                // 执行：+1分，标记完成
                totalScore += 1
                todayScore += 1
                tasks = tasks.map { 
                    if (it.id == reminderTask!!.id) it.copy(isCompleted = true) else it 
                }.toMutableList()
                showReminderDialog = false
                reminderTask = null
            },
            onClose = {
                // 关闭：不加分，仅关闭弹窗
                showReminderDialog = false
                reminderTask = null
            }
        )
    }
}

@Composable
fun EmptyTaskView() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "暂无任务",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "点击右下角添加按钮创建任务",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
fun CurrentTimeHeader() {
    var currentTime by remember { mutableStateOf(LocalTime.now()) }
    val formatter = DateTimeFormatter.ofPattern("HH:mm:ss")
    
    // 每秒更新时间
    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(1000)
            currentTime = LocalTime.now()
        }
    }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "当前时间",
                style = MaterialTheme.typography.labelLarge
            )
            Text(
                text = currentTime.format(formatter),
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskCard(
    task: Task,
    onToggleComplete: () -> Unit,
    onToggleEnable: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val timeString = String.format("%02d:%02d", task.hour, task.minute)
    
    val typeColor = when (task.type) {
        TaskType.WORK -> WorkTaskColor
        TaskType.LIFE -> LifeTaskColor
        TaskType.OTHER -> OtherTaskColor
    }
    
    var showMenu by remember { mutableStateOf(false) }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (task.isCompleted) 
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            else 
                MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 完成状态按钮
            IconButton(onClick = onToggleComplete) {
                Icon(
                    imageVector = if (task.isCompleted) 
                        Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                    contentDescription = if (task.isCompleted) "已完成" else "未完成",
                    tint = if (task.isCompleted) 
                        MaterialTheme.colorScheme.primary 
                    else 
                        MaterialTheme.colorScheme.outline
                )
            }
            
            // 时间
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.width(55.dp)
            ) {
                Text(
                    text = timeString,
                    style = MaterialTheme.typography.titleMedium,
                    color = if (task.isEnabled) MaterialTheme.colorScheme.primary 
                           else MaterialTheme.colorScheme.outline,
                    textDecoration = if (task.isCompleted) 
                        androidx.compose.ui.text.style.TextDecoration.LineThrough 
                    else 
                        null
                )
            }
            
            Divider(
                modifier = Modifier
                    .height(35.dp)
                    .width(1.dp),
                color = MaterialTheme.colorScheme.outlineVariant
            )
            
            Spacer(modifier = Modifier.width(12.dp))
            
            // 任务内容
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = task.content.take(10),
                    style = MaterialTheme.typography.titleMedium,
                    textDecoration = if (task.isCompleted) 
                        androidx.compose.ui.text.style.TextDecoration.LineThrough 
                    else 
                        null,
                    color = if (task.isCompleted) 
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    else 
                        MaterialTheme.colorScheme.onSurface,
                    maxLines = 1
                )
                // 类型标签
                AssistChip(
                    onClick = {},
                    label = { 
                        Text(
                            when (task.type) {
                                TaskType.WORK -> "工作"
                                TaskType.LIFE -> "生活"
                                TaskType.OTHER -> "其他"
                            },
                            style = MaterialTheme.typography.labelSmall
                        )
                    },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = typeColor.copy(alpha = 0.1f),
                        labelColor = typeColor
                    ),
                    modifier = Modifier.height(28.dp)
                )
            }
            
            // 更多操作菜单
            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(Icons.Default.MoreVert, contentDescription = "更多")
                }
                
                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text(if (task.isEnabled) "禁用提醒" else "启用提醒") },
                        onClick = {
                            onToggleEnable()
                            showMenu = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("编辑") },
                        leadingIcon = { Icon(Icons.Default.Edit, null) },
                        onClick = {
                            onEdit()
                            showMenu = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("删除") },
                        leadingIcon = { 
                            Icon(
                                Icons.Default.Delete, 
                                null,
                                tint = MaterialTheme.colorScheme.error
                            ) 
                        },
                        onClick = {
                            onDelete()
                            showMenu = false
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun TaskDialog(
    title: String,
    task: Task?,
    onDismiss: () -> Unit,
    onConfirm: (Task) -> Unit
) {
    val context = LocalContext.current
    val isEditing = task != null
    
    var taskContent by remember { mutableStateOf(task?.content ?: "") }
    var selectedType by remember { mutableStateOf(task?.type ?: TaskType.WORK) }
    var selectedHour by remember { mutableStateOf(task?.hour ?: 9) }
    var selectedMinute by remember { mutableStateOf(task?.minute ?: 0) }
    var isEnabled by remember { mutableStateOf(task?.isEnabled ?: true) }
    
    val timePickerDialog = remember {
        TimePickerDialog(
            context,
            { _, hour, minute ->
                selectedHour = hour
                selectedMinute = minute
            },
            selectedHour,
            selectedMinute,
            true
        )
    }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 时间选择
                OutlinedCard(
                    onClick = { timePickerDialog.show() },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("提醒时间")
                        Text(
                            String.format("%02d:%02d", selectedHour, selectedMinute),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                
                OutlinedTextField(
                    value = taskContent,
                    onValueChange = { 
                        if (it.length <= 10) taskContent = it 
                    },
                    label = { Text("任务内容 *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    supportingText = { 
                        Text("${taskContent.length}/10") 
                    }
                )
                
                Text("任务类型", style = MaterialTheme.typography.labelLarge)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TaskType.values().forEach { type ->
                        FilterChip(
                            selected = selectedType == type,
                            onClick = { selectedType = type },
                            label = { 
                                Text(
                                    when (type) {
                                        TaskType.WORK -> "工作"
                                        TaskType.LIFE -> "生活"
                                        TaskType.OTHER -> "其他"
                                    }
                                )
                            }
                        )
                    }
                }
                
                // 启用开关
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("启用提醒", style = MaterialTheme.typography.bodyLarge)
                    Switch(
                        checked = isEnabled,
                        onCheckedChange = { isEnabled = it }
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val newTask = Task(
                        id = task?.id ?: 0,
                        content = taskContent,
                        hour = selectedHour,
                        minute = selectedMinute,
                        type = selectedType,
                        isEnabled = isEnabled,
                        isCompleted = task?.isCompleted ?: false
                    )
                    onConfirm(newTask)
                },
                enabled = taskContent.isNotBlank()
            ) {
                Text("保存")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}
