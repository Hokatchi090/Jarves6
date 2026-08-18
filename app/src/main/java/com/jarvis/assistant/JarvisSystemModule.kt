package com.jarvis.assistant

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.os.BatteryManager
import android.provider.Settings
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class JarvisSystemModule(
    private val activity: Activity,
    private val speak: (String) -> Unit
) {

    private var flashOn = false

    fun execute(intent: JarvisIntent): Boolean {
        return when (intent.type) {
            JarvisIntentType.SYSTEM_FLASH -> handleFlash(intent.argument)
            JarvisIntentType.SYSTEM_BATTERY -> handleBattery()
            JarvisIntentType.SYSTEM_TIME -> handleTime()
            JarvisIntentType.SYSTEM_CAMERA -> handleCamera()
            JarvisIntentType.SYSTEM_SETTINGS -> handleSettings()
            else -> false
        }
    }

    private fun handleFlash(argument: String): Boolean {
        val turnOn = argument != "off"

        return try {
            val cameraManager =
                activity.getSystemService(Context.CAMERA_SERVICE) as CameraManager

            val cameraId = cameraManager.cameraIdList.firstOrNull { id ->
                cameraManager.getCameraCharacteristics(id)
                    .get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
            } ?: return false

            cameraManager.setTorchMode(cameraId, turnOn)
            flashOn = turnOn

            speak(if (turnOn) "\u062A\u0645 \u062A\u0634\u063A\u064A\u0644 \u0627\u0644\u0641\u0644\u0627\u0634." else "\u062A\u0645 \u0625\u064A\u0642\u0627\u0641 \u0627\u0644\u0641\u0644\u0627\u0634.")
            true
        } catch (e: Exception) {
            speak("\u062A\u0639\u0630\u0631 \u0627\u0644\u062A\u062D\u0643\u0645 \u0628\u0627\u0644\u0641\u0644\u0627\u0634.")
            true
        }
    }

    private fun handleBattery(): Boolean {
        return try {
            val batteryManager =
                activity.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
            val level = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
            speak("\u0627\u0644\u0628\u0637\u0627\u0631\u064A\u0629 \u0639\u0646\u062F $level \u0628\u0627\u0644\u0645\u0626\u0629.")
            true
        } catch (e: Exception) {
            speak("\u062A\u0639\u0630\u0631 \u0642\u0631\u0627\u0621\u0629 \u0645\u0633\u062A\u0648\u0649 \u0627\u0644\u0628\u0637\u0627\u0631\u064A\u0629.")
            true
        }
    }

    private fun handleTime(): Boolean {
        val time = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
        speak("\u0627\u0644\u0648\u0642\u062A \u0627\u0644\u0622\u0646 $time.")
        return true
    }

    private fun handleCamera(): Boolean {
        return try {
            activity.startActivity(Intent("android.media.action.IMAGE_CAPTURE"))
            speak("\u062A\u0645 \u0641\u062A\u062D \u0627\u0644\u0643\u0627\u0645\u064A\u0631\u0627.")
            true
        } catch (e: Exception) {
            speak("\u062A\u0639\u0630\u0631 \u0641\u062A\u062D \u0627\u0644\u0643\u0627\u0645\u064A\u0631\u0627.")
            true
        }
    }

    private fun handleSettings(): Boolean {
        return try {
            activity.startActivity(Intent(Settings.ACTION_SETTINGS))
            speak("\u062A\u0645 \u0641\u062A\u062D \u0627\u0644\u0625\u0639\u062F\u0627\u062F\u0627\u062A.")
            true
        } catch (e: Exception) {
            speak("\u062A\u0639\u0630\u0631 \u0641\u062A\u062D \u0627\u0644\u0625\u0639\u062F\u0627\u062F\u0627\u062A.")
            true
        }
    }
}
