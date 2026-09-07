package com.zhenci.app.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.zhenci.app.data.entity.Task

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

        // ------------------------------------------------------------------
        // 【每日重复】的关键续排点。
        // 收到广播 = 当前这一次要被触发了，立即把【明天同刻】的下一次闹钟排好。
        // 放在这里（而非依赖 ReminderWorker 播完后再排）保证：即使后续弹窗/TTS/进程
        // 未能顺利完成，下一天的日程也绝不会断。scheduleTask 内部会因当前时刻已过而
        // 自动顺延到明天，天然幂等，不会错位。
        // ------------------------------------------------------------------
        try {
            val task = Task(
                id = taskId,
                content = content,
                hour = hour,
                minute = minute,
                isEnabled = true
            )
            AlarmScheduler(context).scheduleTask(task)
            Log.d(TAG, "onReceive: 已续排任务 $taskId 明天的下一次（每日重复链续上）")
        } catch (e: Exception) {
            Log.e(TAG, "onReceive: 续排失败 ${e.message}", e)
        }

        // ------------------------------------------------------------------
        // 之后由 WorkManager 执行 弹窗/通知/语音播报 等耗时提醒动作。
        // 此处使用 enqueue (REPLACE) 而不是 cancel+重新排，避免打断上面刚排好的续排。
        // ------------------------------------------------------------------
        val inputData = Data.Builder()
            .putLong("task_id", taskId)
            .putString("task_content", content)
            .putInt("task_hour", hour)
            .putInt("task_minute", minute)
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
        Log.d(TAG, "onReceive: WorkManager 提醒任务已提交")
    }
}
