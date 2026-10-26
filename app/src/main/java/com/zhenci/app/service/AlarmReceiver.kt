package com.zhenci.app.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager

/**
 * 单闹钟模型的触发与续排点。
 *
 * 方案 A 之后，全库只有一个闹钟。它触发时代表"当前时刻到点的那一条任务"，
 * 本 Receiver 做三件事：
 *   1. 读取 Intent 里携带的任务快照，**回查数据库确认该任务仍然启用**（防止已删任务复活）；
 *   2. 立刻调用 AlarmScheduler.reschedule() 计算并排下一条（链式续排，与 TTS 成败解耦）；
 *   3. 把提醒动作交给 WorkManager。
 *
 * 安全说明：本 Receiver 在 Manifest 中 exported=false，只能被本 App 的
 * PendingIntent 触发，第三方 App 无法伪造广播。
 */
class AlarmReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "AlarmReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val taskId = intent.getLongExtra(AlarmScheduler.EXTRA_TASK_ID, -1)
        val content = intent.getStringExtra(AlarmScheduler.EXTRA_TASK_CONTENT) ?: "针刺"
        val hour = intent.getIntExtra(AlarmScheduler.EXTRA_TASK_HOUR, 0)
        val minute = intent.getIntExtra(AlarmScheduler.EXTRA_TASK_MINUTE, 0)
        Log.d(TAG, "onReceive: taskId=$taskId, content=$content, time=$hour:$minute")

        if (taskId <= 0) {
            // 非法/占位广播（例如 cancelGlobalAlarm 用的空 PendingIntent），直接忽略
            Log.w(TAG, "onReceive: 非法 taskId=$taskId，忽略")
            return
        }

        // ---------------------------------------------------------------
        // 1+2. 回查数据库 + 立即续排下一条
        //
        // 旧实现直接用 Intent 里的 extra 重建 Task 并把 isEnabled 硬编码为 true，
        // 于是"用户已删除/已禁用的任务"会在次日被续排回来继续响。
        // 现在先回查：任务不存在或已禁用 → 不派发提醒，但仍然重排全局闹钟。
        // ---------------------------------------------------------------
        val stillActive: Boolean = try {
            val dao = com.zhenci.app.data.database.AppDatabase
                .getDatabase(context.applicationContext).taskDao()
            val current = kotlinx.coroutines.runBlocking { dao.getAllTasksSync() }
                .firstOrNull { it.id == taskId }
            val active = current != null && current.isEnabled
            if (!active) {
                Log.w(TAG, "onReceive: 任务 $taskId 已被删除或禁用，跳过本次提醒")
            }
            active
        } catch (e: Exception) {
            // 读库失败时保守处理：宁可多响不可漏响（按仍有效处理）
            Log.e(TAG, "onReceive: 回查任务失败，按仍有效处理: ${e.message}", e)
            true
        }

        dispatchReminder(context, taskId, content, hour, minute,
            rescheduleFirst = true, shouldNotify = stillActive)
    }

    private fun dispatchReminder(
        context: Context,
        taskId: Long,
        content: String,
        hour: Int,
        minute: Int,
        rescheduleFirst: Boolean,
        shouldNotify: Boolean = true
    ) {
        // 先续排，保证即使后续提醒动作崩溃，明天的闹钟也已经排上
        try {
            AlarmScheduler(context).reschedule(triggeredTaskId = taskId)
            Log.d(TAG, "dispatchReminder: 已续排全局下一条闹钟")
        } catch (e: Exception) {
            Log.e(TAG, "dispatchReminder: 续排失败 ${e.message}", e)
        }

        if (!shouldNotify) return

        val inputData = Data.Builder()
            .putLong(AlarmScheduler.EXTRA_TASK_ID, taskId)
            .putString(AlarmScheduler.EXTRA_TASK_CONTENT, content)
            .putInt(AlarmScheduler.EXTRA_TASK_HOUR, hour)
            .putInt(AlarmScheduler.EXTRA_TASK_MINUTE, minute)
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
        Log.d(TAG, "dispatchReminder: WorkManager 提醒任务已提交")
    }
}
