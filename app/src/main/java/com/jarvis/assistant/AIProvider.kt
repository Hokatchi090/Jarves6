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
 * واجهة موحّدة لأي مزوّد ذكاء اصطناعي، حتى نقدر نبدّل بينهم بسهولة ونقارن:
 * جودة الدارجة، سرعة الرد، الاستقرار، السعر... كل معيار يقيّمه المستخدم بنفسه بالتجربة
 * الفعلية على جهازه — هذا شيء ما نقدرش نختبره أو نضمنه من الكود وحده.
 */
interface AIProvider {
    val displayName: String
    fun isConfigured(): Boolean
    fun ask(
        message: String,
        history: List<Pair<String, String>>,
        systemPrompt: String,
        client: OkHttpClient,
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit
    )
}

/** يعتمد على Gemini (Google) — نفس المحرك الأساسي المستخدم أصلًا في التطبيق */
class GeminiProvider(private val apiKey: String) : AIProvider {
    override val displayName = "GEMINI"
    override fun isConfigured() = apiKey.isNotBlank()

    override fun ask(
        message: String, history: List<Pair<String, String>>, systemPrompt: String,
        client: OkHttpClient, onSuccess: (String) -> Unit, onError: (String) -> Unit
    ) {
        val contents = JSONArray()
        for ((role, text) in history) {
            contents.put(JSONObject().apply {
                put("role", role)
                put("parts", JSONArray().put(JSONObject().apply { put("text", text) }))
            })
        }
        contents.put(JSONObject().apply {
            put("role", "user")
            put("parts", JSONArray().put(JSONObject().apply { put("text", message) }))
        })
        val body = JSONObject().apply {
            put("system_instruction", JSONObject().apply {
                put("parts", JSONArray().put(JSONObject().apply { put("text", systemPrompt) }))
            })
            put("contents", contents)
        }
        val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.6-flash:generateContent?key=$apiKey"
        val request = Request.Builder()
            .url(url)
            .post(body.toString().toRequestBody("application/json".toMediaTypeOrNull()))
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) = onError("\u0641\u0634\u0644 \u0627\u0644\u0627\u062A\u0635\u0627\u0644 \u0628ـGemini")
            override fun onResponse(call: Call, response: Response) {
                try {
                    val json = JSONObject(response.body?.string() ?: "")
                    if (json.has("error")) {
                        onError(json.getJSONObject("error").optString("message", "\u062E\u0637\u0623 \u063A\u064A\u0631 \u0645\u0639\u0631\u0648\u0641"))
                        return
                    }
                    val reply = json.getJSONArray("candidates").getJSONObject(0)
                        .getJSONObject("content").getJSONArray("parts").getJSONObject(0).getString("text")
                    onSuccess(reply.trim())
                } catch (e: Exception) {
                    onError("\u062A\u0639\u0630\u0651\u0631 \u0641\u0647\u0645 \u0631\u062F Gemini")
                }
            }
        })
    }
}

/** يعتمد على OpenAI (ChatGPT) — يحتاج مفتاح API خاص بك، مدفوع حسب الاستخدام */
class OpenAIProvider(private val apiKey: String) : AIProvider {
    override val displayName = "OPENAI"
    override fun isConfigured() = apiKey.isNotBlank()

    override fun ask(
        message: String, history: List<Pair<String, String>>, systemPrompt: String,
        client: OkHttpClient, onSuccess: (String) -> Unit, onError: (String) -> Unit
    ) {
        val messages = JSONArray()
        messages.put(JSONObject().apply { put("role", "system"); put("content", systemPrompt) })
        for ((role, text) in history) {
            val mappedRole = if (role == "model") "assistant" else "user"
            messages.put(JSONObject().apply { put("role", mappedRole); put("content", text) })
        }
        messages.put(JSONObject().apply { put("role", "user"); put("content", message) })

        val body = JSONObject().apply {
            put("model", "gpt-4o-mini")
            put("messages", messages)
        }
        val request = Request.Builder()
            .url("https://api.openai.com/v1/chat/completions")
            .addHeader("Authorization", "Bearer $apiKey")
            .addHeader("Content-Type", "application/json")
            .post(body.toString().toRequestBody("application/json".toMediaTypeOrNull()))
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) = onError("\u0641\u0634\u0644 \u0627\u0644\u0627\u062A\u0635\u0627\u0644 \u0628ـOpenAI")
            override fun onResponse(call: Call, response: Response) {
                try {
                    val json = JSONObject(response.body?.string() ?: "")
                    if (json.has("error")) {
                        onError(json.getJSONObject("error").optString("message", "\u062E\u0637\u0623 \u063A\u064A\u0631 \u0645\u0639\u0631\u0648\u0641"))
                        return
                    }
                    val reply = json.getJSONArray("choices").getJSONObject(0)
                        .getJSONObject("message").getString("content")
                    onSuccess(reply.trim())
                } catch (e: Exception) {
                    onError("\u062A\u0639\u0630\u0651\u0631 \u0641\u0647\u0645 \u0631\u062F OpenAI")
                }
            }
        })
    }
}

/**
 * "محلي" بالمعنى الواقعي: قواعد أوفلاين ثابتة + خادم احتياطي تسويه بنفسك (ONLINE_CHAT_ENDPOINT).
 * ملاحظة صريحة: هذا ليس نموذج ذكاء اصطناعي حقيقي يشتغل بالكامل على الهاتف — نموذج لغوي محلي
 * حقيقي (زي Llama) يحتاج ذاكرة ومعالج أقوى بكثير من هاتف اقتصادي عادي، وحجم تحميل ضخم (غيغابايتات).
 * هذا الخيار يشتغل بسرعة فورية ومجاني 100%، لكنه محدود لأسئلة بسيطة معروفة مسبقًا فقط.
 */
class LocalProvider(
    private val offlineEndpoint: String,
    private val offlineRules: (String) -> String?
) : AIProvider {
    override val displayName = "LOCAL"
    override fun isConfigured() = true

    override fun ask(
        message: String, history: List<Pair<String, String>>, systemPrompt: String,
        client: OkHttpClient, onSuccess: (String) -> Unit, onError: (String) -> Unit
    ) {
        val ruleReply = offlineRules(message)
        if (ruleReply != null) {
            onSuccess(ruleReply)
            return
        }
        if (offlineEndpoint.isBlank()) {
            onError("\u0645\u0627 \u0641\u0647\u0645\u062A\u0634\u060C \u0648\u0645\u0627\u0641\u064A\u0634 \u062E\u0627\u062F\u0645 \u0645\u062D\u0644\u064A \u0645\u0639\u062F (ONLINE_CHAT_ENDPOINT \u0641\u0627\u0631\u063A)")
            return
        }
        val body = JSONObject().apply { put("message", message) }
            .toString().toRequestBody("application/json".toMediaTypeOrNull())
        val request = Request.Builder().url(offlineEndpoint).post(body).build()
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) = onError("\u0645\u0627 \u0642\u062F\u0631\u062A \u0623\u0648\u0635\u0644 \u0644\u0644\u062E\u0627\u062F\u0645 \u0627\u0644\u0645\u062D\u0644\u064A")
            override fun onResponse(call: Call, response: Response) {
                val raw = response.body?.string() ?: ""
                val reply = try {
                    val json = JSONObject(raw)
                    json.optString("reply").ifBlank { json.optString("response").ifBlank { json.optString("text").ifBlank { raw } } }
                } catch (e: Exception) { raw }
                onSuccess(reply.ifBlank { "\u0645\u0627 \u0631\u062C\u0639 \u0627\u0644\u062E\u0627\u062F\u0645 \u0623\u064A \u0631\u062F" })
            }
        })
    }
}
