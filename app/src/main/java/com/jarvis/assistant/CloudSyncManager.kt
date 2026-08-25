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

// \u0645\u0632\u0627\u0645\u0646\u0629 \u0633\u062D\u0627\u0628\u064A\u0629 \u0645\u062C\u0627\u0646\u064A\u0629 \u0645\u0639 Supabase (\u0637\u0628\u0642\u0629 \u0645\u062C\u0627\u0646\u064A\u0629 500 \u0645\u064A\u063A\u0627 \u0628\u0627\u064A\u062A)
// \u064A\u0633\u062A\u0639\u0645\u0644 REST API \u0627\u0644\u0639\u0627\u062F\u064A \u0628\u062F\u0648\u0646 \u0623\u064A SDK \u062B\u0642\u064A\u0644\u0629\u060C \u0646\u0641\u0633 \u0637\u0631\u064A\u0642\u0629 \u0637\u0644\u0628\u0627\u062A Gemini \u0627\u0644\u0645\u0648\u062C\u0648\u062F\u0629 \u0623\u0635\u0644\u0627\u064B
//
// \u0642\u0628\u0644 \u0627\u0644\u0627\u0633\u062A\u0639\u0645\u0627\u0644: \u0623\u0646\u0634\u0626 \u062D\u0633\u0627\u0628 \u0645\u062C\u0627\u0646\u064A \u0641\u064A supabase.com\u060C \u0623\u0646\u0634\u0626 \u0645\u0634\u0631\u0648\u0639\u060C \u0648\u0634\u063A\u0651\u0644 \u0641\u064A \u0642\u0633\u0645 SQL Editor:
//
// create table jarvis_backups (
//   device_id text not null,
//   data_key text not null,
//   data_value text not null,
//   updated_at timestamptz default now(),
//   primary key (device_id, data_key)
// );
// alter table jarvis_backups enable row level security;
// create policy "allow all for now" on jarvis_backups for all using (true) with check (true);
//
// \u0628\u0639\u062F\u0647\u0627 \u062E\u0630 Project URL \u0648 anon public key \u0645\u0646 Settings -> API \u0648\u062D\u0637\u0647\u0645 \u0641\u064A local.properties (SUPABASE_URL, SUPABASE_ANON_KEY)
class CloudSyncManager(private val deviceId: String) {

    private val client = OkHttpClient()

    fun isConfigured(): Boolean {
        return BuildConfig.SUPABASE_URL.isNotBlank() && BuildConfig.SUPABASE_ANON_KEY.isNotBlank()
    }

    // \u064A\u062D\u0641\u0638/\u064A\u062D\u062F\u0651\u062B \u0642\u064A\u0645\u0629 \u0646\u0635\u064A\u0629 \u062A\u062D\u062A \u0645\u0641\u062A\u0627\u062D \u0645\u0639\u064A\u0651\u0646 (upsert)
    fun backup(dataKey: String, dataValue: String, onResult: (Boolean, String?) -> Unit) {
        if (!isConfigured()) {
            onResult(false, "\u0627\u0644\u0633\u062D\u0627\u0628\u0629 \u0627\u0644\u0633\u062D\u0627\u0628\u064A\u0629 \u063A\u064A\u0631 \u0645\u0636\u0628\u0648\u0637\u0629")
            return
        }
        val url = "${BuildConfig.SUPABASE_URL}/rest/v1/jarvis_backups"
        val json = JSONArray().put(
            JSONObject().apply {
                put("device_id", deviceId)
                put("data_key", dataKey)
                put("data_value", dataValue)
            }
        )
        val body = json.toString().toRequestBody("application/json".toMediaTypeOrNull())
        val request = Request.Builder()
            .url(url)
            .addHeader("apikey", BuildConfig.SUPABASE_ANON_KEY)
            .addHeader("Authorization", "Bearer ${BuildConfig.SUPABASE_ANON_KEY}")
            .addHeader("Content-Type", "application/json")
            .addHeader("Prefer", "resolution=merge-duplicates,return=minimal")
            .post(body)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                onResult(false, e.message)
            }

            override fun onResponse(call: Call, response: Response) {
                val success = response.isSuccessful
                val errorMsg = if (!success) response.body?.string() else null
                response.close()
                onResult(success, errorMsg)
            }
        })
    }

    // \u064A\u0633\u062A\u0631\u062C\u0639 \u0642\u064A\u0645\u0629 \u0645\u062E\u0632\u0651\u0646\u0629 \u0633\u0627\u0628\u0642\u0627\u064B \u062A\u062D\u062A \u0645\u0641\u062A\u0627\u062D \u0645\u0639\u064A\u0651\u0646
    fun restore(dataKey: String, onResult: (Boolean, String?) -> Unit) {
        if (!isConfigured()) {
            onResult(false, "\u0627\u0644\u0633\u062D\u0627\u0628\u0629 \u0627\u0644\u0633\u062D\u0627\u0628\u064A\u0629 \u063A\u064A\u0631 \u0645\u0636\u0628\u0648\u0637\u0629")
            return
        }
        val url = "${BuildConfig.SUPABASE_URL}/rest/v1/jarvis_backups" +
            "?device_id=eq.$deviceId&data_key=eq.$dataKey&select=data_value"
        val request = Request.Builder()
            .url(url)
            .addHeader("apikey", BuildConfig.SUPABASE_ANON_KEY)
            .addHeader("Authorization", "Bearer ${BuildConfig.SUPABASE_ANON_KEY}")
            .get()
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                onResult(false, e.message)
            }

            override fun onResponse(call: Call, response: Response) {
                try {
                    val text = response.body?.string() ?: "[]"
                    response.close()
                    val arr = JSONArray(text)
                    if (arr.length() == 0) {
                        onResult(false, "\u0645\u0627\u0641\u064A\u0634 \u0646\u0633\u062E\u0629 \u0645\u062D\u0641\u0648\u0638\u0629")
                    } else {
                        val value = arr.getJSONObject(0).getString("data_value")
                        onResult(true, value)
                    }
                } catch (e: Exception) {
                    onResult(false, e.message)
                }
            }
        })
    }
}
