package com.zhenci.app.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import androidx.core.app.NotificationCompat
import com.zhenci.app.MainActivity
import com.zhenci.app.R
import com.zhenci.app.ReminderActivity
import com.zhenci.app.ZhenciApplication
import kotlinx.coroutines.*
import java.util.*

/**
 * 前台服务用于显示提醒弹窗和语音播报
 * Android 10+ 限制后台启动 Activity，使用前台服务可以绕过此限制
 */
class ReminderForegroundService : Service() {

    companion object {
        private const val TAG = "ReminderForegroundService"
        private const val CHANNEL_ID = "zhenci_reminder_service"
        private const val NOTIFICATION_ID = 10001

        fun start(context: Context, taskId: Long, content: String, hour: Int, minute: Int) {
            val intent = Intent(context, ReminderForegroundService::class.java).apply {
                putExtra("task_id", taskId)
                putExtra("task_content", content)
                putExtra("task_hour", hour)
                putExtra("task_minute", minute)
            }
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }
    }

    private var tts: TextToSpeech? = null
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val taskId = intent?.getLongExtra("task_id", -1) ?: -1
        val content = intent?.getStringExtra("task_content") ?: "针刺提醒"
        val hour = intent?.getIntExtra("task_hour", 0) ?: 0
        val minute = intent?.getIntExtra("task_minute", 0) ?: 0

        Log.d(TAG, "onStartCommand: taskId=$taskId, content=$content, time=$hour:$minute")

        if (taskId == -1L) {
            stopSelf()
            return START_NOT_STICKY
        }

        // 启动前台服务
        startForeground(NOTIFICATION_ID, createServiceNotification(content))

        // 唤醒屏幕
        wakeUpScreen()

        // 启动 ReminderActivity 显示弹窗
        startReminderActivity(taskId, content, hour, minute)

        // 显示系统通知
        showNotification(taskId, content)

        // 播放提示音
        playAlarmSound()

        // 语音播报
        serviceScope.launch {
            speakWithTTS(content)
            
            // 播报完成后延迟停止服务
            delay(5000)
            stopSelf()
        }

        // 重新设置明天的闹钟
        rescheduleForTomorrow(taskId, content, hour, minute)

        return START_NOT_STICKY
    }

    private fun wakeUpScreen() {
        try {
            val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
            val wakeLock = powerManager.newWakeLock(
                PowerManager.FULL_WAKE_LOCK or
                PowerManager.ACQUIRE_CAUSES_WAKEUP or
                PowerManager.ON_AFTER_RELEASE,
                "Zhenci::ReminderForegroundServiceWakeLock"
            )
            wakeLock.acquire(60000)
        } catch (e: Exception) {
            Log.e(TAG, "唤醒屏幕失败: ${e.message}")
        }
    }

    private fun startReminderActivity(taskId: Long, content: String, hour: Int, minute: Int) {
        val intent = Intent(this, ReminderActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS
            putExtra("task_id", taskId)
            putExtra("task_content", content)
            putExtra("task_hour", hour)
            putExtra("task_minute", minute)
        }

        try {
            startActivity(intent)
            Log.d(TAG, "ReminderActivity 启动成功")
        } catch (e: Exception) {
            Log.e(TAG, "启动 ReminderActivity 失败: ${e.message}")
            e.printStackTrace()
        }
    }

    private fun showNotification(taskId: Long, content: String) {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this, taskId.toInt(), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, ZhenciApplication.CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("针刺提醒")
            .setContentText(content)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setVibrate(longArrayOf(0, 1000, 500, 1000))
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(true)
            .build()

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(taskId.toInt(), notification)
    }

    private fun playAlarmSound() {
        try {
            val mediaPlayer = android.media.MediaPlayer().apply {
                setAudioAttributes(
                    android.media.AudioAttributes.Builder()
                        .setUsage(android.media.AudioAttributes.USAGE_ALARM)
                        .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                setDataSource(this@ReminderForegroundService, android.provider.Settings.System.DEFAULT_NOTIFICATION_URI)
                prepare()
                start()
            }
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                mediaPlayer.release()
            }, 3000)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private suspend fun speakWithTTS(message: String) {
        val deferred = CompletableDeferred<Unit>()
        
        tts = TextToSpeech(this) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val result = tts?.setLanguage(Locale.CHINESE)
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    tts?.setLanguage(Locale.SIMPLIFIED_CHINESE)
                }

                tts?.setSpeechRate(0.9f)

                if (Build.VERSION.SDK_INT >= 21) {
                    val audioAttributes = android.media.AudioAttributes.Builder()
                        .setUsage(android.media.AudioAttributes.USAGE_ALARM)
                        .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build()
                    tts?.setAudioAttributes(audioAttributes)
                }

                tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {
                        Log.d(TAG, "TTS 开始播报")
                    }
                    override fun onDone(utteranceId: String?) {
                        Log.d(TAG, "TTS 播报完成")
                        deferred.complete(Unit)
                    }
                    override fun onError(utteranceId: String?) {
                        Log.e(TAG, "TTS 播报错误")
                        deferred.complete(Unit)
                    }
                })

                val params = Bundle()
                params.putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "zhenci")
                tts?.speak(message.take(50), TextToSpeech.QUEUE_FLUSH, params, "zhenci")
            } else {
                deferred.complete(Unit)
            }
        }

        withTimeoutOrNull(15000) {
            deferred.await()
        }
        tts?.shutdown()
    }

    private fun rescheduleForTomorrow(taskId: Long, content: String, hour: Int, minute: Int) {
        try {
            val scheduler = AlarmScheduler(this)
            val task = com.zhenci.app.data.entity.Task(
                id = taskId,
                content = content,
                hour = hour,
                minute = minute,
                isEnabled = true
            )
            scheduler.scheduleDailyRepeating(task)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "针刺提醒服务",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "用于显示提醒弹窗和语音播报"
                setSound(null, null)
                enableVibration(false)
            }
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createServiceNotification(content: String): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("针刺提醒")
            .setContentText("正在提醒: $content")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        tts?.shutdown()
        serviceScope.cancel()
    }
}
