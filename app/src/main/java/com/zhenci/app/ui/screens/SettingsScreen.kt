package com.zhenci.app.ui.screens

import android.content.Intent
import android.speech.tts.TextToSpeech
import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen() {
    val context = LocalContext.current
    var notificationsEnabled by remember { mutableStateOf(true) }
    var voiceEnabled by remember { mutableStateOf(true) }
    var volume by remember { mutableStateOf(0.8f) }
    var ttsStatus by remember { mutableStateOf("未测试") }
    
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