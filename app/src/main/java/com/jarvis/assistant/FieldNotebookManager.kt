package com.jarvis.assistant

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// \u062F\u0641\u062A\u0631 \u0645\u064A\u062F\u0627\u0646\u064A \u062C\u064A\u0648\u0644\u0648\u062C\u064A: \u064A\u062E\u0632\u0651\u0646 \u0643\u0644 \u0645\u0644\u0627\u062D\u0638\u0629 \u0645\u0639 \u0645\u0648\u0642\u0639 GPS \u062D\u0642\u064A\u0642\u064A \u0648\u0648\u0642\u062A \u062D\u0642\u064A\u0642\u064A\u060C \u0645\u0641\u064A\u062F \u062C\u062F\u0627\u064B \u0644\u0637\u0627\u0644\u0628 \u0627\u0644\u062C\u064A\u0648\u0644\u0648\u062C\u064A\u0627 \u0648\u0642\u062A \u0627\u0644\u0639\u0645\u0644 \u0627\u0644\u0645\u064A\u062F\u0627\u0646\u064A
class FieldNotebookManager(private val context: Context) {

    data class FieldEntry(
        val timestamp: Long,
        val lat: Double,
        val lon: Double,
        val note: String,
        val rockType: String
    )

    private fun prefs() = context.getSharedPreferences("jarvis_field_notebook", Context.MODE_PRIVATE)

    fun addEntry(lat: Double, lon: Double, note: String, rockType: String = "") {
        val entries = getAllRaw()
        val entry = JSONObject().apply {
            put("timestamp", System.currentTimeMillis())
            put("lat", lat)
            put("lon", lon)
            put("note", note)
            put("rockType", rockType)
        }
        entries.put(entry)
        prefs().edit().putString("entries", entries.toString()).apply()
    }

    private fun getAllRaw(): JSONArray {
        val raw = prefs().getString("entries", null) ?: return JSONArray()
        return try {
            JSONArray(raw)
        } catch (e: Exception) {
            JSONArray()
        }
    }

    fun getAll(): List<FieldEntry> {
        val arr = getAllRaw()
        val list = mutableListOf<FieldEntry>()
        for (i in 0 until arr.length()) {
            val obj = arr.getJSONObject(i)
            list.add(
                FieldEntry(
                    timestamp = obj.optLong("timestamp"),
                    lat = obj.optDouble("lat"),
                    lon = obj.optDouble("lon"),
                    note = obj.optString("note"),
                    rockType = obj.optString("rockType")
                )
            )
        }
        return list.sortedByDescending { it.timestamp }
    }

    fun count(): Int = getAllRaw().length()

    fun clearAll() {
        prefs().edit().remove("entries").apply()
    }

    fun formatEntry(entry: FieldEntry): String {
        val dateStr = SimpleDateFormat("dd/MM HH:mm", Locale.getDefault()).format(Date(entry.timestamp))
        val rockPart = if (entry.rockType.isNotBlank()) " (${entry.rockType})" else ""
        return "$dateStr$rockPart: ${entry.note} [${"%.5f".format(entry.lat)}, ${"%.5f".format(entry.lon)}]"
    }

    // \u064A\u0635\u062F\u0651\u0631 \u0643\u0644 \u0627\u0644\u0645\u0644\u0627\u062D\u0638\u0627\u062A \u0643\u0645\u0644\u0641 CSV \u0646\u0635\u064A\u060C \u0645\u0641\u064A\u062F \u0644\u0644\u0627\u0633\u062A\u064A\u0631\u0627\u062F \u0644\u0640 Excel \u0623\u0648 GIS \u0644\u0627\u062D\u0642\u0627\u064B
    fun exportAsCsv(): String {
        val header = "timestamp,latitude,longitude,rock_type,note\n"
        val rows = getAll().joinToString("\n") { entry ->
            val dateStr = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(entry.timestamp))
            val safeNote = entry.note.replace(",", ";")
            "$dateStr,${entry.lat},${entry.lon},${entry.rockType},$safeNote"
        }
        return header + rows
    }

    // \u064A\u062D\u0641\u0638 \u0645\u0644\u0641 CSV \u062D\u0642\u064A\u0642\u064A \u0639\u0644\u0649 \u0627\u0644\u062C\u0647\u0627\u0632\u060C \u0628\u0644\u0627 \u0623\u064A \u0627\u0644\u062A\u0635\u0627\u0644 \u0625\u0646\u062A\u0631\u0646\u062A (\u0623\u0648\u0641\u0644\u0627\u064A\u0646 100\u066A)
    fun exportToFile(): String? {
        if (count() == 0) return null
        return try {
            val dir = context.getExternalFilesDir(android.os.Environment.DIRECTORY_DOCUMENTS)
                ?: context.filesDir
            if (!dir.exists()) dir.mkdirs()
            val fileName = "field_notebook_${System.currentTimeMillis()}.csv"
            val file = java.io.File(dir, fileName)
            java.io.FileOutputStream(file).use { it.write(exportAsCsv().toByteArray(Charsets.UTF_8)) }
            file.absolutePath
        } catch (e: Exception) {
            null
        }
    }
}
