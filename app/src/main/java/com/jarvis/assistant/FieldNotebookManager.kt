package com.jarvis.assistant

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// دفتر ميداني جيولوجي: يخزّن كل ملاحظة مع موقع GPS حقيقي ووقت حقيقي وصورة اختيارية
// 100% أوفلاين — SharedPreferences فقط
class FieldNotebookManager(private val context: Context) {

    data class FieldEntry(
        val timestamp: Long,
        val lat: Double,
        val lon: Double,
        val note: String,
        val rockType: String,
        val photoPath: String = ""     // ← جديد: مسار صورة مرفقة (فارغ إذا لا يوجد)
    )

    private fun prefs() =
        context.getSharedPreferences("jarvis_field_notebook", Context.MODE_PRIVATE)

    // ─── إضافة ملاحظة ─────────────────────────────────────────────────────
    fun addEntry(lat: Double, lon: Double, note: String,
                 rockType: String = "", photoPath: String = "") {
        val arr = getAllRaw()
        val obj = JSONObject().apply {
            put("timestamp", System.currentTimeMillis())
            put("lat",       lat)
            put("lon",       lon)
            put("note",      note)
            put("rockType",  rockType)
            put("photoPath", photoPath)
        }
        arr.put(obj)
        prefs().edit().putString("entries", arr.toString()).apply()
    }

    // ─── ربط صورة بآخر ملاحظة ────────────────────────────────────────────
    fun attachPhotoToLastEntry(photoPath: String): Boolean {
        val arr = getAllRaw()
        if (arr.length() == 0) return false
        // آخر ملاحظة هي الأحدث (أعلى index)
        val lastIdx = arr.length() - 1
        val obj = arr.getJSONObject(lastIdx)
        obj.put("photoPath", photoPath)
        arr.put(lastIdx, obj)
        prefs().edit().putString("entries", arr.toString()).apply()
        return true
    }

    // ─── قراءة ─────────────────────────────────────────────────────────────
    private fun getAllRaw(): JSONArray {
        val raw = prefs().getString("entries", null) ?: return JSONArray()
        return try { JSONArray(raw) } catch (_: Exception) { JSONArray() }
    }

    fun getAll(): List<FieldEntry> {
        val arr = getAllRaw()
        val list = mutableListOf<FieldEntry>()
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            list.add(FieldEntry(
                timestamp = o.optLong("timestamp"),
                lat       = o.optDouble("lat"),
                lon       = o.optDouble("lon"),
                note      = o.optString("note"),
                rockType  = o.optString("rockType"),
                photoPath = o.optString("photoPath", "")
            ))
        }
        return list.sortedByDescending { it.timestamp }
    }

    fun count(): Int = getAllRaw().length()

    fun clearAll() = prefs().edit().remove("entries").apply()

    // ─── تنسيق للعرض الصوتي ──────────────────────────────────────────────
    fun formatEntry(entry: FieldEntry): String {
        val dateStr = SimpleDateFormat("dd/MM HH:mm", Locale.getDefault())
            .format(Date(entry.timestamp))
        val rockPart  = if (entry.rockType.isNotBlank())  " (${entry.rockType})" else ""
        val photoPart = if (entry.photoPath.isNotBlank()) " 📷" else ""
        return "$dateStr$rockPart$photoPart: ${entry.note} " +
               "[${"%.5f".format(entry.lat)}, ${"%.5f".format(entry.lon)}]"
    }

    // ─── تصدير CSV (يشمل عمود الصورة) ────────────────────────────────────
    // أوفلاين 100% — لا إنترنت
    fun exportAsCsv(): String {
        val header = "timestamp,latitude,longitude,rock_type,photo_path,note\n"
        val rows = getAll().joinToString("\n") { e ->
            val date     = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                               .format(Date(e.timestamp))
            val safeNote = e.note.replace(",", ";").replace("\n", " ")
            "$date,${e.lat},${e.lon},${e.rockType},${e.photoPath},$safeNote"
        }
        return header + rows
    }

    fun exportToFile(): String? {
        if (count() == 0) return null
        return try {
            val dir = context.getExternalFilesDir(android.os.Environment.DIRECTORY_DOCUMENTS)
                ?: context.filesDir
            if (!dir.exists()) dir.mkdirs()
            val file = java.io.File(dir, "field_notebook_${System.currentTimeMillis()}.csv")
            java.io.FileOutputStream(file).use {
                it.write(exportAsCsv().toByteArray(Charsets.UTF_8))
            }
            file.absolutePath
        } catch (_: Exception) { null }
    }
}
