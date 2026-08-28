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

// \u0639\u0645\u064A\u0644 \u0645\u0648\u062D\u0651\u062F \u0644\u0640 Gemini API \u2014 \u064A\u062C\u0645\u0651\u0639 \u0645\u0646\u0637\u0642 5 \u0627\u0633\u062A\u062F\u0639\u0627\u0621\u0627\u062A \u0645\u062A\u0637\u0627\u0628\u0642\u0629 \u0643\u0627\u0646\u062A \u0645\u0643\u0631\u0631\u0629 \u0641\u064A MainActivity.kt
// (askGemini, askGeminiForCode, askGeminiForHologram, fetchAiSuggestions...)
class GeminiClient(private val apiKey: String) {

    private val client = OkHttpClient()

    fun isConfigured(): Boolean = apiKey.isNotBlank()

    // \u0627\u0644\u0637\u0631\u064A\u0642\u0629 \u0627\u0644\u0639\u0627\u0645\u0629: \u062A\u0642\u0628\u0644 \u0645\u062D\u062A\u0648\u0649 \u0645\u062D\u0627\u062F\u062B\u0629 \u0643\u0627\u0645\u0644 (contents) \u0645\u0639 \u062A\u0639\u0644\u064A\u0645\u0629 \u0646\u0638\u0627\u0645 \u0627\u062E\u062A\u064A\u0627\u0631\u064A\u0629 \u2014 \u062A\u0633\u062A\u0639\u0645\u0644 \u0645\u0644\u064A \u0643\u0627\u064A\u0646 \u0630\u0627\u0643\u0631\u0629 \u0645\u062D\u0627\u062F\u062B\u0629 (\u0645\u062B\u0644 askGemini)
    fun generate(
        contents: JSONArray,
        systemInstruction: String? = null,
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        if (!isConfigured()) {
            onError("\u0645\u0627\u0641\u064A\u0634 \u0645\u0641\u062A\u0627\u062D Gemini \u0645\u0636\u0628\u0648\u0637")
            return
        }

        val jsonBody = JSONObject().apply {
            if (systemInstruction != null) {
                put("system_instruction", JSONObject().apply {
                    put("parts", JSONArray().put(JSONObject().apply { put("text", systemInstruction) }))
                })
            }
            put("contents", contents)
        }

        val body = jsonBody.toString().toRequestBody("application/json".toMediaTypeOrNull())
        val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.6-flash:generateContent"
        val request = Request.Builder()
            .url(url)
            .addHeader("Content-Type", "application/json")
            .addHeader("x-goog-api-key", apiKey)
            .post(body)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                onError("\u0645\u0627 \u0642\u062F\u0631\u062A \u0623\u0648\u0635\u0644 \u0644\u0644\u0646\u062A: ${e.message}")
            }

            override fun onResponse(call: Call, response: Response) {
                try {
                    val responseText = response.body?.string() ?: ""
                    val json = JSONObject(responseText)
                    if (json.has("error")) {
                        val errMsg = json.getJSONObject("error").optString("message", "\u062E\u0637\u0623 \u063A\u064A\u0631 \u0645\u0639\u0631\u0648\u0641")
                        onError("\u062E\u0637\u0623 \u0645\u0646 Gemini: $errMsg")
                        return
                    }
                    val reply = json.getJSONArray("candidates")
                        .getJSONObject(0)
                        .getJSONObject("content")
                        .getJSONArray("parts")
                        .getJSONObject(0)
                        .getString("text")
                    onSuccess(reply.trim())
                } catch (e: Exception) {
                    onError("\u062A\u0639\u0630\u0631 \u062A\u062D\u0644\u064A\u0644 \u0631\u062F Gemini: ${e.message}")
                }
            }
        })
    }

    // \u0637\u0631\u064A\u0642\u0629 \u0645\u0628\u0633\u0651\u0637\u0629: \u0633\u0624\u0627\u0644 \u0648\u0627\u062D\u062F \u0628\u0644\u0627 \u0630\u0627\u0643\u0631\u0629 \u0645\u062D\u0627\u062F\u062B\u0629 \u2014 \u062A\u0633\u062A\u0639\u0645\u0644\u0647\u0627 askGeminiForCode/askGeminiForHologram/fetchAiSuggestions
    fun generateSimple(
        prompt: String,
        systemInstruction: String? = null,
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        val contents = JSONArray().put(
            JSONObject().apply {
                put("role", "user")
                put("parts", JSONArray().put(JSONObject().apply { put("text", prompt) }))
            }
        )
        generate(contents, systemInstruction, onSuccess, onError)
    }
}
