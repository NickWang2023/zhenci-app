package com.zhenci.app.service

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.zhenci.app.MainActivity
import com.zhenci.app.ZhenciApplication
import kotlinx.coroutines.delay

class ReminderWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val taskId = inputData.getLong("task_id", -1)
        val content = inputData.getString("task_content") ?: "针刺提醒"
        
        // 唤醒屏幕
        val powerManager = applicationContext.getSystemService(Context.POWER_SERVICE) as PowerManager
        val wakeLock = powerManager.newWakeLock(
            PowerManager.FULL_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP or PowerManager.ON_AFTER_RELEASE,
            "Zhenci::ReminderWakeLock"
        )
        wakeLock.acquire(60000) // 延长到60秒，确保语音播报完成

        try {
            // 显示通知
            showNotification(applicationContext, "针刺提醒", content)
            
            // 播放提示音
            playAlarmSound(applicationContext)
            
            // 语音播报 - 使用前台服务确保不被杀死
            val message = content.take(50) // 增加到50字
            val ttsIntent = Intent(applicationContext, TTSService::class.java).apply {
                putExtra("message", message)
                putExtra("task_id", taskId)
            }
            
            // Android O+ 需要使用 startForegroundService
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                applicationContext.startForegroundService(ttsIntent)
            } else {
                applicationContext.startService(ttsIntent)
            }
            
            // 等待语音播报完成
            delay(10000)
            
            return Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            return Result.retry()
        } finally {
            wakeLock.release()
        }
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
            .setOngoing(true) // 设置为正在进行，防止被系统清除
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
                setDataSource(context, android.provider.Settings.System.DEFAULT_NOTIFICATION_URI)
                prepare()
                start()
            }
            // 播放3秒后停止
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                mediaPlayer.release()
            }, 3000)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}