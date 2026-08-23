package com.jarvis.assistant

import android.annotation.SuppressLint
import android.app.Activity
import android.os.Bundle
import android.view.ViewGroup
import android.webkit.WebSettings
import android.webkit.WebView
import android.widget.FrameLayout

// \u0634\u0627\u0634\u0629 \u0645\u062A\u0635\u0641\u062D \u0643\u0627\u0645\u0644\u0629\u060C \u062A\u0641\u062A\u062D \u0639\u0646\u062F \u0627\u0644\u0636\u063A\u0637 \u062B\u0644\u0627\u062B \u0645\u0631\u0627\u062A \u0639\u0644\u0649 \u0627\u0644\u0645\u062A\u0635\u0641\u062D \u0627\u0644\u0635\u063A\u064A\u0631
class BrowserFullscreenActivity : Activity() {

    private lateinit var webView: WebView

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
        settings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
        webView.setBackgroundColor(android.graphics.Color.parseColor("#05080A"))

        setContentView(webView)

        val url = intent.getStringExtra("url") ?: "about:blank"
        webView.loadUrl(url)
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
