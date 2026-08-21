package com.jarvis.assistant

import android.content.Context

// \u0645\u0644\u0641 \u0645\u0646\u0641\u0635\u0644 \u064A\u062F\u064A\u0631 \u062A\u062E\u0632\u064A\u0646 \u0648\u0627\u0633\u062A\u0631\u062C\u0627\u0639 \u0627\u0644\u0645\u0644\u0627\u062D\u0638\u0627\u062A \u0639\u0628\u0631 SharedPreferences
// \u062A\u0645 \u0641\u0635\u0644\u0647 \u0639\u0646 MainActivity \u0644\u062A\u0633\u0647\u064A\u0644 \u0627\u0644\u0642\u0631\u0627\u0621\u0629 \u0648\u062A\u0642\u0644\u064A\u0644 \u0627\u0644\u0627\u0639\u062A\u0645\u0627\u062F \u0639\u0644\u0649 Activity
class NotesManager(private val context: Context) {

    private fun prefs() = context.getSharedPreferences("jarvis_notes", Context.MODE_PRIVATE)

    fun save(note: String) {
        val existing = prefs().getStringSet("notes", mutableSetOf()) ?: mutableSetOf()
        val updated = existing.toMutableSet()
        updated.add(note)
        prefs().edit().putStringSet("notes", updated).apply()
    }

    fun getAll(): Set<String> {
        return prefs().getStringSet("notes", setOf()) ?: setOf()
    }
}
