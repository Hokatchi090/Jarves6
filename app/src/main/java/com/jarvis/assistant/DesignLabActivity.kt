package com.jarvis.assistant

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.ViewGroup
import android.webkit.JavascriptInterface
import android.webkit.WebSettings
import android.webkit.WebView
import android.widget.FrameLayout
import android.widget.Toast
import java.io.File
import java.io.FileOutputStream

// \u0646\u0634\u0627\u0637 \u064A\u0633\u062A\u0636\u064A\u0641 \u0645\u062E\u062A\u0628\u0631 \u0627\u0644\u062A\u0635\u0645\u064A\u0645 \u0627\u0644\u062B\u0644\u0627\u062B\u064A DESIGN LAB \u062C\u0648\u0627 WebView
// \u064A\u062D\u0645\u0651\u0644 design_lab.html \u0645\u0646 assets \u0648\u064A\u0648\u0641\u0631 \u062C\u0633\u0631 JavaScript \u0644\u062A\u0635\u062F\u064A\u0631 \u0627\u0644\u062A\u0635\u0645\u064A\u0645 \u0644\u0645\u0644\u0641 JSON \u062D\u0642\u064A\u0642\u064A \u0639\u0644\u0649 \u0627\u0644\u062C\u0647\u0627\u0632
class DesignLabActivity : Activity() {

    private lateinit var webView: WebView

    // \u062C\u0633\u0631 JavaScript <-> Kotlin: \u0627\u0644\u0635\u0641\u062D\u0629 \u062A\u0646\u0627\u062F\u064A exportDesign(json) \u0648\u0646\u062D\u0646\u0627 \u0646\u062D\u0641\u0638\u0647 \u0641\u0639\u0644\u064A\u0627\u064B
    inner class WebAppBridge(private val context: Context) {
        @JavascriptInterface
        fun exportDesign(json: String) {
            try {
                val dir = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)
                    ?: context.filesDir
                if (!dir.exists()) dir.mkdirs()
                val fileName = "jarvis_design_${System.currentTimeMillis()}.json"
                val file = File(dir, fileName)
                FileOutputStream(file).use { it.write(json.toByteArray(Charsets.UTF_8)) }

                runOnUiThread {
                    Toast.makeText(
                        context,
                        "\u062A\u0645 \u0627\u0644\u062D\u0641\u0638: ${file.absolutePath}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            } catch (e: Exception) {
                Log.e("DesignLabActivity", "Export failed: ${e.message}")
                runOnUiThread {
                    Toast.makeText(context, "\u0645\u0627 \u0642\u062F\u0631\u062A \u0646\u062D\u0641\u0638 \u0627\u0644\u062A\u0635\u0645\u064A\u0645", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        webView = WebView(this)
        webView.layoutParams = FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )

        val settings: WebSettings = webView.settings
        settings.javaScriptEnabled = true
        settings.domStorageEnabled = true
        settings.allowFileAccess = true
        settings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW

        webView.addJavascriptInterface(WebAppBridge(this), "AndroidBridge")

        setContentView(webView)

        webView.loadUrl("file:///android_asset/design_lab.html")
    }

    override fun onPause() {
        super.onPause()
        webView.onPause()
        webView.pauseTimers()
    }

    override fun onResume() {
        super.onResume()
        webView.resumeTimers()
        webView.onResume()
    }

    override fun onDestroy() {
        webView.clearHistory()
        webView.clearCache(true)
        (webView.parent as? ViewGroup)?.removeView(webView)
        webView.destroy()
        super.onDestroy()
    }
}
