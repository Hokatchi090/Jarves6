package com.jarvis.assistant

import java.util.Locale

class JarvisCommandRouter(
    private val legacyHandler: (String) -> Unit,
    private val appLauncher: ((String) -> Boolean)? = null,
    private val systemHandler: ((JarvisIntent) -> Boolean)? = null,
    private val appsHandler: ((JarvisIntent) -> Boolean)? = null
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

            JarvisIntentType.UNKNOWN -> {
                legacyHandler(text)
            }
        }
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

            else -> JarvisIntent(JarvisIntentType.UNKNOWN, originalText = text)
        }
    }

    private fun normalize(text: String): String {
        return text
            .trim()
            .lowercase(Locale.getDefault())
            .replace(Regex("\\s+"), " ")
    }
}
