package com.zhenci.app.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class AlarmReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "AlarmReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val taskId = intent.getLongExtra("task_id", -1)
        val content = intent.getStringExtra("task_content") ?: "针刺"
        val hour = intent.getIntExtra("task_hour", 0)
        val minute = intent.getIntExtra("task_minute", 0)
        Log.d(TAG, "onReceive: 收到闹钟广播 taskId=$taskId, content=$content, time=$hour:$minute")
        
        // 使用前台服务显示提醒弹窗和语音播报
        // Android 10+ 限制后台启动 Activity，前台服务可以绕过此限制
        ReminderForegroundService.start(context, taskId, content, hour, minute)
        Log.d(TAG, "onReceive: 前台服务已启动")
    }
}