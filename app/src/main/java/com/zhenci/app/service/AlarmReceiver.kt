package com.zhenci.app.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager

class AlarmReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "AlarmReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val taskId = intent.getLongExtra("task_id", -1)
        val content = intent.getStringExtra("task_content") ?: "针刺"
        Log.d(TAG, "onReceive: 收到闹钟广播 taskId=$taskId, content=$content")
        
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
        Log.d(TAG, "onReceive: WorkManager 任务已提交")
    }
}