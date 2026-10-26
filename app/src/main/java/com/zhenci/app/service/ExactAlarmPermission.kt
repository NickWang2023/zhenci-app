package com.zhenci.app.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.core.app.NotificationCompat
import com.zhenci.app.BuildConfig
import com.zhenci.app.R

/**
 * 精确闹钟权限的降级告知。
 *
 * 背景：AlarmScheduler 在 canScheduleExactAlarms() == false 时会静默降级为
 * setAndAllowWhileIdle。这在 Doze 下可能延迟一小时，但旧实现只打一条 Log.w，
 * 用户界面毫无提示——用户以为"提醒准时"，实际错过了。
 *
 * 这里把「静默降级」变成「用户可见」：
 *  - 一条独立的、低打扰度、可长期停留的通知，点击直达系统设置页
 *  - 同一时间只保留一条（固定 id），不会刷屏
 *  - 通知仅在 debug 包之外也生效（这是用户该知道的事实），但内部日志受 BuildConfig 控制
 */
object ExactAlarmPermission {

    private const val TAG = "ExactAlarm"
    private const val CHANNEL_ID = "exact_alarm_permission"
    private const val NOTIFICATION_ID = 9527

    /** 供 UI 读取：当前是否已失去精确闹钟能力（Android 12+）。 */
    fun isDegraded(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return false
        val am = context.getSystemService(Context.ALARM_SERVICE) as? android.app.AlarmManager
            ?: return false
        return !am.canScheduleExactAlarms()
    }

    /** 在降级发生时调用。幂等：重复调用只更新同一条通知。 */
    fun notifyDegraded(context: Context) {
        try {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    "提醒权限状态",
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = "当系统关闭了精确闹钟权限时，提醒可能延迟"
                    setShowBadge(false)
                }
                nm.createNotificationChannel(channel)
            }

            // 点击通知直达本应用的「闹钟和提醒」授权页
            val openSettings = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                data = Uri.parse("package:${context.packageName}")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            val pi = PendingIntent.getActivity(
                context,
                NOTIFICATION_ID,
                openSettings,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_alert)
                .setContentTitle("提醒可能不及时")
                .setContentText("系统已关闭本应用的精确闹钟权限，提醒可能延迟。点击开启。")
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true)
                .setContentIntent(pi)
                .build()

            nm.notify(NOTIFICATION_ID, notification)
            if (BuildConfig.DEBUG) Log.w(TAG, "notifyDegraded: 已提示用户精确闹钟权限缺失")
        } catch (e: Exception) {
            // 通知失败不应影响闹钟本身
            Log.e(TAG, "notifyDegraded 失败: ${e.message}")
        }
    }

    /** 权限恢复后清理提示。 */
    fun clearDegradedNotice(context: Context) {
        try {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.cancel(NOTIFICATION_ID)
        } catch (_: Exception) {
        }
    }
}
