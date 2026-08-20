package com.jarvis.assistant

import android.content.Context

// \u0645\u0644\u0641 \u0645\u0646\u0641\u0635\u0644 \u064A\u062F\u064A\u0631 \u062A\u062E\u0632\u064A\u0646 \u0648\u0627\u0633\u062A\u0631\u062C\u0627\u0639 \u0642\u0627\u0626\u0645\u0629 \u0627\u0644\u0623\u063A\u0627\u0646\u064A \u0639\u0628\u0631 SharedPreferences
// \u062A\u0645 \u0641\u0635\u0644\u0647 \u0639\u0646 MainActivity \u0644\u062A\u0633\u0647\u064A\u0644 \u0627\u0644\u0642\u0631\u0627\u0621\u0629 \u0648\u062A\u0642\u0644\u064A\u0644 \u0627\u0644\u0627\u0639\u062A\u0645\u0627\u062F \u0639\u0644\u0649 Activity
class PlaylistManager(private val context: Context) {

    private fun prefs() = context.getSharedPreferences("jarvis_prefs", Context.MODE_PRIVATE)

    fun getPlaylist(): MutableList<String> {
        val raw = prefs().getString("playlist", "") ?: ""
        return if (raw.isBlank()) mutableListOf() else raw.split("||").toMutableList()
    }

    fun savePlaylist(list: List<String>) {
        prefs().edit().putString("playlist", list.joinToString("||")).apply()
    }

    fun getCurrentIndex(): Int {
        return prefs().getInt("playlist_index", -1)
    }

    fun setCurrentIndex(index: Int) {
        prefs().edit().putInt("playlist_index", index).apply()
    }

    fun clear() {
        savePlaylist(emptyList())
        setCurrentIndex(-1)
    }

    // \u062A\u0636\u064A\u0641 \u0623\u063A\u0646\u064A\u0629 \u0625\u0630\u0627 \u0645\u0627\u0643\u0627\u0646\u062A\u0634 \u0645\u0648\u062C\u0648\u062F\u0629\u060C \u062A\u0631\u062C\u0639 true \u0625\u0630\u0627 \u0632\u064A\u062F\u062A
    fun addSong(name: String): Boolean {
        val list = getPlaylist()
        if (list.contains(name)) return false
        list.add(name)
        savePlaylist(list)
        return true
    }

    // \u062A\u0636\u064A\u0641 \u0627\u0644\u0623\u063A\u0646\u064A\u0629 (\u0625\u0630\u0627 \u0645\u0627\u0643\u0627\u0646\u062A\u0634 \u0645\u0648\u062C\u0648\u062F\u0629) \u0648\u062A\u062D\u0637\u0647\u0627 \u0643\u0623\u063A\u0646\u064A\u0629 \u062D\u0627\u0644\u064A\u0629
    fun addAndSetCurrent(name: String) {
        val list = getPlaylist()
        var index = list.indexOf(name)
        if (index == -1) {
            list.add(name)
            index = list.size - 1
            savePlaylist(list)
        }
        setCurrentIndex(index)
    }
}
