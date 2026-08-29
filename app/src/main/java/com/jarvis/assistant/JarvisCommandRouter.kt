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
                if (!handled) {
                    legacyHandler(text)
                }
            }

            JarvisIntentType.SYSTEM_FLASH,
            JarvisIntentType.SYSTEM_BATTERY,
            JarvisIntentType.SYSTEM_TIME,
            JarvisIntentType.SYSTEM_CAMERA,
            JarvisIntentType.SYSTEM_SETTINGS -> {
                val handled = systemHandler?.invoke(intent) ?: false
                if (!handled) {
                    legacyHandler(text)
                }
            }

            JarvisIntentType.APPS_SHOW,
            JarvisIntentType.APPS_HIDE -> {
                val handled = appsHandler?.invoke(intent) ?: false
                if (!handled) {
                    legacyHandler(text)
                }
            }

            JarvisIntentType.CLOCK_SHOW,
            JarvisIntentType.CLOCK_HIDE -> {
                val handled = clockHandler?.invoke(intent) ?: false
                if (!handled) {
                    legacyHandler(text)
                }
            }

            JarvisIntentType.MODULE_ENABLE,
            JarvisIntentType.MODULE_DISABLE -> {
                val handled = handleModuleToggle(intent)
                if (!handled) {
                    legacyHandler(text)
                }
            }

            JarvisIntentType.MAP_NAVIGATE,
            JarvisIntentType.MAP_DISTANCE -> {
                val handled = mapHandler?.invoke(intent) ?: false
                if (!handled) {
                    legacyHandler(text)
                }
            }

            JarvisIntentType.CONTACT_CALL,
            JarvisIntentType.SMS_READ,
            JarvisIntentType.SMS_EXPLAIN,
            JarvisIntentType.SMS_SEND -> {
                val handled = contactsHandler?.invoke(intent) ?: false
                if (!handled) {
                    legacyHandler(text)
                }
            }

            JarvisIntentType.FIELD_LOG_ADD,
            JarvisIntentType.FIELD_LOG_LIST,
            JarvisIntentType.FIELD_LOG_EXPORT,
            JarvisIntentType.COMPASS_READ -> {
                val handled = geologyHandler?.invoke(intent) ?: false
                if (!handled) {
                    legacyHandler(text)
                }
            }

            JarvisIntentType.SAFETY_FAKE_CALL,
            JarvisIntentType.SAFETY_SEND_LOCATION,
            JarvisIntentType.SAFETY_RECORD_START,
            JarvisIntentType.SAFETY_RECORD_STOP,
            JarvisIntentType.SAFETY_DEFENSE_INFO,
            JarvisIntentType.SAFETY_SET_CONTACT -> {
                val handled = safetyHandler?.invoke(intent) ?: false
                if (!handled) {
                    legacyHandler(text)
                }
            }

            JarvisIntentType.START_LISTENING -> {
                startListeningHandler?.invoke() ?: legacyHandler(text)
            }

            JarvisIntentType.UNKNOWN -> {
                legacyHandler(text)
            }
        }
    }

    // \u064A\u062F\u0648\u0631 \u0639\u0644\u0649 \u0627\u0644\u0648\u062D\u062F\u0629 \u0627\u0644\u0645\u0630\u0643\u0648\u0631\u0629 \u0641\u064A \u0627\u0644\u0623\u0645\u0631 (\u0645\u0637\u0627\u0628\u0642\u0629 \u0627\u0644\u0645\u0639\u0631\u0641/\u0627\u0644\u0639\u0646\u0648\u0627\u0646 \u0627\u0644\u0642\u0635\u064A\u0631) \u0648\u064A\u0641\u0639\u0651\u0644/\u064A\u0639\u0637\u0651\u0644\u0647\u0627 \u0639\u0628\u0631 moduleManager
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

    private fun parse(text: String): JarvisIntent {
        val appPrefixes = listOf("\u0627\u0641\u062A\u062D ", "\u0634\u063A\u0644 ", "open ", "launch ")
        val appPrefix = appPrefixes.firstOrNull { text.startsWith(it) }

        if (appPrefix != null) {
            val appName = text.removePrefix(appPrefix).trim()
            if (appName.isNotBlank()) {
                return JarvisIntent(JarvisIntentType.OPEN_APP, argument = appName, originalText = text)
            }
        }

        return when {
            text.contains("\u0634\u063A\u0644 \u0627\u0644\u0641\u0644\u0627\u0634") ||
                    text.contains("\u0627\u0641\u062A\u062D \u0627\u0644\u0641\u0644\u0627\u0634") ||
                    text.contains("turn on flash") ->
                JarvisIntent(JarvisIntentType.SYSTEM_FLASH, argument = "on", originalText = text)

            text.contains("\u0637\u0641\u064A \u0627\u0644\u0641\u0644\u0627\u0634") ||
                    text.contains("\u0627\u0637\u0641\u064A \u0627\u0644\u0641\u0644\u0627\u0634") ||
                    text.contains("turn off flash") ->
                JarvisIntent(JarvisIntentType.SYSTEM_FLASH, argument = "off", originalText = text)

            text.contains("\u0627\u0644\u0628\u0637\u0627\u0631\u064A\u0629") || text.contains("battery") ->
                JarvisIntent(JarvisIntentType.SYSTEM_BATTERY, originalText = text)

            text.contains("\u0643\u0645 \u0627\u0644\u0633\u0627\u0639\u0629") || text.contains("what time") ->
                JarvisIntent(JarvisIntentType.SYSTEM_TIME, originalText = text)

            text.contains("\u0627\u0641\u062A\u062D \u0627\u0644\u0643\u0627\u0645\u064A\u0631\u0627") || text.contains("open camera") ->
                JarvisIntent(JarvisIntentType.SYSTEM_CAMERA, originalText = text)

            text.contains("\u0627\u0641\u062A\u062D \u0627\u0644\u0627\u0639\u062F\u0627\u062F\u0627\u062A") || text.contains("open settings") ->
                JarvisIntent(JarvisIntentType.SYSTEM_SETTINGS, originalText = text)

            text.contains("\u0627\u0639\u0631\u0636 \u0627\u0644\u062A\u0637\u0628\u064A\u0642\u0627\u062A") ||
                    text.contains("\u0627\u0638\u0647\u0631 \u0627\u0644\u062A\u0637\u0628\u064A\u0642\u0627\u062A") ||
                    text.contains("show apps") ->
                JarvisIntent(JarvisIntentType.APPS_SHOW, originalText = text)

            text.contains("\u0627\u062E\u0641\u064A \u0627\u0644\u062A\u0637\u0628\u064A\u0642\u0627\u062A") ||
                    text.contains("hide apps") ->
                JarvisIntent(JarvisIntentType.APPS_HIDE, originalText = text)

            text.contains("\u0627\u0638\u0647\u0631 \u0627\u0644\u0633\u0627\u0639\u0629") || text.contains("show clock") ->
                JarvisIntent(JarvisIntentType.CLOCK_SHOW, originalText = text)

            text.contains("\u0627\u062E\u0641\u064A \u0627\u0644\u0633\u0627\u0639\u0629") || text.contains("hide clock") ->
                JarvisIntent(JarvisIntentType.CLOCK_HIDE, originalText = text)

            (text.contains("\u0641\u0639\u0651\u0644") || text.contains("\u0634\u063A\u0651\u0644")) && text.contains("\u0648\u062D\u062F\u0629") ->
                JarvisIntent(JarvisIntentType.MODULE_ENABLE, argument = extractModuleName(text), originalText = text)

            (text.contains("\u0639\u0637\u0651\u0644") || text.contains("\u0623\u0648\u0642\u0641")) && text.contains("\u0648\u062D\u062F\u0629") ->
                JarvisIntent(JarvisIntentType.MODULE_DISABLE, argument = extractModuleName(text), originalText = text)

            text.contains("\u0627\u0644\u0645\u0633\u0627\u0641\u0629 \u0628\u064A\u0646") -> {
                val afterMarker = text.substringAfter("\u0627\u0644\u0645\u0633\u0627\u0641\u0629 \u0628\u064A\u0646").trim()
                val cities = afterMarker.split("\u0648", "\u0625\u0644\u0649", "\u0644\u0640")
                    .map { it.trim() }.filter { it.isNotBlank() }
                if (cities.size >= 2) {
                    JarvisIntent(JarvisIntentType.MAP_DISTANCE, argument = "${cities[0]}|${cities[1]}", originalText = text)
                } else {
                    JarvisIntent(JarvisIntentType.UNKNOWN, originalText = text)
                }
            }

            text.startsWith("\u0631\u0648\u062D\u0646\u064A \u0644\u0640") || text.startsWith("\u0631\u0648\u062D\u0646\u064A \u0627\u0644\u0649") ||
                    text.startsWith("\u062E\u0630\u0646\u064A \u0644\u0640") || text.startsWith("navigate to") -> {
                val navPrefixes = listOf("\u0631\u0648\u062D\u0646\u064A \u0644\u0640", "\u0631\u0648\u062D\u0646\u064A \u0627\u0644\u0649", "\u062E\u0630\u0646\u064A \u0644\u0640", "navigate to")
                val matchedPrefix = navPrefixes.first { text.startsWith(it) }
                JarvisIntent(
                    JarvisIntentType.MAP_NAVIGATE,
                    argument = text.removePrefix(matchedPrefix).trim(),
                    originalText = text
                )
            }

            text.startsWith("\u0627\u062A\u0635\u0644 \u0628") ->
                JarvisIntent(JarvisIntentType.CONTACT_CALL, argument = text.removePrefix("\u0627\u062A\u0635\u0644 \u0628").trim(), originalText = text)

            text.contains("\u0627\u0642\u0631\u0627 \u0631\u0633\u0627\u0626\u0644\u064A") || text.contains("\u0631\u0633\u0627\u0626\u0644\u064A \u0627\u0644\u062C\u062F\u064A\u062F\u0629") ->
                JarvisIntent(JarvisIntentType.SMS_READ, originalText = text)

            text.contains("\u0627\u0634\u0631\u062D") && text.contains("\u0631\u0633\u0627\u0644\u0629") ->
                JarvisIntent(JarvisIntentType.SMS_EXPLAIN, originalText = text)

            text.contains("\u0627\u0628\u0639\u062B \u0631\u0633\u0627\u0644\u0629") || text.contains("\u062F\u064A\u0631 \u0631\u0633\u0627\u0644\u0629") -> {
                val marker = if (text.contains("\u0627\u0628\u0639\u062B \u0631\u0633\u0627\u0644\u0629")) "\u0627\u0628\u0639\u062B \u0631\u0633\u0627\u0644\u0629" else "\u062F\u064A\u0631 \u0631\u0633\u0627\u0644\u0629"
                val rest = text.substringAfter(marker).trim()
                val bodyParts = rest.split("\u062A\u0642\u0648\u0644", limit = 2)
                if (bodyParts.size == 2) {
                    val contactName = bodyParts[0].removePrefix("\u0644").trim()
                    val messageText = bodyParts[1].trim()
                    JarvisIntent(JarvisIntentType.SMS_SEND, argument = "$contactName|$messageText", originalText = text)
                } else {
                    JarvisIntent(JarvisIntentType.SMS_SEND, argument = "", originalText = text)
                }
            }

            text.contains("\u0633\u062C\u0651\u0644 \u0645\u0644\u0627\u062D\u0638\u0629 \u0645\u064A\u062F\u0627\u0646\u064A\u0629") || text.contains("\u0623\u0636\u0641 \u0644\u0644\u062F\u0641\u062A\u0631 \u0627\u0644\u0645\u064A\u062F\u0627\u0646\u064A") -> {
                val marker = if (text.contains("\u0633\u062C\u0651\u0644 \u0645\u0644\u0627\u062D\u0638\u0629 \u0645\u064A\u062F\u0627\u0646\u064A\u0629")) "\u0633\u062C\u0651\u0644 \u0645\u0644\u0627\u062D\u0638\u0629 \u0645\u064A\u062F\u0627\u0646\u064A\u0629" else "\u0623\u0636\u0641 \u0644\u0644\u062F\u0641\u062A\u0631 \u0627\u0644\u0645\u064A\u062F\u0627\u0646\u064A"
                JarvisIntent(JarvisIntentType.FIELD_LOG_ADD, argument = text.substringAfter(marker).trim(), originalText = text)
            }

            text.contains("\u0627\u0644\u062F\u0641\u062A\u0631 \u0627\u0644\u0645\u064A\u062F\u0627\u0646\u064A") && (text.contains("\u0648\u0631\u064A\u0646\u064A") || text.contains("\u0627\u0642\u0631\u0627")) ->
                JarvisIntent(JarvisIntentType.FIELD_LOG_LIST, originalText = text)

            text.contains("\u0635\u062F\u0651\u0631") && text.contains("\u0627\u0644\u062F\u0641\u062A\u0631 \u0627\u0644\u0645\u064A\u062F\u0627\u0646\u064A") ->
                JarvisIntent(JarvisIntentType.FIELD_LOG_EXPORT, originalText = text)

            text.contains("\u0627\u0644\u0628\u0648\u0635\u0644\u0629") || text.contains("\u0627\u062A\u062C\u0627\u0647 \u0627\u0644\u0637\u0628\u0642\u0629") || text.contains("strike") ->
                JarvisIntent(JarvisIntentType.COMPASS_READ, originalText = text)

            (text.contains("\u062C\u0627\u0631\u0641\u0633") && text.contains("\u0627\u0628\u062F\u0623") && text.contains("\u0627\u0633\u062A\u0645\u0627\u0639")) ->
                JarvisIntent(JarvisIntentType.START_LISTENING, originalText = text)

            text.contains("\u0627\u062A\u0635\u0627\u0644 \u0645\u0632\u064A\u0641") ->
                JarvisIntent(JarvisIntentType.SAFETY_FAKE_CALL, originalText = text)

            text.contains("\u0627\u0628\u0639\u062B \u0645\u0648\u0642\u0639\u064A") || (text.contains("\u0645\u0648\u0642\u0639\u064A") && text.contains("\u0637\u0648\u0627\u0631\u0626")) ->
                JarvisIntent(JarvisIntentType.SAFETY_SEND_LOCATION, originalText = text)

            text.contains("\u0627\u062D\u0641\u0638 \u0631\u0642\u0645 \u0627\u0644\u0637\u0648\u0627\u0631\u0626") ->
                JarvisIntent(JarvisIntentType.SAFETY_SET_CONTACT, argument = text.substringAfter("\u0627\u0644\u0637\u0648\u0627\u0631\u0626").trim(), originalText = text)

            text.contains("\u0628\u062F\u0627 \u0627\u0644\u062A\u0633\u062C\u064A\u0644") || text.contains("\u0633\u062C\u0644 \u0643\u062F\u0644\u064A\u0644") ->
                JarvisIntent(JarvisIntentType.SAFETY_RECORD_START, originalText = text)

            text.contains("\u0648\u0642\u0641 \u0627\u0644\u062A\u0633\u062C\u064A\u0644") ->
                JarvisIntent(JarvisIntentType.SAFETY_RECORD_STOP, originalText = text)

            text.contains("\u062F\u0641\u0627\u0639 \u0639\u0646 \u0627\u0644\u0646\u0641\u0633") || text.contains("\u0645\u0627 \u0639\u0646\u062F\u064A \u0623\u0645\u0627\u0646") ->
                JarvisIntent(JarvisIntentType.SAFETY_DEFENSE_INFO, originalText = text)

            else -> JarvisIntent(JarvisIntentType.UNKNOWN, originalText = text)
        }
    }

    // \u064A\u0633\u062A\u062E\u0631\u062C \u0627\u0633\u0645 \u0627\u0644\u0648\u062D\u062F\u0629 \u0645\u0646 \u062C\u0645\u0644\u0629 \u0645\u062B\u0644 "\u0641\u0639\u0651\u0644 \u0648\u062D\u062F\u0629 \u0627\u0644\u062E\u0631\u064A\u0637\u0629" -> "\u0627\u0644\u062E\u0631\u064A\u0637\u0629"
    private fun extractModuleName(text: String): String {
        val idx = text.indexOf("\u0648\u062D\u062F\u0629")
        if (idx == -1) return ""
        return text.substring(idx + "\u0648\u062D\u062F\u0629".length).trim()
    }

    private fun normalize(text: String): String {
        return text
            .trim()
            .lowercase(Locale.getDefault())
            .replace(Regex("\\s+"), " ")
    }
}
