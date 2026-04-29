package com.zhenci.app.service

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.Bundle
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
            Log.d(TAG, "doWork: 准备显示提醒弹窗")
            
            // 启动 ReminderActivity 显示弹窗
            startReminderActivity(applicationContext, taskId, content)
            
            Log.d(TAG, "doWork: 提醒弹窗已启动")
            
            // 显示系统通知（作为备用）
            showNotification(applicationContext, "针刺提醒", content, taskId)
            
            // 播放提示音
            playAlarmSound(applicationContext)
            
            // 语音播报 - 直接使用 TTS
            val message = content.take(50) // 增加到50字
            Log.d(TAG, "doWork: 准备语音播报，消息: $message")
            
            // 使用 TTS 直接播报（挂起等待完成）
            speakWithTTSSuspend(applicationContext, message)
            
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

    /**
     * 启动 ReminderActivity 显示提醒弹窗
     */
    private fun startReminderActivity(context: Context, taskId: Long, content: String) {
        val intent = android.content.Intent(context, com.zhenci.app.ReminderActivity::class.java).apply {
            flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK or
                    android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    android.content.Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS
            putExtra("task_id", taskId)
            putExtra("task_content", content)
            // 解析时间
            val timePart = content.substringBefore(" ")
            val hour = timePart.substringBefore(":").toIntOrNull() ?: 9
            val minute = timePart.substringAfter(":").toIntOrNull() ?: 0
            putExtra("task_hour", hour)
            putExtra("task_minute", minute)
        }
        context.startActivity(intent)
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

        // 设置通知声音为默认闹钟声音
        val defaultSoundUri = android.provider.Settings.System.DEFAULT_ALARM_ALERT_URI
            ?: android.provider.Settings.System.DEFAULT_NOTIFICATION_URI

        val notification = NotificationCompat.Builder(context, ZhenciApplication.CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(description)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setVibrate(longArrayOf(0, 1000, 500, 1000))
            .setSound(defaultSoundUri, android.media.AudioManager.STREAM_ALARM) // 使用闹钟音量
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC) // 锁屏显示
            .setFullScreenIntent(pendingIntent, true) // 强制全屏显示（黑屏时唤醒）
            .setOngoing(false)
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
                    
                    // 设置音频属性 (Android 5.0+)
                    if (Build.VERSION.SDK_INT >= 21) {
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

    private suspend fun speakWithTTSSuspend(context: Context, message: String) {
        try {
            Log.d(TAG, "speakWithTTSSuspend: 初始化 TTS")
            
            // 设置闹钟音量最大
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as android.media.AudioManager
            val originalVolume = audioManager.getStreamVolume(android.media.AudioManager.STREAM_ALARM)
            val maxVolume = audioManager.getStreamMaxVolume(android.media.AudioManager.STREAM_ALARM)
            audioManager.setStreamVolume(android.media.AudioManager.STREAM_ALARM, maxVolume, 0)
            Log.d(TAG, "speakWithTTSSuspend: 设置闹钟音量 $originalVolume -> $maxVolume")
            
            val ttsInitDeferred = kotlinx.coroutines.CompletableDeferred<Boolean>()
            val ttsDoneDeferred = kotlinx.coroutines.CompletableDeferred<Unit>()
            var tts: TextToSpeech? = null
            
            tts = TextToSpeech(context) { status ->
                if (status == TextToSpeech.SUCCESS) {
                    Log.d(TAG, "speakWithTTSSuspend: TTS 初始化成功")
                    
                    // 设置中文
                    val result = tts?.setLanguage(Locale.CHINESE)
                    if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                        tts?.setLanguage(Locale.SIMPLIFIED_CHINESE)
                    }
                    
                    // 设置音量为最大
                    tts?.setSpeechRate(0.9f) // 稍微慢一点，更清晰
                    
                    // 设置音频属性 (Android 5.0+) - 使用 ALARM 类型确保黑屏时也能播放
                    if (Build.VERSION.SDK_INT >= 21) {
                        val audioAttributes = AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_ALARM)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                            .setFlags(AudioAttributes.FLAG_AUDIBILITY_ENFORCED) // 强制可听
                            .build()
                        tts?.setAudioAttributes(audioAttributes)
                    }
                    
                    // 设置监听
                    tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                        override fun onStart(utteranceId: String?) {
                            Log.d(TAG, "speakWithTTSSuspend: 开始播报")
                        }
                        override fun onDone(utteranceId: String?) {
                            Log.d(TAG, "speakWithTTSSuspend: 播报完成")
                            // 恢复原始音量
                            audioManager.setStreamVolume(android.media.AudioManager.STREAM_ALARM, originalVolume, 0)
                            tts?.shutdown()
                            ttsDoneDeferred.complete(Unit)
                        }
                        override fun onError(utteranceId: String?) {
                            Log.e(TAG, "speakWithTTSSuspend: 播报错误")
                            // 恢复原始音量
                            audioManager.setStreamVolume(android.media.AudioManager.STREAM_ALARM, originalVolume, 0)
                            tts?.shutdown()
                            ttsDoneDeferred.complete(Unit)
                        }
                    })
                    
                    ttsInitDeferred.complete(true)
                } else {
                    Log.e(TAG, "speakWithTTSSuspend: TTS 初始化失败")
                    audioManager.setStreamVolume(android.media.AudioManager.STREAM_ALARM, originalVolume, 0)
                    tts?.shutdown()
                    ttsInitDeferred.complete(false)
                    ttsDoneDeferred.complete(Unit)
                }
            }
            
            // 等待 TTS 初始化完成
            val initSuccess = ttsInitDeferred.await()
            if (initSuccess) {
                // 播报 - 使用 QUEUE_FLUSH 确保立即播放
                val params = Bundle()
                params.putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "zhenci")
                params.putInt(TextToSpeech.Engine.KEY_PARAM_STREAM, android.media.AudioManager.STREAM_ALARM)
                params.putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, 1.0f) // 最大音量
                val speakResult = tts?.speak(message, TextToSpeech.QUEUE_FLUSH, params, "zhenci")
                Log.d(TAG, "speakWithTTSSuspend: speak() 结果=$speakResult")
                
                // 等待播报完成（最多等15秒）
                kotlinx.coroutines.withTimeoutOrNull(15000) {
                    ttsDoneDeferred.await()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "speakWithTTSSuspend: 异常: ${e.message}")
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