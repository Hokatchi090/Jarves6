package com.jarvis.assistant

import android.app.Activity
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.WindowManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView

// \u0634\u0627\u0634\u0629 \u0645\u0643\u0627\u0644\u0645\u0629 \u0648\u0627\u0631\u062F\u0629 \u0645\u0632\u064A\u0641\u0629\u060C \u0645\u0641\u064A\u062F\u0629 \u0644\u0644\u0647\u0631\u0648\u0628 \u0645\u0646 \u0648\u0636\u0639 \u0645\u0632\u0639\u062C \u0623\u0648 \u063A\u064A\u0631 \u0645\u0631\u064A\u062D
class FakeCallActivity : Activity() {

    private var ringtonePlayer: MediaPlayer? = null
    private val handler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.addFlags(
            WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
        )

        val callerName = intent.getStringExtra("caller_name") ?: "\u0645\u0643\u0627\u0644\u0645\u0629 \u0648\u0627\u0631\u062F\u0629"

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setBackgroundColor(android.graphics.Color.parseColor("#0B0F14"))
            setPadding(40, 40, 40, 40)
        }

        val nameView = TextView(this).apply {
            text = callerName
            textSize = 26f
            setTextColor(android.graphics.Color.WHITE)
            gravity = Gravity.CENTER
        }
        val statusView = TextView(this).apply {
            text = "\u0645\u0643\u0627\u0644\u0645\u0629 \u0648\u0627\u0631\u062F\u0629..."
            textSize = 15f
            setTextColor(android.graphics.Color.parseColor("#8DEFFF"))
            gravity = Gravity.CENTER
            setPadding(0, 12, 0, 60)
        }

        val buttonsRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
        }

        val declineButton = Button(this).apply {
            text = "\u0631\u0641\u0636"
            setBackgroundColor(android.graphics.Color.parseColor("#C0392B"))
            setTextColor(android.graphics.Color.WHITE)
            setOnClickListener { finish() }
        }
        val answerButton = Button(this).apply {
            text = "\u0631\u062F"
            setBackgroundColor(android.graphics.Color.parseColor("#27AE60"))
            setTextColor(android.graphics.Color.WHITE)
            setOnClickListener {
                statusView.text = "00:01"
                stopRingtone()
                startCallTimer(statusView)
            }
        }

        val declineParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        declineParams.setMargins(20, 0, 20, 0)
        val answerParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        answerParams.setMargins(20, 0, 20, 0)

        buttonsRow.addView(declineButton, declineParams)
        buttonsRow.addView(answerButton, answerParams)

        root.addView(nameView)
        root.addView(statusView)
        root.addView(buttonsRow)

        setContentView(root)
        playRingtone()
    }

    private fun playRingtone() {
        try {
            val ringtoneUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
            ringtonePlayer = MediaPlayer().apply {
                setDataSource(this@FakeCallActivity, ringtoneUri)
                isLooping = true
                prepare()
                start()
            }
        } catch (e: Exception) {
            // \u0644\u0627 \u062D\u0627\u062C\u0629 \u0644\u0625\u064A\u0642\u0627\u0641 \u0627\u0644\u0634\u0627\u0634\u0629 \u0625\u0630\u0627 \u0641\u0634\u0644\u062A \u0646\u063A\u0645\u0629 \u0627\u0644\u0631\u0646\u0629\u060C \u0627\u0644\u0634\u0627\u0634\u0629 \u062A\u0628\u0642\u0649 \u0634\u063A\u0627\u0644\u0629 \u0628\u0635\u0645\u062A
        }
    }

    private fun stopRingtone() {
        try {
            ringtonePlayer?.stop()
            ringtonePlayer?.release()
        } catch (e: Exception) {
            // \u062A\u062C\u0627\u0647\u0644 \u0623\u064A \u062E\u0637\u0623 \u0639\u0646\u062F \u0627\u0644\u0625\u064A\u0642\u0627\u0641
        }
        ringtonePlayer = null
    }

    private fun startCallTimer(statusView: TextView) {
        var seconds = 0
        val runnable = object : Runnable {
            override fun run() {
                seconds++
                val mins = seconds / 60
                val secs = seconds % 60
                statusView.text = String.format("%02d:%02d", mins, secs)
                handler.postDelayed(this, 1000L)
            }
        }
        handler.postDelayed(runnable, 1000L)
    }

    override fun onDestroy() {
        stopRingtone()
        handler.removeCallbacksAndMessages(null)
        super.onDestroy()
    }
}
