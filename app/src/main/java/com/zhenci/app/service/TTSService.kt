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
        
        // 立即启动前台服务（华为系统要求 5 秒内）
        val notification = createNotification("准备播报...")
        startForeground(NOTIFICATION_ID, notification)
        Log.d(TAG, "onCreate: 前台服务已启动")
        
        tts = TextToSpeech(this, this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val message = intent?.getStringExtra("message")
        val taskId = intent?.getLongExtra("task_id", -1) ?: -1
        Log.d(TAG, "onStartCommand: 收到播报请求 message=$message, taskId=$taskId")
        
        if (message != null) {
            currentMessage = message
            
            // 更新通知内容
            val notification = createNotification(message)
            val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.notify(NOTIFICATION_ID, notification)
            
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
            
            // 获取默认引擎信息
            val defaultEngine = tts?.defaultEngine
            val engines = tts?.engines
            Log.d(TAG, "onInit: 默认TTS引擎=$defaultEngine")
            Log.d(TAG, "onInit: 所有可用引擎=$engines")
            
            // 获取可用语言列表
            val availableLanguages = tts?.availableLanguages
            Log.d(TAG, "onInit: 可用语言=$availableLanguages")
            
            // 检查当前语言设置
            val currentVoice = tts?.voice
            Log.d(TAG, "onInit: 当前语音=$currentVoice")
            
            // 尝试设置中文 - 华为设备可能需要特殊处理
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
                        // 尝试中文（台湾）
                        result = tts?.setLanguage(Locale("zh", "TW"))
                        Log.d(TAG, "onInit: 设置 zh_TW 结果=$result")
                        
                        if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                            // 尝试中文（香港）
                            result = tts?.setLanguage(Locale("zh", "HK"))
                            Log.d(TAG, "onInit: 设置 zh_HK 结果=$result")
                            
                            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                                Log.w(TAG, "onInit: 中文不支持，使用英文")
                                tts?.setLanguage(Locale.ENGLISH)
                            }
                        }
                    }
                }
            }
            
            // 再次检查当前语音设置
            val finalVoice = tts?.voice
            Log.d(TAG, "onInit: 最终语音设置=$finalVoice")
            
            tts?.setSpeechRate(0.9f)
            tts?.setPitch(1.0f)
            
            // 设置音频流类型为媒体，确保有声音（华为系统对 ALARM 流有限制）
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                val audioAttributes = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build()
                tts?.setAudioAttributes(audioAttributes)
                Log.d(TAG, "onInit: 已设置音频属性为 MEDIA")
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

        // 检查音量（仅记录日志，不修改）
        val audioManager = getSystemService(AUDIO_SERVICE) as AudioManager
        val currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
        val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        Log.d(TAG, "speak: 媒体音量 $currentVolume / $maxVolume")
        
        if (currentVolume == 0) {
            Log.w(TAG, "speak: 警告：媒体音量为0，请在系统设置中调高音量")
        }
        
        // 检查是否静音
        val ringerMode = audioManager.ringerMode
        Log.d(TAG, "speak: 铃声模式=$ringerMode (0=静音, 1=震动, 2=正常)")
        if (ringerMode == AudioManager.RINGER_MODE_SILENT || ringerMode == AudioManager.RINGER_MODE_VIBRATE) {
            Log.w(TAG, "speak: 警告：设备处于静音/震动模式，请切换到正常模式")
        }

        val params = Bundle()
        params.putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "zhenci_tts")
        // 使用音乐流而不是闹钟流，华为系统对闹钟流有限制
        params.putInt(TextToSpeech.Engine.KEY_PARAM_STREAM, AudioManager.STREAM_MUSIC)

        val result = tts?.speak(message, TextToSpeech.QUEUE_FLUSH, params, "zhenci_tts")
        Log.d(TAG, "speak: speak() 返回结果=$result")
        
        // 如果 speak 返回 ERROR，尝试不使用 Bundle 参数再试一次
        if (result == TextToSpeech.ERROR) {
            Log.e(TAG, "speak: speak() 返回错误，尝试不使用 Bundle 参数")
            val retryResult = tts?.speak(message, TextToSpeech.QUEUE_FLUSH, null, "zhenci_tts")
            Log.d(TAG, "speak: 重试结果=$retryResult")
            if (retryResult == TextToSpeech.ERROR) {
                Log.e(TAG, "speak: 重试仍然失败，停止服务")
                stopSelf()
            }
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