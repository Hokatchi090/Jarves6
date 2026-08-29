package com.jarvis.assistant

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.MediaRecorder
import android.os.Build
import android.telephony.SmsManager
import java.io.File

// \u0648\u062D\u062F\u0629 \u0627\u0644\u0633\u0644\u0627\u0645\u0629 \u0627\u0644\u0634\u062E\u0635\u064A\u0629: \u0627\u062A\u0635\u0627\u0644 \u0645\u0632\u064A\u0641\u060C \u0625\u0631\u0633\u0627\u0644 \u0627\u0644\u0645\u0648\u0642\u0639 \u0644\u0631\u0642\u0645 \u0637\u0648\u0627\u0631\u0626\u060C \u062A\u0633\u062C\u064A\u0644 \u0635\u0648\u062A\u064A \u0643\u062F\u0644\u064A\u0644\u060C \u0648\u0637\u0644\u0628 \u0645\u0639\u0644\u0648\u0645\u0627\u062A \u062F\u0641\u0627\u0639 \u0639\u0627\u0645\u0629 \u0645\u0646 Gemini
class JarvisSafetyModule(
    private val activity: Activity,
    private val speak: (String) -> Unit,
    private val askAi: (String) -> Unit,
    private val getCurrentLocation: () -> Pair<Double, Double>
) {

    private var mediaRecorder: MediaRecorder? = null
    private var recordingFilePath: String? = null

    private fun prefs() = activity.getSharedPreferences("jarvis_safety", Context.MODE_PRIVATE)

    fun execute(intent: JarvisIntent): Boolean {
        return when (intent.type) {
            JarvisIntentType.SAFETY_FAKE_CALL -> { triggerFakeCall(); true }
            JarvisIntentType.SAFETY_SEND_LOCATION -> { sendEmergencyLocation(); true }
            JarvisIntentType.SAFETY_RECORD_START -> { startEvidenceRecording(); true }
            JarvisIntentType.SAFETY_RECORD_STOP -> { stopEvidenceRecording(); true }
            JarvisIntentType.SAFETY_DEFENSE_INFO -> { askDefenseInfo(); true }
            JarvisIntentType.SAFETY_SET_CONTACT -> { setEmergencyContact(intent.argument); true }
            else -> false
        }
    }

    // ---------------- \u0631\u0642\u0645 \u0627\u0644\u0637\u0648\u0627\u0631\u0626 \u0627\u0644\u0645\u062D\u0641\u0648\u0638 ----------------

    private fun setEmergencyContact(number: String) {
        if (number.isBlank()) {
            speak("\u0642\u0648\u0644\u064A \u0631\u0642\u0645 \u0627\u0644\u0637\u0648\u0627\u0631\u0626 \u0627\u0644\u0644\u064A \u062A\u062D\u0628 \u062A\u062D\u0641\u0638\u0648")
            return
        }
        prefs().edit().putString("emergency_number", number).apply()
        speak("\u062A\u0645 \u062D\u0641\u0638 \u0631\u0642\u0645 \u0627\u0644\u0637\u0648\u0627\u0631\u0626")
    }

    private fun getEmergencyContact(): String? = prefs().getString("emergency_number", null)

    // ---------------- \u0627\u062A\u0635\u0627\u0644 \u0645\u0632\u064A\u0641 ----------------

    private fun triggerFakeCall() {
        val callerName = prefs().getString("fake_caller_name", "\u0645\u0643\u0627\u0644\u0645\u0629 \u0648\u0627\u0631\u062F\u0629") ?: "\u0645\u0643\u0627\u0644\u0645\u0629 \u0648\u0627\u0631\u062F\u0629"
        val intent = Intent(activity, FakeCallActivity::class.java)
        intent.putExtra("caller_name", callerName)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        activity.startActivity(intent)
    }

    // ---------------- \u0625\u0631\u0633\u0627\u0644 \u0627\u0644\u0645\u0648\u0642\u0639 \u0644\u062C\u0647\u0629 \u0637\u0648\u0627\u0631\u0626 ----------------

    private fun sendEmergencyLocation() {
        val number = getEmergencyContact()
        if (number == null) {
            speak("\u0645\u0627\u0641\u064A\u0634 \u0631\u0642\u0645 \u0637\u0648\u0627\u0631\u0626 \u0645\u062D\u0641\u0648\u0638. \u0642\u0648\u0644\u064A: \u0627\u062D\u0641\u0638 \u0631\u0642\u0645 \u0627\u0644\u0637\u0648\u0627\u0631\u0626 \u062B\u0645 \u0627\u0644\u0631\u0642\u0645")
            return
        }
        val (lat, lon) = getCurrentLocation()
        if (lat == 0.0 && lon == 0.0) {
            speak("\u0645\u0627\u0632\u0627\u0644 \u0645\u0627\u0639\u0646\u062F\u064A \u0645\u0648\u0642\u0639 \u062D\u0642\u064A\u0642\u064A \u0644\u0623\u0628\u0639\u062B\u0647")
            return
        }
        val mapsLink = "https://maps.google.com/?q=$lat,$lon"
        val message = "\u0637\u0648\u0627\u0631\u0626: \u0645\u0648\u0642\u0639\u064A \u0627\u0644\u062D\u0627\u0644\u064A $mapsLink"
        try {
            val smsManager = activity.getSystemService(SmsManager::class.java)
            smsManager.sendTextMessage(number, null, message, null, null)
            speak("\u062A\u0645 \u0625\u0631\u0633\u0627\u0644 \u0645\u0648\u0642\u0639\u0643 \u0644\u062C\u0647\u0629 \u0627\u0644\u0637\u0648\u0627\u0631\u0626")
        } catch (e: Exception) {
            speak("\u0645\u0627 \u0642\u062F\u0631\u062A \u0623\u0628\u0639\u062B \u0631\u0633\u0627\u0644\u0629 \u0627\u0644\u0637\u0648\u0627\u0631\u0626")
        }
    }

    // ---------------- \u062A\u0633\u062C\u064A\u0644 \u0635\u0648\u062A\u064A \u0643\u062F\u0644\u064A\u0644 (\u0645\u062D\u0644\u064A\u0627\u064B \u0639\u0644\u0649 \u0627\u0644\u062C\u0647\u0627\u0632\u060C \u0628\u0644\u0627 \u0625\u0646\u062A\u0631\u0646\u062A) ----------------

    fun startEvidenceRecording() {
        if (mediaRecorder != null) {
            speak("\u0627\u0644\u062A\u0633\u062C\u064A\u0644 \u0634\u063A\u0627\u0644 \u0623\u0635\u0644\u0627\u064B")
            return
        }
        try {
            val dir = activity.getExternalFilesDir(android.os.Environment.DIRECTORY_MUSIC) ?: activity.filesDir
            if (!dir.exists()) dir.mkdirs()
            val file = File(dir, "evidence_${System.currentTimeMillis()}.m4a")
            recordingFilePath = file.absolutePath

            val recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(activity)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }
            recorder.setAudioSource(MediaRecorder.AudioSource.MIC)
            recorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            recorder.setOutputFile(file.absolutePath)
            recorder.prepare()
            recorder.start()
            mediaRecorder = recorder
            speak("\u0628\u062F\u0627 \u0627\u0644\u062A\u0633\u062C\u064A\u0644")
        } catch (e: Exception) {
            speak("\u0645\u0627 \u0642\u062F\u0631\u062A \u0623\u0628\u062F\u0627 \u0627\u0644\u062A\u0633\u062C\u064A\u0644")
            mediaRecorder = null
        }
    }

    fun stopEvidenceRecording() {
        val recorder = mediaRecorder ?: run {
            speak("\u0645\u0627\u0643\u0627\u0634 \u062A\u0633\u062C\u064A\u0644 \u062C\u0627\u0631\u064A")
            return
        }
        try {
            recorder.stop()
            recorder.release()
            speak("\u0627\u0646\u062D\u0641\u0638 \u0627\u0644\u062A\u0633\u062C\u064A\u0644 \u0641\u064A: $recordingFilePath")
        } catch (e: Exception) {
            speak("\u0635\u0627\u0631 \u062E\u0637\u0623 \u0648\u0642\u062A \u0625\u0646\u0647\u0627\u0621 \u0627\u0644\u062A\u0633\u062C\u064A\u0644")
        } finally {
            mediaRecorder = null
        }
    }

    // ---------------- \u0645\u0639\u0644\u0648\u0645\u0627\u062A \u062F\u0641\u0627\u0639 \u0639\u0627\u0645\u0629 (\u0639\u0628\u0631 Gemini) ----------------

    private fun askDefenseInfo() {
        askAi(
            "Give general, widely-taught personal safety and self-defense advice for someone feeling unsafe or " +
                "threatened: de-escalation, situational awareness, how to attract attention and get to safety, and " +
                "basic escape techniques taught in beginner self-defense classes. Keep it practical and brief."
        )
    }
}
