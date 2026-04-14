package com.zhenci.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(
    totalScore: Int = 0,
    todayScore: Int = 0,
    totalExecuted: Int = 0,
    todayExecuted: Int = 0,
    currentStreak: Int = 0,
    maxStreak: Int = 0
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("积分统计") },
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
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 总积分卡片
            ScoreCard(
                title = "总积分",
                score = totalScore,
                icon = Icons.Default.Star,
                color = MaterialTheme.colorScheme.primary
            )
            
            // 今日积分卡片
            ScoreCard(
                title = "今日积分",
                score = todayScore,
                icon = Icons.Default.TrendingUp,
                color = MaterialTheme.colorScheme.secondary
            )
            
            // 执行统计
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                StatBox(
                    title = "今日执行",
                    value = todayExecuted.toString(),
                    modifier = Modifier.weight(1f)
                )
                StatBox(
                    title = "累计执行",
                    value = totalExecuted.toString(),
                    modifier = Modifier.weight(1f)
                )
            }
            
            // 连续打卡
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                StatBox(
                    title = "当前连续",
                    value = "$currentStreak 天",
                    icon = Icons.Default.LocalFireDepartment,
                    iconColor = MaterialTheme.colorScheme.error,
                    modifier = Modifier.weight(1f)
                )
                StatBox(
                    title = "最高连续",
                    value = "$maxStreak 天",
                    icon = Icons.Default.EmojiEvents,
                    iconColor = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.weight(1f)
                )
            }
            
            // 积分规则说明
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "积分规则",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("• 点击「执行」按钮：+1分")
                    Text("• 点击「关闭」按钮：不加分")
                    Text("• 连续每日执行：保持连续天数")
                }
            }
        }
    }
}

@Composable
fun ScoreCard(
    title: String,
    score: Int,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: androidx.compose.ui.graphics.Color
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = color.copy(alpha = 0.1f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = score.toString(),
                    fontSize = 48.sp,
                    fontWeight = FontWeight.Bold,
                    color = color
                )
            }
        }
    }
}

@Composable
fun StatBox(
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    iconColor: androidx.compose.ui.graphics.Color? = null,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconColor ?: MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
            Text(
                text = value,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}