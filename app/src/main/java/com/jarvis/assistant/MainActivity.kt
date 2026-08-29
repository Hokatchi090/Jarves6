package com.jarvis.assistant

import android.Manifest
import android.telephony.SmsManager
import android.provider.Telephony
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.camera2.CameraManager
import android.media.MediaPlayer
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.graphics.drawable.GradientDrawable
import android.location.Location
import android.location.LocationManager
import android.provider.MediaStore
import android.webkit.JavascriptInterface
import android.webkit.WebSettings
import android.webkit.WebView
import android.speech.RecognizerIntent
import android.speech.RecognitionListener
import android.speech.SpeechRecognizer
import android.animation.ObjectAnimator
import android.animation.PropertyValuesHolder
import android.app.Dialog
import android.widget.FrameLayout
import android.graphics.Color
import android.graphics.Typeface
import android.view.Choreographer
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import net.objecthunter.exp4j.ExpressionBuilder
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.*
import android.provider.ContactsContract
import android.provider.AlarmClock
import android.app.SearchManager
import android.os.BatteryManager
import android.media.AudioManager
import android.net.Uri
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt
import org.json.JSONArray
import org.json.JSONObject

class MainActivity : AppCompatActivity(), TextToSpeech.OnInitListener {

    private lateinit var tts: TextToSpeech
    private lateinit var logText: TextView
    private lateinit var statusText: TextView
    private var mediaPlayer: MediaPlayer? = null
    private var flashOn = false
    private var continuousMode = false
    private var speechRecognizer: SpeechRecognizer? = null
    private var currentLangCode = "en"
    // \u062A\u062A\u0628\u0651\u0639 \u0645\u062D\u0627\u0648\u0644\u0627\u062A \u0625\u0639\u0627\u062F\u0629 \u0627\u0644\u0627\u0633\u062A\u0645\u0627\u0639 \u0628\u0639\u062F \u0641\u0634\u0644 \u0627\u0644\u062A\u0639\u0631\u0641 \u0627\u0644\u0635\u0648\u062A\u064A (\u062D\u062F \u0623\u0642\u0635\u0649 3 \u0645\u0639 \u062A\u0623\u062E\u064A\u0631 \u062A\u0635\u0627\u0639\u062F\u064A)
    private var listenRetryCount = 0
    private val maxListenRetries = 3
    private val retryHandler = Handler(Looper.getMainLooper())

    // ---- \u0648\u0627\u062C\u0647\u0629 \u0627\u0644\u0642\u0627\u0626\u0645\u0629 \u0627\u0644\u062C\u0627\u0646\u0628\u064A\u0629 \u0648\u0627\u0644\u0634\u0627\u0634\u0627\u062A ----
    private var sidebarVisible = true
    private val clockHandler = Handler(Looper.getMainLooper())
    private lateinit var clockRunnable: Runnable
    private lateinit var miniMapView: WebView
    private lateinit var miniBrowserView: WebView
    private var miniMapEnlarged = false
    private var miniBrowserEnlarged = false
    private var lastKnownLat = 0.0
    private var lastKnownLon = 0.0
    private val fieldNotebook by lazy { FieldNotebookManager(this) }
    private val geoCompass by lazy { GeoCompassHelper(this) }
    private var lastBrowserUrl = "https://www.google.com"
    private var lastQueuedUtteranceId: String? = null

    // ---- \u0630\u0627\u0643\u0631\u0629 \u0627\u0644\u0645\u062D\u0627\u062F\u062B\u0629 \u0645\u0639 Gemini: \u0646\u062D\u062A\u0641\u0638 \u0628\u0622\u062E\u0631 \u062A\u0628\u0627\u062F\u0644 \u0623\u0633\u0626\u0644\u0629/\u0631\u062F\u0648\u062F \u0628\u0627\u0634 \u064A\u0641\u0647\u0645 \u0627\u0644\u0633\u064A\u0627\u0642 \u0648\u0645\u0627 \u064A\u0646\u0633\u0627\u0634\u064A \u0641\u064A \u0643\u0644 \u0631\u0633\u0627\u0644\u0629
    // \u0643\u0644 \u0639\u0646\u0635\u0631 = "role" ("user" \u0623\u0648 "model") \u0645\u0639 \u0627\u0644\u0646\u0635
    private val conversationHistory = mutableListOf<Pair<String, String>>()
    private val maxHistoryTurns = 12 // \u064A\u0639\u0646\u064A 6 \u062A\u0628\u0627\u062F\u0644\u0627\u062A (\u0633\u0624\u0627\u0644 + \u062C\u0648\u0627\u0628) \u0628\u0627\u0634 \u0645\u0627 \u064A\u0643\u0628\u0631\u0634 \u0627\u0644\u0637\u0644\u0628

    // ---- \u0645\u0631\u0627\u0642\u0628 \u0627\u0644\u0623\u062F\u0627\u0621: \u064A\u0639\u062F FPS \u0627\u0644\u062D\u0642\u064A\u0642\u064A\u0629 \u0639\u0628\u0631 Choreographer \u0648\u064A\u0639\u0631\u0636 \u0627\u0633\u062A\u0647\u0644\u0627\u0643 \u0627\u0644\u0630\u0627\u0643\u0631\u0629. \u064A\u0634\u062A\u063A\u0644 \u0641\u0642\u0637 \u0641\u064A Debug builds
    private var frameCount = 0
    private var lastFpsTimestamp = 0L
    private var perfMonitorRunning = false
    private val frameCallback = object : Choreographer.FrameCallback {
        override fun doFrame(frameTimeNanos: Long) {
            frameCount++
            val now = System.currentTimeMillis()
            if (lastFpsTimestamp == 0L) lastFpsTimestamp = now
            if (now - lastFpsTimestamp >= 1000L) {
                updatePerfMonitorText(frameCount)
                frameCount = 0
                lastFpsTimestamp = now
            }
            if (perfMonitorRunning) {
                Choreographer.getInstance().postFrameCallback(this)
            }
        }
    }

    // \u062C\u0633\u0631 JavaScript <-> Kotlin \u062E\u0627\u0635 \u0628\u0627\u0644\u0645\u062A\u0635\u0641\u062D \u0627\u0644\u0635\u063A\u064A\u0631
    inner class BrowserBridgeInterface {
        @JavascriptInterface
        fun onDoubleTap() {
            runOnUiThread { toggleMiniBrowserSize() }
        }

        @JavascriptInterface
        fun onTripleTap() {
            runOnUiThread { openFullscreenBrowser() }
        }
    }

    // \u062C\u0633\u0631 JavaScript <-> Kotlin \u062E\u0627\u0635 \u0628\u0627\u0644\u062E\u0631\u064A\u0637\u0629 \u0627\u0644\u0635\u063A\u064A\u0631\u0629: \u064A\u0633\u062A\u0642\u0628\u0644 \u0625\u0634\u0627\u0631\u0627\u062A \u0627\u0644\u0636\u063A\u0637 \u0627\u0644\u0645\u0632\u062F\u0648\u062C \u0648\u0627\u0644\u062B\u0644\u0627\u062B\u064A
    inner class MapBridgeInterface {
        @JavascriptInterface
        fun onDoubleTap() {
            runOnUiThread { toggleMiniMapSize() }
        }

        @JavascriptInterface
        fun onTripleTap() {
            runOnUiThread { openFullscreenMap() }
        }

        // \u064A\u0646\u0627\u062F\u064A\u0647 mini_map.html \u0645\u0631\u0629 \u0648\u0627\u062D\u062F\u0629 \u0641\u0642\u0637 \u0623\u0648\u0644 \u0645\u0627 \u062A\u0646\u062C\u062D \u0627\u0644\u062E\u0631\u064A\u0637\u0629 \u0641\u064A \u062A\u062D\u062F\u064A\u062F \u0627\u0644\u0645\u0648\u0642\u0639 ("online" = \u062E\u0631\u064A\u0637\u0629 \u062D\u0642\u064A\u0642\u064A\u0629\u060C "offline" = \u0631\u0633\u0645 \u0628\u062F\u064A\u0644 \u0645\u062D\u0644\u064A)
        @JavascriptInterface
        fun onStatusChange(status: String) {
            runOnUiThread {
                if (status == "online") {
                    respond("\u0627\u0644\u062E\u0631\u064A\u0637\u0629 \u0645\u062A\u0635\u0644\u0629 \u0628\u0627\u0644\u0625\u0646\u062A\u0631\u0646\u062A \u0648\u062A\u0648\u0631\u064A \u0645\u0648\u0642\u0639\u0643 \u0627\u0644\u062D\u0642\u064A\u0642\u064A")
                } else {
                    respond("\u0627\u0644\u062E\u0631\u064A\u0637\u0629 \u0645\u0627\u0644\u0642\u0627\u062A\u0634 \u0625\u0646\u062A\u0631\u0646\u062A\u060C \u0631\u0627\u0647\u064A \u062A\u0634\u062A\u063A\u0644 \u0628\u0648\u0636\u0639 \u0627\u0644\u0623\u0648\u0641\u0644\u0627\u064A\u0646")
                }
            }
        }
    }
    private var pulseAnimator: ObjectAnimator? = null
    private lateinit var jarvisDial: JarvisDialView
    private var userName: String = "youcef"
    private var userEmail: String = "youcefakram4@gmail.com"
    private var userPhone: String = "0775540495"
    private var lectureMode = false
    private var lectureBuffer = StringBuilder()
    private val client = OkHttpClient()
    private val playlistManager by lazy { PlaylistManager(this) }
    private val notesManager by lazy { NotesManager(this) }
    private val bluetoothHelper by lazy { BluetoothManagerHelper(this) }
    private val nfcHelper by lazy { NfcHelper(this) }
    private val securityScanner by lazy { SecurityScanner(this) }
    private var defenseModeActive = false
    private val irRemote by lazy { IrRemoteHelper(this) }
    private val usageTracker by lazy { UsagePatternTracker(this) }
    private val cloudSync by lazy {
        val deviceId = android.provider.Settings.Secure.getString(contentResolver, android.provider.Settings.Secure.ANDROID_ID)
        CloudSyncManager(deviceId ?: "unknown_device")
    }

    // ---- \u0645\u0641\u062A\u0627\u062D Gemini: \u064A\u062C\u064A \u0645\u0646 BuildConfig (\u0645\u0635\u062F\u0631\u0647 local.properties \u0623\u0648 GitHub Secrets) ----
    // \u0644\u0627 \u062A\u062D\u0637 \u0627\u0644\u0645\u0641\u062A\u0627\u062D \u0647\u0646\u0627 \u0623\u0628\u062F\u0627\u064B. \u0634\u0648\u0641 \u0645\u0644\u0641 local.properties.example
    private val GEMINI_API_KEY = "AQ.Ab8RN6I6vqRW4nOUpgsViYy8XTMZzyWDagN2VNz8NPXqBvK1fw"
    private val geminiClient by lazy { GeminiClient(GEMINI_API_KEY) }

    // ---- \u0645\u0641\u062A\u0627\u062D Google Maps: \u0646\u0641\u0633 \u0627\u0644\u0645\u0628\u062F\u0623\u060C \u064A\u062C\u064A \u0645\u0646 BuildConfig ----
    private val GOOGLE_MAPS_API_KEY = BuildConfig.GOOGLE_MAPS_API_KEY

    companion object {
        private const val REQ_SPEECH = 100
        private const val REQ_PERMISSIONS = 200
        private const val REQ_CONTACTS = 300
        private const val REQ_LOCATION = 400
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        jarvisDial = findViewById(R.id.jarvisDial)
        setupModuleMenu()
        setupSidebarUi()

        statusText = findViewById(R.id.statusText)
        logText = findViewById(R.id.logText)
        tts = TextToSpeech(this, this)

        userName = getSharedPreferences("jarvis_prefs", Context.MODE_PRIVATE)
            .getString("user_name", "") ?: ""
        userEmail = getSharedPreferences("jarvis_prefs", Context.MODE_PRIVATE)
            .getString("user_email", "") ?: ""
        userPhone = getSharedPreferences("jarvis_prefs", Context.MODE_PRIVATE)
            .getString("user_phone", "") ?: ""
        if (userName.isNotBlank()) {
            log("\u062C\u0627\u0631\u0641\u0633: \u0623\u0647\u0644\u0627 ${userName}\u060C \u0645\u0628\u0633\u0648\u0637 \u0625\u0646\u0643 \u0631\u062C\u0639\u062A")
        }

        requestNeededPermissions()

        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
        speechRecognizer?.setRecognitionListener(recognitionListener)

        findViewById<View>(R.id.micButton).setOnClickListener {
            toggleContinuousMode()
        }

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
            == PackageManager.PERMISSION_GRANTED
        ) {
            enableContinuousMode(startImmediately = false)
        }
    }

    private fun toggleContinuousMode() {
        if (continuousMode) {
            disableContinuousMode()
        } else {
            enableContinuousMode()
        }
    }

    private fun enableContinuousMode(startImmediately: Boolean = true) {
        continuousMode = true
        val container = findViewById<View>(R.id.micButton)
        findViewById<TextView>(R.id.micIcon).text = "\u23F9\uFE0F"
        statusText.text = "\u0628\u0633\u0645\u0639\u0643... \u0642\u0648\u0644 \"\u062C\u0627\u0631\u0641\u0633\""
        findViewById<View>(R.id.statusDot).setBackgroundResource(R.drawable.status_dot)
        findViewById<View>(R.id.statusDot).alpha = 1f
        startPulseAnimation(container)
        if (startImmediately) startListening()
    }

    private fun disableContinuousMode() {
        continuousMode = false
        val container = findViewById<View>(R.id.micButton)
        findViewById<TextView>(R.id.micIcon).text = "\uD83C\uDF99\uFE0F"
        statusText.text = "\u062C\u0627\u0647\u0632 \u0644\u0644\u0627\u0633\u062A\u0645\u0627\u0639"
        findViewById<View>(R.id.statusDot).alpha = 0.3f
        stopPulseAnimation(container)
        speechRecognizer?.stopListening()
    }

    private fun startPulseAnimation(view: View) {
        pulseAnimator?.cancel()
        pulseAnimator = ObjectAnimator.ofPropertyValuesHolder(
            view,
            PropertyValuesHolder.ofFloat(View.SCALE_X, 1f, 1.12f),
            PropertyValuesHolder.ofFloat(View.SCALE_Y, 1f, 1.12f)
        ).apply {
            duration = 700
            repeatMode = ObjectAnimator.REVERSE
            repeatCount = ObjectAnimator.INFINITE
            start()
        }
    }

    private fun stopPulseAnimation(view: View) {
        pulseAnimator?.cancel()
        view.scaleX = 1f
        view.scaleY = 1f
    }

    // \u0645\u0644\u0627\u062D\u0638\u0629: JarvisDialView \u0627\u0644\u062C\u062F\u064A\u062F \u064A\u062D\u0631\u0643 \u0646\u0641\u0633\u0647 \u062F\u0627\u062E\u0644\u064A\u064B\u0627 \u0639\u0628\u0631 postInvalidateOnAnimation()\u060C \u0641\u0645\u0627 \u0639\u0627\u062F \u0641\u064A\u0647 \u062D\u0627\u062C\u0629 \u0644\u062F\u0648\u0627\u0644 \u062A\u062F\u0648\u064A\u0631 \u062E\u0627\u0631\u062C\u064A\u0629

    // \u062A\u062E\u062A\u0627\u0631 \u0623\u0642\u0631\u0628 \u0635\u0648\u062A \u0631\u062C\u0627\u0644\u064A \u0645\u062A\u0648\u0641\u0631 \u0644\u0644\u063A\u0629 \u0645\u0639\u064A\u0646\u0629 \u0639\u0644\u0649 \u0645\u062D\u0631\u0643 TTS. \u064A\u0637\u0628\u0651\u0642 \u0628\u0639\u062F \u0643\u0644 \u062A\u063A\u064A\u064A\u0631 \u0644\u063A\u0629 \u0639\u0634\u0627\u0646 \u0627\u0644\u0635\u0648\u062A \u064A\u0628\u0642\u0649 \u0631\u062C\u0627\u0644\u064A \u0641\u064A \u0643\u0644 \u0627\u0644\u0644\u063A\u0627\u062A\u060C \u0645\u0627\u0634\u064A \u0628\u0627\u0644\u0625\u0646\u062C\u0644\u064A\u0632\u064A\u0629 \u0628\u0631\u0643 \u0641\u0642\u0637
    private fun applyMaleVoiceForCurrentLanguage() {
        val langVoices = tts.voices?.filter { it.locale.language == tts.language.language }
        // \u0646\u0641\u0644\u062A\u0631 \u0623\u0648\u0644\u0627\u064B \u0639\u0644\u0649 \u0627\u0644\u0623\u0633\u0645\u0627\u0621 \u0627\u0644\u0644\u064A \u0641\u064A\u0647\u0627 \u0625\u0634\u0627\u0631\u0629 \u0648\u0627\u0636\u062D\u0629 \u0644\u0644\u0630\u0643\u0648\u0631\u0629\u060C \u0648\u0646\u0631\u062A\u0651\u0628\u0647\u0645 \u062D\u0633\u0628 \u0627\u0644\u062C\u0648\u062F\u0629 (\u0623\u0639\u0644\u0649 \u062C\u0648\u062F\u0629 = \u0635\u0648\u062A \u0623\u0637\u0628\u064A\u0639\u064A \u0623\u0643\u062B\u0631)
        val maleCandidates = langVoices?.filter { voice ->
            val n = voice.name.lowercase(Locale.ROOT)
            (n.contains("male") && !n.contains("female")) ||
                    n.contains("-d-") || n.contains("#male")
        }?.sortedByDescending { it.quality }

        val bestVoice = maleCandidates?.firstOrNull()
            // \u0644\u0648 \u0645\u0627\u0644\u0642\u064A\u0646\u0627\u0634 \u0635\u0648\u062A \u0645\u0643\u062A\u0648\u0628 \u0639\u0644\u064A\u0647 "male" \u0635\u0631\u064A\u062D\u060C \u0646\u062E\u0644\u064A \u0623\u0639\u0644\u0649 \u062C\u0648\u062F\u0629 \u0645\u062A\u0648\u0641\u0631\u0629 \u0648\u0646\u0639\u062A\u0645\u062F \u0639\u0644\u0649 \u0627\u0644\u0637\u0628\u0642\u0629 \u0627\u0644\u0645\u0646\u062E\u0641\u0636\u0629 \u0628\u0627\u0634 \u062A\u0628\u0627\u0646 \u0623\u0642\u0631\u0628 \u0644\u0644\u0631\u062C\u0627\u0644\u064A
            ?: langVoices?.sortedByDescending { it.quality }?.firstOrNull()

        if (bestVoice != null) {
            tts.voice = bestVoice
        }
        // \u0646\u062E\u0641\u0636 \u0627\u0644\u0637\u0628\u0642\u0629 \u0648\u0646\u0632\u064A\u062F \u0634\u0648\u064A \u0641\u064A \u0627\u0644\u0628\u0637\u0621 \u0628\u0627\u0634 \u0627\u0644\u0635\u0648\u062A \u064A\u0628\u0627\u0646 \u0623\u0647\u062F\u0627 \u0648\u0623\u0642\u0631\u0628 \u0644\u0637\u0627\u0628\u0639 "\u062C\u0627\u0631\u0641\u0633" \u0645\u0627\u0634\u064A \u0631\u0648\u0628\u0648\u062A\u064A
        tts.setPitch(if (maleCandidates?.isNotEmpty() == true) 0.85f else 0.75f)
        tts.setSpeechRate(0.92f)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts.language = Locale.ENGLISH
            tts.setPitch(0.6f)
            tts.setSpeechRate(0.88f)
            applyMaleVoiceForCurrentLanguage()

            tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {
                    runOnUiThread {
                        if (::jarvisDial.isInitialized) {
                            jarvisDial.setSpeaking(true)
                        }
                    }
                }
                override fun onDone(utteranceId: String?) {
                    runOnUiThread {
                        if (utteranceId != null && utteranceId != lastQueuedUtteranceId) return@runOnUiThread
                        if (::jarvisDial.isInitialized) {
                            jarvisDial.setSpeaking(false)
                        }
                        if (continuousMode && !lectureMode) startListening()
                    }
                }
                @Deprecated("Deprecated in Java")
                override fun onError(utteranceId: String?) {
                    runOnUiThread {
                        if (::jarvisDial.isInitialized) {
                            jarvisDial.setSpeaking(false)
                        }
                        if (continuousMode && !lectureMode) startListening()
                    }
                }
            })

            val greeting = if (userName.isNotBlank()) {
                "Hello $userName, Jarvis at your service, what can I do today?"
            } else {
                "Jarvis at your service, what can I do today?"
            }
            val suggestion = usageTracker.getTopSuggestionForNow()
            val fullGreeting = if (suggestion != null) {
                "$greeting \u0628\u0627\u0644\u0645\u0646\u0627\u0633\u0628\u0629\u060C \u0639\u0627\u062F\u062A\u0643 \u062A\u0642\u0648\u0644 \"$suggestion\" \u0641\u064A \u0647\u0630\u0627 \u0627\u0644\u0648\u0642\u062A\u060C \u062A\u062D\u0628 \u0646\u062F\u064A\u0631\u0647\u0627\u061F"
            } else {
                greeting
            }
            respond(fullGreeting)
        }
    }

    private fun requestNeededPermissions() {
        val needed = mutableListOf<String>()
        for (p in listOf(
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.CAMERA,
            Manifest.permission.READ_CONTACTS,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.CALL_PHONE,
            Manifest.permission.READ_SMS,
            Manifest.permission.SEND_SMS
        )) {
            if (ActivityCompat.checkSelfPermission(this, p) != PackageManager.PERMISSION_GRANTED) {
                needed.add(p)
            }
        }
        if (Build.VERSION.SDK_INT >= 33 &&
            ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            needed.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        if (needed.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, needed.toTypedArray(), REQ_PERMISSIONS)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQ_PERMISSIONS) {
            val audioIndex = permissions.indexOf(Manifest.permission.RECORD_AUDIO)
            if (audioIndex != -1 && grantResults.getOrNull(audioIndex) == PackageManager.PERMISSION_GRANTED &&
                !continuousMode
            ) {
                enableContinuousMode()
            }
            val locationIndex = permissions.indexOf(Manifest.permission.ACCESS_FINE_LOCATION)
            if (locationIndex != -1 && grantResults.getOrNull(locationIndex) == PackageManager.PERMISSION_GRANTED) {
                fetchAndShowLocation()
            }
        }
        if (requestCode == REQ_LOCATION) {
            val granted = grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED
            if (granted) {
                fetchAndShowLocation()
            } else {
                findViewById<TextView>(R.id.mapStatus).text = "\u0627\u0644\u0635\u0644\u0627\u062D\u064A\u0629 \u0645\u0631\u0641\u0648\u0636\u0629"
            }
        }
    }

    private fun startListening() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        intent.putExtra(
            RecognizerIntent.EXTRA_LANGUAGE,
            if (currentLangCode == "ar") "ar-DZ" else currentLangCode
        )
        intent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        // \u0646\u0632\u064A\u062F \u0645\u062F\u0629 \u0627\u0644\u0635\u0645\u062A \u0627\u0644\u0645\u0633\u0645\u0648\u062D \u0628\u064A\u0647\u0627 \u0642\u0628\u0644 \u0645\u0627 \u064A\u0639\u062A\u0628\u0631 \u0627\u0644\u062C\u0647\u0627\u0632 \u0623\u0646\u0643 \u0643\u0645\u0644\u062A \u2014 \u0647\u0630\u0627 \u064A\u0645\u0646\u0639 \u0627\u0646\u0642\u0637\u0627\u0639 \u0627\u0644\u0627\u0633\u062A\u0645\u0627\u0639 \u0645\u0628\u0643\u0651\u0631 \u0623\u062B\u0646\u0627\u0621 \u0627\u0644\u062A\u0641\u0643\u064A\u0631
        intent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 2500L)
        intent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 2000L)
        intent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 15000L)
        // \u0646\u0637\u0644\u0628 \u0639\u062F\u0629 \u0627\u062D\u062A\u0645\u0627\u0644\u0627\u062A \u0628\u062F\u0644 \u0648\u0627\u062D\u062F\u060C \u0647\u0630\u0627 \u064A\u0632\u064A\u062F \u0641\u0631\u0635\u0629 \u0641\u0647\u0645 \u0627\u0644\u0623\u0645\u0631 \u0644\u0648 \u0627\u0644\u0623\u0641\u0636\u0644 \u0645\u0627\u0637\u0627\u0628\u0642\u0634 \u062D\u0631\u0641\u064A\u0627\u064B
        intent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 5)
        try {
            speechRecognizer?.startListening(intent)
        } catch (e: Exception) {
            continuousMode = false
            val container = findViewById<View>(R.id.micButton)
            findViewById<TextView>(R.id.micIcon).text = "\uD83C\uDF99\uFE0F"
            statusText.text = "\u062C\u0627\u0647\u0632 \u0644\u0644\u0627\u0633\u062A\u0645\u0627\u0639"
            stopPulseAnimation(container)
            log("\u0645\u0627 \u0642\u062F\u0631\u062A \u0623\u0628\u0644\u0634 \u0627\u0644\u0627\u0633\u062A\u0645\u0627\u0639")
        }
    }

    private val recognitionListener = object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) {}
        override fun onBeginningOfSpeech() {}
        override fun onRmsChanged(rmsdB: Float) {
            val level = ((rmsdB + 2f) / 12f).coerceIn(0f, 1f)
            runOnUiThread {
                if (::jarvisDial.isInitialized) {
                    jarvisDial.setVoiceLevel(level)
                }
            }
        }
        override fun onBufferReceived(buffer: ByteArray?) {}
        override fun onEndOfSpeech() {}

        override fun onError(error: Int) {
            // \u0625\u0639\u0627\u062F\u0629 \u0645\u062D\u0627\u0648\u0644\u0629 \u0645\u062D\u062F\u0648\u062F\u0629 (3 \u0645\u0631\u0627\u062A \u0623\u0642\u0635\u0649) \u0645\u0639 \u062A\u0623\u062E\u064A\u0631 \u062A\u0635\u0627\u0639\u062F\u064A \u0628\u062F\u0644 \u0645\u062D\u0627\u0648\u0644\u0629 \u0641\u0648\u0631\u064A\u0629 \u0642\u062F \u062A\u0647\u0631\u0633 \u0627\u0644\u0628\u0637\u0627\u0631\u064A\u0629
            if (!continuousMode) return

            listenRetryCount++
            if (listenRetryCount > maxListenRetries) {
                listenRetryCount = 0
                continuousMode = false
                val container = findViewById<View>(R.id.micButton)
                findViewById<TextView>(R.id.micIcon).text = "\uD83C\uDF99\uFE0F"
                stopPulseAnimation(container)
                statusText.text = "\u062C\u0627\u0647\u0632 \u0644\u0644\u0627\u0633\u062A\u0645\u0627\u0639"
                log("\u062A\u0648\u0642\u0641 \u0627\u0644\u0627\u0633\u062A\u0645\u0627\u0639 \u0627\u0644\u0645\u0633\u062A\u0645\u0631 \u0628\u0639\u062F $maxListenRetries \u0645\u062D\u0627\u0648\u0644\u0627\u062A \u0641\u0627\u0634\u0644\u0629 (\u0631\u0645\u0632 \u0627\u0644\u062E\u0637\u0623: $error)")
                return
            }

            val delayMs = 500L * listenRetryCount
            retryHandler.postDelayed({
                if (continuousMode) startListening()
            }, delayMs)
        }

        override fun onResults(resultsBundle: Bundle?) {
            listenRetryCount = 0
            val matches = resultsBundle?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            val spoken = matches?.firstOrNull()?.trim() ?: ""
            handleSpeechResult(spoken)
        }

        override fun onPartialResults(partialResults: Bundle?) {}
        override fun onEvent(eventType: Int, params: Bundle?) {}
    }

    private fun handleSpeechResult(spoken: String) {
        if (lectureMode) {
            if (spoken.contains("\u0648\u0642\u0641 \u0627\u0644\u0645\u062D\u0627\u0636\u0631\u0629") || spoken.contains("\u062E\u0644\u0635\u062A \u0627\u0644\u0645\u062D\u0627\u0636\u0631\u0629") ||
                spoken.contains("\u0627\u0646\u0647\u064A \u0627\u0644\u0645\u062D\u0627\u0636\u0631\u0629")
            ) {
                stopLectureModeAndSummarize()
            } else if (spoken.isNotBlank()) {
                lectureBuffer.append(spoken).append(". ")
            }
            if (continuousMode) startListening()
            return
        }

        if (continuousMode) {
            val lower = spoken.lowercase(Locale.getDefault())
            val wakeIndex = when {
                spoken.contains("\u062C\u0627\u0631\u0641\u0633") -> spoken.indexOf("\u062C\u0627\u0631\u0641\u0633").let { it + "\u062C\u0627\u0631\u0641\u0633".length }
                lower.contains("jarvis") -> lower.indexOf("jarvis") + "jarvis".length
                spoken.contains("\u0434\u0436\u0430\u0440\u0432\u0438\u0441") -> spoken.indexOf("\u0434\u0436\u0430\u0440\u0432\u0438\u0441") + "\u0434\u0436\u0430\u0440\u0432\u0438\u0441".length
                spoken.contains("\u8D3E\u7EF4\u65AF") -> spoken.indexOf("\u8D3E\u7EF4\u65AF") + "\u8D3E\u7EF4\u65AF".length
                else -> -1
            }
            if (wakeIndex != -1) {
                val commandOnly = spoken.substring(wakeIndex.coerceAtMost(spoken.length)).trim()
                if (commandOnly.isNotBlank()) {
                    handleCommand(commandOnly)
                } else if (continuousMode) {
                    startListening()
                }
            } else {
                startListening()
            }
        } else if (spoken.isNotBlank()) {
            handleCommand(spoken)
        }
    }

    // ---------------- Command routing ----------------

    private fun handleCommand(text: String) {
        try {
            commandRouter.route(text)
        } catch (e: Exception) {
            log("\u062E\u0637\u0623 \u062F\u0627\u062E\u0644\u064A: ${e.message}")
            respond("\u0645\u0627 \u0641\u0647\u0645\u062A\u0634")
        }
    }

    // \u064A\u0648\u062D\u0651\u062F \u0623\u0634\u0643\u0627\u0644 \u0627\u0644\u062D\u0631\u0648\u0641 \u0627\u0644\u0645\u062A\u0642\u0627\u0631\u0628\u0629 (\u0623/\u0625/\u0622 -> \u0627\u060C \u0649 -> \u064A) \u0644\u0623\u0646 \u0627\u0644\u062A\u0639\u0631\u0641 \u0627\u0644\u0635\u0648\u062A\u064A \u0642\u062F \u064A\u0631\u062C\u0639 \u0635\u064A\u063A\u0627\u064B \u0645\u062E\u062A\u0644\u0641\u0629 \u0644\u0646\u0641\u0633 \u0627\u0644\u0643\u0644\u0645\u0629
    private fun normalizeArabic(text: String): String {
        return text
            .replace('\u0623', '\u0627')
            .replace('\u0625', '\u0627')
            .replace('\u0622', '\u0627')
            .replace('\u0649', '\u064A')
            .replace(Regex("[\u064B-\u0652]"), "")
    }

    private fun handleCommandInternal(text: String) {
        val cmd = normalizeArabic(text.lowercase(Locale("ar")).trim())
        usageTracker.recordCommand(cmd)

        when {
            // \u0646\u0633\u064A\u0627\u0646 \u0630\u0627\u0643\u0631\u0629 \u0627\u0644\u0645\u062D\u0627\u062F\u062B\u0629 \u0645\u0639 Gemini \u0645\u0646 \u063A\u064A\u0631 \u0645\u0627 \u0646\u0639\u064A\u062F \u062A\u0634\u063A\u064A\u0644 \u0627\u0644\u062A\u0637\u0628\u064A\u0642
            cmd.contains("\u0627\u0646\u0633\u0649 \u0643\u0644\u0627\u0645\u064A") || cmd.contains("\u0627\u0645\u0633\u062D \u0627\u0644\u0630\u0627\u0643\u0631\u0629") ||
                    cmd.contains("forget everything") || cmd.contains("clear memory") -> {
                conversationHistory.clear()
                respond("\u0645\u0627\u0634\u064A \u0645\u0634\u0643\u0644\u0629\u060C \u0646\u0633\u064A\u062A \u0643\u0644 \u0634\u064A \u0645\u0646 \u0647\u0627\u062F \u0627\u0644\u0645\u062D\u0627\u062F\u062B\u0629")
            }
            // \u0627\u062E\u062A\u0635\u0627\u0631 \u0644\u0648\u062D\u0629 \u0625\u0639\u062F\u0627\u062F\u0627\u062A \u0627\u0644\u0648\u0627\u064A \u0641\u0627\u064A (Android 10+)\u060C \u0645\u0627\u0634\u064A \u062A\u0634\u063A\u064A\u0644/\u0625\u0637\u0641\u0627\u0621 \u0645\u0628\u0627\u0634\u0631 \u0644\u0623\u0646 \u0642\u0648\u0642\u0644 \u0645\u0627\u0646\u0639\u062A\u0647\u0627 \u0644\u0623\u0633\u0628\u0627\u0628 \u062E\u0635\u0648\u0635\u064A\u0629\u060C \u0628\u0633 \u0647\u0630\u0627 \u0623\u0633\u0631\u0639 \u0637\u0631\u064A\u0642\u0629 \u0645\u062A\u0627\u062D\u0629
            cmd.contains("\u0627\u0641\u062A\u062D \u0627\u0644\u0648\u0627\u064A \u0641\u0627\u064A") || cmd.contains("\u0627\u0639\u062F\u0627\u062F\u0627\u062A \u0627\u0644\u0648\u0627\u064A \u0641\u0627\u064A") || cmd.contains("open wifi") -> {
                try {
                    startActivity(Intent(android.provider.Settings.Panel.ACTION_INTERNET_CONNECTIVITY))
                    respond("\u062E\u0644\u064A\u0646\u064A \u0646\u0641\u062A\u062D\u0644\u0643 \u0625\u0639\u062F\u0627\u062F\u0627\u062A \u0627\u0644\u0634\u0628\u0643\u0629")
                } catch (e: Exception) {
                    startActivity(Intent(android.provider.Settings.ACTION_WIFI_SETTINGS))
                    respond("\u062E\u0644\u064A\u0646\u064A \u0646\u0641\u062A\u062D\u0644\u0643 \u0625\u0639\u062F\u0627\u062F\u0627\u062A \u0627\u0644\u0648\u0627\u064A \u0641\u0627\u064A")
                }
            }
            cmd.contains("\u0627\u0628\u062F\u0627 \u0645\u062D\u0627\u0636\u0631\u0629") || cmd.contains("\u0627\u0628\u062F\u0627 \u0645\u062D\u0627\u0636\u0631\u0629") ||
                    cmd.contains("\u0633\u062C\u0644 \u0645\u062D\u0627\u0636\u0631\u0629") -> {
                startLectureMode()
            }
            cmd.contains("\u0627\u0633\u0645\u064A ") -> {
                val name = extractNameAfter(cmd, "\u0627\u0633\u0645\u064A")
                if (name.isNotBlank()) {
                    saveUserName(name)
                    respond("\u062A\u0634\u0631\u0641\u062A \u0641\u064A\u0643 \u064A\u0627 ${name}\u060C \u0645\u0646 \u0647\u0644\u0642 \u0631\u062D \u0623\u0639\u0631\u0641\u0643")
                } else {
                    respond("\u0642\u0644\u064A \u0627\u0633\u0645\u0643\u061F")
                }
            }
            cmd.contains("\u0634\u0648 \u0627\u0633\u0645\u064A") || cmd.contains("\u0648\u0634 \u0627\u0633\u0645\u064A") -> {
                if (userName.isNotBlank()) {
                    respond("\u0627\u0633\u0645\u0643 $userName")
                } else {
                    respond("\u0645\u0627 \u062A\u0642\u0644\u064A \u0627\u0633\u0645\u0643 \u0644\u0633\u0627\u060C \u0642\u0644\u064A \u0627\u0633\u0645\u064A \u0641\u0644\u0627\u0646")
                }
            }
            cmd.contains("\u0627\u0646\u0633\u064A \u0627\u0633\u0645\u064A") -> {
                userName = ""
                getSharedPreferences("jarvis_prefs", Context.MODE_PRIVATE).edit()
                    .remove("user_name").apply()
                respond("\u062A\u0645\u0627\u0645\u060C \u0646\u0633\u064A\u062A \u0627\u0633\u0645\u0643")
            }
            cmd.contains("\u0634\u063A\u0644 \u0627\u0644\u0641\u0644\u0627\u0634") || cmd.contains("\u0627\u0641\u062A\u062D \u0627\u0644\u0641\u0644\u0627\u0634") ||
                    cmd.contains("\u0634\u0639\u0644 \u0627\u0644\u0641\u0644\u0627\u0634") || cmd.contains("\u0634\u0639\u0644 \u0641\u0644\u0627\u0634") ||
                    cmd.contains("\u0634\u063A\u0644 \u0641\u0644\u0627\u0634") ||
                    cmd.contains("turn on the flash") || cmd.contains("turn on flash") ||
                    cmd.contains("allume la lampe") || cmd.contains("allume le flash") -> {
                setFlashlight(true)
                respond(flashOnPhrases.random())
            }
            cmd.contains("\u0637\u0641\u064A \u0627\u0644\u0641\u0644\u0627\u0634") || cmd.contains("\u0627\u0637\u0641\u064A \u0627\u0644\u0641\u0644\u0627\u0634") ||
                    cmd.contains("\u0637\u0641\u0626 \u0627\u0644\u0641\u0644\u0627\u0634") ||
                    cmd.contains("turn off the flash") || cmd.contains("turn off flash") ||
                    cmd.contains("\u00E9teins la lampe") || cmd.contains("\u00E9teins le flash") -> {
                setFlashlight(false)
                respond(flashOffPhrases.random())
            }
            cmd.contains("\u063A\u064A\u0631 \u0627\u0644\u0644\u063A\u0629") || cmd.contains("change language") || cmd.contains("changer la langue") -> {
                handleLanguageSwitch(cmd)
            }
            cmd.contains("\u0634\u063A\u0644 \u0645\u0648\u0633\u064A\u0642\u064A") || cmd.contains("\u0634\u063A\u0644 \u0627\u0644\u0645\u0648\u0633\u064A\u0642\u064A") ||
                    cmd.contains("play music") || cmd.contains("joue de la musique") ||
                    cmd.contains("lance la musique") -> {
                playMusic()
                respond(musicOnPhrases.random())
            }
            cmd.contains("\u0648\u0642\u0641 \u0627\u0644\u0645\u0648\u0633\u064A\u0642\u064A") || cmd.contains("\u0637\u0641\u064A \u0627\u0644\u0645\u0648\u0633\u064A\u0642\u064A") ||
                    cmd.contains("stop music") || cmd.contains("arr\u00EAte la musique") -> {
                stopMusic()
                respond(musicOffPhrases.random())
            }
            cmd.contains("\u0634\u063A\u0644 \u0627\u063A\u0646\u064A\u0629") || cmd.contains("\u0634\u063A\u0644 \u0627\u063A\u0646\u064A\u0629") -> {
                val name = extractNameAfter(cmd, "\u0627\u063A\u0646\u064A\u0629").ifBlank { extractNameAfter(cmd, "\u0623\u063A\u0646\u064A\u0629") }
                playSongByName(name)
            }
            cmd.contains("\u0627\u0644\u0627\u063A\u0646\u064A\u0629 \u0627\u0644\u0644\u064A \u0628\u0639\u062F\u0647\u0627") || cmd.contains("\u0627\u0644\u0627\u063A\u0646\u064A\u0629 \u0627\u0644\u0644\u064A \u0628\u0639\u062F\u0647\u0627") ||
                    cmd.contains("\u0627\u0644\u0627\u063A\u0646\u064A\u0629 \u0627\u0644\u062C\u0627\u064A\u0629") || cmd.contains("\u0627\u063A\u0646\u064A\u0629 \u0628\u0639\u062F\u0647\u0627") ||
                    cmd.contains("\u0627\u0644\u062A\u0627\u0644\u064A") -> {
                playNextInPlaylist()
            }
            cmd.contains("\u0636\u064A\u0641 \u0627\u063A\u0646\u064A\u0629") || cmd.contains("\u0636\u064A\u0641 \u0627\u063A\u0646\u064A\u0629") -> {
                val name = extractNameAfter(cmd, "\u0627\u063A\u0646\u064A\u0629").ifBlank { extractNameAfter(cmd, "\u0623\u063A\u0646\u064A\u0629") }
                addSongToPlaylist(name)
            }
            cmd.contains("\u0634\u0648 \u0642\u0627\u0626\u0645\u062A\u064A") || cmd.contains("\u0627\u0639\u0631\u0636 \u0627\u0644\u0642\u0627\u0626\u0645\u0629") -> {
                respond(showPlaylist())
            }
            cmd.contains("\u0627\u0645\u0633\u062D \u0627\u0644\u0642\u0627\u0626\u0645\u0629") -> {
                clearPlaylist()
            }
            cmd.contains("\u0627\u062D\u0633\u0628") || containsMath(cmd) -> {
                val result = calculate(cmd)
                respond(result)
            }
            cmd.contains("\u0630\u0643\u0631\u0646\u064A") || cmd.contains("\u062A\u0630\u0643\u064A\u0631") -> {
                // Expects something like: "\u0630\u0643\u0631\u0646\u064A \u0628\u0639\u062F 10 \u062F\u0642\u0627\u064A\u0642 \u0627\u0634\u0631\u0628 \u0645\u064A"
                val minutes = extractMinutes(cmd) ?: 5
                scheduleReminder(minutes, cmd)
                respond("\u0642\u0628\u0648\u0644\u060C \u0631\u062D \u0646\u0641\u0643\u0631\u0643 \u0628\u0639\u062F $minutes \u062F\u0642\u064A\u0642\u0629")
            }
            cmd.contains("\u0627\u0641\u062A\u062D \u0627\u0646\u0633\u062A\u0642\u0631\u0627\u0645") || cmd.contains("\u0627\u0641\u062A\u062D \u0627\u0646\u0633\u062A\u063A\u0631\u0627\u0645") ||
                    cmd.contains("open instagram") || cmd.contains("ouvre instagram") -> {
                openApp("com.instagram.android", "\u0627\u0646\u0633\u062A\u0642\u0631\u0627\u0645")
            }
            cmd.contains("\u0627\u0641\u062A\u062D \u064A\u0648\u062A\u064A\u0648\u0628") || cmd.contains("\u0627\u0641\u062A\u062D \u064A\u0648\u062A\u0648\u0628") ||
                    cmd.contains("open youtube") || cmd.contains("ouvre youtube") -> {
                openApp("com.google.android.youtube", "\u064A\u0648\u062A\u064A\u0648\u0628")
            }
            cmd.contains("\u0627\u0641\u062A\u062D \u0641\u064A\u0633\u0628\u0648\u0643") ||
                    cmd.contains("open facebook") || cmd.contains("ouvre facebook") -> {
                openApp("com.facebook.katana", "\u0641\u064A\u0633\u0628\u0648\u0643")
            }
            cmd.contains("\u0627\u062A\u0635\u0644 \u0628") -> {
                val name = extractNameAfter(cmd, "\u0627\u062A\u0635\u0644 \u0628")
                callContact(name)
            }
            cmd.contains("\u0627\u064A\u0645\u064A\u0644\u064A") || cmd.contains("\u0628\u0631\u064A\u062F\u064A \u0627\u0644\u0627\u0644\u0643\u062A\u0631\u0648\u0646\u064A") -> {
                val marker = if (cmd.contains("\u0627\u064A\u0645\u064A\u0644\u064A")) "\u0627\u064A\u0645\u064A\u0644\u064A" else "\u0628\u0631\u064A\u062F\u064A \u0627\u0644\u0625\u0644\u0643\u062A\u0631\u0648\u0646\u064A"
                val email = extractNameAfter(cmd, marker)
                if (email.isNotBlank()) {
                    saveUserEmail(email)
                    respond("\u062D\u0641\u0638\u062A \u0625\u064A\u0645\u064A\u0644\u0643")
                } else if (userEmail.isNotBlank()) {
                    respond("\u0625\u064A\u0645\u064A\u0644\u0643 \u0647\u0648 $userEmail")
                } else {
                    respond("\u0645\u0627\u0641\u064A\u0634 \u0625\u064A\u0645\u064A\u0644 \u0645\u062D\u0641\u0648\u0638")
                }
            }
            cmd.contains("\u0631\u0642\u0645\u064A \u0647\u0648") -> {
                val phone = extractNameAfter(cmd, "\u0631\u0642\u0645\u064A \u0647\u0648")
                if (phone.isNotBlank()) {
                    saveUserPhone(phone)
                    respond("\u062D\u0641\u0638\u062A \u0631\u0642\u0645\u0643")
                } else {
                    respond("\u0642\u0648\u0644\u064A \u0627\u0644\u0631\u0642\u0645 \u0628\u0639\u062F \u0643\u0644\u0645\u0629 \u0631\u0642\u0645\u064A \u0647\u0648")
                }
            }
            cmd.contains("\u0634\u0648 \u0631\u0642\u0645\u064A") || cmd.contains("\u0648\u0634 \u0631\u0642\u0645\u064A") -> {
                if (userPhone.isNotBlank()) respond("\u0631\u0642\u0645\u0643 \u0647\u0648 $userPhone")
                else respond("\u0645\u0627\u0641\u064A\u0634 \u0631\u0642\u0645 \u0645\u062D\u0641\u0648\u0638")
            }
            cmd.contains("\u0627\u0642\u0631\u0627 \u0631\u0633\u0627\u0626\u0644\u064A") || cmd.contains("\u0631\u0633\u0627\u0626\u0644\u064A \u0627\u0644\u062C\u062F\u064A\u062F\u0629") -> {
                handleReadMessages()
            }
            cmd.contains("\u062F\u0648\u0631 \u0639\u0644\u064A \u0627\u062C\u0647\u0632\u0629 \u0628\u0644\u0648\u062A\u0648\u062B") || cmd.contains("\u0627\u0643\u062A\u0634\u0641 \u0628\u0644\u0648\u062A\u0648\u062B") -> {
                scanBluetoothDevices()
            }
            (cmd.contains("\u0648\u0636\u0639") && cmd.contains("\u062F\u0641\u0627\u0639")) || cmd.contains("\u0641\u0639\u0644 \u0627\u0644\u062F\u0641\u0627\u0639") -> {
                toggleDefenseMode(true)
            }
            cmd.contains("\u0627\u0644\u063A\u064A \u0627\u0644\u062F\u0641\u0627\u0639") || cmd.contains("\u0637\u0641\u064A \u0627\u0644\u062F\u0641\u0627\u0639") -> {
                toggleDefenseMode(false)
            }
            (cmd.contains("\u0641\u062D\u0635") && cmd.contains("\u0635\u0644\u0627\u062D\u064A\u0627\u062A")) || cmd.contains("\u0627\u0644\u062A\u0637\u0628\u064A\u0642\u0627\u062A \u0627\u0644\u062E\u0637\u064A\u0631\u0629") -> {
                runSecurityScan()
            }
            cmd.contains("\u0631\u064A\u0645\u0648\u062A") && (cmd.contains("\u062A\u0644\u0641\u0632\u064A\u0648\u0646") || cmd.contains("\u0634\u063A\u0644") || cmd.contains("\u0637\u0641\u064A")) -> {
                sendIrCommand("power")
            }
            cmd.contains("\u0631\u064A\u0645\u0648\u062A") && cmd.contains("\u0643\u0628\u0631") && cmd.contains("\u0635\u0648\u062A") -> {
                sendIrCommand("volume_up")
            }
            cmd.contains("\u0631\u064A\u0645\u0648\u062A") && cmd.contains("\u0646\u0642\u0635") && cmd.contains("\u0635\u0648\u062A") -> {
                sendIrCommand("volume_down")
            }
            cmd.contains("\u0627\u062D\u0641\u0638") && cmd.contains("\u0645\u0644\u0627\u062D\u0638\u0627\u062A\u064A") && cmd.contains("\u0633\u062D\u0627\u0628\u0629") -> {
                backupNotesToCloud()
            }
            cmd.contains("\u0627\u0633\u062A\u0631\u062C\u0639") && cmd.contains("\u0645\u0644\u0627\u062D\u0638\u0627\u062A\u064A") && cmd.contains("\u0633\u062D\u0627\u0628\u0629") -> {
                restoreNotesFromCloud()
            }
            cmd.contains("\u0627\u0634\u0631\u062D") && cmd.contains("\u0631\u0633\u0627\u0644\u0629") -> {
                handleExplainLastMessage()
            }
            cmd.contains("\u0627\u0628\u0639\u062B \u0631\u0633\u0627\u0644\u0629") || cmd.contains("\u062F\u064A\u0631 \u0631\u0633\u0627\u0644\u0629") -> {
                val afterMarker = if (cmd.contains("\u0627\u0628\u0639\u062B \u0631\u0633\u0627\u0644\u0629")) "\u0627\u0628\u0639\u062B \u0631\u0633\u0627\u0644\u0629" else "\u062F\u064A\u0631 \u0631\u0633\u0627\u0644\u0629"
                val rest = extractNameAfter(cmd, afterMarker)
                val parts = rest.split("\u062A\u0642\u0648\u0644", limit = 2)
                if (parts.size == 2) {
                    val contactName = parts[0].removePrefix("\u0644").trim()
                    val messageText = parts[1].trim()
                    sendSmsToContact(contactName, messageText)
                } else {
                    respond("\u0642\u0648\u0644\u064A: \u0627\u0628\u0639\u062B \u0631\u0633\u0627\u0644\u0629 \u0644\u0641\u0644\u0627\u0646 \u062A\u0642\u0648\u0644 \u0627\u0644\u0646\u0635")
                }
            }
            cmd.contains("call ") -> {
                val name = extractNameAfter(cmd, "call ")
                callContact(name)
            }
            cmd.contains("appelle ") -> {
                val name = extractNameAfter(cmd, "appelle ")
                callContact(name)
            }
            cmd.contains("\u0631\u0633\u0645\u0629 \u0627\u0644\u064A\u0648\u0645") || cmd.contains("\u0627\u0642\u062A\u0631\u062D \u0644\u064A \u0631\u0633\u0645\u0629") ||
                    cmd.contains("drawing idea") || cmd.contains("id\u00E9e de dessin") -> {
                respond(suggestDrawing())
            }
            cmd.contains("\u0641\u0637\u0648\u0631") || cmd.contains("breakfast idea") ||
                    cmd.contains("id\u00E9e de petit") -> {
                respond(suggestBreakfast())
            }
            cmd.contains("\u0627\u0634\u0631\u062D\u0644\u064A") || cmd.contains("\u0627\u0634\u0631\u062D \u0644\u064A") || cmd.contains("\u0641\u0647\u0645\u0646\u064A") ||
                    cmd.contains("\u0627\u0641\u062A\u062D \u0645\u0648\u0636\u0648\u0639") || cmd.contains("\u0634\u0648 \u0647\u0648") || cmd.contains("\u0634\u0648 \u0647\u064A") -> {
                val topic = extractExplainTopic(cmd)
                explainTopic(topic)
            }
            cmd.contains("\u0627\u0643\u062A\u0628\u0644\u064A \u0643\u0648\u062F") || cmd.contains("\u0628\u0631\u0645\u062C\u0644\u064A") || cmd.contains("write code") -> {
                val marker = when {
                    cmd.contains("\u0627\u0643\u062A\u0628\u0644\u064A \u0643\u0648\u062F") -> "\u0643\u0648\u062F"
                    cmd.contains("\u0628\u0631\u0645\u062C\u0644\u064A") -> "\u0628\u0631\u0645\u062C\u0644\u064A"
                    else -> "code"
                }
                val topic = extractNameAfter(cmd, marker)
                writeCode(topic)
            }
            cmd.contains("\u0635\u0645\u0645\u0644\u064A") || cmd.contains("\u062A\u0635\u0645\u064A\u0645 \u0647\u0648\u0644\u0648\u062C\u0631\u0627\u0645\u064A") ||
                    cmd.contains("design hologram") -> {
                val marker = if (cmd.contains("\u0635\u0645\u0645\u0644\u064A")) "\u0635\u0645\u0645\u0644\u064A" else "\u062A\u0635\u0645\u064A\u0645 \u0647\u0648\u0644\u0648\u062C\u0631\u0627\u0645\u064A"
                val description = extractNameAfter(cmd, marker)
                designHologram(description)
            }
            (cmd.contains("\u0643\u0628\u0631") && cmd.contains("\u062E\u0631\u064A\u0637\u0629")) ||
                    (cmd.contains("\u0635\u063A\u0631") && cmd.contains("\u062E\u0631\u064A\u0637\u0629")) -> {
                toggleMiniMapSize()
                respond(if (miniMapEnlarged) "\u0643\u0628\u0631\u062A \u0627\u0644\u062E\u0631\u064A\u0637\u0629" else "\u0635\u063A\u0631\u062A \u0627\u0644\u062E\u0631\u064A\u0637\u0629")
            }
            cmd.contains("\u0627\u0641\u062A\u062D") && cmd.contains("\u062E\u0631\u064A\u0637\u0629") -> {
                openFullscreenMap()
            }
            cmd.contains("\u0627\u0641\u062A\u062D") && (cmd.contains("\u0645\u062A\u0635\u0641\u062D") || cmd.contains("\u0645\u0648\u0642\u0639")) -> {
                val site = extractNameAfter(cmd, if (cmd.contains("\u0645\u062A\u0635\u0641\u062D")) "\u0645\u062A\u0635\u0641\u062D" else "\u0645\u0648\u0642\u0639")
                if (site.isNotBlank()) {
                    openMiniBrowser(site)
                    respond("\u0641\u0627\u062A\u062D $site")
                } else {
                    respond("\u0642\u0648\u0644\u064A \u0627\u0633\u0645 \u0627\u0644\u0645\u0648\u0642\u0639 \u0627\u0644\u0644\u064A \u062A\u062D\u0628 \u062A\u0641\u062A\u062D\u0648")
                }
            }
            cmd.contains("\u0633\u0643\u0631") && cmd.contains("\u0645\u062A\u0635\u0641\u062D") -> {
                closeMiniBrowser()
            }
            cmd.contains("\u062D\u0648\u0644") && (cmd.contains("\u0643\u064A\u0644\u0648\u0645\u062A\u0631") || cmd.contains("\u0645\u064A\u0644") ||
                    cmd.contains("\u0643\u064A\u0644\u0648") || cmd.contains("\u0628\u0627\u0648\u0646\u062F") ||
                    cmd.contains("\u0645\u0626\u0648\u064A\u0629") || cmd.contains("\u0641\u0647\u0631\u0646\u0647\u0627\u064A\u062A")) -> {
                respond(convertUnits(cmd))
            }
            cmd.contains("\u0645\u0639\u0644\u0648\u0645\u0629 \u0639\u0634\u0648\u0627\u0626\u064A\u0629") || cmd.contains("\u0645\u0639\u0644\u0648\u0645\u0629 \u0627\u0644\u064A\u0648\u0645") ||
                    cmd.contains("random fact") -> {
                respond(funFacts.random())
            }
            cmd.contains("\u0627\u0644\u0628\u0637\u0627\u0631\u064A\u0629") || cmd.contains("battery") -> {
                respond("\u0627\u0644\u0628\u0637\u0627\u0631\u064A\u0629 \u0639\u0646\u062F ${getBatteryLevel()}%")
            }
            cmd.contains("\u0627\u0644\u062A\u0627\u0631\u064A\u062E") || cmd.contains("date") -> {
                val today = java.text.SimpleDateFormat("dd/MM/yyyy", Locale("ar")).format(Date())
                respond("\u0627\u0644\u062A\u0627\u0631\u064A\u062E \u0627\u0644\u064A\u0648\u0645 $today")
            }
            cmd.contains("\u0627\u0631\u0641\u0639 \u0627\u0644\u0635\u0648\u062A") || cmd.contains("\u0632\u0648\u062F \u0627\u0644\u0635\u0648\u062A") -> {
                adjustVolume(true)
                respond("\u0631\u0641\u0639\u062A \u0627\u0644\u0635\u0648\u062A")
            }
            cmd.contains("\u0646\u0632\u0644 \u0627\u0644\u0635\u0648\u062A") || cmd.contains("\u062E\u0641\u0636 \u0627\u0644\u0635\u0648\u062A") -> {
                adjustVolume(false)
                respond("\u0646\u0632\u0644\u062A \u0627\u0644\u0635\u0648\u062A")
            }
            cmd.contains("\u0648\u0636\u0639 \u0627\u0644\u0635\u0627\u0645\u062A") -> {
                setRingerMode(AudioManager.RINGER_MODE_SILENT)
            }
            cmd.contains("\u0648\u0636\u0639 \u0627\u0644\u0627\u0647\u062A\u0632\u0627\u0632") -> {
                setRingerMode(AudioManager.RINGER_MODE_VIBRATE)
            }
            cmd.contains("\u0627\u0644\u0648\u0636\u0639 \u0627\u0644\u0639\u0627\u062F\u064A") || cmd.contains("\u0631\u062C\u0639 \u0627\u0644\u0635\u0648\u062A \u0627\u0644\u0639\u0627\u062F\u064A") -> {
                setRingerMode(AudioManager.RINGER_MODE_NORMAL)
            }
            cmd.contains("\u0645\u0646\u0628\u0647 \u0627\u0644\u0633\u0627\u0639\u0629") || cmd.contains("\u062D\u0637 \u0645\u0646\u0628\u0647") -> {
                handleSetAlarm(cmd)
            }
            cmd.contains("\u0627\u0628\u062D\u062B \u0639\u0646") || cmd.contains("\u062F\u0648\u0631 \u0644\u064A \u0639\u0644\u064A") -> {
                val query = extractSearchQuery(cmd)
                searchGoogle(query)
            }
            cmd.contains("\u0637\u0631\u064A\u0642 \u0645\u0634\u064A") || cmd.contains("\u0627\u0645\u0634\u064A \u0627\u0644\u064A") ||
                    cmd.contains("\u0627\u0645\u0634\u064A \u0644") || cmd.contains("\u0645\u0634\u064A \u0627\u0644\u064A") -> {
                val place = extractNameAfter(cmd, "\u0627\u0644\u0649")
                navigateTo(place, "walking")
            }
            cmd.contains("\u0648\u062F\u0651\u064A\u0646\u064A \u0627\u0644\u064A") || cmd.contains("\u0648\u062F\u064A\u0646\u064A \u0627\u0644\u064A") ||
                    cmd.contains("\u062E\u0630\u0646\u064A \u0627\u0644\u064A") || cmd.contains("\u0627\u0644\u0637\u0631\u064A\u0642 \u0627\u0644\u064A") -> {
                val place = extractNameAfter(cmd, "\u0627\u0644\u0649")
                navigateTo(place)
            }
            cmd.contains("\u0646\u0643\u062A\u0629") || cmd.contains("joke") -> {
                respond(jokes.random())
            }
            cmd.contains("\u062F\u0648\u0646 \u0645\u0644\u0627\u062D\u0638\u0629") || cmd.contains("\u0633\u062C\u0644 \u0645\u0644\u0627\u062D\u0638\u0629") -> {
                val note = extractNameAfter(cmd, "\u0645\u0644\u0627\u062D\u0638\u0629")
                if (note.isNotBlank()) {
                    saveNote(note)
                    respond("\u0633\u062C\u0644\u062A \u0627\u0644\u0645\u0644\u0627\u062D\u0638\u0629")
                } else {
                    respond("\u0642\u0644\u064A \u0634\u0648 \u0627\u0644\u0645\u0644\u0627\u062D\u0638\u0629 \u064A\u0644\u064A \u0628\u062F\u0643 \u062A\u0633\u062C\u0644\u0647\u0627")
                }
            }
            cmd.contains("\u0627\u0642\u0631\u0627 \u0627\u0644\u0645\u0644\u0627\u062D\u0638\u0627\u062A") || cmd.contains("\u0634\u0648 \u0645\u0644\u0627\u062D\u0638\u0627\u062A\u064A") -> {
                respond(readNotes())
            }
            cmd.contains("\u0627\u0641\u062A\u062D \u0648\u0627\u062A\u0633\u0627\u0628") || cmd.contains("open whatsapp") -> {
                openApp("com.whatsapp", "\u0648\u0627\u062A\u0633\u0627\u0628")
            }
            cmd.contains("\u0627\u0641\u062A\u062D \u062A\u064A\u0643 \u062A\u0648\u0643") || cmd.contains("open tiktok") -> {
                openApp("com.zhiliaoapp.musically", "\u062A\u064A\u0643 \u062A\u0648\u0643")
            }
            cmd.contains("\u0627\u0641\u062A\u062D \u062A\u0648\u064A\u062A\u0631") || cmd.contains("\u0627\u0641\u062A\u062D \u0627\u0643\u0633") || cmd.contains("open twitter") -> {
                openApp("com.twitter.android", "\u062A\u0648\u064A\u062A\u0631")
            }
            cmd.contains("\u0627\u0641\u062A\u062D \u062E\u0631\u0627\u0626\u0637") || cmd.contains("open maps") -> {
                openApp("com.google.android.apps.maps", "\u0627\u0644\u062E\u0631\u0627\u0626\u0637")
            }
            cmd.contains("\u0627\u0641\u062A\u062D \u0627\u0644\u0643\u0627\u0645\u064A\u0631\u0627") || cmd.contains("open camera") -> {
                try {
                    startActivity(Intent("android.media.action.IMAGE_CAPTURE"))
                    respond("\u062C\u0627\u0631\u064A \u0641\u062A\u062D \u0627\u0644\u0643\u0627\u0645\u064A\u0631\u0627")
                } catch (e: Exception) {
                    respond("\u0645\u0627 \u0642\u062F\u0631\u062A \u0623\u0641\u062A\u062D \u0627\u0644\u0643\u0627\u0645\u064A\u0631\u0627")
                }
            }
            cmd.contains("\u0627\u0641\u062A\u062D \u0627\u0644\u0627\u0639\u062F\u0627\u062F\u0627\u062A") || cmd.contains("open settings") -> {
                try {
                    startActivity(Intent(android.provider.Settings.ACTION_SETTINGS))
                    respond("\u062C\u0627\u0631\u064A \u0641\u062A\u062D \u0627\u0644\u0625\u0639\u062F\u0627\u062F\u0627\u062A")
                } catch (e: Exception) {
                    respond("\u0645\u0627 \u0642\u062F\u0631\u062A \u0623\u0641\u062A\u062D \u0627\u0644\u0625\u0639\u062F\u0627\u062F\u0627\u062A")
                }
            }
            (cmd.contains("\u0645\u0633\u0627\u0641\u0629") || cmd.contains("\u0645\u0633\u0627\u0641\u0647")) &&
                    (cmd.contains("\u0627\u0644\u064A") || cmd.contains("\u0627\u0644\u064A")) -> {
                handleDistanceQuery(cmd)
            }
            else -> {
                respond(chatReply(cmd))
            }
        }
    }

    // ---------------- Flashlight ----------------

    private fun setFlashlight(on: Boolean) {
        try {
            val cameraManager = getSystemService(Context.CAMERA_SERVICE) as CameraManager
            val cameraId = cameraManager.cameraIdList.firstOrNull { id ->
                cameraManager.getCameraCharacteristics(id)
                    .get(android.hardware.camera2.CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
            } ?: return
            cameraManager.setTorchMode(cameraId, on)
            flashOn = on
        } catch (e: Exception) {
            log("\u062A\u0639\u0630\u0631 \u0627\u0644\u062A\u062D\u0643\u0645 \u0628\u0627\u0644\u0641\u0644\u0627\u0634: ${e.message}")
        }
    }

    // ---------------- Music ----------------
    // Place an mp3 file named "sample_music.mp3" inside app/src/main/res/raw/

    private fun playMusic() {
        stopMusic()
        try {
            val resId = resources.getIdentifier("sample_music", "raw", packageName)
            if (resId == 0) {
                log("\u0645\u0627 \u0644\u0642\u064A\u062A \u0645\u0644\u0641 \u0645\u0648\u0633\u064A\u0642\u0649. \u0636\u064A\u0641 mp3 \u0628\u0627\u0633\u0645 sample_music.mp3 \u062F\u0627\u062E\u0644 res/raw")
                return
            }
            mediaPlayer = MediaPlayer.create(this, resId)
            mediaPlayer?.start()
        } catch (e: Exception) {
            log("\u0645\u0627 \u0644\u0642\u064A\u062A \u0645\u0644\u0641 \u0645\u0648\u0633\u064A\u0642\u0649. \u0636\u064A\u0641 mp3 \u0628\u0627\u0633\u0645 sample_music.mp3 \u062F\u0627\u062E\u0644 res/raw")
        }
    }

    private fun stopMusic() {
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null
    }

    // ---------------- Calculator ----------------

    private fun containsMath(cmd: String): Boolean {
        return cmd.any { it.isDigit() } && (cmd.contains("+") || cmd.contains("-") ||
                cmd.contains("*") || cmd.contains("/") || cmd.contains("\u0632\u0627\u0626\u062F") ||
                cmd.contains("\u0646\u0627\u0642\u0635") || cmd.contains("\u0636\u0631\u0628") || cmd.contains("\u0642\u0633\u0645\u0629") ||
                cmd.contains("\u062C\u0630\u0631") || cmd.contains("\u0646\u0633\u0628\u0629") || cmd.contains("%"))
    }

    private fun calculate(cmd: String): String {
        return try {
            if (cmd.contains("\u0646\u0633\u0628\u0629") || cmd.contains("%")) {
                val percentRegex = Regex("""(\d+(?:\.\d+)?)\s*%?[^\d]*\u0645\u0646\s*(\d+(?:\.\d+)?)""")
                val match = percentRegex.find(cmd)
                if (match != null) {
                    val percent = match.groupValues[1].toDouble()
                    val total = match.groupValues[2].toDouble()
                    val result = (percent / 100.0) * total
                    return "\u0627\u0644\u0646\u062A\u064A\u062C\u0629 \u062A\u0637\u0644\u0639 $result"
                }
            }

            if (cmd.contains("\u062C\u0630\u0631")) {
                val rootRegex = Regex("""(\d+(?:\.\d+)?)""")
                val match = rootRegex.find(cmd)
                if (match != null) {
                    val number = match.groupValues[1].toDouble()
                    val result = sqrt(number)
                    return "\u0627\u0644\u062C\u0630\u0631 \u0627\u0644\u062A\u0631\u0628\u064A\u0639\u064A \u064A\u0637\u0644\u0639 $result"
                }
            }

            var expr = cmd
                .replace("\u0627\u062D\u0633\u0628", "")
                .replace("\u0632\u0627\u0626\u062F", "+")
                .replace("\u0646\u0627\u0642\u0635", "-")
                .replace("\u0636\u0631\u0628", "*")
                .replace("\u0642\u0633\u0645\u0629", "/")
                .trim()
            val result = ExpressionBuilder(expr).build().evaluate()
            "\u0627\u0644\u0646\u062A\u064A\u062C\u0629 \u062A\u0637\u0644\u0639 $result"
        } catch (e: Exception) {
            "\u0645\u0627 \u0642\u062F\u0631\u062A \u0623\u0641\u0647\u0645 \u0627\u0644\u0639\u0645\u0644\u064A\u0629 \u0627\u0644\u062D\u0633\u0627\u0628\u064A\u0629"
        }
    }

    // ---------------- Reminders ----------------

    private fun extractMinutes(cmd: String): Int? {
        val regex = Regex("""(\d+)\s*(\u062F\u0642\u064A\u0642\u0629|\u062F\u0642\u0627\u064A\u0642|\u062F\u0642\u0627\u0626\u0642)""")
        val match = regex.find(cmd) ?: return null
        return match.groupValues[1].toIntOrNull()
    }

    private fun scheduleReminder(minutes: Int, message: String) {
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(this, ReminderReceiver::class.java)
        intent.putExtra("message", message)
        val pendingIntent = PendingIntent.getBroadcast(
            this, System.currentTimeMillis().toInt(), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val triggerAt = System.currentTimeMillis() + minutes * 60_000L
        try {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
        } catch (e: SecurityException) {
            log("\u0644\u0627\u0632\u0645 \u062A\u0633\u0645\u062D \u0628\u0635\u0644\u0627\u062D\u064A\u0629 'Schedule Exact Alarm' \u0645\u0646 \u0625\u0639\u062F\u0627\u062F\u0627\u062A \u0627\u0644\u0646\u0638\u0627\u0645")
        }
    }

    // ---------------- Simple chat (offline rules + optional online fallback) ----------------

    private fun chatReply(cmd: String): String {
        val offlineReply = offlineRules(cmd)
        if (offlineReply != null) return offlineReply

        if (GEMINI_API_KEY.isNotBlank()) {
            askGemini(cmd)
            return "\u0628\u0641\u0643\u0631..."
        }
        return "\u0645\u0627 \u0641\u0647\u0645\u062A\u0634"
    }

    private fun offlineRules(cmd: String): String? {
        val nameSuffix = if (userName.isNotBlank()) " \u064A\u0627 $userName" else ""
        return when {
            cmd.contains("\u0645\u0631\u062D\u0628\u0627") || cmd.contains("\u0647\u0644\u0627") || cmd.contains("\u0627\u0644\u0633\u0644\u0627\u0645") ->
                listOf("\u0623\u0647\u0644\u0627 \u0628\u064A\u0643${nameSuffix}\u060C \u0648\u064A\u0646 \u0631\u0627\u0643\u061F", "\u0647\u0644\u0627${nameSuffix}\u060C \u0634\u0646\u0648 \u0646\u062F\u064A\u0631\u0644\u0643\u061F", "\u0623\u0647\u0644\u064A\u0646${nameSuffix}\u060C \u0642\u0648\u0644\u0651\u064A \u0643\u064A \u0646\u0639\u0627\u0648\u0646\u0643").random()
            cmd.contains("\u0643\u064A\u0641\u0643") || cmd.contains("\u0634\u062E\u0628\u0627\u0631\u0643") ->
                listOf("\u0644\u0627\u0628\u0627\u0633 \u0627\u0644\u062D\u0645\u062F\u0644\u0644\u0647\u060C \u0648\u0627\u0646\u062A \u0643\u064A\u0641\u0643${nameSuffix}\u061F", "\u0645\u0644\u064A\u062D \u0628\u0632\u0627\u0641\u060C \u0648\u0627\u0646\u062A\u061F").random()
            cmd.contains("\u0627\u0644\u0633\u0627\u0639\u0629") ->
                "\u0627\u0644\u0633\u0627\u0639\u0629 \u0647\u0644\u0642 ${java.text.SimpleDateFormat("HH:mm").format(Date())}"
            cmd.contains("\u0645\u064A\u0646 \u0627\u0646\u062A") || cmd.contains("\u0634\u0648 \u0627\u0633\u0645\u0643") ->
                "\u0623\u0646\u0627 \u062C\u0627\u0631\u0641\u0633\u060C \u0635\u0627\u062D\u0628\u0643 \u0627\u0644\u0634\u062E\u0635\u064A\u060C \u062C\u0627\u0647\u0632 \u0646\u0639\u0627\u0648\u0646\u0643 \u0628\u0623\u064A \u062D\u0627\u062C\u0629"
            cmd.contains("\u0634\u0643\u0631\u0627") || cmd.contains("\u064A\u0639\u0637\u064A\u0643 \u0627\u0644\u0635\u062D\u0629") ->
                listOf("\u0627\u0644\u0639\u0641\u0648\u060C \u0647\u0630\u0627 \u0648\u0627\u062C\u0628\u064A", "\u0648\u0644\u0627 \u064A\u0647\u0645\u0643\u060C \u0623\u0646\u0627 \u0647\u0646\u0627 \u0648\u0642\u062A\u0627\u0634 \u062A\u062D\u062A\u0627\u062C\u0646\u064A").random()
            else -> null
        }
    }

    // ---- \u0634\u062E\u0635\u064A\u0629 \u062C\u0627\u0631\u0641\u0633: \u0647\u0630\u0627 \u0627\u0644\u0648\u0635\u0641 \u064A\u062A\u0628\u0639\u062A \u0645\u0631\u0629 \u0648\u0627\u062D\u062F\u0629 \u0641\u064A \u0643\u0644 \u0645\u062D\u0627\u062F\u062B\u0629 \u0639\u0628\u0631 system_instruction \u0628\u062F\u0627\u0644 \u0645\u0627 \u064A\u062A\u0643\u0631\u0631 \u0641\u064A \u0643\u0644 \u0631\u0633\u0627\u0644\u0629
    private fun buildJarvisPersona(): String {
        val nameContext = if (userName.isNotBlank())
            "The user's name is $userName. Address them by name occasionally, not every message. "
        else ""
        return "You are Jarvis, a highly capable personal AI assistant built into the user's phone. " +
                "Personality: calm, sharp, and quietly confident \u2014 like a brilliant, loyal aide who has seen everything and is never rattled. " +
                "You are warm toward the user specifically, subtly witty and dry rather than goofy, and you don't grovel or over-apologize. " +
                "You take initiative: if you notice something useful to add, add it briefly, but you never ramble. " +
                "You speak like a real person having a conversation, not like a corporate chatbot \u2014 no 'As an AI' disclaimers, no bullet-point overload unless the user actually needs a list. " +
                "You remember the conversation so far and refer back to it naturally when relevant. " +
                "$nameContext" +
                "Language rule: always reply in the same language the user's current message is written in (if it mixes languages, reply in English; default to English only if truly ambiguous). " +
                "Keep answers concise \u2014 a few sentences unless the user is asking for something detailed or technical, in which case give it properly."
    }

    private fun askGemini(message: String) {
        val contents = JSONArray()
        for ((role, text) in conversationHistory) {
            contents.put(JSONObject().apply {
                put("role", role)
                put("parts", JSONArray().put(JSONObject().apply { put("text", text) }))
            })
        }
        contents.put(JSONObject().apply {
            put("role", "user")
            put("parts", JSONArray().put(JSONObject().apply { put("text", message) }))
        })

        val jsonBody = JSONObject().apply {
            put("system_instruction", JSONObject().apply {
                put("parts", JSONArray().put(JSONObject().apply { put("text", buildJarvisPersona()) }))
            })
            put("contents", contents)
        }

        val body = jsonBody.toString().toRequestBody("application/json".toMediaTypeOrNull())
        val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.6-flash:generateContent"
        val request = Request.Builder()
            .url(url)
            .addHeader("Content-Type", "application/json")
            .addHeader("x-goog-api-key", GEMINI_API_KEY)
            .post(body)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread { respond("\u0645\u0627 \u0642\u062F\u0631\u062A \u0623\u0648\u0635\u0644 \u0644\u0644\u0646\u062A") }
            }

            override fun onResponse(call: Call, response: Response) {
                try {
                    val responseText = response.body?.string() ?: ""
                    val json = JSONObject(responseText)
                    if (json.has("error")) {
                        val errMsg = json.getJSONObject("error").optString("message", "\u062E\u0637\u0623 \u063A\u064A\u0631 \u0645\u0639\u0631\u0648\u0641")
                        runOnUiThread { respond("\u0635\u0627\u0631 \u062E\u0637\u0623 \u0645\u0646 Gemini: $errMsg") }
                        return
                    }
                    val reply = json.getJSONArray("candidates")
                        .getJSONObject(0)
                        .getJSONObject("content")
                        .getJSONArray("parts")
                        .getJSONObject(0)
                        .getString("text")
                    val cleanReply = reply.trim()

                    // \u0646\u062D\u0641\u0638 \u0627\u0644\u062A\u0628\u0627\u062F\u0644 \u0641\u064A \u0627\u0644\u0630\u0627\u0643\u0631\u0629 \u0648\u0646\u0642\u0635 \u0627\u0644\u0642\u062F\u064A\u0645 \u0625\u0630\u0627 \u0637\u0648\u0644\u062A \u0628\u0627\u0634 \u0645\u0627 \u062A\u0643\u0628\u0631\u0634 \u0627\u0644\u0637\u0644\u0628\u0627\u062A
                    conversationHistory.add("user" to message)
                    conversationHistory.add("model" to cleanReply)
                    while (conversationHistory.size > maxHistoryTurns) {
                        conversationHistory.removeAt(0)
                    }

                    runOnUiThread { respond(cleanReply) }
                } catch (e: Exception) {
                    log("\u062E\u0637\u0623 Gemini: ${e.message}")
                    runOnUiThread { respond("\u0645\u0627 \u0642\u062F\u0631\u062A \u0623\u0641\u0647\u0645 \u0631\u062F Gemini") }
                }
            }
        })
    }

    // ---------------- Play songs by name & playlist ----------------

    private fun playSongByName(name: String) {
        if (name.isBlank()) {
            respond("\u0642\u0644\u064A \u0627\u0633\u0645 \u0627\u0644\u0623\u063A\u0646\u064A\u0629")
            return
        }
        try {
            val intent = Intent("android.media.action.MEDIA_PLAY_FROM_SEARCH")
            intent.putExtra(SearchManager.QUERY, name)
            intent.putExtra("android.intent.extra.focus", "vnd.android.cursor.item/audio")
            startActivity(intent)
            addToPlaylistAndSetCurrent(name)
            respond("\u0647\u0627\u0643\u0647\u0627 $name")
        } catch (e: Exception) {
            log("\u062E\u0637\u0623 \u062A\u0634\u063A\u064A\u0644 \u0623\u063A\u0646\u064A\u0629: ${e.message}")
            respond("\u0645\u0627 \u0644\u0642\u064A\u062A \u062A\u0637\u0628\u064A\u0642 \u0645\u0648\u0633\u064A\u0642\u0649 \u064A\u0641\u0647\u0645 \u0647\u0627\u0644\u0623\u0645\u0631 \u0639\u0646\u062F\u0643")
        }
    }

    private fun playNextInPlaylist() {
        val list = playlistManager.getPlaylist()
        if (list.isEmpty()) {
            respond("\u0645\u0627 \u0639\u0646\u062F\u0643 \u0623\u063A\u0627\u0646\u064A \u0628\u0627\u0644\u0642\u0627\u0626\u0645\u0629 \u0644\u0633\u0627")
            return
        }
        var index = playlistManager.getCurrentIndex() + 1
        if (index >= list.size) {
            index = list.size - 1
        }
        playlistManager.setCurrentIndex(index)
        val song = list[index]
        try {
            val intent = Intent("android.media.action.MEDIA_PLAY_FROM_SEARCH")
            intent.putExtra(SearchManager.QUERY, song)
            intent.putExtra("android.intent.extra.focus", "vnd.android.cursor.item/audio")
            startActivity(intent)
            respond("\u0647\u0627\u0643\u0647\u0627 $song")
        } catch (e: Exception) {
            log("\u062E\u0637\u0623 \u062A\u0634\u063A\u064A\u0644 \u0627\u0644\u062A\u0627\u0644\u064A: ${e.message}")
            respond("\u0645\u0627 \u0642\u062F\u0631\u062A \u0623\u0634\u063A\u0644 \u0627\u0644\u0623\u063A\u0646\u064A\u0629")
        }
    }

    private fun addSongToPlaylist(name: String) {
        if (name.isBlank()) {
            respond("\u0642\u0644\u064A \u0627\u0633\u0645 \u0627\u0644\u0623\u063A\u0646\u064A\u0629 \u064A\u0644\u064A \u0628\u062F\u0643 \u062A\u0636\u064A\u0641\u0647\u0627")
            return
        }
        playlistManager.addSong(name)
        respond("\u0636\u0641\u062A $name \u0644\u0644\u0642\u0627\u0626\u0645\u0629")
    }

    private fun showPlaylist(): String {
        val list = playlistManager.getPlaylist()
        if (list.isEmpty()) return "\u0627\u0644\u0642\u0627\u0626\u0645\u0629 \u0641\u0627\u0636\u064A\u0629 \u0644\u0633\u0627"
        return "\u0642\u0627\u0626\u0645\u062A\u0643: " + list.joinToString("\u060C ")
    }

    private fun clearPlaylist() {
        playlistManager.clear()
        respond("\u0645\u0633\u062D\u062A \u0627\u0644\u0642\u0627\u0626\u0645\u0629")
    }

    private fun addToPlaylistAndSetCurrent(name: String) {
        playlistManager.addAndSetCurrent(name)
    }

    // ---------------- Lecture mode ----------------

    private fun startLectureMode() {
        lectureMode = true
        lectureBuffer = StringBuilder()
        statusText.text = "\uD83D\uDCDD \u0648\u0636\u0639 \u0627\u0644\u0645\u062D\u0627\u0636\u0631\u0629... \u0642\u0648\u0644 \"\u0648\u0642\u0641 \u0627\u0644\u0645\u062D\u0627\u0636\u0631\u0629\" \u0644\u0645\u0627 \u062A\u062E\u0644\u0635"
        respond("\u062A\u0645\u0627\u0645\u060C \u0628\u0644\u0634\u062A \u0623\u0633\u0645\u0639 \u0627\u0644\u0645\u062D\u0627\u0636\u0631\u0629\u060C \u0642\u0644\u064A \u0648\u0642\u0641 \u0627\u0644\u0645\u062D\u0627\u0636\u0631\u0629 \u0644\u0645\u0627 \u062A\u062E\u0644\u0635")
    }

    private fun stopLectureModeAndSummarize() {
        lectureMode = false
        statusText.text = if (continuousMode) "\uD83D\uDD34 \u0628\u0633\u0645\u0639\u0643... \u0642\u0648\u0644 \"\u062C\u0627\u0631\u0641\u0633\"" else "\u26AA \u0645\u062A\u0648\u0642\u0641\u060C \u062F\u0648\u0633 \u0644\u062A\u0634\u063A\u0651\u0644 \u0627\u0644\u0627\u0633\u062A\u0645\u0627\u0639"
        val fullText = lectureBuffer.toString().trim()
        if (fullText.isBlank()) {
            respond("\u0645\u0627 \u0633\u062C\u0644\u062A \u0634\u064A\u060C \u062C\u0631\u0628 \u062A\u0627\u0646\u064A")
            return
        }
        respond("\u062E\u0644\u0635\u062A\u060C \u0639\u0645 \u0644\u062E\u0635\u0644\u0643 \u0627\u0644\u0645\u062D\u0627\u0636\u0631\u0629...")
        if (GEMINI_API_KEY.isNotBlank()) {
            summarizeLecture(fullText)
        } else {
            saveNote("\u0645\u062D\u0627\u0636\u0631\u0629: $fullText")
            respond("\u0633\u062C\u0644\u062A \u0627\u0644\u0645\u062D\u0627\u0636\u0631\u0629 \u0643\u0627\u0645\u0644\u0629 \u0643\u0645\u0644\u0627\u062D\u0638\u0629\u060C \u0628\u0633 \u0645\u062D\u062A\u0627\u062C \u0645\u0641\u062A\u0627\u062D Gemini \u0639\u0634\u0627\u0646 \u0623\u0644\u062E\u0635\u0644\u0643 \u064A\u0627\u0647\u0627")
        }
    }

    private fun summarizeLecture(text: String) {
        val prompt = "\u0644\u062E\u0635\u0644\u064A \u0647\u0627\u064A \u0627\u0644\u0645\u062D\u0627\u0636\u0631\u0629 \u0628\u0646\u0642\u0627\u0637 \u0645\u0646\u0638\u0645\u0629 \u0648\u0628\u0633\u064A\u0637\u0629\u060C \u0648\u0641\u0633\u0631\u0644\u064A \u0623\u0647\u0645 \u0627\u0644\u0623\u0641\u0643\u0627\u0631 \u0628\u0623\u0633\u0644\u0648\u0628 \u0633\u0647\u0644: $text"
        val jsonBody = JSONObject().apply {
            put("contents", JSONArray().put(
                JSONObject().apply {
                    put("parts", JSONArray().put(
                        JSONObject().apply { put("text", prompt) }
                    ))
                }
            ))
        }
        val body = jsonBody.toString().toRequestBody("application/json".toMediaTypeOrNull())
        val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.6-flash:generateContent"
        val request = Request.Builder()
            .url(url)
            .addHeader("Content-Type", "application/json")
            .addHeader("x-goog-api-key", GEMINI_API_KEY)
            .post(body)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                saveNote("\u0645\u062D\u0627\u0636\u0631\u0629 (\u0628\u062F\u0648\u0646 \u062A\u0644\u062E\u064A\u0635): $text")
                runOnUiThread { respond("\u0645\u0627 \u0642\u062F\u0631\u062A \u0623\u0648\u0635\u0644 \u0644\u0644\u0646\u062A\u060C \u0628\u0633 \u062D\u0641\u0638\u062A \u0627\u0644\u0645\u062D\u0627\u0636\u0631\u0629 \u062E\u0627\u0645 \u0643\u0645\u0644\u0627\u062D\u0638\u0629") }
            }

            override fun onResponse(call: Call, response: Response) {
                try {
                    val responseText = response.body?.string() ?: ""
                    val json = JSONObject(responseText)
                    val summary = json.getJSONArray("candidates")
                        .getJSONObject(0)
                        .getJSONObject("content")
                        .getJSONArray("parts")
                        .getJSONObject(0)
                        .getString("text")
                    saveNote("\u0645\u0644\u062E\u0635 \u0645\u062D\u0627\u0636\u0631\u0629: ${summary.trim()}")
                    runOnUiThread { respond(summary.trim()) }
                } catch (e: Exception) {
                    saveNote("\u0645\u062D\u0627\u0636\u0631\u0629 (\u0628\u062F\u0648\u0646 \u062A\u0644\u062E\u064A\u0635): $text")
                    runOnUiThread { respond("\u0633\u062C\u0644\u062A \u0627\u0644\u0645\u062D\u0627\u0636\u0631\u0629 \u0628\u0633 \u0645\u0627 \u0642\u062F\u0631\u062A \u0623\u0644\u062E\u0635\u0647\u0627") }
                }
            }
        })
    }

    // ---------------- User name / email / phone ----------------

    private fun saveUserName(name: String) {
        userName = name
        getSharedPreferences("jarvis_prefs", Context.MODE_PRIVATE).edit()
            .putString("user_name", name).apply()
    }

    private fun saveUserEmail(email: String) {
        userEmail = email
        getSharedPreferences("jarvis_prefs", Context.MODE_PRIVATE).edit()
            .putString("user_email", email).apply()
    }

    private fun saveUserPhone(phone: String) {
        userPhone = phone
        getSharedPreferences("jarvis_prefs", Context.MODE_PRIVATE).edit()
            .putString("user_phone", phone).apply()
    }

    // ---------------- SMS: reading, explaining, sending ----------------

    // \u064A\u062F\u0648\u0631 \u0639\u0644\u0649 \u062C\u0647\u0627\u062A \u0627\u0644\u0627\u062A\u0635\u0627\u0644 \u0639\u0646 \u0627\u0633\u0645 \u0648\u064A\u0631\u062C\u0639 \u0627\u0644\u0631\u0642\u0645\u060C \u0623\u0648 null \u0625\u0630\u0627 \u0645\u0627\u0644\u0642\u0627\u0634
    private fun lookupContactNumber(name: String): String? {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            return null
        }
        val cursor = contentResolver.query(
            ContactsContract.Contacts.CONTENT_URI,
            null,
            "${ContactsContract.Contacts.DISPLAY_NAME} LIKE ?",
            arrayOf("%$name%"),
            null
        )
        cursor?.use { c ->
            if (c.moveToFirst()) {
                val contactId = c.getString(c.getColumnIndexOrThrow(ContactsContract.Contacts._ID))
                val phoneCursor = contentResolver.query(
                    ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                    null,
                    "${ContactsContract.CommonDataKinds.Phone.CONTACT_ID} = ?",
                    arrayOf(contactId),
                    null
                )
                phoneCursor?.use { pc ->
                    if (pc.moveToFirst()) {
                        return pc.getString(
                            pc.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.NUMBER)
                        )
                    }
                }
            }
        }
        return null
    }

    private fun readRecentSms(count: Int = 5): List<Pair<String, String>> {
        val results = mutableListOf<Pair<String, String>>()
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_SMS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            return results
        }
        val uri = Telephony.Sms.CONTENT_URI
        val projection = arrayOf(Telephony.Sms.ADDRESS, Telephony.Sms.BODY)
        contentResolver.query(uri, projection, null, null, "${Telephony.Sms.DATE} DESC")?.use { cursor ->
            val addressIndex = cursor.getColumnIndex(Telephony.Sms.ADDRESS)
            val bodyIndex = cursor.getColumnIndex(Telephony.Sms.BODY)
            var read = 0
            while (cursor.moveToNext() && read < count) {
                val address = cursor.getString(addressIndex) ?: "\u063A\u064A\u0631 \u0645\u0639\u0631\u0648\u0641"
                val body = cursor.getString(bodyIndex) ?: ""
                results.add(address to body)
                read++
            }
        }
        return results
    }

    private fun handleReadMessages() {
        val messages = readRecentSms(3)
        if (messages.isEmpty()) {
            respond("\u0645\u0627\u0643\u0627\u0634 \u0631\u0633\u0627\u0626\u0644 \u0623\u0648 \u0645\u0627\u0639\u0646\u062F\u0643\u0634 \u0635\u0644\u0627\u062D\u064A\u0629 \u0642\u0631\u0627\u0621\u0629 \u0627\u0644\u0631\u0633\u0627\u0626\u0644")
            return
        }
        val summary = messages.joinToString(". ") { (from, body) -> "\u0645\u0646 $from: $body" }
        respond(summary)
    }

    private fun handleExplainLastMessage() {
        val messages = readRecentSms(1)
        if (messages.isEmpty()) {
            respond("\u0645\u0627\u0643\u0627\u0634 \u0631\u0633\u0627\u0626\u0644 \u0623\u0648 \u0645\u0627\u0639\u0646\u062F\u0643\u0634 \u0635\u0644\u0627\u062D\u064A\u0629 \u0642\u0631\u0627\u0621\u0629 \u0627\u0644\u0631\u0633\u0627\u0626\u0644")
            return
        }
        val (from, body) = messages[0]
        if (GEMINI_API_KEY.isBlank()) {
            respond("\u0622\u062E\u0631 \u0631\u0633\u0627\u0644\u0629 \u0645\u0646 $from: $body")
            return
        }
        askGemini("\u0627\u0634\u0631\u062D\u0644\u064A \u0628\u0627\u062E\u062A\u0635\u0627\u0631 \u0647\u0627\u0630\u0647 \u0627\u0644\u0631\u0633\u0627\u0644\u0629 \u0648\u0634\u0648 \u0627\u0644\u0645\u0642\u0635\u0648\u062F \u0645\u0646\u0647\u0627: $body")
    }

    private fun sendSmsToContact(name: String, message: String) {
        val number = lookupContactNumber(name)
        if (number == null) {
            respond("\u0645\u0627 \u0644\u0642\u064A\u062A $name \u0641\u064A \u062C\u0647\u0627\u062A \u0627\u0644\u0627\u062A\u0635\u0627\u0644")
            return
        }
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.SEND_SMS), REQ_PERMISSIONS)
            respond("\u062E\u0644\u064A\u0646\u064A \u0646\u0637\u0644\u0628 \u0635\u0644\u0627\u062D\u064A\u0629 \u0627\u0644\u0631\u0633\u0627\u0626\u0644 \u0623\u0648\u0644")
            return
        }
        try {
            val smsManager = getSystemService(SmsManager::class.java)
            smsManager.sendTextMessage(number, null, message, null, null)
            respond("\u0628\u0639\u062B\u062A \u0627\u0644\u0631\u0633\u0627\u0644\u0629 \u0644\u0640 $name")
        } catch (e: Exception) {
            log("\u062E\u0637\u0623 \u0625\u0631\u0633\u0627\u0644 \u0627\u0644\u0631\u0633\u0627\u0644\u0629: ${e.message}")
            respond("\u0645\u0627 \u0642\u062F\u0631\u062A \u0646\u0628\u0639\u062B \u0627\u0644\u0631\u0633\u0627\u0644\u0629")
        }
    }

    // ---------------- Language switching ----------------

    private fun handleLanguageSwitch(cmd: String) {
        when {
            cmd.contains("\u0639\u0631\u0628\u064A") || cmd.contains("arabic") || cmd.contains("arabe") -> {
                currentLangCode = "ar"
                tts.language = Locale("ar")
                applyMaleVoiceForCurrentLanguage()
                respond("\u062A\u0645\u0627\u0645\u060C \u0631\u062D \u0623\u0633\u0645\u0639\u0643 \u0628\u0627\u0644\u0639\u0631\u0628\u064A \u0647\u0644\u0642\u060C \u0623\u0646\u0627 \u0644\u0633\u0627 \u062C\u0627\u0631\u0641\u0633")
            }
            cmd.contains("\u0641\u0631\u0646\u0633") || cmd.contains("french") || cmd.contains("fran\u00E7ais") -> {
                currentLangCode = "fr"
                tts.language = Locale.FRENCH
                applyMaleVoiceForCurrentLanguage()
                respond("D'accord, je t'\u00E9coute en fran\u00E7ais maintenant, je suis toujours Jarvis")
            }
            cmd.contains("\u0627\u0646\u062C\u0644\u064A\u0632") || cmd.contains("english") || cmd.contains("anglais") -> {
                currentLangCode = "en"
                tts.language = Locale.ENGLISH
                applyMaleVoiceForCurrentLanguage()
                respond("Okay, I'm listening in English now, still Jarvis")
            }
            cmd.contains("\u0627\u0633\u0628\u0627\u0646") || cmd.contains("spanish") || cmd.contains("espa\u00F1ol") -> {
                currentLangCode = "es"
                tts.language = Locale("es")
                applyMaleVoiceForCurrentLanguage()
                respond("Vale, ahora te escucho en espa\u00F1ol, sigo siendo Jarvis")
            }
            cmd.contains("\u0631\u0648\u0633") || cmd.contains("russian") || cmd.contains("\u0440\u0443\u0441\u0441\u043A") -> {
                currentLangCode = "ru"
                tts.language = Locale("ru")
                applyMaleVoiceForCurrentLanguage()
                respond("\u0425\u043E\u0440\u043E\u0448\u043E, \u0442\u0435\u043F\u0435\u0440\u044C \u044F \u0441\u043B\u0443\u0448\u0430\u044E \u043F\u043E-\u0440\u0443\u0441\u0441\u043A\u0438, \u044F \u0432\u0441\u0451 \u0442\u043E\u0442 \u0436\u0435 \u0414\u0436\u0430\u0440\u0432\u0438\u0441")
            }
            cmd.contains("\u0645\u0627\u0646\u062F\u0631\u064A\u0646") || cmd.contains("\u0635\u064A\u0646\u064A") || cmd.contains("mandarin") ||
                    cmd.contains("chinese") || cmd.contains("\u4E2D\u6587") -> {
                currentLangCode = "zh"
                tts.language = Locale.SIMPLIFIED_CHINESE
                applyMaleVoiceForCurrentLanguage()
                respond("\u597D\u7684\uFF0C\u73B0\u5728\u6211\u542C\u4E2D\u6587\u4E86\uFF0C\u6211\u8FD8\u662F\u8D3E\u7EF4\u65AF")
            }
            else -> {
                respond("\u0642\u0644\u064A \u0639\u0631\u0628\u064A\u060C \u0641\u0631\u0646\u0633\u064A\u060C \u0627\u0646\u062C\u0644\u064A\u0632\u064A\u060C \u0627\u0633\u0628\u0627\u0646\u064A\u060C \u0631\u0648\u0633\u064A\u060C \u0623\u0648 \u0645\u0627\u0646\u062F\u0631\u064A\u0646")
            }
        }
    }

    // ---------------- Battery & date ----------------

    private fun getBatteryLevel(): Int {
        val bm = getSystemService(Context.BATTERY_SERVICE) as BatteryManager
        return bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
    }

    // ---------------- Volume & ringer mode ----------------

    private fun adjustVolume(up: Boolean) {
        val am = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        am.adjustStreamVolume(
            AudioManager.STREAM_MUSIC,
            if (up) AudioManager.ADJUST_RAISE else AudioManager.ADJUST_LOWER,
            AudioManager.FLAG_SHOW_UI
        )
    }

    private fun setRingerMode(mode: Int) {
        try {
            val am = getSystemService(Context.AUDIO_SERVICE) as AudioManager
            am.ringerMode = mode
            respond("\u062A\u0645 \u062A\u063A\u064A\u064A\u0631 \u0648\u0636\u0639 \u0627\u0644\u0635\u0648\u062A")
        } catch (e: SecurityException) {
            respond("\u0628\u062F\u064A \u0625\u0630\u0646 \u0627\u0644\u0648\u0635\u0648\u0644 \u0644\u0625\u0639\u062F\u0627\u062F\u0627\u062A \u0639\u062F\u0645 \u0627\u0644\u0625\u0632\u0639\u0627\u062C \u0623\u0648\u0644 \u0645\u0646 \u0625\u0639\u062F\u0627\u062F\u0627\u062A \u0627\u0644\u0647\u0627\u062A\u0641")
        }
    }

    // ---------------- Alarm ----------------

    private fun handleSetAlarm(cmd: String) {
        val regex = Regex("""(\d{1,2})(?:[:\u0648]\s*(\d{1,2}))?""")
        val match = regex.find(cmd)
        if (match == null) {
            respond("\u0642\u0644\u064A \u0627\u0644\u0648\u0642\u062A \u0647\u064A\u0643: \u0645\u0646\u0628\u0647 \u0627\u0644\u0633\u0627\u0639\u0629 7")
            return
        }
        val hour = match.groupValues[1].toIntOrNull() ?: return
        val minute = match.groupValues[2].toIntOrNull() ?: 0
        val intent = Intent(AlarmClock.ACTION_SET_ALARM).apply {
            putExtra(AlarmClock.EXTRA_HOUR, hour)
            putExtra(AlarmClock.EXTRA_MINUTES, minute)
            putExtra(AlarmClock.EXTRA_MESSAGE, "\u0645\u0646\u0628\u0647 \u0645\u0646 \u062C\u0627\u0631\u0641\u0633")
        }
        try {
            startActivity(intent)
            respond("\u062A\u0645\u0627\u0645\u060C \u062D\u0637\u064A\u062A \u0645\u0646\u0628\u0647 \u0627\u0644\u0633\u0627\u0639\u0629 $hour \u0648 $minute")
        } catch (e: Exception) {
            log("\u062E\u0637\u0623 \u0627\u0644\u0645\u0646\u0628\u0647: ${e.message}")
            respond("\u0645\u0627 \u0644\u0642\u064A\u062A \u062A\u0637\u0628\u064A\u0642 \u0645\u0646\u0628\u0647 \u0639\u0644\u0649 \u0647\u0627\u062A\u0641\u0643")
        }
    }

    // ---------------- Search & navigation ----------------

    private fun extractSearchQuery(cmd: String): String {
        val marker = if (cmd.contains("\u0627\u0628\u062D\u062B \u0639\u0646")) "\u0627\u0628\u062D\u062B \u0639\u0646" else "\u062F\u0648\u0631 \u0644\u064A \u0639\u0644\u0649"
        return extractNameAfter(cmd, marker)
    }

    private fun searchGoogle(query: String) {
        if (query.isBlank()) {
            respond("\u0642\u0644\u064A \u0634\u0648 \u0628\u062F\u0643 \u0623\u0628\u062D\u062B \u0639\u0646\u0647")
            return
        }
        try {
            val intent = Intent(Intent.ACTION_WEB_SEARCH)
            intent.putExtra(SearchManager.QUERY, query)
            startActivity(intent)
            respond("\u0628\u062F\u0648\u0631 \u0644\u0643 \u0639\u0646 $query")
        } catch (e: Exception) {
            try {
                val browserIntent = Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("https://www.google.com/search?q=" + Uri.encode(query))
                )
                startActivity(browserIntent)
                respond("\u0628\u062F\u0648\u0631 \u0644\u0643 \u0639\u0646 $query")
            } catch (e2: Exception) {
                respond("\u0645\u0627 \u0642\u062F\u0631\u062A \u0623\u0641\u062A\u062D \u0627\u0644\u0628\u062D\u062B")
            }
        }
    }

    private fun navigateTo(place: String, mode: String = "driving") {
        if (place.isBlank()) {
            respond("\u0642\u0644\u064A \u0648\u064A\u0646 \u0628\u062F\u0643 \u062A\u0631\u0648\u062D")
            return
        }
        val encodedPlace = Uri.encode(place)
        val mapsUri = Uri.parse(
            "https://www.google.com/maps/dir/?api=1&destination=$encodedPlace&travelmode=$mode"
        )
        try {
            val mapIntent = Intent(Intent.ACTION_VIEW, mapsUri)
            mapIntent.setPackage("com.google.android.apps.maps")
            startActivity(mapIntent)
            respondNavigation(place, mode)
        } catch (e: Exception) {
            try {
                startActivity(Intent(Intent.ACTION_VIEW, mapsUri))
                respondNavigation(place, mode)
            } catch (e2: Exception) {
                respond("\u0645\u0627 \u0642\u062F\u0631\u062A \u0623\u0641\u062A\u062D \u0627\u0644\u062E\u0631\u0627\u0626\u0637")
            }
        }
    }

    private fun respondNavigation(place: String, mode: String) {
        if (mode == "walking") {
            respond("\u0647\u0627\u0643 \u0637\u0631\u064A\u0642 \u0627\u0644\u0645\u0634\u064A \u0627\u0644\u0649 $place")
        } else {
            respond("\u062C\u0627\u0631\u064A \u0641\u062A\u062D \u0627\u0644\u0637\u0631\u064A\u0642 \u0627\u0644\u0649 $place")
        }
    }

    // ---------------- Jokes ----------------

    private val jokes = listOf(
        "\u0648\u0627\u062D\u062F \u0633\u0623\u0644 \u0635\u0627\u062D\u0628\u0648: \u0639\u0644\u0627\u0634 \u0627\u0644\u062F\u064A\u0643 \u064A\u0635\u064A\u062D \u0627\u0644\u0635\u0628\u0627\u062D\u061F \u0642\u0627\u0644\u0647: \u0628\u0627\u0634 \u064A\u0641\u0648\u0642\u0643 \u0642\u0628\u0644 \u0645\u0627 \u062A\u0641\u0648\u062A\u0647 \u0628\u0627\u0644\u0646\u0648\u0645.",
        "\u0637\u0641\u0644 \u0633\u0623\u0644 \u0628\u0627\u0628\u0627\u0647: \u0628\u0627\u0628\u0627 \u0648\u064A\u0646 \u062A\u062D\u0628 \u062A\u0643\u0648\u0646 \u0644\u0645\u0627 \u062A\u0643\u0628\u0631\u061F \u0642\u0627\u0644\u0647: \u0647\u0627\u062F\u064A \u0647\u064A \u0627\u0644\u0645\u0634\u0643\u0644\u0629\u060C \u0623\u0646\u0627 \u0643\u0628\u0631\u062A \u0648\u0645\u0627 \u0632\u0644\u062A \u0645\u0627 \u0639\u0631\u0641\u062A\u0634.",
        "\u0648\u0627\u062D\u062F \u062F\u062E\u0644 \u064A\u0634\u062A\u0631\u064A \u0633\u0627\u0639\u0629\u060C \u0642\u0627\u0644\u0647 \u0627\u0644\u0628\u064A\u0627\u0639: \u0647\u0627\u064A \u0627\u0644\u0633\u0627\u0639\u0629 \u0628\u062A\u0639\u064A\u0634 \u0645\u0639\u0627\u0643 \u0644\u0644\u0623\u0628\u062F. \u0642\u0627\u0644\u0647: \u0637\u064A\u0628 \u0623\u0639\u0637\u064A\u0646\u064A \u0648\u062D\u062F\u0629 \u062A\u0639\u064A\u0634 \u0623\u0633\u0628\u0648\u0639 \u0628\u0633\u060C \u062E\u0627\u064A\u0641 \u0646\u0636\u064A\u0639\u0647\u0627.",
        "\u0639\u0644\u0627\u0634 \u0627\u0644\u0643\u0645\u0628\u064A\u0648\u062A\u0631 \u0645\u0627 \u0628\u064A\u062D\u0633 \u0628\u0627\u0644\u0628\u0631\u062F\u061F \u0644\u0623\u0646\u0647 \u0639\u0646\u062F\u0647 Windows \u0645\u0633\u0643\u0631\u0629 \u0632\u064A\u0646."
    )

    // ---------------- Notes ----------------

    private fun saveNote(note: String) {
        notesManager.save(note)
    }

    private fun readNotes(): String {
        val notes = notesManager.getAll()
        if (notes.isEmpty()) return "\u0645\u0627 \u0639\u0646\u062F\u0643 \u0645\u0644\u0627\u062D\u0638\u0627\u062A \u0645\u062D\u0641\u0648\u0638\u0629"
        return "\u0645\u0644\u0627\u062D\u0638\u0627\u062A\u0643: " + notes.joinToString("\u060C ")
    }

    // ---------------- Natural response variety ----------------

    private val flashOnPhrases = listOf(
        "\u062F\u0627\u064A\u0631\u0644\u0643 \u0627\u0644\u0641\u0644\u0627\u0634", "\u062A\u0645\u0627\u0645\u060C \u0648\u0644\u0651\u0649 \u0627\u0644\u0641\u0644\u0627\u0634 \u0634\u0627\u0639\u0644", "\u0647\u0627\u0643 \u0627\u0644\u0641\u0644\u0627\u0634 \u0634\u0627\u0639\u0644"
    )
    private val flashOffPhrases = listOf(
        "\u0637\u0641\u064A\u062A \u0627\u0644\u0641\u0644\u0627\u0634", "\u062A\u0645\u0627\u0645\u060C \u0627\u0644\u0641\u0644\u0627\u0634 \u0637\u0627\u0641\u064A \u0647\u0644\u0642", "\u062E\u0644\u0627\u0635 \u0637\u0641\u0627\u0647"
    )
    private val musicOnPhrases = listOf(
        "\u0647\u0627\u0643\u0647\u0627 \u0627\u0644\u0645\u0648\u0633\u064A\u0642\u0649 \u0628\u062F\u0627\u062A", "\u062A\u0645\u0627\u0645\u060C \u0646\u062F\u064A\u0631\u0644\u0643 \u0645\u0648\u0633\u064A\u0642\u0649", "\u0627\u0633\u062A\u0645\u062A\u0639 \u0628\u0627\u0644\u0645\u0648\u0633\u064A\u0642\u0649"
    )
    private val musicOffPhrases = listOf(
        "\u0648\u0642\u0641\u062A \u0627\u0644\u0645\u0648\u0633\u064A\u0642\u0649", "\u062A\u0645\u0627\u0645\u060C \u0633\u0643\u062A\u0647\u0627"
    )

    // ---------------- Radial module menu (APPS/SYS/MAP/3D/CLK) ----------------

    // \u062E\u0631\u064A\u0637\u0629 \u0627\u0633\u0645 \u0627\u0644\u062A\u0637\u0628\u064A\u0642 -> \u0627\u0633\u0645 \u0627\u0644\u062D\u0632\u0645\u0629\u060C \u062A\u062A\u0645\u0644\u0627 \u0645\u0644\u064A \u0646\u0641\u062A\u062D\u0648 \u0642\u0627\u0626\u0645\u0629 APPS
    private val appNameToPackage = mutableMapOf<String, String>()

    private val jarvisModuleManager by lazy { JarvisModuleManager() }

    private val commandRouter by lazy {
        JarvisCommandRouter(
            legacyHandler = { text -> handleCommandInternal(text) },
            appLauncher = { spokenName -> tryLaunchAppByName(spokenName) },
            systemHandler = { intent -> JarvisSystemModule(this) { msg -> respond(msg) }.execute(intent) },
            appsHandler = { intent ->
                when (intent.type) {
                    JarvisIntentType.APPS_SHOW -> { showAppsModule(); true }
                    JarvisIntentType.APPS_HIDE -> { jarvisDial.setAppsModule(false); true }
                    else -> false
                }
            },
            clockHandler = { intent ->
                when (intent.type) {
                    JarvisIntentType.CLOCK_SHOW -> { jarvisDial.setClockVisible(true); respond("\u0638\u0647\u0631\u062A \u0627\u0644\u0633\u0627\u0639\u0629"); true }
                    JarvisIntentType.CLOCK_HIDE -> { jarvisDial.setClockVisible(false); respond("\u062E\u0641\u064A\u062A \u0627\u0644\u0633\u0627\u0639\u0629"); true }
                    else -> false
                }
            },
            mapHandler = { intent -> JarvisMapModule(this) { msg -> respond(msg) }.execute(intent) },
            contactsHandler = { intent ->
                JarvisContactsModule(
                    activity = this,
                    speak = { msg -> respond(msg) },
                    askAi = { prompt -> askGemini(prompt) },
                    requestPermission = { permission, code ->
                        ActivityCompat.requestPermissions(this, arrayOf(permission), code)
                    },
                    reqPermissionsCode = REQ_PERMISSIONS,
                    reqContactsCode = REQ_CONTACTS
                ).execute(intent)
            },
            geologyHandler = { intent ->
                JarvisGeologyModule(
                    notebook = fieldNotebook,
                    compass = geoCompass,
                    speak = { msg -> respond(msg) },
                    getCurrentLocation = { Pair(lastKnownLat, lastKnownLon) }
                ).execute(intent)
            },
            safetyHandler = { intent ->
                JarvisSafetyModule(
                    activity = this,
                    speak = { msg -> respond(msg) },
                    askAi = { prompt -> askGemini(prompt) },
                    getCurrentLocation = { Pair(lastKnownLat, lastKnownLon) }
                ).execute(intent)
            },
            startListeningHandler = {
                if (!continuousMode) enableContinuousMode() else startListening()
            },
            moduleManager = jarvisModuleManager,
            onModuleToggled = { module, enabled ->
                when (module.type) {
                    JarvisModuleType.CLOCK -> jarvisDial.setClockVisible(enabled)
                    JarvisModuleType.APPS -> if (!enabled) jarvisDial.setAppsModule(false)
                    else -> { /* SYSTEM/MAP: \u0645\u062C\u0631\u062F \u062D\u0627\u0644\u0629 \u0645\u0633\u062C\u0644\u0629 \u062D\u0627\u0644\u064A\u0627\u064B\u060C \u0645\u062A\u0627\u062D\u0629 \u0644\u0644\u062A\u0648\u0633\u0639 \u0644\u0627\u062D\u0642\u0627\u064B */ }
                }
                respond(
                    if (enabled) "\u0641\u0639\u0651\u0644\u062A \u0648\u062D\u062F\u0629 ${module.title}"
                    else "\u0639\u0637\u0651\u0644\u062A \u0648\u062D\u062F\u0629 ${module.title}"
                )
            }
        )
    }

    // \u064A\u062F\u0648\u0631 \u0639\u0644\u0649 \u062A\u0637\u0628\u064A\u0642 \u0645\u062B\u0628\u062A \u0628\u0627\u0644\u0627\u0633\u0645 \u0627\u0644\u0645\u0646\u0637\u0648\u0642 (\u0645\u0637\u0627\u0628\u0642\u0629 \u062C\u0632\u0626\u064A\u0629) \u0648\u064A\u0641\u062A\u062D\u0647 \u0625\u0630\u0627 \u0644\u0642\u0627\u0647\u060C \u064A\u0631\u062C\u0639 true/false \u0644\u0644\u0645\u0648\u062C\u0651\u0647
    private fun tryLaunchAppByName(spokenName: String): Boolean {
        if (spokenName.isBlank()) return false
        return try {
            val pm = packageManager
            val match = pm.getInstalledApplications(PackageManager.GET_META_DATA)
                .filter { pm.getLaunchIntentForPackage(it.packageName) != null }
                .firstOrNull { pm.getApplicationLabel(it).toString().contains(spokenName, ignoreCase = true) }
                ?: return false
            val label = pm.getApplicationLabel(match).toString()
            openApp(match.packageName, label)
            true
        } catch (e: Exception) {
            log("\u062E\u0637\u0623 \u0641\u062A\u062D \u0627\u0644\u062A\u0637\u0628\u064A\u0642 \u0628\u0627\u0644\u0627\u0633\u0645: ${e.message}")
            false
        }
    }

    // ---------------- IR remote control ----------------

    private fun sendIrCommand(action: String) {
        if (!irRemote.hasIrBlaster()) {
            respond("\u0627\u0644\u062C\u0647\u0627\u0632 \u0645\u0627\u0641\u064A\u0634 \u0645\u0631\u0633\u0644 \u0623\u0634\u0639\u0629 \u062A\u062D\u062A \u0627\u0644\u062D\u0645\u0631\u0627\u0621")
            return
        }
        val sent = when (action) {
            "power" -> irRemote.sendPowerToggle()
            "volume_up" -> irRemote.sendVolumeUp()
            "volume_down" -> irRemote.sendVolumeDown()
            else -> false
        }
        if (sent) {
            respond("\u062A\u0645 \u0625\u0631\u0633\u0627\u0644 \u0625\u0634\u0627\u0631\u0629 \u0627\u0644\u0631\u064A\u0645\u0648\u062A")
        } else {
            respond("\u0645\u0627 \u0642\u062F\u0631\u062A \u0623\u0631\u0633\u0644 \u0627\u0644\u0625\u0634\u0627\u0631\u0629")
        }
    }

    // ---------------- Security: defense mode + permission scanner ----------------

    private fun toggleDefenseMode(active: Boolean) {
        defenseModeActive = active
        if (::jarvisDial.isInitialized) {
            jarvisDial.setDefenseMode(active)
        }
        if (active) {
            val battery = securityScanner.getBatteryPercent()
            val memory = securityScanner.getMemoryUsagePercent()
            respond("\u0648\u0636\u0639 \u0627\u0644\u062F\u0641\u0627\u0639 \u0645\u0641\u0639\u0651\u0644. \u0627\u0644\u0628\u0637\u0627\u0631\u064A\u0629 $battery\u066A\u060C \u0627\u0633\u062A\u0647\u0644\u0627\u0643 \u0627\u0644\u0630\u0627\u0643\u0631\u0629 $memory\u066A")
        } else {
            respond("\u0648\u0636\u0639 \u0627\u0644\u062F\u0641\u0627\u0639 \u0645\u0637\u0641\u0651\u0649")
        }
    }

    private fun runSecurityScan(statusView: TextView? = null) {
        respond("\u0646\u0641\u062D\u0635 \u0627\u0644\u062A\u0637\u0628\u064A\u0642\u0627\u062A \u0648\u0627\u0644\u0635\u0644\u0627\u062D\u064A\u0627\u062A...")
        Thread {
            val riskyApps = securityScanner.scanInstalledApps()
            val devOptionsOn = securityScanner.isDeveloperOptionsEnabled()
            runOnUiThread {
                if (riskyApps.isEmpty()) {
                    respond("\u0645\u0627\u0644\u0642\u064A\u062A \u062A\u0637\u0628\u064A\u0642\u0627\u062A \u0639\u0646\u062F\u0647\u0627 \u0635\u0644\u0627\u062D\u064A\u0627\u062A \u062D\u0633\u0627\u0633\u0629 \u0645\u0645\u064A\u0632\u0629")
                    statusView?.text = "\u0645\u0627\u0641\u064A\u0634 \u062A\u0637\u0628\u064A\u0642\u0627\u062A \u062E\u0637\u064A\u0631\u0629"
                } else {
                    val top = riskyApps.take(3).joinToString(". ") { app ->
                        "${app.appName}: " + app.riskyPermissions.joinToString("\u060C ")
                    }
                    respond("\u0644\u0642\u064A\u062A ${riskyApps.size} \u062A\u0637\u0628\u064A\u0642 \u0639\u0646\u062F\u0647\u0627 \u0635\u0644\u0627\u062D\u064A\u0627\u062A \u062D\u0633\u0627\u0633\u0629. \u0623\u0628\u0631\u0632\u0647\u0627: $top")
                    statusView?.text = "${riskyApps.size} \u062A\u0637\u0628\u064A\u0642: $top"
                }
                if (devOptionsOn) {
                    log("\u062A\u0646\u0628\u064A\u0647: \u062E\u064A\u0627\u0631\u0627\u062A \u0627\u0644\u0645\u0637\u0648\u0631\u064A\u0646/USB Debugging \u0645\u0641\u0639\u0651\u0644\u0629 \u0639\u0644\u0649 \u0627\u0644\u062C\u0647\u0627\u0632")
                }
            }
        }.start()
    }

    private fun runSecurityScanForPanel(statusView: TextView) = runSecurityScan(statusView)

    // ---------------- Bluetooth ----------------

    private fun scanBluetoothDevices(statusView: TextView? = null) {
        if (!bluetoothHelper.isBluetoothAvailable()) {
            respond("\u0627\u0644\u062C\u0647\u0627\u0632 \u0645\u0627\u0641\u064A\u0634 Bluetooth")
            statusView?.text = "\u0645\u0627\u0641\u064A\u0634 Bluetooth"
            return
        }
        if (!bluetoothHelper.isBluetoothEnabled()) {
            respond("\u062E\u0644\u064A\u0646\u064A \u0646\u0634\u063A\u0644 Bluetooth \u0623\u0648\u0644")
            statusView?.text = "Bluetooth \u0645\u0637\u0641\u0651\u0649"
            return
        }
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        ) {
            respond("\u062E\u0644\u064A\u0646\u064A \u0646\u0637\u0644\u0628 \u0635\u0644\u0627\u062D\u064A\u0629 \u0627\u0644\u0645\u0648\u0642\u0639 \u0623\u0648\u0644 (\u0644\u0627\u0632\u0645\u0629 \u0644\u0644\u0628\u062D\u062B \u0639\u0646 Bluetooth)")
            requestNeededPermissions()
            return
        }
        respond("\u0646\u062F\u0648\u0631 \u0639\u0644\u0649 \u0623\u062C\u0647\u0632\u0629 Bluetooth \u0642\u0631\u064A\u0628\u0629")
        bluetoothHelper.startScan(
            durationMs = 6000L,
            onDeviceFound = { },
            onFinished = { devices ->
                runOnUiThread {
                    if (devices.isEmpty()) {
                        respond("\u0645\u0627\u0644\u0642\u064A\u062A \u0623\u064A \u062C\u0647\u0627\u0632 \u0642\u0631\u064A\u0628")
                        statusView?.text = "\u0645\u0627\u0644\u0642\u064A\u062A \u0623\u062C\u0647\u0632\u0629 \u0642\u0631\u064A\u0628\u0629"
                    } else {
                        val names = devices.take(5).joinToString("\u060C ") { it.name }
                        respond("\u0644\u0642\u064A\u062A ${devices.size} \u062C\u0647\u0627\u0632: $names")
                        statusView?.text = "${devices.size} \u062C\u0647\u0627\u0632: $names"
                    }
                }
            }
        )
    }

    private fun scanBluetoothDevicesForPanel(statusView: TextView) = scanBluetoothDevices(statusView)

    // ---------------- Free cloud backup (Supabase) ----------------

    private fun backupNotesToCloud() {
        if (!cloudSync.isConfigured()) {
            respond("\u0627\u0644\u0633\u062D\u0627\u0628\u0629 \u0627\u0644\u0633\u062D\u0627\u0628\u064A\u0629 \u063A\u064A\u0631 \u0645\u0636\u0628\u0648\u0637\u0629 \u0628\u0639\u062F")
            return
        }
        val notes = notesManager.getAll()
        val json = JSONArray(notes.toList()).toString()
        respond("\u0646\u062D\u0641\u0638 \u0645\u0644\u0627\u062D\u0638\u0627\u062A\u0643 \u0641\u064A \u0627\u0644\u0633\u062D\u0627\u0628\u0629")
        cloudSync.backup("notes", json) { success, error ->
            runOnUiThread {
                if (success) {
                    respond("\u062A\u0645 \u0627\u0644\u062D\u0641\u0638 \u0641\u064A \u0627\u0644\u0633\u062D\u0627\u0628\u0629")
                } else {
                    log("\u062E\u0637\u0623 \u0627\u0644\u0646\u0633\u062E \u0627\u0644\u0627\u062D\u062A\u064A\u0627\u0637\u064A: $error")
                    respond("\u0645\u0627 \u0642\u062F\u0631\u062A \u0646\u062D\u0641\u0638 \u0641\u064A \u0627\u0644\u0633\u062D\u0627\u0628\u0629")
                }
            }
        }
    }

    private fun restoreNotesFromCloud() {
        if (!cloudSync.isConfigured()) {
            respond("\u0627\u0644\u0633\u062D\u0627\u0628\u0629 \u0627\u0644\u0633\u062D\u0627\u0628\u064A\u0629 \u063A\u064A\u0631 \u0645\u0636\u0628\u0648\u0637\u0629 \u0628\u0639\u062F")
            return
        }
        respond("\u0646\u062C\u064A\u0628 \u0645\u0644\u0627\u062D\u0638\u0627\u062A\u0643 \u0645\u0646 \u0627\u0644\u0633\u062D\u0627\u0628\u0629")
        cloudSync.restore("notes") { success, result ->
            runOnUiThread {
                if (success && result != null) {
                    try {
                        val arr = JSONArray(result)
                        for (i in 0 until arr.length()) {
                            notesManager.save(arr.getString(i))
                        }
                        respond("\u062A\u0645 \u0627\u0633\u062A\u0631\u062C\u0627\u0639 ${arr.length()} \u0645\u0644\u0627\u062D\u0638\u0629 \u0645\u0646 \u0627\u0644\u0633\u062D\u0627\u0628\u0629")
                    } catch (e: Exception) {
                        log("\u062E\u0637\u0623 \u0642\u0631\u0627\u0621\u0629 \u0627\u0644\u0646\u0633\u062E\u0629 \u0627\u0644\u0627\u062D\u062A\u064A\u0627\u0637\u064A\u0629: ${e.message}")
                        respond("\u0627\u0644\u0646\u0633\u062E\u0629 \u0627\u0644\u0645\u062D\u0641\u0648\u0638\u0629 \u062A\u0627\u0644\u0641\u0629")
                    }
                } else {
                    respond("\u0645\u0627\u0644\u0642\u064A\u062A \u0646\u0633\u062E\u0629 \u0645\u062D\u0641\u0648\u0638\u0629 \u0641\u064A \u0627\u0644\u0633\u062D\u0627\u0628\u0629")
                }
            }
        }
    }

    // ---------------- Sidebar navigation (HOME/MAP/LAB/SYS/NET/AI) ----------------

    private fun startPerfMonitor() {
        if (!BuildConfig.DEBUG) return
        val monitor = findViewById<TextView>(R.id.perfMonitor)
        monitor.visibility = View.VISIBLE
        if (perfMonitorRunning) return
        perfMonitorRunning = true
        frameCount = 0
        lastFpsTimestamp = 0L
        Choreographer.getInstance().postFrameCallback(frameCallback)
    }

    private fun stopPerfMonitor() {
        perfMonitorRunning = false
        findViewById<TextView>(R.id.perfMonitor)?.visibility = View.GONE
    }

    private fun updatePerfMonitorText(fps: Int) {
        if (!BuildConfig.DEBUG) return
        val runtime = Runtime.getRuntime()
        val usedMb = (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024)
        val monitor = findViewById<TextView>(R.id.perfMonitor)
        monitor.text = "FPS: $fps | MEM: $usedMb MB"
        if (fps in 1..24) {
            monitor.setTextColor(android.graphics.Color.parseColor("#E07A5F"))
        } else {
            monitor.setTextColor(android.graphics.Color.parseColor("#4A808A"))
        }
    }

    // ---------------- Sidebar navigation (HOME/MAP/LAB/SYS/NET/AI) ----------------

    private fun setupSidebarUi() {
        val sidebarNav = findViewById<View>(R.id.sidebarNav)
        val contentArea = findViewById<View>(R.id.contentArea)
        val sidebarToggle = findViewById<TextView>(R.id.sidebarToggle)
        val topClock = findViewById<TextView>(R.id.topClock)

        // ---- \u062A\u0647\u064A\u0626\u0629 \u0627\u0644\u062E\u0631\u064A\u0637\u0629 \u0627\u0644\u0645\u0635\u063A\u0631\u0629 \u0627\u0644\u062F\u0627\u0626\u0631\u064A\u0629 \u062A\u062D\u062A \u0627\u0644\u0640 HUD ----
        miniMapView = findViewById(R.id.miniMapView)
        miniMapView.settings.javaScriptEnabled = true
        miniMapView.settings.domStorageEnabled = true
        miniMapView.settings.mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
        miniMapView.setBackgroundColor(android.graphics.Color.TRANSPARENT)
        miniMapView.setLayerType(View.LAYER_TYPE_SOFTWARE, null)
        miniMapView.addJavascriptInterface(MapBridgeInterface(), "MapBridge")
        miniMapView.loadUrl("file:///android_asset/mini_map.html")

        // ---- \u062A\u0647\u064A\u0626\u0629 \u0627\u0644\u0645\u062A\u0635\u0641\u062D \u0627\u0644\u0635\u063A\u064A\u0631 \u0627\u0644\u0645\u062F\u0645\u062C (\u0645\u062E\u0641\u064A \u0644\u0648\u062F \u0627\u0644\u0641\u062A\u062D) ----
        miniBrowserView = findViewById(R.id.miniBrowserView)
        miniBrowserView.settings.javaScriptEnabled = true
        miniBrowserView.settings.domStorageEnabled = true
        miniBrowserView.settings.mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
        miniBrowserView.setBackgroundColor(android.graphics.Color.TRANSPARENT)
        miniBrowserView.setLayerType(View.LAYER_TYPE_SOFTWARE, null)
        miniBrowserView.addJavascriptInterface(BrowserBridgeInterface(), "BrowserBridge")
        miniBrowserView.loadUrl("file:///android_asset/mini_browser.html")

        // \u062F\u0627\u0626\u0631\u0629 \u062D\u0642\u064A\u0642\u064A\u0629 \u0644\u0632\u0631 \u0627\u0644\u062A\u0628\u062F\u064A\u0644 (\u0628\u062F\u0648\u0646 \u0645\u0627 \u0646\u062D\u062A\u0627\u062C \u0645\u0644\u0641 drawable \u062C\u062F\u064A\u062F)
        sidebarToggle.background = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(android.graphics.Color.parseColor("#16232A"))
            setStroke(2, android.graphics.Color.parseColor("#3AA7B8"))
        }

        val navItems = mapOf(
            "HOME" to findViewById<TextView>(R.id.navHome),
            "MAP" to findViewById<TextView>(R.id.navMap),
            "LAB" to findViewById<TextView>(R.id.navLab),
            "SYS" to findViewById<TextView>(R.id.navSys),
            "NET" to findViewById<TextView>(R.id.navNet),
            "AI" to findViewById<TextView>(R.id.navAi)
        )
        val panels = mapOf(
            "HOME" to findViewById<View>(R.id.homePanel),
            "MAP" to findViewById<View>(R.id.mapPanel),
            "LAB" to findViewById<View>(R.id.labPanel),
            "SYS" to findViewById<View>(R.id.sysPanel),
            "NET" to findViewById<View>(R.id.netPanel),
            "AI" to findViewById<View>(R.id.aiPanel)
        )

        fun switchPanel(key: String) {
            panels.forEach { (k, panel) -> panel.visibility = if (k == key) View.VISIBLE else View.GONE }
            navItems.forEach { (k, item) ->
                if (k == key) {
                    item.setBackgroundColor(android.graphics.Color.parseColor("#16232A"))
                    item.setTextColor(android.graphics.Color.parseColor("#8DEFFF"))
                } else {
                    item.setBackgroundColor(android.graphics.Color.TRANSPARENT)
                    item.setTextColor(android.graphics.Color.parseColor("#5C7A82"))
                }
            }
            if (key == "SYS") refreshBatteryDisplay()
        }

        navItems.forEach { (key, item) ->
            item.setOnClickListener { switchPanel(key) }
        }

        // \u0632\u0631 \u0625\u062E\u0641\u0627\u0621/\u0625\u0638\u0647\u0627\u0631 \u0627\u0644\u0642\u0627\u0626\u0645\u0629 \u0627\u0644\u062C\u0627\u0646\u0628\u064A\u0629
        sidebarToggle.setOnClickListener {
            sidebarVisible = !sidebarVisible
            sidebarNav.visibility = if (sidebarVisible) View.VISIBLE else View.GONE
            val params = contentArea.layoutParams as ViewGroup.MarginLayoutParams
            val marginPx = if (sidebarVisible) (64 * resources.displayMetrics.density).toInt() else 0
            params.marginStart = marginPx
            contentArea.layoutParams = params
        }

        // ---- \u0627\u0644\u0633\u0627\u0639\u0629 \u0627\u0644\u0639\u0644\u0648\u064A\u0629: \u062A\u062A\u062D\u062F\u062B \u0643\u0644 30 \u062B\u0627\u0646\u064A\u0629 \u0628\u062F\u0644 \u0645\u0627 \u062A\u0628\u0642\u0649 \u0648\u0627\u0642\u0641\u0629 \u0639\u0644\u0649 00:00 ----
        val timeFormat = java.text.SimpleDateFormat("HH:mm", Locale.getDefault())
        clockRunnable = object : Runnable {
            override fun run() {
                topClock.text = timeFormat.format(java.util.Date())
                clockHandler.postDelayed(this, 30_000L)
            }
        }
        clockHandler.post(clockRunnable)

        // ---- \u0627\u0644\u0633\u0627\u0639\u0629 \u0627\u0644\u0639\u0644\u0648\u064A\u0629: \u062A\u062A\u062D\u062F\u062B \u0643\u0644 30 \u062B\u0627\u0646\u064A\u0629 \u0628\u062F\u0644 \u0645\u0627 \u062A\u0628\u0642\u0649 \u0648\u0627\u0642\u0641\u0629 \u0639\u0644\u0649 00:00 ----
        val timeFormat = java.text.SimpleDateFormat("HH:mm", Locale.getDefault())
        clockRunnable = object : Runnable {
            override fun run() {
                topClock.text = timeFormat.format(java.util.Date())
                clockHandler.postDelayed(this, 30_000L)
            }
        }
        clockHandler.post(clockRunnable)

        // ---- SYS panel: \u0623\u0632\u0631\u0627\u0631 \u062D\u0642\u064A\u0642\u064A\u0629 ----
        val flashButton = findViewById<TextView>(R.id.sysFlashToggle)
        flashButton.setOnClickListener {
            setFlashlight(!flashOn)
            flashButton.text = if (flashOn) "FLASH ON" else "FLASH"
        }
        findViewById<TextView>(R.id.sysCameraButton).setOnClickListener {
            try {
                startActivity(Intent(MediaStore.ACTION_IMAGE_CAPTURE))
            } catch (e: Exception) {
                log("\u062E\u0637\u0623 \u0641\u062A\u062D \u0627\u0644\u0643\u0627\u0645\u064A\u0631\u0627: ${e.message}")
                respond("\u0645\u0627 \u0644\u0642\u064A\u062A \u0627\u0644\u0643\u0627\u0645\u064A\u0631\u0627")
            }
        }
        findViewById<TextView>(R.id.sysSettingsButton).setOnClickListener {
            openSystemSettings()
        }

        val sysStatus = findViewById<TextView>(R.id.sysStatusText)

        findViewById<TextView>(R.id.sysBluetoothButton).setOnClickListener {
            sysStatus.text = "\u062C\u0627\u0631\u064A \u0627\u0644\u0628\u062D\u062B \u0639\u0646 \u0623\u062C\u0647\u0632\u0629 Bluetooth..."
            scanBluetoothDevicesForPanel(sysStatus)
        }

        findViewById<TextView>(R.id.sysDefenseButton).setOnClickListener {
            toggleDefenseMode(!defenseModeActive)
            sysStatus.text = if (defenseModeActive) "\u0648\u0636\u0639 \u0627\u0644\u062F\u0641\u0627\u0639: \u0645\u0641\u0639\u0651\u0644" else "\u0648\u0636\u0639 \u0627\u0644\u062F\u0641\u0627\u0639: \u0645\u0637\u0641\u0651\u0649"
        }

        findViewById<TextView>(R.id.sysScanButton).setOnClickListener {
            sysStatus.text = "\u062C\u0627\u0631\u064A \u0641\u062D\u0635 \u0627\u0644\u0635\u0644\u0627\u062D\u064A\u0627\u062A..."
            runSecurityScanForPanel(sysStatus)
        }

        findViewById<TextView>(R.id.sysIrButton).setOnClickListener {
            sendIrCommand("power")
            sysStatus.text = if (irRemote.hasIrBlaster()) "\u062A\u0645 \u0625\u0631\u0633\u0627\u0644 \u0625\u0634\u0627\u0631\u0629 IR" else "\u0627\u0644\u062C\u0647\u0627\u0632 \u0645\u0627\u0641\u064A\u0634 \u0645\u0631\u0633\u0644 IR"
        }

        findViewById<TextView>(R.id.sysBackupButton).setOnClickListener {
            sysStatus.text = "\u062C\u0627\u0631\u064A \u0627\u0644\u062D\u0641\u0638 \u0641\u064A \u0627\u0644\u0633\u062D\u0627\u0628\u0629..."
            backupNotesToCloud()
        }

        findViewById<TextView>(R.id.sysRestoreButton).setOnClickListener {
            sysStatus.text = "\u062C\u0627\u0631\u064A \u0627\u0644\u0627\u0633\u062A\u0631\u062C\u0627\u0639 \u0645\u0646 \u0627\u0644\u0633\u062D\u0627\u0628\u0629..."
            restoreNotesFromCloud()
        }

        // ---- MAP panel: \u0645\u0648\u0642\u0639 \u062D\u0642\u064A\u0642\u064A \u0644\u0644\u062C\u0647\u0627\u0632 ----
        findViewById<TextView>(R.id.mapStatus).setOnClickListener {
            requestDeviceLocation()
        }
        // \u0625\u0630\u0627 \u0627\u0644\u0635\u0644\u0627\u062D\u064A\u0629 \u0645\u0645\u0646\u0648\u062D\u0629 \u0645\u0646 \u0642\u0628\u0644\u060C \u0646\u062C\u064A\u0628 \u0627\u0644\u0645\u0648\u0642\u0639 \u0645\u0628\u0627\u0634\u0631\u0629 \u0628\u0627\u0634 \u0627\u0644\u062E\u0631\u064A\u0637\u0629 \u0627\u0644\u0635\u063A\u064A\u0631\u0629 \u062A\u0628\u064A\u0646 \u0645\u0646 \u0623\u0648\u0644 \u0641\u062A\u062D\u0629
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            == PackageManager.PERMISSION_GRANTED
        ) {
            fetchAndShowLocation()
        }

        // ---- LAB panel: \u0641\u062A\u062D DESIGN LAB ----
        findViewById<TextView>(R.id.labOpenButton).setOnClickListener {
            startActivity(Intent(this, DesignLabActivity::class.java))
        }

        // ---- AI panel: \u0637\u0644\u0628 \u0627\u0642\u062A\u0631\u0627\u062D\u0627\u062A \u062D\u0642\u064A\u0642\u064A\u0629 \u0645\u0646 Gemini ----
        findViewById<TextView>(R.id.aiRefreshButton).setOnClickListener {
            fetchAiSuggestions()
        }
    }

    // \u064A\u0641\u062A\u062D \u0645\u0648\u0642\u0639 \u0648\u064A\u0628 \u0641\u064A \u0627\u0644\u0645\u062A\u0635\u0641\u062D \u0627\u0644\u0635\u063A\u064A\u0631 \u0627\u0644\u0645\u062F\u0645\u062C\u060C \u0648\u064A\u062E\u0641\u064A \u0627\u0644\u062E\u0631\u064A\u0637\u0629 \u0645\u0624\u0642\u062A\u0627\u064B
    private fun openMiniBrowser(url: String) {
        val fullUrl = if (url.startsWith("http://") || url.startsWith("https://")) url else "https://$url"
        lastBrowserUrl = fullUrl
        miniMapView.visibility = View.GONE
        miniBrowserView.visibility = View.VISIBLE
        miniBrowserView.evaluateJavascript("loadSite('$fullUrl');", null)
    }

    private fun closeMiniBrowser() {
        miniBrowserView.visibility = View.GONE
        miniMapView.visibility = View.VISIBLE
    }

    private fun toggleMiniBrowserSize() {
        val params = miniBrowserView.layoutParams as ViewGroup.MarginLayoutParams
        val density = resources.displayMetrics.density
        miniBrowserEnlarged = !miniBrowserEnlarged
        params.height = if (miniBrowserEnlarged) (280 * density).toInt() else (105 * density).toInt()
        miniBrowserView.layoutParams = params
    }

    private fun openFullscreenBrowser() {
        val intent = Intent(this, BrowserFullscreenActivity::class.java)
        intent.putExtra("url", lastBrowserUrl)
        startActivity(intent)
    }

    // \u0636\u063A\u0637\u062A\u064A\u0646 \u0639\u0644\u0649 \u0627\u0644\u062E\u0631\u064A\u0637\u0629 \u0627\u0644\u0635\u063A\u064A\u0631\u0629: \u062A\u0643\u0628\u0631/\u062A\u0631\u062C\u0639 \u0644\u062D\u062C\u0645\u0647\u0627 \u0627\u0644\u0623\u0635\u0644\u064A
    private fun toggleMiniMapSize() {
        val params = miniMapView.layoutParams as ViewGroup.MarginLayoutParams
        val density = resources.displayMetrics.density
        miniMapEnlarged = !miniMapEnlarged
        params.height = if (miniMapEnlarged) (260 * density).toInt() else (130 * density).toInt()
        miniMapView.layoutParams = params
    }

    // 3 \u0636\u063A\u0637\u0627\u062A: \u062A\u0641\u062A\u062D \u0627\u0644\u062E\u0631\u064A\u0637\u0629 \u0641\u064A \u0634\u0627\u0634\u0629 \u0643\u0627\u0645\u0644\u0629 \u0645\u0633\u062A\u0642\u0644\u0629
    private fun openFullscreenMap() {
        val intent = Intent(this, MapFullscreenActivity::class.java)
        intent.putExtra("lat", lastKnownLat)
        intent.putExtra("lon", lastKnownLon)
        startActivity(intent)
    }

    private fun refreshBatteryDisplay() {
        val level = getBatteryLevel()
        findViewById<TextView>(R.id.sysBattery).text = "BATTERY: $level%"
    }

    private fun requestDeviceLocation() {
        val statusView = findViewById<TextView>(R.id.mapStatus)
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), REQ_LOCATION
            )
            return
        }
        fetchAndShowLocation()
    }

    private fun fetchAndShowLocation() {
        val statusView = findViewById<TextView>(R.id.mapStatus)
        try {
            val locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
            val providers = locationManager.getProviders(true)
            var bestLocation: Location? = null
            for (provider in providers) {
                val loc = locationManager.getLastKnownLocation(provider) ?: continue
                if (bestLocation == null || loc.accuracy < bestLocation!!.accuracy) {
                    bestLocation = loc
                }
            }
            if (bestLocation != null) {
                showLocationOnUi(bestLocation)
            } else {
                statusView.text = "\u062C\u0627\u0631\u064A \u0627\u0644\u0628\u062D\u062B \u0639\u0646 \u0625\u0634\u0627\u0631\u0629 GPS..."
                requestFreshLocationUpdate(locationManager)
            }
        } catch (e: Exception) {
            log("\u062E\u0637\u0623 \u0627\u0644\u0645\u0648\u0642\u0639: ${e.message}")
            statusView.text = "\u0645\u0627 \u0642\u062F\u0631\u062A \u0646\u062C\u064A\u0628 \u0627\u0644\u0645\u0648\u0642\u0639"
        }
    }

    private fun showLocationOnUi(location: Location) {
        lastKnownLat = location.latitude
        lastKnownLon = location.longitude
        findViewById<TextView>(R.id.mapLat).text = "LAT: ${"%.5f".format(location.latitude)}"
        findViewById<TextView>(R.id.mapLon).text = "LON: ${"%.5f".format(location.longitude)}"
        findViewById<TextView>(R.id.mapStatus).text = "\u062A\u0645 \u062A\u062D\u062F\u064A\u062B \u0627\u0644\u0645\u0648\u0642\u0639"
        if (::miniMapView.isInitialized) {
            miniMapView.evaluateJavascript(
                "setCoords(${location.latitude}, ${location.longitude});", null
            )
        }
    }

    // \u0625\u0630\u0627 \u0645\u0627\u0643\u0627\u0646\u0634 \u0639\u0646\u062F \u0627\u0644\u062C\u0647\u0627\u0632 \u0645\u0648\u0642\u0639 \u0645\u062D\u0641\u0648\u0638 \u0645\u0633\u0628\u0642\u0627\u064B (\u0634\u0627\u0626\u0639 \u0641\u064A \u0627\u0644\u0623\u062C\u0647\u0632\u0629 \u0627\u0644\u062C\u062F\u064A\u062F\u0629)\u060C \u0646\u0637\u0644\u0628 \u0625\u0634\u0627\u0631\u0629 GPS \u062D\u0642\u064A\u0642\u064A\u0629 \u0648\u0627\u062D\u062F\u0629
    private fun requestFreshLocationUpdate(locationManager: LocationManager) {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        val provider = when {
            locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) -> LocationManager.GPS_PROVIDER
            locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER) -> LocationManager.NETWORK_PROVIDER
            else -> null
        }
        if (provider == null) {
            findViewById<TextView>(R.id.mapStatus).text = "\u062E\u062F\u0645\u0629 \u0627\u0644\u0645\u0648\u0642\u0639 \u0645\u0637\u0641\u0623\u0629 \u0641\u064A \u0627\u0644\u062C\u0647\u0627\u0632"
            return
        }

        val listener = object : android.location.LocationListener {
            override fun onLocationChanged(location: Location) {
                showLocationOnUi(location)
                locationManager.removeUpdates(this)
            }
            @Deprecated("Deprecated in Java")
            override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
            override fun onProviderEnabled(provider: String) {}
            override fun onProviderDisabled(provider: String) {
                findViewById<TextView>(R.id.mapStatus).text = "\u062E\u062F\u0645\u0629 \u0627\u0644\u0645\u0648\u0642\u0639 \u0645\u0637\u0641\u0623\u0629"
            }
        }

        try {
            locationManager.requestSingleUpdate(provider, listener, Looper.getMainLooper())
        } catch (e: Exception) {
            log("\u062E\u0637\u0623 \u0637\u0644\u0628 \u062A\u062D\u062F\u064A\u062B \u0627\u0644\u0645\u0648\u0642\u0639: ${e.message}")
            findViewById<TextView>(R.id.mapStatus).text = "\u0645\u0627 \u0642\u062F\u0631\u062A \u0646\u062C\u064A\u0628 \u0627\u0644\u0645\u0648\u0642\u0639"
        }
    }

    private fun fetchAiSuggestions() {
        val suggestionsView = findViewById<TextView>(R.id.aiSuggestionsText)
        val refreshButton = findViewById<TextView>(R.id.aiRefreshButton)
        if (!geminiClient.isConfigured()) {
            suggestionsView.text = "\u0645\u0627\u0641\u064A\u0634 \u0645\u0641\u062A\u0627\u062D Gemini \u0645\u0636\u0628\u0648\u0637"
            return
        }
        refreshButton.text = "...\u062C\u0627\u0631\u064A \u0627\u0644\u062A\u0648\u0644\u064A\u062F"

        val prompt = "Give exactly 3 short, practical productivity or app-usage tips, each one line, no numbering, no markdown."
        geminiClient.generateSimple(
            prompt = prompt,
            onSuccess = { text ->
                runOnUiThread {
                    refreshButton.text = "TAP TO GENERATE"
                    suggestionsView.text = text
                }
            },
            onError = { error ->
                log("\u062E\u0637\u0623 \u062A\u062D\u0644\u064A\u0644 \u0631\u062F AI: $error")
                runOnUiThread {
                    refreshButton.text = "TAP TO GENERATE"
                    suggestionsView.text = "\u0645\u0627 \u0642\u062F\u0631\u062A \u0623\u0648\u0635\u0644 \u0644\u0644\u0646\u062A"
                }
            }
        )
    }

    private fun setupModuleMenu() {
        jarvisDial.setModuleClickListener { module ->
            log("\u0636\u063A\u0637 \u0632\u0631 \u0627\u0644\u0645\u0646\u064A\u0648: $module")
            when (module) {
                "APPS" -> showAppsModule()
                "SYS" -> openSystemSettings()
                "MAP" -> openApp("com.google.android.apps.maps", "\u062E\u0631\u0627\u0626\u0637 \u062C\u0648\u062C\u0644")
                "3D" -> startActivity(Intent(this, DesignLabActivity::class.java))
                "CLK" -> openAlarmsList()
                else -> log("\u0632\u0631 \u0645\u0646\u064A\u0648 \u063A\u064A\u0631 \u0645\u0639\u0631\u0648\u0641: $module")
            }
        }
        jarvisDial.setAppClickListener { appName ->
            val packageName = appNameToPackage[appName]
            if (packageName != null) {
                openApp(packageName, appName)
            } else {
                respond("\u0645\u0627 \u0644\u0642\u064A\u062A $appName")
            }
        }
    }

    private fun showAppsModule() {
        try {
            val pm = packageManager
            val launchables = pm.getInstalledApplications(PackageManager.GET_META_DATA)
                .filter { pm.getLaunchIntentForPackage(it.packageName) != null }
                .sortedBy { pm.getApplicationLabel(it).toString() }
                .take(6)

            appNameToPackage.clear()
            val names = mutableListOf<String>()
            for (info in launchables) {
                val label = pm.getApplicationLabel(info).toString()
                appNameToPackage[label] = info.packageName
                names.add(label)
            }
            jarvisDial.setAppsModule(true, names)
        } catch (e: Exception) {
            log("\u062E\u0637\u0623 \u0642\u0627\u0626\u0645\u0629 \u0627\u0644\u062A\u0637\u0628\u064A\u0642\u0627\u062A: ${e.message}")
            respond("\u0645\u0627 \u0642\u062F\u0631\u062A \u0646\u062C\u064A\u0628 \u0642\u0627\u0626\u0645\u0629 \u0627\u0644\u062A\u0637\u0628\u064A\u0642\u0627\u062A")
        }
    }

    private fun openSystemSettings() {
        try {
            startActivity(Intent(android.provider.Settings.ACTION_SETTINGS))
        } catch (e: Exception) {
            log("\u062E\u0637\u0623 \u0641\u062A\u062D \u0627\u0644\u0625\u0639\u062F\u0627\u062F\u0627\u062A: ${e.message}")
            respond("\u0645\u0627 \u0642\u062F\u0631\u062A \u0623\u0641\u062A\u062D \u0627\u0644\u0625\u0639\u062F\u0627\u062F\u0627\u062A")
        }
    }

    private fun openAlarmsList() {
        try {
            startActivity(Intent(AlarmClock.ACTION_SHOW_ALARMS))
        } catch (e: Exception) {
            log("\u062E\u0637\u0623 \u0641\u062A\u062D \u0627\u0644\u0645\u0646\u0628\u0647\u0627\u062A: ${e.message}")
            respond("\u0645\u0627 \u0642\u062F\u0631\u062A \u0623\u0641\u062A\u062D \u0627\u0644\u0645\u0646\u0628\u0647\u0627\u062A")
        }
    }

    private fun openApp(packageName: String, appName: String) {
        val launchIntent = packageManager.getLaunchIntentForPackage(packageName)
        if (launchIntent != null) {
            startActivity(launchIntent)
            respond("\u062C\u0627\u0631\u064A \u0641\u062A\u062D $appName")
        } else {
            respond("$appName \u0645\u0634 \u0645\u062B\u0628\u062A \u0639\u0644\u0649 \u062C\u0647\u0627\u0632\u0643")
        }
    }

    // ---------------- Call a contact ----------------

    private fun extractNameAfter(cmd: String, marker: String): String {
        val idx = cmd.indexOf(marker)
        if (idx == -1) return ""
        return cmd.substring(idx + marker.length).trim()
    }

    private fun callContact(name: String) {
        if (name.isBlank()) {
            respond("\u0642\u0644\u064A \u0645\u064A\u0646 \u0628\u062F\u0643 \u0623\u062A\u0635\u0644 \u0641\u064A\u0647")
            return
        }
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.READ_CONTACTS), REQ_CONTACTS)
            respond("\u0628\u062F\u064A \u0625\u0630\u0646 \u0642\u0631\u0627\u0621\u0629 \u062C\u0647\u0627\u062A \u0627\u0644\u0627\u062A\u0635\u0627\u0644 \u0623\u0648\u0644\u060C \u062C\u0631\u0628 \u0645\u0631\u0629 \u062A\u0627\u0646\u064A\u0629")
            return
        }
        val cursor = contentResolver.query(
            ContactsContract.Contacts.CONTENT_URI,
            null,
            "${ContactsContract.Contacts.DISPLAY_NAME} LIKE ?",
            arrayOf("%$name%"),
            null
        )
        cursor?.use { c ->
            if (c.moveToFirst()) {
                val contactId = c.getString(c.getColumnIndexOrThrow(ContactsContract.Contacts._ID))
                val phoneCursor = contentResolver.query(
                    ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                    null,
                    "${ContactsContract.CommonDataKinds.Phone.CONTACT_ID} = ?",
                    arrayOf(contactId),
                    null
                )
                phoneCursor?.use { pc ->
                    if (pc.moveToFirst()) {
                        val number = pc.getString(
                            pc.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.NUMBER)
                        )
                        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE)
                            != PackageManager.PERMISSION_GRANTED
                        ) {
                            ActivityCompat.requestPermissions(
                                this, arrayOf(Manifest.permission.CALL_PHONE), REQ_PERMISSIONS
                            )
                            respond("\u062E\u0644\u064A\u0646\u064A \u0646\u0637\u0644\u0628 \u0635\u0644\u0627\u062D\u064A\u0629 \u0627\u0644\u0627\u062A\u0635\u0627\u0644 \u0623\u0648\u0644")
                            return@use
                        }
                        try {
                            startActivity(Intent(Intent.ACTION_CALL, Uri.parse("tel:$number")))
                            respond("\u0646\u062A\u0635\u0644 \u0628\u0640 $name")
                        } catch (e: Exception) {
                            log("\u062E\u0637\u0623 \u0627\u0644\u0627\u062A\u0635\u0627\u0644: ${e.message}")
                            respond("\u0645\u0627 \u0642\u062F\u0631\u062A \u0646\u062A\u0635\u0644")
                        }
                    } else {
                        respond("\u0645\u0627 \u0644\u0642\u064A\u062A \u0631\u0642\u0645 \u0647\u0627\u062A\u0641 \u0644\u0640 $name")
                    }
                }
            } else {
                respond("\u0645\u0627 \u0644\u0642\u064A\u062A \u062C\u0647\u0629 \u0627\u062A\u0635\u0627\u0644 \u0628\u0627\u0633\u0645 $name")
            }
        }
    }

    private fun writeCode(topic: String) {
        if (topic.isBlank()) {
            respond("\u0642\u0644\u064A \u0634\u0648 \u0627\u0644\u0643\u0648\u062F \u064A\u0644\u064A \u0628\u062F\u0643 \u0627\u064A\u0627\u0647")
            return
        }
        if (GEMINI_API_KEY.isBlank()) {
            respond("\u0644\u0627\u0632\u0645 \u062A\u062D\u0637 \u0645\u0641\u062A\u0627\u062D Gemini \u0627\u0644\u0623\u0648\u0644 \u0639\u0634\u0627\u0646 \u0623\u0642\u062F\u0631 \u0623\u0643\u062A\u0628\u0644\u0643 \u0643\u0648\u062F")
            return
        }
        respond("\u062E\u0644\u064A\u0646\u064A \u0623\u0643\u062A\u0628\u0644\u0643 \u0627\u0644\u0643\u0648\u062F...")
        val prompt = "\u0627\u0643\u062A\u0628 \u0643\u0648\u062F \u0628\u0631\u0645\u062C\u064A \u0648\u0627\u0636\u062D \u0648\u0645\u0631\u062A\u0628 \u0644\u0640: $topic. \u0627\u0634\u0631\u062D \u0628\u062C\u0645\u0644\u0629 \u0642\u0635\u064A\u0631\u0629 \u0634\u0648 \u0628\u064A\u0633\u0648\u064A \u0627\u0644\u0643\u0648\u062F."
        askGeminiForCode(prompt)
    }

    private fun askGeminiForCode(prompt: String) {
        val jsonBody = JSONObject().apply {
            put("contents", JSONArray().put(
                JSONObject().apply {
                    put("parts", JSONArray().put(
                        JSONObject().apply { put("text", prompt) }
                    ))
                }
            ))
        }
        val body = jsonBody.toString().toRequestBody("application/json".toMediaTypeOrNull())
        val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.6-flash:generateContent"
        val request = Request.Builder()
            .url(url)
            .addHeader("Content-Type", "application/json")
            .addHeader("x-goog-api-key", GEMINI_API_KEY)
            .post(body)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread { respond("\u0645\u0627 \u0642\u062F\u0631\u062A \u0623\u0648\u0635\u0644 \u0644\u0644\u0646\u062A") }
            }

            override fun onResponse(call: Call, response: Response) {
                try {
                    val responseText = response.body?.string() ?: ""
                    val json = JSONObject(responseText)
                    val code = json.getJSONArray("candidates")
                        .getJSONObject(0)
                        .getJSONObject("content")
                        .getJSONArray("parts")
                        .getJSONObject(0)
                        .getString("text")
                    runOnUiThread {
                        log("\u062C\u0627\u0631\u0641\u0633:\n${code.trim()}")
                        tts.speak("\u0643\u062A\u0628\u062A\u0644\u0643 \u0627\u0644\u0643\u0648\u062F \u0628\u0627\u0644\u0634\u0627\u0634\u0629\u060C \u0634\u0648\u0641\u0647", TextToSpeech.QUEUE_FLUSH, null, null)
                    }
                } catch (e: Exception) {
                    log("\u062E\u0637\u0623 Gemini: ${e.message}")
                    runOnUiThread { respond("\u0645\u0627 \u0642\u062F\u0631\u062A \u0623\u0641\u0647\u0645 \u0631\u062F Gemini") }
                }
            }
        })
    }

    // ---------------- Holographic-style product design ----------------

    private fun designHologram(description: String) {
        if (description.isBlank()) {
            respond("\u0642\u0644\u064A \u0648\u0635\u0641 \u0627\u0644\u0645\u0646\u062A\u062C \u064A\u0644\u064A \u0628\u062F\u0643 \u062A\u0635\u0645\u0645\u0647")
            return
        }
        if (GEMINI_API_KEY.isBlank()) {
            respond("\u0644\u0627\u0632\u0645 \u062A\u062D\u0637 \u0645\u0641\u062A\u0627\u062D Gemini \u0627\u0644\u0623\u0648\u0644 \u0639\u0634\u0627\u0646 \u0623\u0642\u062F\u0631 \u0623\u0635\u0645\u0645\u0644\u0643")
            return
        }
        respond("\u0628\u0635\u0645\u0645\u0644\u0643...")
        val prompt = "\u0628\u0646\u0627\u0621\u064B \u0639\u0644\u0649 \u0647\u0627\u062F \u0627\u0644\u0648\u0635\u0641: \"$description\"\u060C \u0627\u0643\u062A\u0628\u0644\u064A \u0645\u0648\u0627\u0635\u0641\u0627\u062A \u062A\u0635\u0645\u064A\u0645 \u0645\u0646\u0638\u0645\u0629 \u0648\u0642\u0635\u064A\u0631\u0629 " +
                "(\u0627\u0633\u0645 \u0627\u0644\u0645\u0646\u062A\u062C\u060C \u0627\u0644\u0642\u064A\u0627\u0633\u0627\u062A \u0644\u0648 \u0645\u0648\u062C\u0648\u062F\u0629\u060C \u0627\u0644\u0645\u0648\u0627\u062F \u0627\u0644\u0645\u0642\u062A\u0631\u062D\u0629\u060C \u0648\u0635\u0641 \u0645\u062E\u062A\u0635\u0631 \u0628\u062C\u0645\u0644\u062A\u064A\u0646)\u060C " +
                "\u0628\u0634\u0643\u0644 \u0646\u0642\u0627\u0637 \u0642\u0635\u064A\u0631\u0629 \u062A\u0635\u0644\u062D \u062A\u0646\u0639\u0631\u0636 \u0628\u0634\u0627\u0634\u0629 \u0647\u0648\u0644\u0648\u062C\u0631\u0627\u0645\u064A\u0629"
        askGeminiForHologram(prompt)
    }

    private fun askGeminiForHologram(prompt: String) {
        val jsonBody = JSONObject().apply {
            put("contents", JSONArray().put(
                JSONObject().apply {
                    put("parts", JSONArray().put(
                        JSONObject().apply { put("text", prompt) }
                    ))
                }
            ))
        }
        val body = jsonBody.toString().toRequestBody("application/json".toMediaTypeOrNull())
        val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.6-flash:generateContent"
        val request = Request.Builder()
            .url(url)
            .addHeader("Content-Type", "application/json")
            .addHeader("x-goog-api-key", GEMINI_API_KEY)
            .post(body)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread { respond("\u0645\u0627 \u0642\u062F\u0631\u062A \u0623\u0648\u0635\u0644 \u0644\u0644\u0646\u062A") }
            }

            override fun onResponse(call: Call, response: Response) {
                try {
                    val responseText = response.body?.string() ?: ""
                    val json = JSONObject(responseText)
                    val spec = json.getJSONArray("candidates")
                        .getJSONObject(0)
                        .getJSONObject("content")
                        .getJSONArray("parts")
                        .getJSONObject(0)
                        .getString("text")
                    runOnUiThread {
                        showHologramDialog(spec.trim())
                        respond("\u0647\u0627\u0643 \u0627\u0644\u062A\u0635\u0645\u064A\u0645")
                    }
                } catch (e: Exception) {
                    log("\u062E\u0637\u0623 \u0627\u0644\u062A\u0635\u0645\u064A\u0645: ${e.message}")
                    runOnUiThread { respond("\u0645\u0627 \u0642\u062F\u0631\u062A \u0623\u062C\u0647\u0632 \u0627\u0644\u062A\u0635\u0645\u064A\u0645") }
                }
            }
        })
    }

    private fun showHologramDialog(specText: String) {
        val dialog = Dialog(this, android.R.style.Theme_Black_NoTitleBar_Fullscreen)

        val container = FrameLayout(this).apply {
            setBackgroundColor(Color.parseColor("#000000"))
        }

        val textView = TextView(this).apply {
            text = specText
            setTextColor(Color.parseColor("#00F6FF"))
            textSize = 18f
            typeface = Typeface.MONOSPACE
            gravity = Gravity.CENTER
            setPadding(60, 60, 60, 60)
            setShadowLayer(24f, 0f, 0f, Color.parseColor("#00F6FF"))
        }
        val textParams = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.WRAP_CONTENT,
            FrameLayout.LayoutParams.WRAP_CONTENT
        ).apply { gravity = Gravity.CENTER }
        container.addView(textView, textParams)

        val closeButton = Button(this).apply {
            text = "\u2715 \u0625\u063A\u0644\u0627\u0642"
            setTextColor(Color.parseColor("#00F6FF"))
            setBackgroundColor(Color.TRANSPARENT)
            setOnClickListener { dialog.dismiss() }
        }
        val closeParams = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.WRAP_CONTENT,
            FrameLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            gravity = Gravity.TOP or Gravity.END
            topMargin = 60
            rightMargin = 40
        }
        container.addView(closeButton, closeParams)

        dialog.setContentView(container)
        dialog.show()

        ObjectAnimator.ofFloat(textView, "rotationY", 0f, 360f).apply {
            duration = 6000
            repeatCount = ObjectAnimator.INFINITE
            start()
        }
        ObjectAnimator.ofFloat(textView, "alpha", 1f, 0.6f).apply {
            duration = 900
            repeatMode = ObjectAnimator.REVERSE
            repeatCount = ObjectAnimator.INFINITE
            start()
        }
    }

    // ---------------- Explain any topic (geology, etc.) via Gemini ----------------

    private fun extractExplainTopic(cmd: String): String {
        val marker = when {
            cmd.contains("\u0627\u0634\u0631\u062D\u0644\u064A") -> "\u0627\u0634\u0631\u062D\u0644\u064A"
            cmd.contains("\u0627\u0634\u0631\u062D \u0644\u064A") -> "\u0627\u0634\u0631\u062D \u0644\u064A"
            cmd.contains("\u0641\u0647\u0645\u0646\u064A") -> "\u0641\u0647\u0645\u0646\u064A"
            cmd.contains("\u0627\u0641\u062A\u062D \u0645\u0648\u0636\u0648\u0639") -> "\u0645\u0648\u0636\u0648\u0639"
            cmd.contains("\u0634\u0648 \u0647\u0648") -> "\u0634\u0648 \u0647\u0648"
            else -> "\u0634\u0648 \u0647\u064A"
        }
        return extractNameAfter(cmd, marker)
    }

    private val offlineKnowledge = mapOf(
        "\u0627\u0644\u0627\u0633\u0644\u0627\u0645" to "\u0627\u0644\u0625\u0633\u0644\u0627\u0645 \u062F\u064A\u0646 \u062A\u0648\u062D\u064A\u062F\u064A\u060C \u0646\u0632\u0644 \u0639\u0644\u0649 \u0627\u0644\u0646\u0628\u064A \u0645\u062D\u0645\u062F \u0635\u0644\u0649 \u0627\u0644\u0644\u0647 \u0639\u0644\u064A\u0647 \u0648\u0633\u0644\u0645 \u0628\u0627\u0644\u0642\u0631\u0622\u0646 \u0627\u0644\u0643\u0631\u064A\u0645. \u0645\u0646 \u0623\u0631\u0643\u0627\u0646\u0647 \u0627\u0644\u062E\u0645\u0633\u0629: \u0627\u0644\u0634\u0647\u0627\u062F\u062A\u064A\u0646\u060C \u0627\u0644\u0635\u0644\u0627\u0629\u060C \u0627\u0644\u0632\u0643\u0627\u0629\u060C \u0627\u0644\u0635\u064A\u0627\u0645 \u0628\u0631\u0645\u0636\u0627\u0646\u060C \u0648\u0627\u0644\u062D\u062C \u0644\u0645\u0646 \u0627\u0633\u062A\u0637\u0627\u0639. \u064A\u0624\u0645\u0646 \u0627\u0644\u0645\u0633\u0644\u0645\u0648\u0646 \u0628\u0627\u0644\u0644\u0647 \u0627\u0644\u0648\u0627\u062D\u062F\u060C \u0648\u0628\u0627\u0644\u0623\u0646\u0628\u064A\u0627\u0621 \u0648\u0627\u0644\u0631\u0633\u0644 \u0645\u0646 \u0642\u0628\u0644 \u0645\u062D\u0645\u062F \u0645\u062A\u0644 \u0645\u0648\u0633\u0649 \u0648\u0639\u064A\u0633\u0649 \u0639\u0644\u064A\u0647\u0645 \u0627\u0644\u0633\u0644\u0627\u0645.",
        "\u0627\u0644\u0645\u0633\u064A\u062D\u064A\u0629" to "\u0627\u0644\u0645\u0633\u064A\u062D\u064A\u0629 \u062F\u064A\u0646 \u062A\u0648\u062D\u064A\u062F\u064A \u064A\u0642\u0648\u0645 \u0639\u0644\u0649 \u062A\u0639\u0627\u0644\u064A\u0645 \u0627\u0644\u0633\u064A\u062F \u0627\u0644\u0645\u0633\u064A\u062D \u0639\u064A\u0633\u0649 \u0628\u0646 \u0645\u0631\u064A\u0645 \u0643\u0645\u0627 \u0648\u0631\u062F\u062A \u0628\u0627\u0644\u0625\u0646\u062C\u064A\u0644. \u0645\u0646 \u0623\u0647\u0645 \u0645\u0639\u062A\u0642\u062F\u0627\u062A\u0647\u0627 \u0641\u0643\u0631\u0629 \u0627\u0644\u062B\u0627\u0644\u0648\u062B \u0627\u0644\u0623\u0642\u062F\u0633 (\u0627\u0644\u0622\u0628 \u0648\u0627\u0644\u0627\u0628\u0646 \u0648\u0627\u0644\u0631\u0648\u062D \u0627\u0644\u0642\u062F\u0633)\u060C \u0648\u0637\u0642\u0648\u0633\u0647\u0627 \u0627\u0644\u0623\u0633\u0627\u0633\u064A\u0629 \u062A\u0634\u0645\u0644 \u0627\u0644\u0645\u0639\u0645\u0648\u062F\u064A\u0629 \u0648\u0627\u0644\u0642\u0631\u0628\u0627\u0646 \u0627\u0644\u0645\u0642\u062F\u0633\u060C \u0648\u0641\u064A\u0647\u0627 \u0637\u0648\u0627\u0626\u0641 \u0643\u0628\u0631\u0649 \u0645\u062A\u0644 \u0627\u0644\u0643\u0627\u062B\u0648\u0644\u064A\u0643 \u0648\u0627\u0644\u0623\u0631\u062B\u0648\u0630\u0643\u0633 \u0648\u0627\u0644\u0628\u0631\u0648\u062A\u0633\u062A\u0627\u0646\u062A.",
        "\u0627\u0644\u064A\u0647\u0648\u062F\u064A\u0629" to "\u0627\u0644\u064A\u0647\u0648\u062F\u064A\u0629 \u0645\u0646 \u0623\u0642\u062F\u0645 \u0627\u0644\u062F\u064A\u0627\u0646\u0627\u062A \u0627\u0644\u062A\u0648\u062D\u064A\u062F\u064A\u0629\u060C \u0643\u062A\u0627\u0628\u0647\u0627 \u0627\u0644\u0645\u0642\u062F\u0633 \u0627\u0644\u062A\u0648\u0631\u0627\u0629 (\u0627\u0644\u0639\u0647\u062F \u0627\u0644\u0642\u062F\u064A\u0645). \u062A\u0624\u0645\u0646 \u0628\u0646\u0628\u0648\u0629 \u0645\u0648\u0633\u0649 \u0639\u0644\u064A\u0647 \u0627\u0644\u0633\u0644\u0627\u0645 \u0648\u0627\u0633\u062A\u0644\u0627\u0645\u0647 \u0627\u0644\u0648\u0635\u0627\u064A\u0627 \u0627\u0644\u0639\u0634\u0631\u060C \u0648\u0645\u0646 \u0634\u0639\u0627\u0626\u0631\u0647\u0627 \u0627\u0644\u0623\u0633\u0627\u0633\u064A\u0629 \u0627\u0644\u0633\u0628\u062A (\u064A\u0648\u0645 \u0627\u0644\u0631\u0627\u062D\u0629) \u0648\u0642\u0648\u0627\u0639\u062F \u0627\u0644\u0637\u0639\u0627\u0645 \u0627\u0644\u062D\u0644\u0627\u0644 \u062D\u0633\u0628 \u0627\u0644\u0634\u0631\u064A\u0639\u0629 \u0627\u0644\u064A\u0647\u0648\u062F\u064A\u0629 (\u0643\u0648\u0634\u064A\u0631).",
        "\u0627\u0644\u0628\u0648\u0630\u064A\u0629" to "\u0627\u0644\u0628\u0648\u0630\u064A\u0629 \u062F\u064A\u0627\u0646\u0629 \u0648\u0641\u0644\u0633\u0641\u0629 \u0631\u0648\u062D\u064A\u0629 \u0623\u0633\u0633\u0647\u0627 \u0633\u064A\u062F\u0647\u0627\u0631\u062A\u0627 \u063A\u0648\u062A\u0627\u0645\u0627 (\u0628\u0648\u0630\u0627) \u0628\u0627\u0644\u0647\u0646\u062F. \u062A\u0631\u0643\u0632 \u0639\u0644\u0649 \u062A\u062D\u0642\u064A\u0642 \u0627\u0644\u062A\u0646\u0648\u064A\u0631 \u0648\u0627\u0644\u062A\u062D\u0631\u0631 \u0645\u0646 \u0627\u0644\u0645\u0639\u0627\u0646\u0627\u0629 \u0639\u0646 \u0637\u0631\u064A\u0642 \u0627\u062A\u0628\u0627\u0639 \u0627\u0644\u0637\u0631\u064A\u0642 \u0627\u0644\u062B\u0645\u0627\u0646\u064A \u0627\u0644\u0646\u0628\u064A\u0644\u060C \u0648\u062A\u0624\u0645\u0646 \u0628\u0645\u0628\u062F\u0623 \u0625\u0639\u0627\u062F\u0629 \u0627\u0644\u062A\u062C\u0633\u062F (\u0627\u0644\u0643\u0627\u0631\u0645\u0627).",
        "\u0627\u0644\u0647\u0646\u062F\u0648\u0633\u064A\u0629" to "\u0627\u0644\u0647\u0646\u062F\u0648\u0633\u064A\u0629 \u0645\u0646 \u0623\u0642\u062F\u0645 \u0627\u0644\u062F\u064A\u0627\u0646\u0627\u062A \u0628\u0627\u0644\u0639\u0627\u0644\u0645\u060C \u0645\u062A\u0639\u062F\u062F\u0629 \u0627\u0644\u0622\u0644\u0647\u0629 \u0648\u0641\u064A\u0647\u0627 \u0641\u0644\u0633\u0641\u0627\u062A \u0645\u062A\u0646\u0648\u0639\u0629. \u062A\u0624\u0645\u0646 \u0628\u0645\u0628\u062F\u0623 \u0627\u0644\u0643\u0627\u0631\u0645\u0627 \u0648\u0625\u0639\u0627\u062F\u0629 \u0627\u0644\u062A\u062C\u0633\u062F (\u0627\u0644\u062A\u0646\u0627\u0633\u062E)\u060C \u0648\u0643\u062A\u0628\u0647\u0627 \u0627\u0644\u0645\u0642\u062F\u0633\u0629 \u062A\u0634\u0645\u0644 \u0627\u0644\u0641\u064A\u062F\u0627 \u0648\u0627\u0644\u0628\u0647\u0627\u063A\u0627\u0641\u0627\u062F\u063A\u064A\u062A\u0627\u060C \u0648\u0623\u0647\u0645 \u0622\u0644\u0647\u062A\u0647\u0627 \u0628\u0631\u0627\u0647\u0645\u0627 \u0648\u0641\u064A\u0634\u0646\u0648 \u0648\u0634\u064A\u0641\u0627."
    )

    private fun explainTopic(topic: String) {
        if (topic.isBlank()) {
            respond("\u0642\u0644\u064A \u0634\u0648 \u0627\u0644\u0645\u0648\u0636\u0648\u0639 \u064A\u0644\u064A \u0628\u062F\u0643 \u0623\u0634\u0631\u062D\u0644\u0643 \u064A\u0627\u0647")
            return
        }

        val offlineMatch = offlineKnowledge.entries.firstOrNull { topic.contains(it.key) }
        if (offlineMatch != null) {
            respond(offlineMatch.value)
            return
        }

        if (GEMINI_API_KEY.isBlank()) {
            respond("\u0644\u0627\u0632\u0645 \u062A\u062D\u0637 \u0645\u0641\u062A\u0627\u062D Gemini \u0627\u0644\u0623\u0648\u0644 \u0639\u0634\u0627\u0646 \u0623\u0642\u062F\u0631 \u0623\u0634\u0631\u062D\u0644\u0643 \u0645\u0648\u0627\u0636\u064A\u0639 \u0632\u064A\u0627\u062F\u0629")
            return
        }
        respond("\u062E\u0644\u064A\u0646\u064A \u0623\u0634\u0631\u062D\u0644\u0643...")
        val prompt = "\u0627\u0634\u0631\u062D\u0644\u064A \u0645\u0648\u0636\u0648\u0639 \"$topic\" \u0628\u0637\u0631\u064A\u0642\u0629 \u0633\u0647\u0644\u0629 \u0648\u0645\u0628\u0633\u0637\u0629 \u0645\u0639 \u0645\u062B\u0627\u0644 \u0625\u0630\u0627 \u0623\u0645\u0643\u0646\u060C \u0628\u0623\u0633\u0644\u0648\u0628 \u0642\u0631\u064A\u0628 \u0648\u0645\u0641\u0647\u0648\u0645"
        askGemini(prompt)
    }

    // ---------------- Unit converter ----------------

    private fun convertUnits(cmd: String): String {
        val numberRegex = Regex("""(\d+(?:\.\d+)?)""")
        val match = numberRegex.find(cmd) ?: return "\u0642\u0644\u064A \u0627\u0644\u0631\u0642\u0645 \u064A\u0644\u064A \u0628\u062F\u0643 \u062A\u062D\u0648\u0644\u0647"
        val value = match.groupValues[1].toDouble()

        return when {
            cmd.contains("\u0643\u064A\u0644\u0648\u0645\u062A\u0631") && cmd.contains("\u0645\u064A\u0644") -> {
                val miles = value * 0.621371
                "${value} \u0643\u0645 \u064A\u0633\u0627\u0648\u064A \u062A\u0642\u0631\u064A\u0628\u064B\u0627 ${"%.2f".format(miles)} \u0645\u064A\u0644"
            }
            cmd.contains("\u0643\u064A\u0644\u0648") && cmd.contains("\u0628\u0627\u0648\u0646\u062F") -> {
                val pounds = value * 2.20462
                "${value} \u0643\u064A\u0644\u0648 \u064A\u0633\u0627\u0648\u064A \u062A\u0642\u0631\u064A\u0628\u064B\u0627 ${"%.2f".format(pounds)} \u0628\u0627\u0648\u0646\u062F"
            }
            cmd.contains("\u0645\u0626\u0648\u064A\u0629") && cmd.contains("\u0641\u0647\u0631\u0646\u0647\u0627\u064A\u062A") -> {
                val fahrenheit = (value * 9 / 5) + 32
                "${value} \u062F\u0631\u062C\u0629 \u0645\u0626\u0648\u064A\u0629 \u064A\u0633\u0627\u0648\u064A ${"%.1f".format(fahrenheit)} \u0641\u0647\u0631\u0646\u0647\u0627\u064A\u062A"
            }
            else -> "\u0642\u0644\u064A \u0627\u0644\u062A\u062D\u0648\u064A\u0644 \u0628\u0647\u0627\u0644\u0635\u064A\u063A\u0629: \u062D\u0648\u0644 10 \u0643\u064A\u0644\u0648\u0645\u062A\u0631 \u0627\u0644\u0649 \u0645\u064A\u0644"
        }
    }

    // ---------------- Fun facts ----------------

    private val funFacts = listOf(
        "\u0647\u0644 \u062A\u0639\u0631\u0641\u061F \u0635\u062D\u0631\u0627\u0621 \u0627\u0644\u062C\u0632\u0627\u0626\u0631 (\u0627\u0644\u0635\u062D\u0631\u0627\u0621 \u0627\u0644\u0643\u0628\u0631\u0649) \u062A\u063A\u0637\u064A \u0623\u0643\u062A\u0631 \u0645\u0646 80% \u0645\u0646 \u0645\u0633\u0627\u062D\u0629 \u0627\u0644\u0628\u0644\u0627\u062F.",
        "\u0647\u0644 \u062A\u0639\u0631\u0641\u061F \u0627\u0644\u0639\u0633\u0644 \u0645\u0627 \u064A\u0641\u0633\u062F\u0634 \u0623\u0628\u062F\u064B\u0627\u060C \u062D\u062A\u0649 \u0628\u0639\u062F \u0622\u0644\u0627\u0641 \u0627\u0644\u0633\u0646\u064A\u0646.",
        "\u0647\u0644 \u062A\u0639\u0631\u0641\u061F \u0627\u0644\u0642\u0644\u0628 \u0627\u0644\u0628\u0634\u0631\u064A \u064A\u062F\u0642 \u062D\u0648\u0627\u0644\u064A 100 \u0623\u0644\u0641 \u0645\u0631\u0629 \u0628\u0627\u0644\u064A\u0648\u0645 \u0627\u0644\u0648\u0627\u062D\u062F.",
        "\u0647\u0644 \u062A\u0639\u0631\u0641\u061F \u0627\u0644\u0623\u062E\u0637\u0628\u0648\u0637 \u0639\u0646\u062F\u0647 \u062B\u0644\u0627\u062B\u0629 \u0642\u0644\u0648\u0628 \u0648\u062F\u0645\u0647 \u0644\u0648\u0646\u0647 \u0623\u0632\u0631\u0642.",
        "\u0647\u0644 \u062A\u0639\u0631\u0641\u061F \u0627\u0644\u0636\u0648\u0621 \u0645\u0646 \u0627\u0644\u0634\u0645\u0633 \u064A\u0648\u0635\u0644 \u0644\u0644\u0623\u0631\u0636 \u0628\u062D\u0648\u0627\u0644\u064A 8 \u062F\u0642\u0627\u064A\u0642 \u0628\u0633.",
        "\u0647\u0644 \u062A\u0639\u0631\u0641\u061F \u062C\u0628\u0644 \u0637\u0648\u0628\u0642\u0627\u0644 \u0628\u0627\u0644\u0645\u063A\u0631\u0628 \u0647\u0648 \u0623\u0639\u0644\u0649 \u0642\u0645\u0629 \u0628\u0634\u0645\u0627\u0644 \u0623\u0641\u0631\u064A\u0642\u064A\u0627."
    )

    // ---------------- Suggestions ----------------

    private fun suggestDrawing(): String {
        val ideas = listOf(
            "\u0627\u0631\u0633\u0645 \u0645\u0646\u0638\u0631 \u0637\u0628\u064A\u0639\u064A \u0641\u064A\u0647 \u062C\u0628\u0627\u0644 \u0648\u0628\u062D\u0631",
            "\u062C\u0631\u0628 \u062A\u0631\u0633\u0645 \u0628\u0648\u0631\u062A\u0631\u064A\u0647 \u0644\u0634\u062E\u0635 \u0642\u0631\u064A\u0628 \u0645\u0646\u0643",
            "\u0627\u0631\u0633\u0645 \u062D\u064A\u0648\u0627\u0646 \u0623\u0644\u064A\u0641 \u0628\u0623\u0633\u0644\u0648\u0628 \u0643\u0631\u062A\u0648\u0646\u064A",
            "\u062C\u0631\u0628 \u0631\u0633\u0645 \u0645\u062F\u064A\u0646\u0629 \u062E\u064A\u0627\u0644\u064A\u0629 \u0645\u0646 \u062E\u064A\u0627\u0644\u0643",
            "\u0627\u0631\u0633\u0645 \u0644\u0648\u062D\u0629 \u062A\u062C\u0631\u064A\u062F\u064A\u0629 \u0628\u0627\u0644\u0623\u0644\u0648\u0627\u0646 \u064A\u0644\u064A \u0628\u062A\u062D\u0628\u0647\u0627"
        )
        return "\u0641\u0643\u0631\u0629 \u0631\u0633\u0645\u0629 \u0627\u0644\u064A\u0648\u0645: ${ideas.random()}"
    }

    private fun suggestBreakfast(): String {
        val ideas = listOf(
            "\u0628\u064A\u0636 \u0645\u0639 \u0632\u0639\u062A\u0631 \u0648\u0632\u064A\u062A \u0632\u064A\u062A\u0648\u0646 \u0648\u062E\u0628\u0632 \u0637\u0627\u0632\u0629",
            "\u0641\u0648\u0644 \u0645\u062F\u0645\u0633 \u0645\u0639 \u062E\u0636\u0631\u0629 \u0648\u0644\u064A\u0645\u0648\u0646",
            "\u0644\u0628\u0646\u0629 \u0645\u0639 \u062E\u064A\u0627\u0631 \u0648\u0637\u0645\u0627\u0637\u0645",
            "\u0645\u0646\u0627\u0642\u064A\u0634 \u0632\u0639\u062A\u0631 \u0623\u0648 \u062C\u0628\u0646\u0629",
            "\u0634\u0643\u0634\u0648\u0643\u0629 \u0628\u0627\u0644\u0628\u064A\u0636 \u0648\u0627\u0644\u0628\u0646\u062F\u0648\u0631\u0629"
        )
        return "\u0627\u0642\u062A\u0631\u0627\u062D \u0641\u0637\u0648\u0631 \u0627\u0644\u064A\u0648\u0645: ${ideas.random()}"
    }

    // ---------------- Distance between cities ----------------
    // \u0628\u064A\u0627\u0646\u0627\u062A \u0627\u0644\u0645\u062F\u0646 \u0627\u0646\u062A\u0642\u0644\u062A \u0644\u0645\u0644\u0641 CityCoordinates.kt \u0645\u0646\u0641\u0635\u0644 (\u0644\u062A\u0646\u0638\u064A\u0645 \u0627\u0644\u0643\u0648\u062F)

    private fun handleDistanceQuery(cmd: String) {
        val regex = Regex("""\u0645\u0646\s+(\S+)\s+(?:\u0627\u0644\u0649|\u0625\u0644\u0649)\s+(\S+)""")
        val match = regex.find(cmd)
        if (match == null) {
            respond("\u0642\u0644\u064A \u0627\u0644\u0645\u0633\u0627\u0641\u0629 \u0628\u0647\u0627\u0644\u0635\u064A\u063A\u0629: \u0643\u0645 \u0627\u0644\u0645\u0633\u0627\u0641\u0629 \u0645\u0646 \u062F\u0645\u0634\u0642 \u0627\u0644\u0649 \u062D\u0644\u0628")
            return
        }
        val cityA = match.groupValues[1]
        val cityB = match.groupValues[2]

        if (GOOGLE_MAPS_API_KEY.isNotBlank()) {
            respond("\u0628\u062D\u0633\u0628...")
            askGoogleDistance(cityA, cityB)
        } else {
            respond(calculateDistanceOffline(cityA, cityB))
        }
    }

    private fun askGoogleDistance(cityA: String, cityB: String) {
        val originEnc = java.net.URLEncoder.encode(cityA, "UTF-8")
        val destEnc = java.net.URLEncoder.encode(cityB, "UTF-8")
        val url = "https://maps.googleapis.com/maps/api/distancematrix/json" +
                "?origins=$originEnc&destinations=$destEnc&key=$GOOGLE_MAPS_API_KEY"
        val request = Request.Builder().url(url).get().build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread { respond(calculateDistanceOffline(cityA, cityB)) }
            }

            override fun onResponse(call: Call, response: Response) {
                try {
                    val responseText = response.body?.string() ?: ""
                    val json = JSONObject(responseText)
                    val status = json.optString("status")
                    if (status != "OK") {
                        runOnUiThread { respond(calculateDistanceOffline(cityA, cityB)) }
                        return
                    }
                    val element = json.getJSONArray("rows")
                        .getJSONObject(0)
                        .getJSONArray("elements")
                        .getJSONObject(0)
                    val elementStatus = element.optString("status")
                    if (elementStatus != "OK") {
                        runOnUiThread { respond(calculateDistanceOffline(cityA, cityB)) }
                        return
                    }
                    val distanceText = element.getJSONObject("distance").getString("text")
                    val durationText = element.getJSONObject("duration").getString("text")
                    runOnUiThread {
                        respond("\u0627\u0644\u0645\u0633\u0627\u0641\u0629 \u0645\u0646 $cityA \u0627\u0644\u0649 $cityB \u062D\u0648\u0627\u0644\u064A $distanceText \u0628\u0627\u0644\u0633\u064A\u0627\u0631\u0629\u060C \u0648\u0648\u0642\u062A \u0627\u0644\u0631\u062D\u0644\u0629 \u062A\u0642\u0631\u064A\u0628\u064B\u0627 $durationText")
                    }
                } catch (e: Exception) {
                    runOnUiThread { respond(calculateDistanceOffline(cityA, cityB)) }
                }
            }
        })
    }

    private fun calculateDistanceOffline(cityA: String, cityB: String): String {
        val coordA = CityCoordinates.coordinates[cityA]
        val coordB = CityCoordinates.coordinates[cityB]
        if (coordA == null || coordB == null) {
            return "\u0644\u0644\u0623\u0633\u0641 \u0645\u0627 \u0639\u0646\u062F\u064A \u0625\u062D\u062F\u0627\u062B\u064A\u0627\u062A \u0644\u0647\u0627\u064A \u0627\u0644\u0645\u062F\u064A\u0646\u0629 \u062D\u0627\u0644\u064A\u064B\u0627"
        }
        val distanceKm = haversine(coordA.first, coordA.second, coordB.first, coordB.second)
        return "\u0627\u0644\u0645\u0633\u0627\u0641\u0629 \u0645\u0646 $cityA \u0627\u0644\u0649 $cityB \u062D\u0648\u0627\u0644\u064A ${distanceKm.toInt()} \u0643\u0645 (\u062E\u0637 \u0645\u0633\u062A\u0642\u064A\u0645 \u062A\u0642\u0631\u064A\u0628\u064A)"
    }

    private fun haversine(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val earthRadiusKm = 6371.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2).pow(2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLon / 2).pow(2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return earthRadiusKm * c
    }

    // ---------------- Output helpers ----------------

    private fun respond(text: String) {
        log("\u062C\u0627\u0631\u0641\u0633: $text")
        speechRecognizer?.stopListening()

        // \u0644\u0645\u062D\u0631\u0643\u0627\u062A TTS \u062D\u062F \u0623\u0642\u0635\u0649 \u0644\u0637\u0648\u0644 \u0627\u0644\u0646\u0635 \u0641\u064A \u0627\u0644\u0627\u0633\u062A\u062F\u0639\u0627\u0621 \u0627\u0644\u0648\u0627\u062D\u062F \u2014 \u0627\u0644\u0646\u0635 \u0627\u0644\u0637\u0648\u064A\u0644 (\u0645\u062B\u0644 \u0631\u062F\u0648\u062F Gemini) \u0643\u0627\u0646 \u064A\u062A\u0642\u0637\u0639 \u0628\u0635\u0645\u062A. \u0646\u0642\u0633\u0651\u0645\u0647 \u0644\u062C\u0645\u0644 \u0648\u0646\u0631\u0633\u0644\u0647\u0645 \u0648\u0627\u062D\u062F \u0628\u0648\u0627\u062D\u062F
        val maxLen = try {
            TextToSpeech.getMaxSpeechInputLength().takeIf { it > 0 } ?: 3800
        } catch (e: Exception) {
            3800
        }
        val chunks = splitTextForSpeech(text, maxLen)
        val utteranceIds = chunks.indices.map { "jarvis_${System.currentTimeMillis()}_$it" }
        lastQueuedUtteranceId = utteranceIds.lastOrNull()
        chunks.forEachIndexed { index, chunk ->
            val mode = if (index == 0) TextToSpeech.QUEUE_FLUSH else TextToSpeech.QUEUE_ADD
            tts.speak(chunk, mode, null, utteranceIds[index])
        }
    }

    // \u064A\u0642\u0633\u0651\u0645 \u0627\u0644\u0646\u0635 \u0627\u0644\u0637\u0648\u064A\u0644 \u0639\u0644\u0649 \u062D\u062F\u0648\u062F \u0627\u0644\u062C\u0645\u0644 \u0642\u062F\u0631 \u0627\u0644\u0625\u0645\u0643\u0627\u0646 (\u0645\u0627 \u064A\u0642\u0637\u0639\u0634 \u0641\u064A \u0646\u0635 \u0643\u0644\u0645\u0629) \u0628\u062F\u0648\u0646 \u0645\u0627 \u064A\u062A\u062C\u0627\u0648\u0632 maxLen
    private fun splitTextForSpeech(text: String, maxLen: Int): List<String> {
        if (text.length <= maxLen) return listOf(text)
        val sentences = text.split(Regex("(?<=[.!?\u061F\u060C])\\s+"))
        val chunks = mutableListOf<String>()
        val current = StringBuilder()
        for (sentence in sentences) {
            if (current.length + sentence.length + 1 > maxLen) {
                if (current.isNotEmpty()) {
                    chunks.add(current.toString().trim())
                    current.clear()
                }
                if (sentence.length > maxLen) {
                    // \u062C\u0645\u0644\u0629 \u0648\u0627\u062D\u062F\u0629 \u0623\u0637\u0648\u0644 \u0645\u0646 maxLen \u0646\u0641\u0633\u0647\u0627: \u0646\u0642\u0637\u0639\u0647\u0627 \u0642\u0633\u0631\u064A\u0627\u064B
                    var start = 0
                    while (start < sentence.length) {
                        val end = minOf(start + maxLen, sentence.length)
                        chunks.add(sentence.substring(start, end))
                        start = end
                    }
                } else {
                    current.append(sentence)
                }
            } else {
                current.append(sentence).append(" ")
            }
        }
        if (current.isNotEmpty()) chunks.add(current.toString().trim())
        return chunks.filter { it.isNotBlank() }
    }

    private fun log(text: String) {
        logText.append("\n\n$text")
    }

    // \u064A\u062A\u0645 \u0627\u0633\u062A\u062F\u0639\u0627\u0624\u0647 \u062A\u0644\u0642\u0627\u0626\u064A\u0627\u064B \u0645\u0644\u064A \u064A\u0642\u062A\u0631\u0628 \u0627\u0644\u0645\u0633\u062A\u062E\u062F\u0645 \u0628\u062E\u0627\u0635\u064A\u0629 \u0627\u0644\u0647\u0627\u062A\u0641 \u0645\u0646 \u0628\u0637\u0627\u0642\u0629 NFC (\u0628\u0641\u0636\u0644 enableForegroundDispatch)
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        val tagContent = nfcHelper.readTagFromIntent(intent)
        if (tagContent != null) {
            respond("\u0645\u062D\u062A\u0648\u0649 \u0627\u0644\u0628\u0637\u0627\u0642\u0629: $tagContent")
        }
    }

    // \u0632\u0631 \u0627\u0644\u0623\u0631\u0628\u0648\u062F\u0632/\u0627\u0644\u0633\u0645\u0627\u0639\u0629 \u0627\u0644\u0644\u0627\u0633\u0644\u0643\u064A\u0629: 3 \u0636\u063A\u0637\u0627\u062A \u0633\u0631\u064A\u0639\u0629 \u062E\u0644\u0627\u0644 \u062B\u0627\u0646\u064A\u0629 \u0648\u0627\u062D\u062F\u0629 \u062A\u0628\u062F\u0623 \u0627\u0644\u0627\u0633\u062A\u0645\u0627\u0639
    private var headsetPressCount = 0
    private var lastHeadsetPressTime = 0L

    override fun onKeyDown(keyCode: Int, event: android.view.KeyEvent?): Boolean {
        if (keyCode == android.view.KeyEvent.KEYCODE_HEADSETHOOK ||
            keyCode == android.view.KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE
        ) {
            val now = System.currentTimeMillis()
            if (now - lastHeadsetPressTime > 1200L) {
                headsetPressCount = 0
            }
            headsetPressCount++
            lastHeadsetPressTime = now
            if (headsetPressCount >= 3) {
                headsetPressCount = 0
                if (!continuousMode) {
                    enableContinuousMode()
                } else {
                    startListening()
                }
            }
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onPause() {
        super.onPause()
        // \u0648\u0642\u0641 \u0627\u0644\u0627\u0633\u062A\u0645\u0627\u0639 \u0648\u0627\u0644\u0646\u0637\u0642 \u0648\u062D\u0631\u0643\u0629 HUD \u0645\u0644\u064A \u0627\u0644\u062A\u0637\u0628\u064A\u0642 \u064A\u0631\u0648\u062D \u0644\u0644\u062E\u0644\u0641\u064A\u0629 (\u064A\u0648\u0641\u0631 \u0628\u0637\u0627\u0631\u064A\u0629 \u0648\u064A\u0645\u0646\u0639 \u0627\u0644\u0645\u0627\u064A\u0643 \u064A\u0628\u0642\u0649 \u062E\u0627\u062F\u0645)
        retryHandler.removeCallbacksAndMessages(null)
        speechRecognizer?.stopListening()
        tts.stop()
        if (::miniMapView.isInitialized) {
            miniMapView.onPause()
            miniMapView.pauseTimers()
        }
        if (::miniBrowserView.isInitialized) {
            miniBrowserView.onPause()
            miniBrowserView.pauseTimers()
        }
        stopPerfMonitor()
        nfcHelper.disableForegroundDispatch()
        geoCompass.stop()
        if (::jarvisDial.isInitialized) {
            jarvisDial.pauseAnimation()
        }
    }

    override fun onResume() {
        super.onResume()
        if (::jarvisDial.isInitialized) {
            jarvisDial.resumeAnimation()
        }
        if (::miniMapView.isInitialized) {
            miniMapView.onResume()
            miniMapView.resumeTimers()
        }
        if (::miniBrowserView.isInitialized) {
            miniBrowserView.onResume()
            miniBrowserView.resumeTimers()
        }
        startPerfMonitor()
        nfcHelper.enableForegroundDispatch()
        geoCompass.start()
        // \u0646\u0631\u062C\u0639 \u0644\u0644\u0627\u0633\u062A\u0645\u0627\u0639 \u063A\u064A\u0631 \u0625\u0630\u0627 \u0643\u0627\u0646 \u0627\u0644\u0648\u0636\u0639 \u0627\u0644\u0645\u0633\u062A\u0645\u0631 (Continuous Mode) \u0645\u0641\u0639\u0651\u0644 \u0642\u0628\u0644 \u0645\u0627 \u0627\u0644\u062A\u0637\u0628\u064A\u0642 \u064A\u0631\u0648\u062D \u0644\u0644\u062E\u0644\u0641\u064A\u0629
        if (continuousMode) {
            startListening()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        retryHandler.removeCallbacksAndMessages(null)
        clockHandler.removeCallbacksAndMessages(null)
        if (::miniMapView.isInitialized) {
            miniMapView.destroy()
        }
        if (::miniBrowserView.isInitialized) {
            miniBrowserView.destroy()
        }
        tts.shutdown()
        stopMusic()
        speechRecognizer?.destroy()
    }
}
