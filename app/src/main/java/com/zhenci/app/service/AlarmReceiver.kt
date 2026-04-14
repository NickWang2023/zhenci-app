package com.zhenci.app.service

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import com.zhenci.app.MainActivity
import com.zhenci.app.R
import com.zhenci.app.ZhenciApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val taskId = intent.getLongExtra("task_id", -1)
        val content = intent.getStringExtra("task_content") ?: "针刺"
        
        // 唤醒屏幕
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        val wakeLock = powerManager.newWakeLock(
            PowerManager.FULL_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP,
            "Zhenci::AlarmWakeLock"
        )
        wakeLock.acquire(30000)

        // 显示通知
        showNotification(context, "针刺提醒", content)

        // 播放语音（直接播报内容，10字）
        val message = content.take(10)
        val ttsIntent = Intent(context, TTSService::class.java).apply {
            putExtra("message", message)
        }
        context.startService(ttsIntent)

        // 播放提示音
        playAlarmSound(context)

        wakeLock.release()
    }

    private fun showNotification(context: Context, title: String, description: String) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, ZhenciApplication.CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(description)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setVibrate(longArrayOf(0, 1000, 500, 1000))
            .build()

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
    }

    private fun playAlarmSound(context: Context) {
        try {
            val mediaPlayer = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                // 使用系统默认通知音效
                setDataSource(context, android.provider.Settings.System.DEFAULT_NOTIFICATION_URI)
                prepare()
                start()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}