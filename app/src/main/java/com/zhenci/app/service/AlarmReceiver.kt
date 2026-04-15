package com.zhenci.app.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager

class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val taskId = intent.getLongExtra("task_id", -1)
        val content = intent.getStringExtra("task_content") ?: "针刺"
        
        // 通过 WorkManager 执行提醒任务
        val inputData = Data.Builder()
            .putLong("task_id", taskId)
            .putString("task_content", content)
            .build()

        val workRequest = OneTimeWorkRequestBuilder<ReminderWorker>()
            .setInputData(inputData)
            .addTag("alarm_task_$taskId")
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            "alarm_task_$taskId",
            ExistingWorkPolicy.REPLACE,
            workRequest
        )
    }
}