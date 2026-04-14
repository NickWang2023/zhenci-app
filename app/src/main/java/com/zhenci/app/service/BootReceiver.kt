package com.zhenci.app.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            // 重启后重新设置所有闹钟
            val alarmScheduler = AlarmScheduler(context)
            
            CoroutineScope(Dispatchers.IO).launch {
                // 这里可以从数据库读取所有启用的任务并重新设置闹钟
                // 简化处理：让用户重新打开应用时自动重置
            }
        }
    }
}