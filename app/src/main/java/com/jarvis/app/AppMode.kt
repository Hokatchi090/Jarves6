package com.jarvisx.app

/**
 * يتحكم في هوية النسخة المصدَّرة من نفس الكود.
 * غيّر MODE إلى "FAMILY" وأعد بناء الـ APK لكل فرد من العائلة.
 * لتوليد كود عائلة مختلف لكل عائلة/جهة، غيّر FAMILY_CODE قبل كل بناء.
 */
object AppMode {
    const val MODE: String = "ADMIN" // أو "FAMILY"
    const val FAMILY_CODE: String = "FAMILY-7X9K2"

    fun isAdmin(): Boolean = MODE == "ADMIN"
    fun isFamily(): Boolean = MODE == "FAMILY"
}
