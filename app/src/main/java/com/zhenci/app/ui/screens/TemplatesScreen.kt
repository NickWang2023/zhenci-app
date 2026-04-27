package com.zhenci.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.zhenci.app.data.entity.Template
import com.zhenci.app.viewmodel.TemplateViewModel
import android.app.Application
import android.widget.Toast

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TemplatesScreen() {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    
    val viewModel: TemplateViewModel = viewModel(
        factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                return TemplateViewModel(context.applicationContext as Application) as T
            }
        }
    )
    
    val templates by viewModel.templates.collectAsState()
    
    var showCreateDialog by remember { mutableStateOf(false) }
    var showImportDialog by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    var editingTemplate by remember { mutableStateOf<Template?>(null) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var deletingTemplate by remember { mutableStateOf<Template?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("日程模板") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        floatingActionButton = {
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SmallFloatingActionButton(
                    onClick = { showImportDialog = true }
                ) {
                    Icon(Icons.Default.FileUpload, contentDescription = "导入模板")
                }
                FloatingActionButton(onClick = { showCreateDialog = true }) {
                    Icon(Icons.Default.Add, contentDescription = "创建模板")
                }
            }
        }
    ) { padding ->
        if (templates.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "暂无模板",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "点击右下角创建按钮添加模板",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(templates, key = { it.id }) { template ->
                    TemplateCard(
                        template = template,
                        onEdit = { 
                            editingTemplate = template
                            showEditDialog = true
                        },
                        onDuplicate = { 
                            viewModel.duplicateTemplate(template)
                            Toast.makeText(context, "已复制模板", Toast.LENGTH_SHORT).show()
                        },
                        onDelete = { 
                            deletingTemplate = template
                            showDeleteConfirm = true
                        },
                        onExport = { 
                            val json = viewModel.exportTemplate(template)
                            clipboardManager.setText(AnnotatedString(json))
                            Toast.makeText(context, "模板已复制到剪贴板", Toast.LENGTH_SHORT).show()
                        },
                        onApply = { 
                            viewModel.applyTemplate(template) {
                                Toast.makeText(context, "模板已应用到今日任务", Toast.LENGTH_SHORT).show()
                            }
                        },
                        onSetDefault = {
                            viewModel.setDefaultTemplate(template.id)
                            Toast.makeText(context, "已设为默认模板", Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            }
        }
    }

    if (showCreateDialog) {
        CreateTemplateDialog(
            onDismiss = { showCreateDialog = false },
            onConfirm = { name, description ->
                viewModel.addTemplate(name, description)
                showCreateDialog = false
                Toast.makeText(context, "模板创建成功", Toast.LENGTH_SHORT).show()
            }
        )
    }

    if (showEditDialog && editingTemplate != null) {
        EditTemplateDialog(
            template = editingTemplate!!,
            onDismiss = { 
                showEditDialog = false
                editingTemplate = null
            },
            onConfirm = { name, description ->
                val updatedTemplate = editingTemplate!!.copy(
                    name = name,
                    description = description
                )
                viewModel.updateTemplate(updatedTemplate)
                showEditDialog = false
                editingTemplate = null
                Toast.makeText(context, "模板更新成功", Toast.LENGTH_SHORT).show()
            }
        )
    }

    if (showDeleteConfirm && deletingTemplate != null) {
        AlertDialog(
            onDismissRequest = { 
                showDeleteConfirm = false
                deletingTemplate = null
            },
            title = { Text("确认删除") },
            text = { Text("确定要删除模板「${deletingTemplate?.name}」吗？") },
            confirmButton = {
                TextButton(
                    onClick = {
                        deletingTemplate?.let {
                            viewModel.deleteTemplate(it)
                            Toast.makeText(context, "模板已删除", Toast.LENGTH_SHORT).show()
                        }
                        showDeleteConfirm = false
                        deletingTemplate = null
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
                    deletingTemplate = null
                }) {
                    Text("取消")
                }
            }
        )
    }

    if (showImportDialog) {
        ImportTemplateDialog(
            onDismiss = { showImportDialog = false },
            onConfirm = { json ->
                viewModel.importTemplate(
                    json = json,
                    onSuccess = {
                        showImportDialog = false
                        Toast.makeText(context, "模板导入成功", Toast.LENGTH_SHORT).show()
                    },
                    onError = { error ->
                        Toast.makeText(context, "导入失败: $error", Toast.LENGTH_LONG).show()
                    }
                )
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TemplateCard(
    template: Template,
    onEdit: () -> Unit,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit,
    onExport: () -> Unit,
    onApply: () -> Unit,
    onSetDefault: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = template.name,
                        style = MaterialTheme.typography.titleLarge
                    )
                    if (template.description.isNotBlank()) {
                        Text(
                            text = template.description,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (template.isDefault) {
                        Spacer(modifier = Modifier.height(4.dp))
                        AssistChip(
                            onClick = {},
                            label = { Text("默认") },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer
                            )
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onApply,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("应用")
                }
                IconButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, contentDescription = "编辑")
                }
                IconButton(onClick = onDuplicate) {
                    Icon(Icons.Default.ContentCopy, contentDescription = "复制")
                }
                IconButton(onClick = onExport) {
                    Icon(Icons.Default.FileDownload, contentDescription = "导出")
                }
                if (!template.isDefault) {
                    IconButton(onClick = onSetDefault) {
                        Icon(Icons.Default.CheckCircle, contentDescription = "设为默认")
                    }
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
}

@Composable
fun CreateTemplateDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("创建模板") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("模板名称") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("模板描述") },
                    minLines = 2
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(name, description) },
                enabled = name.isNotBlank()
            ) {
                Text("创建")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

@Composable
fun EditTemplateDialog(
    template: Template,
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit
) {
    var name by remember { mutableStateOf(template.name) }
    var description by remember { mutableStateOf(template.description) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("编辑模板") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("模板名称") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("模板描述") },
                    minLines = 2
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(name, description) },
                enabled = name.isNotBlank()
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

@Composable
fun ImportTemplateDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var jsonText by remember { mutableStateOf("") }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("导入模板") },
        text = {
            Column {
                Text(
                    "粘贴模板 JSON 数据",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                OutlinedTextField(
                    value = jsonText,
                    onValueChange = { jsonText = it },
                    label = { Text("模板数据") },
                    minLines = 5,
                    maxLines = 10
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(jsonText) },
                enabled = jsonText.isNotBlank()
            ) {
                Text("导入")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}
