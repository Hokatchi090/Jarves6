package com.jarvis.assistant

import android.content.Context
import android.os.Build
import android.provider.Settings
import android.speech.tts.TextToSpeech
import android.widget.TextView
import android.widget.Toast
import java.io.File
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.*

/**
 * LegalGuardianModule
 * توثيق الأدلة قانونياً، تحذيرات قانونية ذكية، توليد بلاغ رسمي.
 * المسار: app/src/main/java/com/jarvis/assistant/LegalGuardianModule.kt
 */
class LegalGuardianModule(
    private val context: Context,
    private val tts: TextToSpeech? = null,
    private val securityGuard: SecurityGuardModule? = null,
    private val tvLog: TextView? = null
) {

    // ─── 1. طابع قانوني رقمي للملفات (Forensic Seal) ────────────────────
    fun generateLegalSeal(evidencePath: String): String {
        val file = File(evidencePath)
        if (!file.exists()) return "⚠️ الملف غير موجود: $evidencePath"

        val hash = MessageDigest.getInstance("SHA-256")
            .digest(file.readBytes())
            .joinToString("") { "%02x".format(it) }

        val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss Z", Locale.US).format(Date())
        val deviceId  = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
        val loc       = securityGuard?.getLastKnownLocation()

        val seal = """
[LEGAL_SEAL_CERTIFICATE]
Device ID  : $deviceId
Timestamp  : $timestamp
Coordinates: ${loc?.latitude ?: "N/A"}, ${loc?.longitude ?: "N/A"}
SHA-256    : $hash
Signed by  : Jarvis-X Guardian
        """.trimIndent()

        // حفظ ملف الطابع بجانب الدليل
        val sealFile = File(file.parent, "${file.nameWithoutExtension}_seal.txt")
        sealFile.writeText(seal)

        logEvent("📜 طابع قانوني ← ${sealFile.name}")
        return seal
    }

    // ─── 2. تحذير قانوني ذكي ─────────────────────────────────────────────
    fun showLegalNotice(scenario: String) {
        val message = getFallbackLegalMessage(scenario)
        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
        tts?.speak(message, TextToSpeech.QUEUE_FLUSH, null, null)
        logEvent("📜 تحذير قانوني صدر.")
    }

    private fun getFallbackLegalMessage(scenario: String): String = when {
        scenario.contains("تحرش") || scenario.contains("عنف") || scenario.contains("اعتداء") ->
            "أنت تخالف قانون العقوبات (الاعتداء والتهديد). تم توثيق الموقع والصوت. " +
            "سيتم رفع الملف للجهات المختصة ما لم تتوقف فوراً."

        scenario.contains("سرقة") ->
            "هذا الفعل يندرج ضمن السرقة المشددة. الجهاز موثق بـ GPS والأدلة محفوظة. " +
            "التراجع الآن هو خيارك الوحيد."

        scenario.contains("مضايقة") ->
            "المضايقة المتكررة تُشكّل جريمة التحرش (المادة 333 ق.ع). " +
            "كل ما يحدث مسجل ومؤرخ."

        else ->
            "تحذير قانوني رسمي: نظام التوثيق القضائي مفعّل. " +
            "كل ما يحدث الآن مسجل ومؤرخ. أنصحك بالتوقف فوراً."
    }

    // ─── 3. بث تصريح الدفاع الشرعي (مسموع) ──────────────────────────────
    fun broadcastSelfDefenseDeclaration() {
        val declaration =
            "تصريح قانوني مسجّل: أنا في حالة دفاع مشروع عن نفسي ضد اعتداء جسدي واضح. " +
            "لقد حاولت تجنب المواجهة لكن الخصم لم يتراجع. " +
            "الأدلة مسجلة وتم إرسال نسخة إلى جهات موثوقة."

        tts?.speak(declaration, TextToSpeech.QUEUE_FLUSH, null, null)
        logEvent("📢 تم بث تصريح الدفاع الشرعي.")
        Toast.makeText(context, "📢 تصريح يُبثّ الآن...", Toast.LENGTH_SHORT).show()
    }

    // ─── 4. توليد بلاغ رسمي (نص جاهز للنسخ) ─────────────────────────────
    fun generateLegalComplaint(
        incidentDescription: String,
        aggressorDescription: String
    ): String {
        val date  = SimpleDateFormat("yyyy/MM/dd", Locale.US).format(Date())
        val time  = SimpleDateFormat("HH:mm",      Locale.US).format(Date())
        val loc   = securityGuard?.getLastKnownLocation()

        val complaint = """
====== محضر إبلاغ رسمي ======
التاريخ    : $date
الوقت      : $time
الموقع     : ${if (loc != null) "${loc.latitude}, ${loc.longitude}" else "غير محدد"}

تفاصيل الواقعة:
$incidentDescription

وصف المعتدي:
$aggressorDescription

الأدلة المرفقة:
  1. تسجيل صوتي (موثق رقمياً بـ SHA-256)
  2. موقع GPS لحظة الواقعة
  3. طابع جهاز رقمي (Device ID)

التوقيع: ___________________
================================
        """.trimIndent()

        logEvent("📝 بلاغ رسمي جاهز.")
        return complaint
    }

    // ─── مساعد تسجيل داخلي ───────────────────────────────────────────────
    private fun logEvent(message: String) {
        val time = SimpleDateFormat("HH:mm:ss", Locale.US).format(Date())
        tvLog?.let {
            val cur = it.text.toString()
            it.text = "[$time] $message\n$cur".take(2000)
        }
    }
}
