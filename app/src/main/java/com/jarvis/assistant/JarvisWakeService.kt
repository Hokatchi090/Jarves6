package com.jarvis.assistant

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.core.app.NotificationCompat
import java.util.Locale

/**
 * خدمة استماع خلفية دائمة (مبدأ Siri): تستمع لكلمة التفعيل "جارفس" حتى
 * والتطبيق مغلق، وعند اكتشافها تفتح MainActivity وتبدأ الاستماع الحقيقي فورًا.
 *
 * ملاحظة مهمة: هذا تعرّف صوتي مبني على إعادة تشغيل SpeechRecognizer بشكل متكرر،
 * وليس محرك "Hotword" منخفض الاستهلاك مثل الموجود فعليًا في أجهزة آيفون/بيكسل
 * (تلك تعتمد على شريحة DSP مخصصة). هذا الحل يستهلك بطارية أكثر لكنه يشتغل بدون
 * أي مكتبة أو حساب خارجي.
 */
class JarvisWakeService : Service() {

    private var speechRecognizer: SpeechRecognizer? = null
    private val handler = Handler(Looper.getMainLooper())
    private var isRunning = false

    companion object {
        const val CHANNEL_ID = "jarvis_wake_service"
        const val NOTIF_ID = 501
        const val ACTION_STOP = "com.jarvis.assistant.STOP_WAKE_SERVICE"
    }

    override fun onCreate() {
        super.onCreate()
        startForeground(NOTIF_ID, buildNotification())
        startWakeListening()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopSelf()
            return START_NOT_STICKY
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun buildNotification(): Notification {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(NotificationManager::class.java)
            val channel = NotificationChannel(
                CHANNEL_ID, "استماع جارفس في الخلفية", NotificationManager.IMPORTANCE_LOW
            )
            manager.createNotificationChannel(channel)
        }

        val stopIntent = Intent(this, JarvisWakeService::class.java).apply { action = ACTION_STOP }
        val stopPending = PendingIntent.getService(
            this, 0, stopIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val openIntent = Intent(this, MainActivity::class.java)
        val openPending = PendingIntent.getActivity(
            this, 0, openIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("جارفس يستمع في الخلفية")
            .setContentText("قل \"جارفس\" في أي وقت")
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setContentIntent(openPending)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "إيقاف", stopPending)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun startWakeListening() {
        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            stopSelf()
            return
        }
        isRunning = true
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
        speechRecognizer?.setRecognitionListener(wakeListener)
        listenOnce()
    }

    private fun listenOnce() {
        if (!isRunning) return
        val recognizerIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ar-DZ")
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 1500L)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 1200L)
        }
        try {
            speechRecognizer?.startListening(recognizerIntent)
        } catch (e: Exception) {
            restartAfterDelay()
        }
    }

    private fun restartAfterDelay(delayMs: Long = 800L) {
        handler.postDelayed({ if (isRunning) listenOnce() }, delayMs)
    }

    private val wakeListener = object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) {}
        override fun onBeginningOfSpeech() {}
        override fun onRmsChanged(rmsdB: Float) {}
        override fun onBufferReceived(buffer: ByteArray?) {}
        override fun onEndOfSpeech() {}

        override fun onError(error: Int) {
            restartAfterDelay()
        }

        override fun onResults(results: Bundle?) {
            val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            val text = matches?.firstOrNull()?.lowercase(Locale.ROOT) ?: ""
            if (text.contains("جارفس") || text.contains("jarvis") || text.contains("جارفيس")) {
                launchMainActivity()
            } else {
                restartAfterDelay(300L)
            }
        }

        override fun onPartialResults(partialResults: Bundle?) {}
        override fun onEvent(eventType: Int, params: Bundle?) {}
    }

    private fun launchMainActivity() {
        val launchIntent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
            putExtra("FROM_WAKE_SERVICE", true)
        }
        startActivity(launchIntent)
        // بعد ما نسلّم للتطبيق، نرتاح لحظة قبل ما نعاود نستمع لكلمة التفعيل من جديد
        restartAfterDelay(2500L)
    }

    override fun onDestroy() {
        isRunning = false
        handler.removeCallbacksAndMessages(null)
        speechRecognizer?.destroy()
        speechRecognizer = null
        super.onDestroy()
    }
}
