package com.jarvis.assistant

import android.Manifest
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.camera2.CameraManager
import android.location.Location
import android.location.LocationManager
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.media.ToneGenerator
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.provider.Settings
import android.speech.tts.TextToSpeech
import android.telephony.SmsManager
import android.util.Log
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

/**
 * SecurityGuardModule
 * أدوات الأمان الشخصي: تسجيل الأدلة، إنذار ردعي، قفل الجهاز، SOS، طوارئ.
 * المسار: app/src/main/java/com/jarvis/assistant/SecurityGuardModule.kt
 *
 * ⚠️ عدّل trustedContacts قبل الاستخدام!
 */
class SecurityGuardModule(
    private val context: Context,
    private val tts: TextToSpeech?,
    private val tvLog: TextView? = null          // اختياري – لعرض السجل على الشاشة
) {
    private val handler       = Handler(Looper.getMainLooper())
    private var mediaRecorder: MediaRecorder? = null
    private var mediaPlayer:   MediaPlayer?   = null
    private var isRecording   = false

    // ⚠️ ضع أرقام أشخاص تثق بهم (بالصيغة الدولية)
    private val trustedContacts = listOf("+213XXXXXXXXX")

    // ─── 1. تسجيل خفي (جمع أدلة) ─────────────────────────────────────────
    fun startStealthRecording() {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED) {
            toast("⚠️ صلاحية الميكروفون مطلوبة!")
            return
        }
        if (isRecording) { toast("🔴 التسجيل شغّال بالفعل."); return }

        try {
            val dir  = context.getExternalFilesDir(null) ?: context.filesDir
            val file = File(dir, "evidence_${System.currentTimeMillis()}.3gp")

            mediaRecorder = MediaRecorder().apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
                setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
                setOutputFile(file.absolutePath)
                prepare()
                start()
            }
            isRecording = true
            vibrate(100)
            logEvent("🔴 بدأ التسجيل → ${file.name}")
            toast("🔴 التسجيل نشط – الملف: ${file.name}")
        } catch (e: Exception) {
            Log.e("SecurityGuard", "Recording failed: ${e.message}")
            toast("❌ فشل التسجيل: ${e.message}")
        }
    }

    fun stopStealthRecording() {
        if (!isRecording) { toast("⏹️ لا يوجد تسجيل نشط."); return }
        try {
            mediaRecorder?.stop(); mediaRecorder?.release(); mediaRecorder = null
            isRecording = false
            logEvent("⏹️ توقف التسجيل وحُفظ.")
            toast("⏹️ تم حفظ الملف.")
        } catch (e: Exception) {
            Log.e("SecurityGuard", "Stop failed: ${e.message}")
        }
    }

    // ─── 2. إنذار ردعي (صوت + فلاش) ──────────────────────────────────────
    fun activateDeterrentSiren() {
        val audio = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        audio.ringerMode = AudioManager.RINGER_MODE_NORMAL
        audio.setStreamVolume(
            AudioManager.STREAM_MUSIC,
            audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC), 0
        )

        // فلاش 3 ثواني
        val cam = context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager
        try {
            cam?.setTorchMode("0", true)
            handler.postDelayed({ try { cam?.setTorchMode("0", false) } catch (_: Exception) {} }, 3000)
        } catch (_: Exception) {}

        // صوت حاد
        try {
            val tone = ToneGenerator(AudioManager.STREAM_MUSIC, 100)
            tone.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 4000)
            handler.postDelayed({ tone.release() }, 4200)
        } catch (_: Exception) {}

        // TTS تحذير
        tts?.speak(
            "تحذير أمني. تم إرسال موقعك إلى جهات الاتصال الموثوقة.",
            TextToSpeech.QUEUE_FLUSH, null, null
        )

        logEvent("🚨 الإنذار الردعي نشط!")
        toast("🚨 الإنذار نشط!")

        handler.postDelayed({
            mediaPlayer?.stop(); mediaPlayer?.release(); mediaPlayer = null
        }, 8000)
    }

    // ─── 3. انفجار حسي (إلهاء + تشتيت – بدون تعليمات عنف) ───────────────
    fun activateSensoryFlashbang() {
        // فلاش مكثف
        val cam = context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager
        try {
            cam?.setTorchMode("0", true)
            handler.postDelayed({ try { cam?.setTorchMode("0", false) } catch (_: Exception) {} }, 1500)
        } catch (_: Exception) {}

        // صوت حاد مزعج
        try {
            val tone = ToneGenerator(AudioManager.STREAM_MUSIC, 100)
            tone.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 2000)
            handler.postDelayed({ tone.release() }, 2200)
        } catch (_: Exception) {}

        // اهتزاز قوي
        vibrate(longArrayOf(0, 300, 100, 300))

        tts?.speak(
            "Alert activated. Move to a safe area immediately.",
            TextToSpeech.QUEUE_FLUSH, null, null
        )

        logEvent("💥 انفجار حسي – تفعيل الإنذار.")
        toast("💥 إنذار حسي نشط!")
    }

    // ─── 4. قفل الجهاز فوراً ──────────────────────────────────────────────
    fun activatePanicLockdown() {
        val dpm   = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        val admin = ComponentName(context, MyDeviceAdminReceiver::class.java)
        if (dpm.isAdminActive(admin)) {
            dpm.lockNow()
            logEvent("🔒 قُفل الجهاز فوراً.")
        } else {
            // طلب تفعيل صلاحية المشرف (مرة واحدة فقط)
            val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
                putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, admin)
                putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION, "مطلوب لقفل الجهاز في حالات الطوارئ")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        }
    }

    // ─── 5. SOS صامت (إرسال الموقع) ───────────────────────────────────────
    fun sendSilentSOS() {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.SEND_SMS)
            != PackageManager.PERMISSION_GRANTED) {
            toast("⚠️ صلاحيات الموقع والـ SMS مطلوبة!")
            return
        }
        val loc  = getLastKnownLocation()
        val link = if (loc != null)
            "https://maps.google.com/?q=${loc.latitude},${loc.longitude}"
        else
            "https://maps.google.com/ (الموقع غير متاح)"

        val msg = "🚨 SOS – أحتاج مساعدة. موقعي: $link  – Jarvis-X"
        val sms = SmsManager.getDefault()

        trustedContacts.forEach { number ->
            try {
                sms.sendTextMessage(number, null, msg, null, null)
                logEvent("📤 SOS أُرسل إلى $number")
            } catch (e: Exception) {
                logEvent("❌ فشل الإرسال إلى $number: ${e.message}")
            }
        }
        toast("📤 تم إرسال SOS إلى ${trustedContacts.size} جهة.")
    }

    // ─── 6. تنشيط وهم اتصال بالأمن ───────────────────────────────────────
    fun simulateFakeCall() {
        tts?.speak(
            "Connecting to security operations center. Please hold.",
            TextToSpeech.QUEUE_FLUSH, null, null
        )
        try {
            mediaPlayer = MediaPlayer.create(context,
                Settings.System.DEFAULT_RINGTONE_URI)
            mediaPlayer?.start()
            handler.postDelayed({
                mediaPlayer?.stop(); mediaPlayer?.release(); mediaPlayer = null
                tts?.speak(
                    "Officer speaking. What is your emergency?",
                    TextToSpeech.QUEUE_FLUSH, null, null
                )
            }, 3000)
        } catch (_: Exception) {}
        logEvent("📞 وهم اتصال بالأمن نشط.")
    }

    // ─── 7. تضحية ذكية (إخفاء البيانات + صوت + هروب) ─────────────────────
    fun smartSacrifice() {
        // أولاً: حفظ SOS قبل أي شيء
        sendSilentSOS()
        // ثم: إنذار لجذب الانتباه
        activateDeterrentSiren()
        // تعليمات صوتية للهروب
        handler.postDelayed({
            tts?.speak(
                "Data backed up. Move to a crowded area immediately.",
                TextToSpeech.QUEUE_FLUSH, null, null
            )
        }, 1500)
        logEvent("📱 تضحية ذكية – SOS أُرسل + إنذار نشط.")
    }

    // ─── 8. إخفاء التطبيق (الانتقال للشاشة الرئيسية) ─────────────────────
    fun activateCamouflage() {
        val i = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_HOME)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(i)
        logEvent("🎭 تبديل الواجهة – الانتقال للشاشة الرئيسية.")
    }

    // ─── 9. إيقاف كل شيء ─────────────────────────────────────────────────
    fun deactivateAll() {
        if (isRecording) stopStealthRecording()
        mediaPlayer?.stop(); mediaPlayer?.release(); mediaPlayer = null
        (context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator).cancel()
        tts?.stop()
        handler.removeCallbacksAndMessages(null)
        logEvent("✅ تم إلغاء جميع إجراءات الطوارئ.")
        toast("✅ كل شيء موقوف.")
    }

    // ─── مساعدات داخلية ──────────────────────────────────────────────────
    fun getLastKnownLocation(): Location? {
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED) return null
        val lm = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return lm.getLastKnownLocation(LocationManager.GPS_PROVIDER)
            ?: lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
    }

    private fun vibrate(ms: Long) {
        val vib = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            vib.vibrate(VibrationEffect.createOneShot(ms, VibrationEffect.DEFAULT_AMPLITUDE))
        else
            @Suppress("DEPRECATION") vib.vibrate(ms)
    }

    private fun vibrate(pattern: LongArray) {
        val vib = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            vib.vibrate(VibrationEffect.createWaveform(pattern, -1))
        else
            @Suppress("DEPRECATION") vib.vibrate(pattern, -1)
    }

    private fun logEvent(message: String) {
        val time = SimpleDateFormat("HH:mm:ss", Locale.US).format(Date())
        tvLog?.let {
            val cur = it.text.toString()
            it.text = "[$time] $message\n$cur".take(2000)
        }
        Log.d("SecurityGuard", "[$time] $message")
    }

    private fun toast(msg: String) =
        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
}
