package com.zhenci.app.ui.screens

import android.app.TimePickerDialog
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.zhenci.app.data.entity.Task
import com.zhenci.app.data.entity.TaskType
import com.zhenci.app.data.entity.Template
import com.zhenci.app.viewmodel.TemplateDetailViewModel
import kotlinx.coroutines.flow.collectLatest

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TemplateDetailScreen(
    template: Template,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val viewModel: TemplateDetailViewModel = viewModel(
        factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                return TemplateDetailViewModel(
                    context.applicationContext as android.app.Application,
                    template.id
                ) as T
            }
        }
    )

    val templateTasks by viewModel.templateTasks.collectAsState()
    var showAddTaskDialog by remember { mutableStateOf(false) }
    var showEditTaskDialog by remember { mutableStateOf(false) }
    var editingTask by remember { mutableStateOf<Task?>(null) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var deletingTask by remember { mutableStateOf<Task?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(template.name)
                        Text(
                            text = "${templateTasks.size} 个任务",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddTaskDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "添加任务")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // 模板描述
            if (template.description.isNotBlank()) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                ) {
                    Text(
                        text = template.description,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(16.dp),
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }

            // 任务列表
            if (templateTasks.isEmpty()) {
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
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(
                        items = templateTasks.sortedBy { it.hour * 60 + it.minute },
                        key = { it.id }
                    ) { task ->
                        TemplateTaskCard(
                            task = task,
                            onEdit = {
                                editingTask = task
                                showEditTaskDialog = true
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
    if (showAddTaskDialog) {
        TemplateTaskDialog(
            title = "添加模板任务",
            task = null,
            templateId = template.id,
            onDismiss = { showAddTaskDialog = false },
            onConfirm = { newTask ->
                viewModel.addTask(newTask)
                showAddTaskDialog = false
            }
        )
    }

    // 编辑任务对话框
    if (showEditTaskDialog && editingTask != null) {
        TemplateTaskDialog(
            title = "编辑模板任务",
            task = editingTask,
            templateId = template.id,
            onDismiss = {
                showEditTaskDialog = false
                editingTask = null
            },
            onConfirm = { updatedTask ->
                viewModel.updateTask(updatedTask)
                showEditTaskDialog = false
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
                            viewModel.deleteTask(task)
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
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TemplateTaskCard(
    task: Task,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val timeString = String.format("%02d:%02d", task.hour, task.minute)

    val typeColor = when (task.type) {
        TaskType.WORK -> com.zhenci.app.ui.theme.WorkTaskColor
        TaskType.LIFE -> com.zhenci.app.ui.theme.LifeTaskColor
        TaskType.OTHER -> com.zhenci.app.ui.theme.OtherTaskColor
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 时间显示
            Surface(
                color = typeColor.copy(alpha = 0.2f),
                shape = MaterialTheme.shapes.small
            ) {
                Text(
                    text = timeString,
                    style = MaterialTheme.typography.titleMedium,
                    color = typeColor,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // 任务内容
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = task.content,
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    text = when (task.type) {
                        TaskType.WORK -> "工作"
                        TaskType.LIFE -> "生活"
                        TaskType.OTHER -> "其他"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = typeColor
                )
            }

            // 操作按钮
            IconButton(onClick = onEdit) {
                Icon(Icons.Default.Edit, contentDescription = "编辑")
            }
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "删除",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
fun TemplateTaskDialog(
    title: String,
    task: Task?,
    templateId: Long,
    onDismiss: () -> Unit,
    onConfirm: (Task) -> Unit
) {
    val context = LocalContext.current

    var taskContent by remember { mutableStateOf(task?.content ?: "") }
    var selectedType by remember { mutableStateOf(task?.type ?: TaskType.WORK) }
    var selectedHour by remember { mutableStateOf(task?.hour ?: 9) }
    var selectedMinute by remember { mutableStateOf(task?.minute ?: 0) }

    val timePickerDialog = TimePickerDialog(
        context,
        { _, hour, minute ->
            selectedHour = hour
            selectedMinute = minute
        },
        selectedHour,
        selectedMinute,
        true
    )

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
                        isEnabled = true,
                        isCompleted = false,
                        templateId = templateId
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
