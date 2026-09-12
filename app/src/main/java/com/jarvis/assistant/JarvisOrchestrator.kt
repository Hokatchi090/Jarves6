package com.jarvis.assistant

import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException

/**
 * العقل المنسّق: يصنّف كل طلب (عبر GPT إن وُجد مفتاحه، وإلا بقواعد كلمات مفتاحية بسيطة
 * كخطة بديلة)، يوجّهه للمعالج المناسب، ثم يمرّر النتيجة الخام مرة أخيرة عبر GPT لإضافة
 * أسلوب جارفس قبل تحويلها لصوت.
 *
 * ملاحظة صادقة: هذا التصنيف الذكي (خطوتين GPT لكل طلب) يزيد الكلفة والوقت مقارنة بالنظام
 * المباشر القديم (كلمات مفتاحية → معالج واحد). لهذا هو اختياري (ORCHESTRATOR MODE)،
 * مو الافتراضي.
 */
class JarvisOrchestrator(
    private val client: OkHttpClient,
    private val openAiKeyProvider: () -> String,
    private val geminiKeyProvider: () -> String
) {
    interface CategoryHandlers {
        fun onNavigation(text: String, callback: (String) -> Unit)
        fun onWeather(text: String, callback: (String) -> Unit)
        fun onGeology(text: String, callback: (String) -> Unit)
        fun onHealth(text: String, callback: (String) -> Unit)
        fun onCoding(text: String, callback: (String) -> Unit)
        fun onNotes(text: String, callback: (String) -> Unit)
        fun onImage(text: String, callback: (String) -> Unit)
        fun onVoice(text: String, callback: (String) -> Unit)
        fun onFitness(text: String, callback: (String) -> Unit)
    }

    private val categories = listOf(
        "GENERAL", "SEARCH", "NAVIGATION", "WEATHER", "VOICE",
        "IMAGE", "GEOLOGY", "HEALTH", "CODING", "NOTES", "FITNESS"
    )

    fun process(
        text: String,
        history: List<Pair<String, String>>,
        handlers: CategoryHandlers,
        onFinal: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        classify(text) { category ->
            when (category) {
                "NAVIGATION" -> handlers.onNavigation(text) { raw -> restyle(raw, onFinal) }
                "WEATHER" -> handlers.onWeather(text) { raw -> restyle(raw, onFinal) }
                "GEOLOGY" -> handlers.onGeology(text) { raw -> restyle(raw, onFinal) }
                "HEALTH" -> handlers.onHealth(text) { raw -> restyle(raw, onFinal) }
                "CODING" -> handlers.onCoding(text) { raw -> onFinal(raw) } // \u0627\u0644\u0643\u0648\u062F \u0645\u0627 \u064A\u062A\u0639\u062F\u0651\u0644\u0634 \u0623\u0633\u0644\u0648\u0628\u064A\u064B\u0627
                "NOTES" -> handlers.onNotes(text) { raw -> restyle(raw, onFinal) }
                "IMAGE" -> handlers.onImage(text) { raw -> onFinal(raw) }
                "VOICE" -> handlers.onVoice(text) { raw -> onFinal(raw) }
                "FITNESS" -> handlers.onFitness(text) { raw -> restyle(raw, onFinal) }
                "SEARCH" -> askGemini(text, history, onFinal, onError)
                else -> askGPT(text, history, onFinal, onError)
            }
        }
    }

    /** يصنّف الطلب عبر GPT إن وُجد مفتاحه، وإلا كلمات مفتاحية بسيطة كخطة بديلة فورية */
    private fun classify(text: String, callback: (String) -> Unit) {
        val openAiKey = openAiKeyProvider()
        if (openAiKey.isBlank()) {
            callback(classifyByKeywords(text))
            return
        }
        val prompt = "\u0635\u0646\u0651\u0641 \u0647\u0630\u0627 \u0627\u0644\u0637\u0644\u0628 \u0625\u0644\u0649 \u0641\u0626\u0629 \u0648\u0627\u062D\u062F\u0629 \u0641\u0642\u0637 \u0645\u0646 \u0647\u0630\u0647 \u0627\u0644\u0642\u0627\u0626\u0645\u0629: ${categories.joinToString(",")}. " +
                "\u0623\u062C\u0628 \u0628\u0643\u0644\u0645\u0629 \u0648\u0627\u062D\u062F\u0629 \u0641\u0642\u0637 \u0628\u062F\u0648\u0646 \u0634\u0631\u062D. \u0627\u0644\u0637\u0644\u0628: \"$text\""
        val body = JSONObject().apply {
            put("model", "gpt-4o-mini")
            put("messages", JSONArray().put(JSONObject().apply {
                put("role", "user"); put("content", prompt)
            }))
            put("temperature", 0)
        }
        val request = Request.Builder()
            .url("https://api.openai.com/v1/chat/completions")
            .addHeader("Authorization", "Bearer $openAiKey")
            .addHeader("Content-Type", "application/json")
            .post(body.toString().toRequestBody("application/json".toMediaTypeOrNull()))
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) = callback(classifyByKeywords(text))
            override fun onResponse(call: Call, response: Response) {
                try {
                    val json = JSONObject(response.body?.string() ?: "")
                    val raw = json.getJSONArray("choices").getJSONObject(0)
                        .getJSONObject("message").getString("content").trim().uppercase()
                    val matched = categories.firstOrNull { raw.contains(it) } ?: classifyByKeywords(text)
                    callback(matched)
                } catch (e: Exception) {
                    callback(classifyByKeywords(text))
                }
            }
        })
    }

    private fun classifyByKeywords(text: String): String {
        val t = text.lowercase()
        return when {
            t.contains("\u0637\u0642\u0633") || t.contains("weather") || t.contains("\u0645\u0637\u0631") || t.contains("rain") -> "WEATHER"
            t.contains("\u062E\u0637\u0648\u0627\u062A") || t.contains("steps") || t.contains("\u0633\u0639\u0631\u0627\u062A") ||
                t.contains("calor") || t.contains("\u0645\u0633\u0627\u0641\u0629") || t.contains("distance") -> "FITNESS"
            t.contains("\u0637\u0631\u064A\u0642") || t.contains("\u0648\u062C\u0647\u0629") || t.contains("\u0645\u0644\u0627\u062D\u0629") -> "NAVIGATION"
            t.contains("\u0635\u062E\u0631") || t.contains("\u0645\u0639\u062F\u0646") || t.contains("\u062C\u064A\u0648\u0644\u0648\u062C\u064A") -> "GEOLOGY"
            t.contains("\u062F\u0648\u0627\u0621") || t.contains("\u0645\u0648\u0639\u062F") || t.contains("\u0635\u062D\u0629") || t.contains("\u0637\u0628\u064A\u0628") -> "HEALTH"
            t.contains("\u0643\u0648\u062F") || t.contains("\u0628\u0631\u0645\u062C") || t.contains("code") -> "CODING"
            t.contains("\u0645\u0644\u0627\u062D\u0638\u0629") || t.contains("\u062F\u0648\u0631 \u0641\u064A") -> "NOTES"
            t.contains("\u0635\u0648\u0631\u0629") || t.contains("\u0635\u0648\u0651\u0631") -> "IMAGE"
            t.contains("\u0635\u0648\u062A") -> "VOICE"
            else -> "GENERAL"
        }
    }

    /** الخطوة الأخيرة: تمرير النتيجة الخام عبر GPT لإضافة أسلوب جارفس (مختصر، ودود، بلا حشو) */
    private fun restyle(rawResult: String, callback: (String) -> Unit) {
        val openAiKey = openAiKeyProvider()
        if (openAiKey.isBlank()) {
            callback(rawResult) // \u0628\u062F\u0648\u0646 GPT\u060C \u0646\u0631\u062C\u0639 \u0627\u0644\u0646\u062A\u064A\u062C\u0629 \u0627\u0644\u062E\u0627\u0645 \u0643\u0645\u0627 \u0647\u064A
            return
        }
        val prompt = "\u0623\u0639\u062F \u0635\u064A\u0627\u063A\u0629 \u0647\u0630\u0647 \u0627\u0644\u0645\u0639\u0644\u0648\u0645\u0629 \u0628\u0623\u0633\u0644\u0648\u0628 \u0645\u0633\u0627\u0639\u062F \u0630\u0643\u064A \u0648\u062F\u0648\u062F \u0627\u0633\u0645\u0647 \u062C\u0627\u0631\u0641\u0633\u060C \u062C\u0645\u0644\u062A\u064A\u0646 \u0625\u0644\u0649 \u062B\u0644\u0627\u062B \u0641\u0642\u0637\u060C \u0628\u062F\u0648\u0646 \u062D\u0634\u0648: \"$rawResult\""
        val body = JSONObject().apply {
            put("model", "gpt-4o-mini")
            put("messages", JSONArray().put(JSONObject().apply {
                put("role", "user"); put("content", prompt)
            }))
        }
        val request = Request.Builder()
            .url("https://api.openai.com/v1/chat/completions")
            .addHeader("Authorization", "Bearer $openAiKey")
            .addHeader("Content-Type", "application/json")
            .post(body.toString().toRequestBody("application/json".toMediaTypeOrNull()))
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) = callback(rawResult)
            override fun onResponse(call: Call, response: Response) {
                try {
                    val json = JSONObject(response.body?.string() ?: "")
                    val styled = json.getJSONArray("choices").getJSONObject(0)
                        .getJSONObject("message").getString("content").trim()
                    callback(styled.ifBlank { rawResult })
                } catch (e: Exception) {
                    callback(rawResult)
                }
            }
        })
    }

    private fun askGPT(
        text: String, history: List<Pair<String, String>>,
        onSuccess: (String) -> Unit, onError: (String) -> Unit
    ) {
        val provider = OpenAIProvider(openAiKeyProvider())
        if (!provider.isConfigured()) {
            askGemini(text, history, onSuccess, onError)
            return
        }
        provider.ask(
            text, history,
            "\u0623\u0646\u062A \u062C\u0627\u0631\u0641\u0633\u060C \u0645\u0633\u0627\u0639\u062F \u0630\u0643\u064A \u0648\u062F\u0648\u062F. \u0623\u062C\u0628 \u0628\u0625\u064A\u062C\u0627\u0632 \u0648\u0648\u0636\u0648\u062D.",
            client, onSuccess, onError
        )
    }

    private fun askGemini(
        text: String, history: List<Pair<String, String>>,
        onSuccess: (String) -> Unit, onError: (String) -> Unit
    ) {
        val provider = GeminiProvider(geminiKeyProvider())
        if (!provider.isConfigured()) {
            onError("\u0645\u0627\u0641\u064A\u0634 \u0623\u064A \u0645\u0641\u062A\u0627\u062D API \u0645\u0641\u0639\u0651\u0644 \u062D\u0627\u0644\u064A\u064B\u0627")
            return
        }
        provider.ask(
            text, history,
            "\u0623\u0646\u062A \u062C\u0627\u0631\u0641\u0633\u060C \u0645\u0633\u0627\u0639\u062F \u0630\u0643\u064A \u0648\u062F\u0648\u062F. \u0623\u062C\u0628 \u0628\u0625\u064A\u062C\u0627\u0632 \u0648\u0648\u0636\u0648\u062D.",
            client, onSuccess, onError
        )
    }
}
