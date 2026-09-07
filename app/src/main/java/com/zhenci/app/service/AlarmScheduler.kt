package com.zhenci.app.service

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.zhenci.app.data.entity.Task
import java.util.Calendar

/**
 * 日程闹钟调度器。
 *
 * 产品模型： 首页「今日日程」中的每条任务 = 「每天固定时间重复」的提醒。
 * 因此所有启用任务本质上都是『每日固定时刻』闹钟。
 *
 * 实现策略：
 *  - Android 6.0+/API 19 之后，setRepeating / setInexactRepeating 一律【不精确】，
 *    会因 Doze/省电而严重漂移，造成“延后播报”。因此本调度器一律使用
 *    【一次性精确闹钟(setExactAndAllowWhileIdle)】指向“距当前最近的下一次该时刻
 *    （今天还没到则今天，已过则明天）”。
 *  - “每日重复”由【链式续排】保证：AlarmReceiver 在收到广播后【第一时间】把下一次
 *    闹钟排好（见 AlarmReceiver.onReceive），与 UI/TTS 是否成功解耦，
 *    彻底摆脱此前依赖 ReminderWorker 播完后手动续期、且进程被杀就断链的问题。
 *  - 系统重启 / 跨零点 / 时区变更 / 应用更新覆盖安装后，由 BootReceiver / DATE_CHANGED
 *    等触发一次性全量重排（见 BootReceiver），不依赖用户打开 App。
 */
class AlarmScheduler(private val context: Context) {

    companion object {
        private const val TAG = "AlarmScheduler"
    }

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    /**
     * 为单个任务排下一个闹钟：排到『今天尚未到点则今天，否则明天』的同刻。
     * 这是所有入口（新建/编辑/启用/续排/开机重排）的统一落点。
     */
    fun scheduleTask(task: Task) {
        if (!task.isEnabled) {
            Log.d(TAG, "scheduleTask: 任务 ${task.id} 未启用，跳过")
            return
        }

        val triggerAt = nextTriggerMillis(task.hour, task.minute)

        // 用“内容 + 时刻”作为请求码候选，避免两个不同 id 撞码；仍以 task.id 为主建立可取消的 key。
        // 因 PendingIntent 用 (context, requestCode, intent) 三元组区分，这里 task.id 保证唯一即可。
        val pendingIntent = buildPendingIntent(task)

        // Android 12+ 需精确闹钟权限，否则降级为非精确 setAndAllowWhileIdle（会有些许漂移但可接受）。
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (alarmManager.canScheduleExactAlarms()) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
            } else {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
        } else {
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
        }

        // setExactAndAllowWhileIdle 同一 PendingIntent 只保留一个闹钟，天然覆盖旧排程。
        Log.d(TAG, "scheduleTask: 任务 ${task.id}「${task.content}」已排下一次 ${formatTime(triggerAt)}（每日重复）")
    }

    /**
     * 返回 task.id 对应的、距 now 最近的下一次（含今天尚未到点情况）毫秒时间戳。
     */
    private fun nextTriggerMillis(hour: Int, minute: Int): Long {
        val now = System.currentTimeMillis()
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (timeInMillis <= now) {
                add(Calendar.DAY_OF_YEAR, 1) // 今天该点已过或正卡在整点刚过 → 顺延到明天
            }
        }
        return calendar.timeInMillis
    }

    /**
     * 取消某个任务对应的闹钟（以及旧的 Worker 队列）。
     */
    fun cancelTask(taskId: Long) {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra("task_id", taskId)
        }
        val pendingIntent = buildPendingIntentById(taskId, intent)
        alarmManager.cancel(pendingIntent)
        Log.d(TAG, "cancelTask: 任务 $taskId 闹钟已取消")
    }

    /**
     * 全量重排：把给定的一组【每日重复】任务全部排到下一次时刻。
     * 供 开机 / 跨零点 / 应用打开 时统一修复调度，防丢防延后。
     */
    fun scheduleAll(tasks: List<Task>) {
        tasks.forEach { scheduleTask(it) }
        Log.d(TAG, "scheduleAll: 已重排 ${tasks.size} 个启用日程")
    }

    // ------------------------------------------------------------------
    // PendingIntent 构造
    // ------------------------------------------------------------------

    private fun buildBaseIntent(task: Task): Intent =
        Intent(context, AlarmReceiver::class.java).apply {
            putExtra("task_id", task.id)
            putExtra("task_content", task.content)
            putExtra("task_hour", task.hour)
            putExtra("task_minute", task.minute)
            // 携带请求时刻，接收方可用 withId 做“仅当仍指向此时刻时才续排”的幂等校验
            putExtra("task_trigger_at", nextTriggerMillis(task.hour, task.minute))
        }

    private fun buildPendingIntent(task: Task): PendingIntent =
        PendingIntent.getBroadcast(
            context,
            task.id.toInt(),
            buildBaseIntent(task),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

    private fun buildPendingIntentById(taskId: Long, intent: Intent): PendingIntent =
        PendingIntent.getBroadcast(
            context,
            taskId.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

    private fun formatTime(millis: Long): String {
        val c = Calendar.getInstance().apply { timeInMillis = millis }
        return "%02d:%02d".format(c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE))
    }
}
