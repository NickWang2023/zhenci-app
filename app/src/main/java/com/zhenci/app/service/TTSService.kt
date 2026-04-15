package com.zhenci.app.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import androidx.core.app.NotificationCompat
import com.zhenci.app.R
import java.util.*

class TTSService : Service(), TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = null
    private var pendingMessage: String? = null
    private var currentMessage: String? = null
    private val CHANNEL_ID = "zhenci_tts_channel"
    private val NOTIFICATION_ID = 1001

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        tts = TextToSpeech(this, this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val message = intent?.getStringExtra("message")
        val taskId = intent?.getLongExtra("task_id", -1) ?: -1
        
        if (message != null) {
            currentMessage = message
            
            // 启动前台服务，防止被系统杀死
            val notification = createNotification(message)
            startForeground(NOTIFICATION_ID, notification)
            
            if (tts?.isSpeaking == true) {
                tts?.stop()
            }
            speak(message)
        }
        
        return START_STICKY // 使用 STICKY 确保服务被杀死后会重启
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = tts?.setLanguage(Locale.CHINESE)
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                // 中文不支持，尝试简体中文
                val chinaResult = tts?.setLanguage(Locale.SIMPLIFIED_CHINESE)
                if (chinaResult == TextToSpeech.LANG_MISSING_DATA || chinaResult == TextToSpeech.LANG_NOT_SUPPORTED) {
                    tts?.setLanguage(Locale.ENGLISH)
                }
            }
            
            tts?.setSpeechRate(0.9f)
            tts?.setPitch(1.0f)

            // 设置播报完成监听器
            tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {}
                
                override fun onDone(utteranceId: String?) {
                    // 播报完成后停止服务
                    stopSelf()
                }
                
                override fun onError(utteranceId: String?) {
                    stopSelf()
                }
            })

            pendingMessage?.let { 
                speak(it)
                pendingMessage = null
            }
        } else {
            // TTS 初始化失败，停止服务
            stopSelf()
        }
    }

    private fun speak(message: String) {
        if (tts == null) {
            pendingMessage = message
            return
        }

        // 确保 TTS 已准备好
        if (tts?.isSpeaking == true) {
            tts?.stop()
        }

        val params = Bundle()
        params.putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "zhenci_tts")
        params.putInt(TextToSpeech.Engine.KEY_PARAM_STREAM, android.media.AudioManager.STREAM_ALARM)

        val result = tts?.speak(message, TextToSpeech.QUEUE_FLUSH, params, "zhenci_tts")
        
        // 如果 speak 返回 ERROR，停止服务
        if (result == TextToSpeech.ERROR) {
            stopSelf()
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "针刺语音播报",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "用于语音提醒播报"
                setSound(null, null)
                enableVibration(false)
            }
            
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(content: String): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("针刺提醒")
            .setContentText("正在播报: $content")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()
    }

    override fun onDestroy() {
        tts?.stop()
        tts?.shutdown()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}