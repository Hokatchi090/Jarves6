package com.jarvis.assistant

data class JarvisModule(
    val id: String,
    val title: String,
    val shortTitle: String,
    val type: JarvisModuleType,
    var enabled: Boolean = true
)

enum class JarvisModuleType {
    CLOCK,
    APPS,
    SYSTEM,
    MAP
}
