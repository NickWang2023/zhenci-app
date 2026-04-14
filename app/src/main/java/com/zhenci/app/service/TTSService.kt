package com.zhenci.app.service

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import java.util.*

class TTSService : Service(), TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = null
    private var pendingMessage: String? = null

    override fun onCreate() {
        super.onCreate()
        tts = TextToSpeech(this, this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val message = intent?.getStringExtra("message")
        if (message != null) {
            if (tts?.isSpeaking == true) {
                tts?.stop()
            }
            speak(message)
        }
        return START_NOT_STICKY
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = tts?.setLanguage(Locale.CHINESE)
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                tts?.setLanguage(Locale.ENGLISH)
            }
            
            tts?.setSpeechRate(0.9f)
            tts?.setPitch(1.0f)

            pendingMessage?.let { speak(it) }
        }
    }

    private fun speak(message: String) {
        if (tts == null) {
            pendingMessage = message
            return
        }

        val params = Bundle()
        params.putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "zhenci")

        tts?.speak(message, TextToSpeech.QUEUE_FLUSH, params, "zhenci")
    }

    override fun onDestroy() {
        tts?.stop()
        tts?.shutdown()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}