package com.zhenci.app.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.zhenci.app.data.entity.AIConfig
import com.zhenci.app.data.repository.AIConfigRepository
import com.zhenci.app.service.AIService
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AIGenerateDialog(
    onDismiss: () -> Unit,
    onScheduleGenerated: (templateName: String, description: String, tasks: List<AIService.GeneratedTask>) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val aiConfigRepository = remember { AIConfigRepository(context) }
    
    var profession by remember { mutableStateOf("") }
    var familySituation by remember { mutableStateOf("") }
    var constraints by remember { mutableStateOf("") }
    var preferences by remember { mutableStateOf("") }
    
    var isGenerating by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    
    // 检查是否配置了API Key
    val hasApiKey = aiConfigRepository.hasApiKey()
    
    AlertDialog(
        onDismissRequest = { if (!isGenerating) onDismiss() },
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Text("AI 智能生成模板")
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (!hasApiKey) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "⚠️ 请先前往「设置」页面配置 AI API Key",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }
                
                if (errorMessage != null) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "❌ $errorMessage",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }
                
                // 职业/身份
                OutlinedTextField(
                    value = profession,
                    onValueChange = { profession = it },
                    label = { Text("职业/身份 *") },
                    placeholder = { Text("例如：中年IT从业者、自由职业者、全职妈妈") },
                    minLines = 2,
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = hasApiKey && !isGenerating
                )
                
                // 家庭情况
                OutlinedTextField(
                    value = familySituation,
                    onValueChange = { familySituation = it },
                    label = { Text("家庭情况") },
                    placeholder = { Text("例如：有两个孩子，一个上小学，一个上幼儿园") },
                    minLines = 2,
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = hasApiKey && !isGenerating
                )
                
                // 时间约束
                OutlinedTextField(
                    value = constraints,
                    onValueChange = { constraints = it },
                    label = { Text("时间约束 *") },
                    placeholder = { Text("例如：早上5:30送孩子上学，晚上8:30接孩子回家") },
                    minLines = 2,
                    maxLines = 4,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = hasApiKey && !isGenerating
                )
                
                // 其他偏好
                OutlinedTextField(
                    value = preferences,
                    onValueChange = { preferences = it },
                    label = { Text("其他偏好/需求") },
                    placeholder = { Text("例如：需要午休1小时，希望有运动时间，晚上要陪孩子写作业") },
                    minLines = 2,
                    maxLines = 4,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = hasApiKey && !isGenerating
                )
                
                // 提示信息
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "💡 提示",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "AI 将根据您提供的信息，生成周一至周五的日程建议。\n\n填写越详细，生成的日程越符合您的需求。",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    scope.launch {
                        isGenerating = true
                        errorMessage = null
                        
                        try {
                            val config = aiConfigRepository.getConfig()
                            val aiService = AIService(config)
                            
                            val request = AIService.ScheduleRequest(
                                profession = profession,
                                familySituation = familySituation,
                                constraints = constraints,
                                preferences = preferences
                            )
                            
                            val result = aiService.generateSchedule(request)
                            
                            result.onSuccess { schedule ->
                                onScheduleGenerated(
                                    schedule.templateName,
                                    schedule.description,
                                    schedule.tasks
                                )
                            }.onFailure { error ->
                                errorMessage = error.message ?: "生成失败，请重试"
                            }
                        } catch (e: Exception) {
                            errorMessage = e.message ?: "发生未知错误"
                        } finally {
                            isGenerating = false
                        }
                    }
                },
                enabled = hasApiKey && 
                         profession.isNotBlank() && 
                         constraints.isNotBlank() && 
                         !isGenerating
            ) {
                if (isGenerating) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                        Text("生成中...")
                    }
                } else {
                    Text("生成日程")
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isGenerating
            ) {
                Text("取消")
            }
        }
    )
}

@Composable
fun AIResultPreviewDialog(
    templateName: String,
    description: String,
    tasks: List<AIService.GeneratedTask>,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("预览生成的日程") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = templateName,
                    style = MaterialTheme.typography.titleMedium
                )
                if (description.isNotBlank()) {
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = "任务列表（共${tasks.size}项）：",
                    style = MaterialTheme.typography.labelMedium
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                tasks.sortedBy { it.hour * 60 + it.minute }.forEach { task ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = String.format("%02d:%02d", task.hour, task.minute),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = task.content,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onConfirm) {
                Text("创建模板")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}