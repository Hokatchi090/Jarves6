package com.jarvis.assistant

import android.content.Context
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.ToneGenerator
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.speech.tts.TextToSpeech
import android.view.WindowManager
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import java.util.*

/**
 * PranksterModule
 * مقالب تعتمد على هاردوير هاتفك فقط (صوت، اهتزاز، فلاش، IR)
 * المسار: app/src/main/java/com/jarvis/assistant/PranksterModule.kt
 */
class PranksterModule(
    private val context: Context,
    private val tts: TextToSpeech? = null,
    private val irHelper: IrRemoteHelper? = null
) {
    private val handler  = Handler(Looper.getMainLooper())
    private var mediaPlayer: MediaPlayer? = null
    private var dialog: AlertDialog? = null

    // ─── 1. شاشة الاختراق المرعبة ─────────────────────────────────────────
    fun executeHackPrank() {
        // رفع الصوت لأقصى درجة
        val audio = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        audio.ringerMode = AudioManager.RINGER_MODE_NORMAL
        audio.setStreamVolume(
            AudioManager.STREAM_MUSIC,
            audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC), 0
        )

        // اهتزاز عنيف
        val vib = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vib.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 500, 200, 500), 0))
        } else {
            @Suppress("DEPRECATION")
            vib.vibrate(longArrayOf(0, 500, 200, 500), 0)
        }

        // شاشة تحذير حمراء
        val view = TextView(context).apply {
            text = """
⚠️ SYSTEM COMPROMISED ⚠️

JARVIS SECURITY - BREACH DETECTED

Analyzing... please wait
${System.currentTimeMillis()}
            """.trimIndent()
            setBackgroundColor(android.graphics.Color.BLACK)
            setTextColor(android.graphics.Color.RED)
            textSize = 26f
            gravity = android.view.Gravity.CENTER
            setPadding(32, 32, 32, 32)
        }

        dialog = AlertDialog.Builder(context, android.R.style.Theme_DeviceDefault_NoActionBar_Fullscreen)
            .setView(view)
            .setCancelable(false)
            .show()

        dialog?.window?.apply {
            addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            val p = attributes; p.screenBrightness = 1.0f; attributes = p
        }

        // صوت إنذار
        try {
            mediaPlayer = MediaPlayer.create(context,
                android.provider.Settings.System.DEFAULT_ALARM_ALERT_URI)
            mediaPlayer?.isLooping = true
            mediaPlayer?.start()
        } catch (_: Exception) { }

        tts?.speak(
            "Warning! System breach detected. Activating countermeasures.",
            TextToSpeech.QUEUE_FLUSH, null, null
        )

        // إيقاف ذاتي بعد 10 ثوانٍ
        handler.postDelayed({ stopPrank() }, 10_000)
    }

    // ─── 2. فيض Toast مخيفة ──────────────────────────────────────────────
    fun executeToastFlood() {
        val messages = arrayOf(
            "📡 Scanning frequencies...",
            "💾 Backup complete  [${Random().nextInt(999)}]",
            "🔍 NFC reader active...",
            "📶 Signal analysis running...",
            "🛰️ Connecting to satellite..."
        )
        var i = 0
        val run = object : Runnable {
            override fun run() {
                if (i < messages.size * 3) {
                    Toast.makeText(context,
                        "${messages[i % messages.size]}  #${Random().nextInt(999)}",
                        Toast.LENGTH_SHORT
                    ).show()
                    i++
                    handler.postDelayed(this, 600)
                }
            }
        }
        handler.post(run)
    }

    // ─── 3. مقلب IR (إطفاء التلفزيون) ────────────────────────────────────
    fun executeIRPrank() {
        if (irHelper == null) {
            Toast.makeText(context, "❌ جهازك لا يدعم IR أو irHelper = null", Toast.LENGTH_LONG).show()
            return
        }
        try {
            // استدعاء أي دالة إرسال IR لديك – عدّل الاسم حسب IrRemoteHelper عندك
            val method = irHelper.javaClass.getMethod("sendTVOff")
            method.invoke(irHelper)
            Toast.makeText(context, "📺 إشارة IR أُرسلت!", Toast.LENGTH_LONG).show()
        } catch (_: Exception) {
            Toast.makeText(context,
                "⚠️ وجّه الهاتف نحو التلفاز وجرّب دالة IR المناسبة في IrRemoteHelper.",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    // ─── 4. وميض فلاش ────────────────────────────────────────────────────
    fun executeFlashPrank() {
        Toast.makeText(context, "📸 تفعيل الفلاش...", Toast.LENGTH_SHORT).show()
        val cam = context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager ?: return
        for (i in 1..6) {
            handler.postDelayed({ try { cam.setTorchMode("0", true)  } catch (_: Exception) {} }, i * 350L)
            handler.postDelayed({ try { cam.setTorchMode("0", false) } catch (_: Exception) {} }, i * 350L + 175)
        }
    }

    // ─── 5. صوت إنذار حاد ────────────────────────────────────────────────
    fun executeAlarmTone() {
        try {
            val audio = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            audio.setStreamVolume(
                AudioManager.STREAM_MUSIC,
                audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC), 0
            )
            val tone = ToneGenerator(AudioManager.STREAM_MUSIC, 100)
            tone.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 3000)
            handler.postDelayed({ tone.release() }, 3200)
        } catch (_: Exception) { }
    }

    // ─── إيقاف كل المقالب ────────────────────────────────────────────────
    fun stopPrank() {
        mediaPlayer?.stop(); mediaPlayer?.release(); mediaPlayer = null
        dialog?.dismiss(); dialog = null
        tts?.stop()
        (context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator).cancel()
        handler.removeCallbacksAndMessages(null)
        Toast.makeText(context, "✅ تم إيقاف المقلب.", Toast.LENGTH_SHORT).show()
    }

    fun destroy() { tts?.shutdown(); mediaPlayer?.release(); dialog?.dismiss() }
}
