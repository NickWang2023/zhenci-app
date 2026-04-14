package com.zhenci.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen() {
    var notificationsEnabled by remember { mutableStateOf(true) }
    var voiceEnabled by remember { mutableStateOf(true) }
    var volume by remember { mutableStateOf(0.8f) }

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