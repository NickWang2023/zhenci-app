package com.zhenci.app.ui.screens

import android.content.Intent
import android.speech.tts.TextToSpeech
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.zhenci.app.data.entity.AIProvider
import com.zhenci.app.data.repository.AIConfigRepository
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen() {
    val context = LocalContext.current
    val aiConfigRepository = remember { AIConfigRepository(context) }
    
    var notificationsEnabled by remember { mutableStateOf(true) }
    var voiceEnabled by remember { mutableStateOf(true) }
    var volume by remember { mutableStateOf(0.8f) }
    var ttsStatus by remember { mutableStateOf("未测试") }
    
    // AI配置对话框状态
    var showAIConfigDialog by remember { mutableStateOf(false) }
    
    // TTS 测试
    val tts = remember { 
        TextToSpeech(context) { status ->
            ttsStatus = if (status == TextToSpeech.SUCCESS) {
                "TTS 引擎正常"
            } else {
                "TTS 引擎初始化失败"
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("设置") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            // 提醒设置
            SettingsSection(title = "提醒设置") {
                ListItem(
                    headlineContent = { Text("启用通知") },
                    supportingContent = { Text("接收定时提醒通知") },
                    leadingContent = {
                        Icon(Icons.Default.Notifications, contentDescription = null)
                    },
                    trailingContent = {
                        Switch(
                            checked = notificationsEnabled,
                            onCheckedChange = { notificationsEnabled = it }
                        )
                    }
                )
                
                ListItem(
                    headlineContent = { Text("语音播报") },
                    supportingContent = { Text("到达时间点自动语音提醒") },
                    leadingContent = {
                        Icon(Icons.Default.VolumeUp, contentDescription = null)
                    },
                    trailingContent = {
                        Switch(
                            checked = voiceEnabled,
                            onCheckedChange = { voiceEnabled = it }
                        )
                    }
                )
                
                if (voiceEnabled) {
                    ListItem(
                        headlineContent = { Text("音量") },
                        supportingContent = {
                            Slider(
                                value = volume,
                                onValueChange = { volume = it },
                                valueRange = 0f..1f
                            )
                        }
                    )
                }
            }

            Divider()

            // 数据管理
            SettingsSection(title = "数据管理") {
                ListItem(
                    headlineContent = { Text("导出所有数据") },
                    supportingContent = { Text("将模板和任务导出为 JSON") }
                )
                ListItem(
                    headlineContent = { Text("导入数据") },
                    supportingContent = { Text("从 JSON 文件导入") }
                )
                ListItem(
                    headlineContent = { Text("清除所有数据") },
                    supportingContent = { Text("删除所有模板和任务") },
                    colors = ListItemDefaults.colors(
                        headlineColor = MaterialTheme.colorScheme.error
                    )
                )
            }

            Divider()
            
            // AI 设置
            SettingsSection(title = "AI 设置") {
                val hasApiKey = aiConfigRepository.hasApiKey()
                ListItem(
                    headlineContent = { Text("AI 模板生成") },
                    supportingContent = { 
                        Text(if (hasApiKey) "已配置 API Key" else "未配置 API Key") 
                    },
                    leadingContent = {
                        Icon(Icons.Default.SmartToy, contentDescription = null)
                    },
                    trailingContent = {
                        if (hasApiKey) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "已配置",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                )
                Button(
                    onClick = { showAIConfigDialog = true },
                    modifier = Modifier.padding(horizontal = 16.dp)
                ) {
                    Text(if (hasApiKey) "修改 AI 配置" else "配置 AI")
                }
            }

            Divider()
            
            // TTS 测试
            SettingsSection(title = "语音测试") {
                ListItem(
                    headlineContent = { Text("TTS 状态") },
                    supportingContent = { Text(ttsStatus) }
                )
                ListItem(
                    headlineContent = { Text("测试语音播报") },
                    supportingContent = { Text("点击测试语音是否正常") },
                    leadingContent = {
                        Icon(Icons.Default.VolumeUp, contentDescription = null)
                    }
                )
                Button(
                    onClick = {
                        if (ttsStatus == "TTS 引擎正常") {
                            tts.language = Locale.CHINESE
                            tts.speak("测试语音播报，如果您听到这段文字，说明语音功能正常", TextToSpeech.QUEUE_FLUSH, null, null)
                        }
                    },
                    modifier = Modifier.padding(horizontal = 16.dp)
                ) {
                    Text("测试语音")
                }
                ListItem(
                    headlineContent = { Text("打开 TTS 设置") },
                    supportingContent = { Text("检查系统语音引擎设置") },
                    leadingContent = {
                        Icon(Icons.Default.Settings, contentDescription = null)
                    }
                )
                Button(
                    onClick = {
                        val intent = Intent()
                        intent.action = "com.android.settings.TTS_SETTINGS"
                        try {
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            // 尝试另一种方式
                            val intent2 = Intent(android.provider.Settings.ACTION_SETTINGS)
                            context.startActivity(intent2)
                        }
                    },
                    modifier = Modifier.padding(horizontal = 16.dp)
                ) {
                    Text("打开系统 TTS 设置")
                }
            }
            
            // AI配置对话框
            if (showAIConfigDialog) {
                AIConfigDialog(
                    repository = aiConfigRepository,
                    onDismiss = { showAIConfigDialog = false }
                )
            }

            Divider()

            // 关于
            SettingsSection(title = "关于") {
                ListItem(
                    headlineContent = { Text("版本") },
                    supportingContent = { Text("1.0.0") },
                    leadingContent = {
                        Icon(Icons.Default.Info, contentDescription = null)
                    }
                )
                ListItem(
                    headlineContent = { Text("关于针刺") },
                    supportingContent = { Text("自律自强，掌控每日") }
                )
            }
        }
    }
}

@Composable
fun SettingsSection(
    title: String,
    content: @Composable () -> Unit
) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
        content()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AIConfigDialog(
    repository: AIConfigRepository,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val currentConfig = remember { repository.getConfig() }
    
    var selectedProvider by remember { mutableStateOf(currentConfig.provider) }
    var apiKey by remember { mutableStateOf(currentConfig.apiKey) }
    var model by remember { mutableStateOf(currentConfig.model) }
    var baseUrl by remember { mutableStateOf(currentConfig.baseUrl) }
    var showApiKey by remember { mutableStateOf(false) }
    
    // 当切换provider时更新默认值
    LaunchedEffect(selectedProvider) {
        if (model.isBlank() || model == AIProvider.KIMI.getDefaultModel() || model == AIProvider.OPENAI.getDefaultModel()) {
            model = selectedProvider.getDefaultModel()
        }
        if (baseUrl.isBlank() || baseUrl == AIProvider.KIMI.getDefaultBaseUrl() || baseUrl == AIProvider.OPENAI.getDefaultBaseUrl()) {
            baseUrl = selectedProvider.getDefaultBaseUrl()
        }
    }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("AI 配置") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 提供商选择
                Text(
                    text = "选择 AI 提供商",
                    style = MaterialTheme.typography.labelMedium
                )
                
                AIProvider.entries.forEach { provider ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        RadioButton(
                            selected = selectedProvider == provider,
                            onClick = { selectedProvider = provider }
                        )
                        Text(
                            text = provider.displayName,
                            modifier = Modifier.align(androidx.compose.ui.Alignment.CenterVertically)
                        )
                    }
                }
                
                Divider()
                
                // API Key
                OutlinedTextField(
                    value = apiKey,
                    onValueChange = { apiKey = it },
                    label = { Text("API Key") },
                    singleLine = true,
                    visualTransformation = if (showApiKey) {
                        androidx.compose.ui.text.input.VisualTransformation.None
                    } else {
                        androidx.compose.ui.text.input.PasswordVisualTransformation()
                    },
                    trailingIcon = {
                        IconButton(onClick = { showApiKey = !showApiKey }) {
                            Icon(
                                imageVector = if (showApiKey) {
                                    Icons.Default.Visibility
                                } else {
                                    Icons.Default.VisibilityOff
                                },
                                contentDescription = if (showApiKey) "隐藏" else "显示"
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
                
                // 模型
                OutlinedTextField(
                    value = model,
                    onValueChange = { model = it },
                    label = { Text("模型名称") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                
                // Base URL（自定义时显示）
                if (selectedProvider == AIProvider.CUSTOM) {
                    OutlinedTextField(
                        value = baseUrl,
                        onValueChange = { baseUrl = it },
                        label = { Text("Base URL") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                
                // 说明文字
                Text(
                    text = when (selectedProvider) {
                        AIProvider.KIMI -> "推荐使用 Kimi，国内访问稳定。获取 API Key: https://platform.moonshot.cn"
                        AIProvider.OPENAI -> "需要 OpenAI API Key。注意：国内访问可能需要代理"
                        AIProvider.CUSTOM -> "支持任何 OpenAI 兼容格式的 API"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val config = com.zhenci.app.data.entity.AIConfig(
                        provider = selectedProvider,
                        apiKey = apiKey.trim(),
                        model = model.trim(),
                        baseUrl = baseUrl.trim()
                    )
                    repository.saveConfig(config)
                    Toast.makeText(context, "AI 配置已保存", Toast.LENGTH_SHORT).show()
                    onDismiss()
                },
                enabled = apiKey.isNotBlank()
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