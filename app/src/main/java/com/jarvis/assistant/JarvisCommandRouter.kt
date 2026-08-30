package com.jarvis.assistant

import java.util.Locale

class JarvisCommandRouter(
    private val legacyHandler: (String) -> Unit,
    private val appLauncher: ((String) -> Boolean)? = null,
    private val systemHandler: ((JarvisIntent) -> Boolean)? = null,
    private val appsHandler: ((JarvisIntent) -> Boolean)? = null,
    private val clockHandler: ((JarvisIntent) -> Boolean)? = null,
    private val mapHandler: ((JarvisIntent) -> Boolean)? = null,
    private val contactsHandler: ((JarvisIntent) -> Boolean)? = null,
    private val geologyHandler: ((JarvisIntent) -> Boolean)? = null,
    private val safetyHandler: ((JarvisIntent) -> Boolean)? = null,
    private val startListeningHandler: (() -> Unit)? = null,
    private val moduleManager: JarvisModuleManager? = null,
    private val onModuleToggled: ((JarvisModule, Boolean) -> Unit)? = null
) {

    fun route(rawText: String) {
        val text = normalize(rawText)
        if (text.isBlank()) return
        val intent = parse(text)

        when (intent.type) {

            JarvisIntentType.OPEN_APP -> {
                val handled = appLauncher?.invoke(intent.argument) ?: false
                if (!handled) legacyHandler(text)
            }

            JarvisIntentType.SYSTEM_FLASH,
            JarvisIntentType.SYSTEM_BATTERY,
            JarvisIntentType.SYSTEM_TIME,
            JarvisIntentType.SYSTEM_CAMERA,
            JarvisIntentType.SYSTEM_SETTINGS -> {
                val handled = systemHandler?.invoke(intent) ?: false
                if (!handled) legacyHandler(text)
            }

            JarvisIntentType.APPS_SHOW,
            JarvisIntentType.APPS_HIDE -> {
                val handled = appsHandler?.invoke(intent) ?: false
                if (!handled) legacyHandler(text)
            }

            JarvisIntentType.CLOCK_SHOW,
            JarvisIntentType.CLOCK_HIDE -> {
                val handled = clockHandler?.invoke(intent) ?: false
                if (!handled) legacyHandler(text)
            }

            JarvisIntentType.MODULE_ENABLE,
            JarvisIntentType.MODULE_DISABLE -> {
                val handled = handleModuleToggle(intent)
                if (!handled) legacyHandler(text)
            }

            JarvisIntentType.MAP_NAVIGATE,
            JarvisIntentType.MAP_DISTANCE -> {
                val handled = mapHandler?.invoke(intent) ?: false
                if (!handled) legacyHandler(text)
            }

            JarvisIntentType.CONTACT_CALL,
            JarvisIntentType.SMS_READ,
            JarvisIntentType.SMS_EXPLAIN,
            JarvisIntentType.SMS_SEND -> {
                val handled = contactsHandler?.invoke(intent) ?: false
                if (!handled) legacyHandler(text)
            }

            // ── جيولوجيا (دفتر + بوصلة + قاعدة صخور + صورة) ───────────
            JarvisIntentType.FIELD_LOG_ADD,
            JarvisIntentType.FIELD_LOG_LIST,
            JarvisIntentType.FIELD_LOG_EXPORT,
            JarvisIntentType.FIELD_PHOTO_ADD,
            JarvisIntentType.COMPASS_READ,
            JarvisIntentType.ROCK_INFO,
            JarvisIntentType.ROCK_SEARCH -> {
                val handled = geologyHandler?.invoke(intent) ?: false
                if (!handled) legacyHandler(text)
            }

            JarvisIntentType.SAFETY_FAKE_CALL,
            JarvisIntentType.SAFETY_SEND_LOCATION,
            JarvisIntentType.SAFETY_RECORD_START,
            JarvisIntentType.SAFETY_RECORD_STOP,
            JarvisIntentType.SAFETY_DEFENSE_INFO,
            JarvisIntentType.SAFETY_SET_CONTACT -> {
                val handled = safetyHandler?.invoke(intent) ?: false
                if (!handled) legacyHandler(text)
            }

            JarvisIntentType.START_LISTENING ->
                startListeningHandler?.invoke() ?: legacyHandler(text)

            JarvisIntentType.UNKNOWN -> legacyHandler(text)
        }
    }

    // ─── تحليل النص ──────────────────────────────────────────────────────
    private fun parse(text: String): JarvisIntent {

        val appPrefixes = listOf("افتح ", "شغل ", "open ", "launch ")
        val appPrefix = appPrefixes.firstOrNull { text.startsWith(it) }
        if (appPrefix != null) {
            val appName = text.removePrefix(appPrefix).trim()
            if (appName.isNotBlank())
                return JarvisIntent(JarvisIntentType.OPEN_APP, argument = appName, originalText = text)
        }

        return when {

            // ── نظام ──────────────────────────────────────────────────────
            text.contains("شغل الفلاش") || text.contains("افتح الفلاش") ||
                    text.contains("turn on flash") ->
                JarvisIntent(JarvisIntentType.SYSTEM_FLASH, argument = "on", originalText = text)

            text.contains("طفي الفلاش") || text.contains("اطفي الفلاش") ||
                    text.contains("turn off flash") ->
                JarvisIntent(JarvisIntentType.SYSTEM_FLASH, argument = "off", originalText = text)

            text.contains("البطارية") || text.contains("battery") ->
                JarvisIntent(JarvisIntentType.SYSTEM_BATTERY, originalText = text)

            text.contains("كم الساعة") || text.contains("what time") ->
                JarvisIntent(JarvisIntentType.SYSTEM_TIME, originalText = text)

            text.contains("افتح الكاميرا") || text.contains("open camera") ->
                JarvisIntent(JarvisIntentType.SYSTEM_CAMERA, originalText = text)

            text.contains("افتح الاعدادات") || text.contains("open settings") ->
                JarvisIntent(JarvisIntentType.SYSTEM_SETTINGS, originalText = text)

            // ── تطبيقات ───────────────────────────────────────────────────
            text.contains("اعرض التطبيقات") || text.contains("اظهر التطبيقات") ||
                    text.contains("show apps") ->
                JarvisIntent(JarvisIntentType.APPS_SHOW, originalText = text)

            text.contains("اخفي التطبيقات") || text.contains("hide apps") ->
                JarvisIntent(JarvisIntentType.APPS_HIDE, originalText = text)

            text.contains("اظهر الساعة") || text.contains("show clock") ->
                JarvisIntent(JarvisIntentType.CLOCK_SHOW, originalText = text)

            text.contains("اخفي الساعة") || text.contains("hide clock") ->
                JarvisIntent(JarvisIntentType.CLOCK_HIDE, originalText = text)

            // ── وحدات ─────────────────────────────────────────────────────
            (text.contains("فعّل") || text.contains("شغّل")) && text.contains("وحدة") ->
                JarvisIntent(JarvisIntentType.MODULE_ENABLE,
                    argument = extractModuleName(text), originalText = text)

            (text.contains("عطّل") || text.contains("أوقف")) && text.contains("وحدة") ->
                JarvisIntent(JarvisIntentType.MODULE_DISABLE,
                    argument = extractModuleName(text), originalText = text)

            // ── خريطة ─────────────────────────────────────────────────────
            text.contains("المسافة بين") -> {
                val after = text.substringAfter("المسافة بين").trim()
                val cities = after.split("و", "إلى", "لـ")
                    .map { it.trim() }.filter { it.isNotBlank() }
                if (cities.size >= 2)
                    JarvisIntent(JarvisIntentType.MAP_DISTANCE,
                        argument = "${cities[0]}|${cities[1]}", originalText = text)
                else
                    JarvisIntent(JarvisIntentType.UNKNOWN, originalText = text)
            }

            text.startsWith("روحني لـ") || text.startsWith("روحني الى") ||
                    text.startsWith("خذني لـ") || text.startsWith("navigate to") -> {
                val navPrefixes = listOf("روحني لـ", "روحني الى", "خذني لـ", "navigate to")
                val matched = navPrefixes.first { text.startsWith(it) }
                JarvisIntent(JarvisIntentType.MAP_NAVIGATE,
                    argument = text.removePrefix(matched).trim(), originalText = text)
            }

            // ── جهات اتصال / رسائل ────────────────────────────────────────
            text.startsWith("اتصل ب") ->
                JarvisIntent(JarvisIntentType.CONTACT_CALL,
                    argument = text.removePrefix("اتصل ب").trim(), originalText = text)

            text.contains("اقرا رسائلي") || text.contains("رسائلي الجديدة") ->
                JarvisIntent(JarvisIntentType.SMS_READ, originalText = text)

            text.contains("اشرح") && text.contains("رسالة") ->
                JarvisIntent(JarvisIntentType.SMS_EXPLAIN, originalText = text)

            text.contains("ابعث رسالة") || text.contains("دير رسالة") -> {
                val marker = if (text.contains("ابعث رسالة")) "ابعث رسالة" else "دير رسالة"
                val rest = text.substringAfter(marker).trim()
                val parts = rest.split("تقول", limit = 2)
                if (parts.size == 2)
                    JarvisIntent(JarvisIntentType.SMS_SEND,
                        argument = "${parts[0].removePrefix("ل").trim()}|${parts[1].trim()}",
                        originalText = text)
                else
                    JarvisIntent(JarvisIntentType.SMS_SEND, argument = "", originalText = text)
            }

            // ══ دفتر ميداني ════════════════════════════════════════════════

            text.contains("سجّل ملاحظة ميدانية") || text.contains("أضف للدفتر الميداني") -> {
                val marker = if (text.contains("سجّل ملاحظة ميدانية"))
                    "سجّل ملاحظة ميدانية" else "أضف للدفتر الميداني"
                JarvisIntent(JarvisIntentType.FIELD_LOG_ADD,
                    argument = text.substringAfter(marker).trim(), originalText = text)
            }

            text.contains("الدفتر الميداني") &&
                    (text.contains("وريني") || text.contains("اقرا")) ->
                JarvisIntent(JarvisIntentType.FIELD_LOG_LIST, originalText = text)

            text.contains("صدّر") && text.contains("الدفتر الميداني") ->
                JarvisIntent(JarvisIntentType.FIELD_LOG_EXPORT, originalText = text)

            // صورة ميدانية: "التقط صورة ميدانية" / "ارفق صورة للملاحظة"
            text.contains("التقط صورة ميدانية") || text.contains("ارفق صورة") ||
                    text.contains("صورة للملاحظة") ->
                JarvisIntent(JarvisIntentType.FIELD_PHOTO_ADD, originalText = text)

            // ══ بوصلة ══════════════════════════════════════════════════════
            text.contains("البوصلة") || text.contains("اتجاه الطبقة") ||
                    text.contains("strike") ->
                JarvisIntent(JarvisIntentType.COMPASS_READ, originalText = text)

            // ══ قاعدة الصخور والمعادن (أوفلاين) ═══════════════════════════

            // "عرّف جرانيت" / "ما هو البازلت" / "اخبرني عن الكالسيت"
            text.startsWith("عرّف ") || text.startsWith("عرف ") ||
                    text.contains("ما هو ال") || text.contains("ما هي ال") ||
                    text.contains("اخبرني عن") || text.contains("معلومات عن") -> {
                val query = extractRockName(text)
                JarvisIntent(JarvisIntentType.ROCK_INFO,
                    argument = query, originalText = text)
            }

            // "صخر أحمر صلب" / "ابحث عن صخر أسود بريق زجاجي"
            text.contains("صخر ") || text.contains("معدن ") ||
                    (text.contains("ابحث عن") && (text.contains("صخر") || text.contains("معدن"))) -> {
                val query = when {
                    text.contains("ابحث عن") -> text.substringAfter("ابحث عن").trim()
                    text.contains("صخر ")    -> text.substringAfter("صخر ").trim()
                    else                     -> text.substringAfter("معدن ").trim()
                }
                JarvisIntent(JarvisIntentType.ROCK_SEARCH,
                    argument = query, originalText = text)
            }

            // ══ أمان ═══════════════════════════════════════════════════════
            (text.contains("جارفس") && text.contains("ابدأ") && text.contains("استماع")) ->
                JarvisIntent(JarvisIntentType.START_LISTENING, originalText = text)

            text.contains("اتصال مزيف") ->
                JarvisIntent(JarvisIntentType.SAFETY_FAKE_CALL, originalText = text)

            text.contains("ابعث موقعي") ||
                    (text.contains("موقعي") && text.contains("طوارئ")) ->
                JarvisIntent(JarvisIntentType.SAFETY_SEND_LOCATION, originalText = text)

            text.contains("احفظ رقم الطوارئ") ->
                JarvisIntent(JarvisIntentType.SAFETY_SET_CONTACT,
                    argument = text.substringAfter("الطوارئ").trim(), originalText = text)

            text.contains("بدا التسجيل") || text.contains("سجل كدليل") ->
                JarvisIntent(JarvisIntentType.SAFETY_RECORD_START, originalText = text)

            text.contains("وقف التسجيل") ->
                JarvisIntent(JarvisIntentType.SAFETY_RECORD_STOP, originalText = text)

            text.contains("دفاع عن النفس") || text.contains("ما عندي أمان") ->
                JarvisIntent(JarvisIntentType.SAFETY_DEFENSE_INFO, originalText = text)

            else -> JarvisIntent(JarvisIntentType.UNKNOWN, originalText = text)
        }
    }

    // ─── مساعدات التحليل ─────────────────────────────────────────────────

    /** يستخرج اسم الصخر من جملة مثل "عرّف الجرانيت" أو "ما هو البازلت" */
    private fun extractRockName(text: String): String {
        val prefixes = listOf(
            "عرّف ", "عرف ", "ما هو ال", "ما هي ال",
            "اخبرني عن ال", "اخبرني عن ", "معلومات عن ال", "معلومات عن "
        )
        for (p in prefixes) {
            if (text.contains(p)) {
                return text.substringAfter(p).trim()
                    .split(" ").firstOrNull()?.trim() ?: ""
            }
        }
        return text.trim()
    }

    private fun extractModuleName(text: String): String {
        val idx = text.indexOf("وحدة")
        if (idx == -1) return ""
        return text.substring(idx + "وحدة".length).trim()
    }

    private fun normalize(text: String): String =
        text.trim().lowercase(Locale.getDefault()).replace(Regex("\\s+"), " ")

    // ─── تبديل الوحدات ────────────────────────────────────────────────────
    private fun handleModuleToggle(intent: JarvisIntent): Boolean {
        val manager = moduleManager ?: return false
        val target = intent.argument.trim()
        if (target.isBlank()) return false
        val module = manager.all().firstOrNull { m ->
            m.id.contains(target, ignoreCase = true) ||
            m.title.contains(target, ignoreCase = true) ||
            m.shortTitle.contains(target, ignoreCase = true)
        } ?: return false
        val enable = intent.type == JarvisIntentType.MODULE_ENABLE
        manager.setEnabled(module.id, enable)
        onModuleToggled?.invoke(module, enable)
        return true
    }
}
