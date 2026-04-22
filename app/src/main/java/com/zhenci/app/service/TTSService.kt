package com.zhenci.app.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import androidx.core.app.NotificationCompat
import com.zhenci.app.R
import java.util.*

class TTSService : Service(), TextToSpeech.OnInitListener {

    companion object {
        private const val TAG = "TTSService"
    }

    private var tts: TextToSpeech? = null
    private var pendingMessage: String? = null
    private var currentMessage: String? = null
    private val CHANNEL_ID = "zhenci_tts_channel"
    private val NOTIFICATION_ID = 1001

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "onCreate: TTS 服务创建")
        createNotificationChannel()
        tts = TextToSpeech(this, this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val message = intent?.getStringExtra("message")
        val taskId = intent?.getLongExtra("task_id", -1) ?: -1
        Log.d(TAG, "onStartCommand: 收到播报请求 message=$message, taskId=$taskId")
        
        if (message != null) {
            currentMessage = message
            
            // 启动前台服务，防止被系统杀死
            val notification = createNotification(message)
            startForeground(NOTIFICATION_ID, notification)
            Log.d(TAG, "onStartCommand: 前台服务已启动")
            
            if (tts?.isSpeaking == true) {
                tts?.stop()
            }
            speak(message)
        } else {
            Log.w(TAG, "onStartCommand: 消息为空，停止服务")
            stopSelf()
        }
        
        return START_STICKY // 使用 STICKY 确保服务被杀死后会重启
    }

    override fun onInit(status: Int) {
        Log.d(TAG, "onInit: TTS 初始化状态=$status")
        if (status == TextToSpeech.SUCCESS) {
            Log.d(TAG, "onInit: TTS 初始化成功")
            
            // 获取可用语言列表
            val availableLanguages = tts?.availableLanguages
            Log.d(TAG, "onInit: 可用语言=$availableLanguages")
            
            // 尝试设置中文
            var result = tts?.setLanguage(Locale.CHINESE)
            Log.d(TAG, "onInit: 设置 Locale.CHINESE 结果=$result")
            
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                // 中文不支持，尝试简体中文
                result = tts?.setLanguage(Locale.SIMPLIFIED_CHINESE)
                Log.d(TAG, "onInit: 设置 Locale.SIMPLIFIED_CHINESE 结果=$result")
                
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    // 尝试中文（中国）
                    result = tts?.setLanguage(Locale("zh", "CN"))
                    Log.d(TAG, "onInit: 设置 zh_CN 结果=$result")
                    
                    if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                        Log.w(TAG, "onInit: 中文不支持，使用英文")
                        tts?.setLanguage(Locale.ENGLISH)
                    }
                }
            }
            
            tts?.setSpeechRate(0.9f)
            tts?.setPitch(1.0f)
            
            // 设置音频流类型为闹钟，确保有声音
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                val audioAttributes = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build()
                tts?.setAudioAttributes(audioAttributes)
                Log.d(TAG, "onInit: 已设置音频属性为 ALARM")
            }

            // 设置播报完成监听器
            tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {
                    Log.d(TAG, "onUtteranceProgressListener: 开始播报 utteranceId=$utteranceId")
                }
                
                override fun onDone(utteranceId: String?) {
                    Log.d(TAG, "onUtteranceProgressListener: 播报完成 utteranceId=$utteranceId")
                    stopSelf()
                }
                
                override fun onError(utteranceId: String?) {
                    Log.e(TAG, "onUtteranceProgressListener: 播报错误 utteranceId=$utteranceId")
                    stopSelf()
                }
            })

            pendingMessage?.let { 
                Log.d(TAG, "onInit: 处理待播报消息: $it")
                speak(it)
                pendingMessage = null
            }
        } else {
            // TTS 初始化失败，停止服务
            Log.e(TAG, "onInit: TTS 初始化失败，状态=$status")
            stopSelf()
        }
    }

    private fun speak(message: String) {
        Log.d(TAG, "speak: 准备播报 '$message'")
        
        if (tts == null) {
            Log.w(TAG, "speak: TTS 未初始化，保存待播报消息")
            pendingMessage = message
            return
        }

        // 确保 TTS 已准备好
        if (tts?.isSpeaking == true) {
            Log.d(TAG, "speak: 停止当前播报")
            tts?.stop()
        }

        // 检查并调整音量
        val audioManager = getSystemService(AUDIO_SERVICE) as AudioManager
        val currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_ALARM)
        val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_ALARM)
        Log.d(TAG, "speak: 闹钟音量 $currentVolume / $maxVolume")
        
        // 如果音量为0，尝试设置为最大音量的一半
        if (currentVolume == 0) {
            Log.w(TAG, "speak: 闹钟音量为0，尝试调整")
            audioManager.setStreamVolume(AudioManager.STREAM_ALARM, maxVolume / 2, 0)
        }
        
        // 检查是否静音
        val ringerMode = audioManager.ringerMode
        Log.d(TAG, "speak: 铃声模式=$ringerMode (0=静音, 1=震动, 2=正常)")
        if (ringerMode == AudioManager.RINGER_MODE_SILENT || ringerMode == AudioManager.RINGER_MODE_VIBRATE) {
            Log.w(TAG, "speak: 设备处于静音/震动模式，尝试切换到正常模式")
            audioManager.ringerMode = AudioManager.RINGER_MODE_NORMAL
        }

        val params = Bundle()
        params.putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "zhenci_tts")
        params.putInt(TextToSpeech.Engine.KEY_PARAM_STREAM, AudioManager.STREAM_ALARM)

        val result = tts?.speak(message, TextToSpeech.QUEUE_FLUSH, params, "zhenci_tts")
        Log.d(TAG, "speak: speak() 返回结果=$result")
        
        // 如果 speak 返回 ERROR，停止服务
        if (result == TextToSpeech.ERROR) {
            Log.e(TAG, "speak: speak() 返回错误")
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