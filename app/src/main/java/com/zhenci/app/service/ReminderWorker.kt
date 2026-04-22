package com.zhenci.app.service

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import android.os.PowerManager
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.zhenci.app.MainActivity
import com.zhenci.app.ZhenciApplication
import kotlinx.coroutines.delay
import java.util.*

class ReminderWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    companion object {
        private const val TAG = "ReminderWorker"
    }

    override suspend fun doWork(): Result {
        val taskId = inputData.getLong("task_id", -1)
        val content = inputData.getString("task_content") ?: "针刺提醒"
        Log.d(TAG, "doWork: 开始执行任务 $taskId - $content")
        
        // 唤醒屏幕
        val powerManager = applicationContext.getSystemService(Context.POWER_SERVICE) as PowerManager
        val wakeLock = powerManager.newWakeLock(
            PowerManager.FULL_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP or PowerManager.ON_AFTER_RELEASE,
            "Zhenci::ReminderWakeLock"
        )
        wakeLock.acquire(60000) // 延长到60秒，确保语音播报完成

        try {
            Log.d(TAG, "doWork: 准备显示通知")
            // 显示通知
            showNotification(applicationContext, "针刺提醒", content, taskId)
            Log.d(TAG, "doWork: 通知已显示")
            
            // 播放提示音
            playAlarmSound(applicationContext)
            
            // 语音播报 - 直接使用 TTS
            val message = content.take(50) // 增加到50字
            Log.d(TAG, "doWork: 准备语音播报，消息: $message")
            
            // 使用 TTS 直接播报
            speakWithTTS(applicationContext, message)
            
            // 等待语音播报完成
            delay(8000)
            
            // 重新设置明天的闹钟（用于每日重复任务）
            rescheduleForTomorrow(taskId, content)
            
            return Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            return Result.retry()
        } finally {
            wakeLock.release()
        }
    }

    private fun showNotification(context: Context, title: String, description: String, taskId: Long) {
        val notificationId = taskId.toInt()
        
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context, notificationId, intent,
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
            .setOngoing(false) // 不设置为正在进行，允许用户清除
            .build()

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(notificationId, notification)
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

    private fun speakWithTTS(context: Context, message: String) {
        try {
            Log.d(TAG, "speakWithTTS: 初始化 TTS")
            var tts: TextToSpeech? = null
            tts = TextToSpeech(context) { status ->
                if (status == TextToSpeech.SUCCESS) {
                    Log.d(TAG, "speakWithTTS: TTS 初始化成功")
                    
                    // 设置中文
                    val result = tts?.setLanguage(Locale.CHINESE)
                    if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                        tts?.setLanguage(Locale.SIMPLIFIED_CHINESE)
                    }
                    
                    // 设置音频属性
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        val audioAttributes = AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                            .build()
                        tts?.setAudioAttributes(audioAttributes)
                    }
                    
                    // 设置监听
                    tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                        override fun onStart(utteranceId: String?) {
                            Log.d(TAG, "speakWithTTS: 开始播报")
                        }
                        override fun onDone(utteranceId: String?) {
                            Log.d(TAG, "speakWithTTS: 播报完成")
                            tts?.shutdown()
                        }
                        override fun onError(utteranceId: String?) {
                            Log.e(TAG, "speakWithTTS: 播报错误")
                            tts?.shutdown()
                        }
                    })
                    
                    // 播报
                    val params = Bundle()
                    params.putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "zhenci")
                    val speakResult = tts?.speak(message, TextToSpeech.QUEUE_FLUSH, params, "zhenci")
                    Log.d(TAG, "speakWithTTS: speak() 结果=$speakResult")
                } else {
                    Log.e(TAG, "speakWithTTS: TTS 初始化失败")
                    tts?.shutdown()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "speakWithTTS: 异常: ${e.message}")
        }
    }

    private fun rescheduleForTomorrow(taskId: Long, content: String) {
        try {
            val scheduler = AlarmScheduler(applicationContext)
            // 从 content 解析时间 (格式: "HH:MM 内容")
            val timePart = content.substringBefore(" ")
            val taskContent = content.substringAfter(" ", "针刺提醒")
            val hour = timePart.substringBefore(":").toIntOrNull() ?: 9
            val minute = timePart.substringAfter(":").toIntOrNull() ?: 0

            val task = com.zhenci.app.data.entity.Task(
                id = taskId,
                content = taskContent,
                hour = hour,
                minute = minute,
                isEnabled = true
            )
            scheduler.scheduleDailyRepeating(task)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}