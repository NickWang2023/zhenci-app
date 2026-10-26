package com.zhenci.app.service

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.room.Room
import com.zhenci.app.MainActivity
import com.zhenci.app.data.database.AppDatabase
import com.zhenci.app.data.entity.Task
import java.util.Calendar

/**
 * 日程闹钟调度器 —— 方案 A：单闹钟 + 链式续排。
 *
 * ============================================================================
 * 【为什么必须是"单闹钟"】
 *
 * 历史实现为每条任务各调用一次 setAlarmClock。但 AOSP 对 setAlarmClock 的实现
 * 是「每个应用只保留一个 activity alarm」——排在后面的会直接覆盖前面的。
 * 结果是：启用 ≥2 条日程时，除了最后排的那条，其余闹钟全部丢失，
 * 表现为"某些提醒随缘不响"，且因为覆盖顺序取决于 DAO 返回顺序，
 * 排查时极难复现。这是历史「每日重复链断掉」问题的真正根因。
 *
 * 现在的模型：
 *   - 全库**只存在一个** PendingIntent / 一个 alarm，永远指向"全库最早的下一个触发时刻"。
 *   - 到点触发后，AlarmReceiver 立即重新计算并排下一条（链式续排），
 *     所以"每日重复"由这条单链承载，而不是每条任务各自一条链。
 *   - 任何增删改 / 开机 / 跨零点 / 时区变更，都只需调用 [reschedule] 重排这一条。
 *
 * 【为什么仍然用 setAlarmClock】
 * setExactAndAllowWhileIdle 在 Doze 下有约 9 分钟一次的应用级限流，
 * 多条日程挨得近时会被串行延后并在设备唤醒时集中释放（"集中补播"）。
 * setAlarmClock 属于系统认定的闹钟类调度，享有 Doze 豁免，是官方推荐做法。
 * 单例限制在新模型下从"缺陷"变成了"恰好匹配"。
 * ============================================================================
 */
class AlarmScheduler(private val context: Context) {

    companion object {
        private const val TAG = "AlarmScheduler"

        /** 单闹钟的固定 requestCode。全库唯一，绝不与任务 id 混用。 */
        private const val REQUEST_CODE_ALARM = 71001

        /** setAlarmClock 的 showIntent 固定 requestCode（点击状态栏图标回主界面）。 */
        private const val REQUEST_CODE_SHOW = 71002

        const val EXTRA_TASK_ID = "task_id"
        const val EXTRA_TASK_CONTENT = "task_content"
        const val EXTRA_TASK_HOUR = "task_hour"
        const val EXTRA_TASK_MINUTE = "task_minute"
        const val EXTRA_TRIGGER_AT = "task_trigger_at"
    }

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    /**
     * 【统一入口】重排全局唯一的那一个闹钟。
     *
     * 传 null 表示"从数据库重新全量计算"；传当前触发的任务表示"续排下一条"。
     * 内部实现一律回查数据库，保证源数据唯一、不会用 Intent 里的过期快照复活已删任务。
     */
    fun reschedule(triggeredTaskId: Long? = null) {
        // getAllTasksSync 是 suspend，这里用 runBlocking 在调用方自己的线程上同步取数。
        // 调用方（BroadcastReceiver goAsync 的 IO 线程 / ViewModel 协程）均非主线程，
        // 数据量仅几十条，阻塞可忽略；用 runBlocking 是为了让旧调用点（scheduleTask /
        // scheduleAll / cancelTask 这些非 suspend 签名）无需全部改成协程。
        val enabled = runCatching {
            kotlinx.coroutines.runBlocking {
                AppDatabase.getDatabase(context.applicationContext)
                    .taskDao()
                    .getAllTasksSync()
            }
        }.getOrElse {
            Log.e(TAG, "reschedule: 读取任务失败，放弃重排: ${it.message}")
            return
        }.filter { it.isEnabled && it.templateId == 0L }

        if (enabled.isEmpty()) {
            cancelGlobalAlarm()
            Log.d(TAG, "reschedule: 无启用日程，已清空全局闹钟")
            return
        }

        val now = System.currentTimeMillis()
        // 找最早的下一次触发时刻
        val next = enabled.minByOrNull { nextTriggerMillis(it, now) } ?: return
        val triggerAt = nextTriggerMillis(next, now)

        armAlarm(next, triggerAt)
        Log.d(TAG, "reschedule: 全局闹钟指向任务 ${next.id}「${next.content}」${formatTime(triggerAt)}" +
                (triggeredTaskId?.let { "（由任务 $it 触发后续排）" } ?: ""))
    }

    /**
     * 真正落一次系统闹钟。
     */
    private fun armAlarm(task: Task, triggerAt: Long) {
        val pendingIntent = buildAlarmPendingIntent(task, triggerAt)

        val canExact = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            alarmManager.canScheduleExactAlarms()
        } else {
            true
        }

        if (canExact) {
            try {
                val showIntent = PendingIntent.getActivity(
                    context,
                    REQUEST_CODE_SHOW,
                    Intent(context, MainActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    },
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                alarmManager.setAlarmClock(
                    AlarmManager.AlarmClockInfo(triggerAt, showIntent),
                    pendingIntent
                )
            } catch (e: Exception) {
                // 部分定制 ROM 不支持 setAlarmClock，降级为精确 idle 闹钟
                Log.w(TAG, "armAlarm: setAlarmClock 失败，降级 setExactAndAllowWhileIdle: ${e.message}")
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
            }
        } else {
            Log.w(TAG, "armAlarm: 无精确闹钟权限，降级 setAndAllowWhileIdle（可能漂移）")
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
            ExactAlarmPermission.notifyDegraded(context)
        }
    }

    /** 取消全局唯一闹钟（无启用任务时调用）。 */
    fun cancelGlobalAlarm() {
        val pi = PendingIntent.getBroadcast(
            context,
            REQUEST_CODE_ALARM,
            buildBaseIntent(taskId = -1, content = "", hour = 0, minute = 0, triggerAt = 0L),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pi)
        pi.cancel()
    }

    /**
     * 兼容旧调用点的入口：单个任务被增/改/启用时，直接触发一次全局重排。
     * 语义从"为这条排一个"变成"重算全局那一个"，因此不需要 task 参数也能工作。
     */
    fun scheduleTask(task: Task) {
        if (!task.isEnabled) {
            Log.d(TAG, "scheduleTask: 任务 ${task.id} 未启用，触发全局重排")
        }
        reschedule()
    }

    /**
     * 兼容旧调用点的入口：全量重排。
     * 参数保留但不再逐条排——新模型下只有一条闹钟，传什么都是一次 reschedule。
     */
    fun scheduleAll(tasks: List<Task>) {
        reschedule()
    }

    /**
     * 兼容旧调用点：取消某任务后重排全局闹钟。
     * 不再按 taskId 单独 cancel——单闹钟模型下取消单条必然要整体重算。
     */
    fun cancelTask(taskId: Long) {
        reschedule()
        Log.d(TAG, "cancelTask: 任务 $taskId 已移除，全局闹钟已重算")
    }

    /**
     * 返回某任务的、距 now 最近的下一次触发毫秒时间戳。
     * 今天该时刻还没到 → 今天；否则顺延到明天。
     */
    private fun nextTriggerMillis(task: Task, now: Long): Long {
        val calendar = Calendar.getInstance().apply {
            timeInMillis = now
            set(Calendar.HOUR_OF_DAY, task.hour)
            set(Calendar.MINUTE, task.minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (timeInMillis <= now) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }
        return calendar.timeInMillis
    }

    // ------------------------------------------------------------------
    // PendingIntent
    // ------------------------------------------------------------------

    private fun buildBaseIntent(
        taskId: Long,
        content: String,
        hour: Int,
        minute: Int,
        triggerAt: Long
    ): Intent = Intent(context, AlarmReceiver::class.java).apply {
        putExtra(EXTRA_TASK_ID, taskId)
        putExtra(EXTRA_TASK_CONTENT, content)
        putExtra(EXTRA_TASK_HOUR, hour)
        putExtra(EXTRA_TASK_MINUTE, minute)
        putExtra(EXTRA_TRIGGER_AT, triggerAt)
    }

    /**
     * 用固定 requestCode + 固定的 extras 结构，保证始终是同一个 PendingIntent。
     * extras 会随 FLAG_UPDATE_CURRENT 更新，但"身份"不变，所以 setAlarmClock
     * 覆盖的是自己而不是新增，天然满足单例要求。
     */
    private fun buildAlarmPendingIntent(task: Task, triggerAt: Long): PendingIntent =
        PendingIntent.getBroadcast(
            context,
            REQUEST_CODE_ALARM,
            buildBaseIntent(task.id, task.content, task.hour, task.minute, triggerAt),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

    private fun formatTime(millis: Long): String {
        val c = Calendar.getInstance().apply { timeInMillis = millis }
        return "%02d:%02d".format(c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE))
    }
}
