package com.jarvis.assistant

enum class JarvisIntentType {
    OPEN_APP,
    SYSTEM_FLASH,
    SYSTEM_BATTERY,
    SYSTEM_TIME,
    SYSTEM_CAMERA,
    SYSTEM_SETTINGS,
    APPS_SHOW,
    APPS_HIDE,
    UNKNOWN
}

data class JarvisIntent(
    val type: JarvisIntentType,
    val argument: String = "",
    val originalText: String = ""
)
