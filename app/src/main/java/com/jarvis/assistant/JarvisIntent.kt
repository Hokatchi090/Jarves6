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
    CLOCK_SHOW,
    CLOCK_HIDE,
    MODULE_ENABLE,
    MODULE_DISABLE,
    MAP_NAVIGATE,
    MAP_DISTANCE,
    CONTACT_CALL,
    SMS_READ,
    SMS_EXPLAIN,
    SMS_SEND,
    FIELD_LOG_ADD,
    FIELD_LOG_LIST,
    FIELD_LOG_EXPORT,
    COMPASS_READ,
    SAFETY_FAKE_CALL,
    SAFETY_SEND_LOCATION,
    SAFETY_RECORD_START,
    SAFETY_RECORD_STOP,
    SAFETY_DEFENSE_INFO,
    SAFETY_SET_CONTACT,
    START_LISTENING,
    UNKNOWN
}

data class JarvisIntent(
    val type: JarvisIntentType,
    val argument: String = "",
    val originalText: String = ""
)
