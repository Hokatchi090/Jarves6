package com.jarvis.assistant

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * JarvisGeologyModule
 * يربط: دفتر ميداني GPS + بوصلة حساسات + قاعدة صخور أوفلاين + صور ميدانية
 * 100% أوفلاين (لا Gemini مطلوب لأي وظيفة هنا)
 * المسار: app/src/main/java/com/jarvis/assistant/JarvisGeologyModule.kt
 *
 * في MainActivity أضف:
 *   const val REQ_FIELD_PHOTO = 9201
 *   ثم في onActivityResult:
 *     if (requestCode == REQ_FIELD_PHOTO && resultCode == RESULT_OK) {
 *         jarvisGeologyModule.onPhotoCaptured()
 *     }
 */
class JarvisGeologyModule(
    private val notebook: FieldNotebookManager,
    private val compass: GeoCompassHelper,
    private val speak: (String) -> Unit,
    private val getCurrentLocation: () -> Pair<Double, Double>,
    private val activity: Activity? = null      // ← مطلوب لميزة الصورة فقط
) {
    companion object {
        const val REQ_FIELD_PHOTO = 9201
    }

    // مسار الصورة المعلّقة (بين إطلاق الكاميرا ونتيجتها)
    private var pendingPhotoPath: String? = null

    // ─── موجّه الأوامر ────────────────────────────────────────────────────
    fun execute(intent: JarvisIntent): Boolean {
        return when (intent.type) {
            JarvisIntentType.FIELD_LOG_ADD    -> { addFieldNote(intent.argument);  true }
            JarvisIntentType.FIELD_LOG_LIST   -> { listFieldNotes();               true }
            JarvisIntentType.FIELD_LOG_EXPORT -> { exportFieldNotes();             true }
            JarvisIntentType.FIELD_PHOTO_ADD  -> { captureFieldPhoto();            true }
            JarvisIntentType.COMPASS_READ     -> { readCompass();                  true }
            JarvisIntentType.ROCK_INFO        -> { lookupRock(intent.argument);    true }
            JarvisIntentType.ROCK_SEARCH      -> { searchRocks(intent.argument);   true }
            else -> false
        }
    }

    // ══ دفتر الميدان ════════════════════════════════════════════════════════

    /**
     * argument: "الملاحظة النصية | نوع الصخر"  (الجزء بعد | اختياري)
     */
    private fun addFieldNote(argument: String) {
        if (argument.isBlank()) {
            speak("قولي الملاحظة اللي تحب تسجلها")
            return
        }
        val (lat, lon) = getCurrentLocation()
        if (lat == 0.0 && lon == 0.0) {
            speak("مازال ما عندي موقع حقيقي، روح لشاشة MAP وطلب الموقع أول")
            return
        }
        val parts    = argument.split("|")
        val note     = parts[0].trim()
        val rockType = if (parts.size > 1) parts[1].trim() else ""

        notebook.addEntry(lat, lon, note, rockType)
        speak("تم تسجيل الملاحظة في الدفتر الميداني. عندك دروك ${notebook.count()} ملاحظة")
    }

    private fun listFieldNotes() {
        val entries = notebook.getAll()
        if (entries.isEmpty()) {
            speak("الدفتر الميداني فاضي لسا")
            return
        }
        val recent = entries.take(3).joinToString(". ") { notebook.formatEntry(it) }
        speak("عندك ${entries.size} ملاحظة. آخر ثلاثة: $recent")
    }

    private fun exportFieldNotes() {
        if (notebook.count() == 0) {
            speak("الدفتر الميداني فاضي، مافيش شي نصدّره")
            return
        }
        val path = notebook.exportToFile()
        if (path != null)
            speak("تم حفظ ${notebook.count()} ملاحظة في ملف CSV: $path")
        else
            speak("ما قدرت أحفظ الملف، تأكد من صلاحية التخزين")
    }

    // ══ صورة ميدانية ════════════════════════════════════════════════════════

    private fun captureFieldPhoto() {
        val act = activity ?: run {
            speak("ميزة الصورة تحتاج تمرير Activity للوحدة")
            return
        }
        if (notebook.count() == 0) {
            speak("سجّل ملاحظة ميدانية أولاً ثم ارفق الصورة")
            return
        }
        try {
            val dir  = act.getExternalFilesDir(Environment.DIRECTORY_PICTURES) ?: act.filesDir
            if (!dir.exists()) dir.mkdirs()
            val name = "field_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())}.jpg"
            val file = File(dir, name)
            pendingPhotoPath = file.absolutePath

            val uri: Uri = FileProvider.getUriForFile(
                act,
                "${act.packageName}.provider",
                file
            )
            val camIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply {
                putExtra(MediaStore.EXTRA_OUTPUT, uri)
            }
            act.startActivityForResult(camIntent, REQ_FIELD_PHOTO)
            speak("افتح الكاميرا — التقط الصورة وهي راح تترفق بآخر ملاحظة")
        } catch (e: Exception) {
            speak("ما قدرت أفتح الكاميرا: ${e.message}")
        }
    }

    /** يستدعيها MainActivity من onActivityResult عند requestCode == REQ_FIELD_PHOTO */
    fun onPhotoCaptured() {
        val path = pendingPhotoPath ?: return
        val ok   = notebook.attachPhotoToLastEntry(path)
        if (ok) speak("تم ربط الصورة بآخر ملاحظة ميدانية")
        else    speak("ما قدرت أربط الصورة")
        pendingPhotoPath = null
    }

    // ══ بوصلة الطبقات ════════════════════════════════════════════════════════

    private fun readCompass() {
        if (!compass.isAvailable()) {
            speak("الجهاز مافيش حساسات بوصلة كافية")
            return
        }
        val bearing   = compass.getBearing()
        val direction = compass.getCompassDirectionLabel()
        val tilt      = compass.getTiltDegrees()
        speak(
            "الاتجاه الحالي ${bearing.toInt()} درجة، نحو $direction. " +
            "ميل الجهاز عن الأفقي ${tilt.toInt()} درجة"
        )
    }

    // ══ قاعدة الصخور والمعادن (أوفلاين) ══════════════════════════════════════

    /**
     * "عرّف جرانيت" — يبحث بالاسم ويقرأ المعلومات صوتياً
     */
    private fun lookupRock(query: String) {
        if (query.isBlank()) {
            speak("قولي اسم الصخر أو المعدن اللي تريد تعريفه")
            return
        }
        val record = RockDatabase.findByName(query)
        if (record == null) {
            speak("ما لقيت $query في القاعدة. جرّب اسم آخر أو قول: صخر [وصف خصائصه]")
            return
        }
        speak(RockDatabase.toSpeechSummary(record))
    }

    /**
     * "صخر أسود بريق زجاجي" — يبحث بالخصائص ويعطي أفضل مطابقة
     */
    private fun searchRocks(description: String) {
        if (description.isBlank()) {
            speak("صف لي الصخر: لونه، صلابته، أو خاصية مميزة")
            return
        }
        val results = RockDatabase.searchByProperties(description)
        when {
            results.isEmpty() ->
                speak("ما لقيت صخرة تطابق: $description. حاول بكلمات مختلفة")

            results.size == 1 -> {
                speak("الأقرب لوصفك: ")
                speak(RockDatabase.toSpeechSummary(results[0]))
            }

            else -> {
                val top3 = results.take(3)
                speak("وجدت ${results.size} نتيجة. الأقرب ثلاثة:")
                top3.forEachIndexed { i, r ->
                    speak("${i + 1}. ${r.nameAr}: ${r.keyFeature}")
                }
            }
        }
    }
}
