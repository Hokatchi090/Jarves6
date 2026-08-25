package com.jarvis.assistant

import android.content.Context
import org.json.JSONObject
import java.util.Calendar

// \u064A\u062A\u0628\u0651\u0639 \u0627\u0644\u0623\u0648\u0627\u0645\u0631 \u0627\u0644\u0644\u064A \u064A\u062F\u064A\u0631\u0647\u0627 \u0627\u0644\u0645\u0633\u062A\u062E\u062F\u0645 \u062D\u0633\u0628 \u0633\u0627\u0639\u0629 \u0627\u0644\u064A\u0648\u0645\u060C \u0648\u064A\u0642\u062A\u0631\u062D \u0627\u0644\u0623\u0645\u0631 \u0627\u0644\u0623\u0643\u062B\u0631 \u062A\u0643\u0631\u0627\u0631\u0627\u064B \u0641\u064A \u0646\u0641\u0633 \u0627\u0644\u0633\u0627\u0639\u0629
// \u0637\u0631\u064A\u0642\u0629 \u0628\u0633\u064A\u0637\u0629 \u0645\u0628\u0646\u064A\u0629 \u0639\u0644\u0649 \u0627\u0644\u062A\u0643\u0631\u0627\u0631 (\u0645\u0627\u0634\u064A \u0630\u0643\u0627\u0621 \u0627\u0635\u0637\u0646\u0627\u0639\u064A \u0645\u0639\u0642\u0651\u062F) \u0644\u0643\u0646 \u062D\u0642\u064A\u0642\u064A\u0629 \u0648\u062A\u0634\u062A\u063A\u0644 \u0645\u062D\u0644\u064A\u0627\u064B \u0628\u0627\u0644\u0643\u0627\u0645\u0644 \u0628\u0644\u0627 \u0623\u064A \u062E\u062F\u0645\u0629 \u062E\u0627\u0631\u062C\u064A\u0629
class UsagePatternTracker(private val context: Context) {

    private fun prefs() = context.getSharedPreferences("jarvis_usage_patterns", Context.MODE_PRIVATE)

    private fun currentHourBucket(): Int = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)

    // \u064A\u0633\u062C\u0651\u0644 \u0623\u0645\u0631 \u0645\u0646\u0641\u0630 \u0641\u064A \u0627\u0644\u0633\u0627\u0639\u0629 \u0627\u0644\u062D\u0627\u0644\u064A\u0629. \u0646\u062A\u062C\u0627\u0647\u0644 \u0627\u0644\u0623\u0648\u0627\u0645\u0631 \u0627\u0644\u0642\u0635\u064A\u0631\u0629 \u062C\u062F\u0627\u064B (\u0645\u062B\u0644 "\u0646\u0639\u0645"/"\u0644\u0627") \u0644\u0623\u0646\u0647\u0627 \u0645\u0627\u062A\u0641\u064A\u062F\u0634 \u0641\u064A \u0627\u0644\u062A\u0648\u0642\u0639
    fun recordCommand(commandText: String) {
        val trimmed = commandText.trim()
        if (trimmed.length < 4) return

        val hour = currentHourBucket()
        val key = "hour_$hour"
        val raw = prefs().getString(key, "{}") ?: "{}"
        val json = try { JSONObject(raw) } catch (e: Exception) { JSONObject() }
        val currentCount = json.optInt(trimmed, 0)
        json.put(trimmed, currentCount + 1)
        prefs().edit().putString(key, json.toString()).apply()
    }

    // \u064A\u0631\u062C\u0639 \u0627\u0644\u0623\u0645\u0631 \u0627\u0644\u0623\u0643\u062B\u0631 \u062A\u0643\u0631\u0627\u0631\u0627\u064B \u0641\u064A \u0627\u0644\u0633\u0627\u0639\u0629 \u0627\u0644\u062D\u0627\u0644\u064A\u0629 (\u0623\u0648 \u0627\u0644\u0633\u0627\u0639\u0629 \u0627\u0644\u0645\u062C\u0627\u0648\u0631\u0629)\u060C \u0623\u0648 null \u0625\u0630\u0627 \u0645\u0627\u0641\u064A\u0634 \u0646\u0645\u0637 \u0648\u0627\u0636\u062D \u0628\u0639\u062F
    fun getTopSuggestionForNow(minOccurrences: Int = 3): String? {
        val hour = currentHourBucket()
        val candidates = listOf(hour, (hour - 1 + 24) % 24, (hour + 1) % 24)

        val combined = mutableMapOf<String, Int>()
        for (h in candidates) {
            val raw = prefs().getString("hour_$h", "{}") ?: "{}"
            val json = try { JSONObject(raw) } catch (e: Exception) { JSONObject() }
            json.keys().forEach { k ->
                combined[k] = (combined[k] ?: 0) + json.optInt(k, 0)
            }
        }

        val best = combined.maxByOrNull { it.value } ?: return null
        return if (best.value >= minOccurrences) best.key else null
    }

    fun clearAll() {
        prefs().edit().clear().apply()
    }
}
