package com.zhenci.app

import android.app.KeyguardManager
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.*
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import com.zhenci.app.ui.components.ReminderDialog
import com.zhenci.app.ui.theme.ZhenciTheme
import com.zhenci.app.viewmodel.TaskViewModel
import kotlinx.coroutines.delay

/**
 * 提醒弹窗 Activity
 * 用于在闹钟触发时显示全屏提醒对话框
 */
class ReminderActivity : ComponentActivity() {

    private val viewModel: TaskViewModel by viewModels {
        object : androidx.lifecycle.ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                return TaskViewModel(application) as T
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 获取任务信息
        val taskId = intent.getLongExtra("task_id", -1)
        val taskContent = intent.getStringExtra("task_content") ?: "针刺提醒"
        val taskHour = intent.getIntExtra("task_hour", 0)
        val taskMinute = intent.getIntExtra("task_minute", 0)

        android.util.Log.d("ReminderActivity", "onCreate: taskId=$taskId, content=$taskContent, time=$taskHour:$taskMinute")

        if (taskId == -1L) {
            android.util.Log.e("ReminderActivity", "Invalid taskId, finishing")
            finish()
            return
        }

        // 唤醒屏幕（黑屏时点亮）
        wakeUpScreen()

        // 设置窗口属性（显示在锁屏上）
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
            )
        }

        // 解锁键盘（如果设备有安全锁屏）
        val keyguardManager = getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            keyguardManager.requestDismissKeyguard(this, null)
        }

        setContent {
            ZhenciTheme {
                var showDialog by remember { mutableStateOf(true) }
                val timeString = String.format("%02d:%02d", taskHour, taskMinute)

                if (showDialog) {
                    var isProcessing by remember { mutableStateOf(false) }
                    
                    ReminderDialog(
                        taskContent = taskContent,
                        taskTime = timeString,
                        isProcessing = isProcessing,
                        onExecute = {
                            // 执行任务：+1分，标记完成
                            android.util.Log.d("ReminderActivity", "onExecute clicked for taskId=$taskId")
                            isProcessing = true
                            lifecycleScope.launch {
                                // 等待任务完成（数据库更新完成）
                                viewModel.executeTask(taskId).await()
                                android.util.Log.d("ReminderActivity", "executeTask completed for taskId=$taskId")
                                // 取消通知
                                cancelNotification(taskId)
                                showDialog = false
                                finish()
                            }
                        },
                        onClose = {
                            // 关闭任务：不加分
                            android.util.Log.d("ReminderActivity", "onClose clicked for taskId=$taskId")
                            isProcessing = true
                            lifecycleScope.launch {
                                // 等待关闭操作完成
                                viewModel.closeTask(taskId).await()
                                android.util.Log.d("ReminderActivity", "closeTask completed for taskId=$taskId")
                                // 取消通知
                                cancelNotification(taskId)
                                showDialog = false
                                finish()
                            }
                        }
                    )
                }
            }
        }

        // 30秒后自动关闭（防止一直占用屏幕）
        lifecycleScope.launch {
            delay(30000)
            if (!isFinishing) {
                finish()
            }
        }
    }

    private fun wakeUpScreen() {
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        val wakeLock = powerManager.newWakeLock(
            PowerManager.FULL_WAKE_LOCK or
            PowerManager.ACQUIRE_CAUSES_WAKEUP or
            PowerManager.ON_AFTER_RELEASE,
            "Zhenci::ReminderActivityWakeLock"
        )
        wakeLock.acquire(30000)
    }

    private fun cancelNotification(taskId: Long) {
        try {
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
            notificationManager.cancel(taskId.toInt())
            android.util.Log.d("ReminderActivity", "通知已取消: taskId=$taskId")
        } catch (e: Exception) {
            android.util.Log.e("ReminderActivity", "取消通知失败: ${e.message}")
        }
    }

    override fun onBackPressed() {
        // 禁止返回键关闭，必须点击按钮
        // 不调用 super.onBackPressed()
    }
}
