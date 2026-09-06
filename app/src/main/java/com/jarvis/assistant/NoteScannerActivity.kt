package com.jarvis.assistant

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import org.json.JSONArray
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * شاشة مسح المستندات/الملاحظات: تصوّر أي ورقة أو شاشة وتحوّلها لنص قابل للحفظ والبحث.
 * ملاحظة مهمة: تدعم فقط النصوص بالحروف اللاتينية (إنجليزي/فرنسي)، وليس العربية،
 * بسبب قيود محرك ML Kit المجاني المستخدم هنا.
 */
class NoteScannerActivity : AppCompatActivity() {

    private lateinit var previewView: PreviewView
    private lateinit var recognizedText: TextView
    private lateinit var captureButton: TextView
    private lateinit var saveButton: TextView

    private val textRecognizer by lazy {
        TextRecognitionHelper { text -> onTextRecognized(text) }
    }

    private var lastRecognizedText: String = ""

    companion object {
        private const val CAMERA_PERMISSION_REQUEST = 3002
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_note_scanner)

        previewView = findViewById(R.id.notePreviewView)
        recognizedText = findViewById(R.id.noteRecognizedText)
        captureButton = findViewById(R.id.noteCaptureButton)
        saveButton = findViewById(R.id.noteSaveButton)

        captureButton.setOnClickListener { captureAndRecognize() }
        saveButton.setOnClickListener { saveCurrentNote() }

        if (hasCameraPermission()) {
            startCamera()
        } else {
            ActivityCompat.requestPermissions(
                this, arrayOf(Manifest.permission.CAMERA), CAMERA_PERMISSION_REQUEST
            )
        }
    }

    private fun hasCameraPermission() =
        ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) ==
            PackageManager.PERMISSION_GRANTED

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == CAMERA_PERMISSION_REQUEST && hasCameraPermission()) {
            startCamera()
        }
    }

    private fun startCamera() {
        val providerFuture = ProcessCameraProvider.getInstance(this)
        providerFuture.addListener({
            val cameraProvider = providerFuture.get()
            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }
            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(this, CameraSelector.DEFAULT_BACK_CAMERA, preview)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun captureAndRecognize() {
        val bitmap = previewView.bitmap
        if (bitmap == null) {
            recognizedText.text = "تعذّر التقاط الصورة، حاول مرة أخرى"
            return
        }
        recognizedText.text = "جاري قراءة النص..."
        textRecognizer.recognize(bitmap, 0)
    }

    private fun onTextRecognized(text: String) {
        runOnUiThread {
            lastRecognizedText = text
            recognizedText.text = if (text.isBlank()) {
                "ما لقيتش نص واضح، حاول تقرّب أكثر (يقرأ فقط إنجليزي/فرنسي حاليًا)"
            } else {
                text
            }
            saveButton.visibility = if (text.isNotBlank()) android.view.View.VISIBLE else android.view.View.GONE
        }
    }

    private fun saveCurrentNote() {
        if (lastRecognizedText.isBlank()) return
        val prefs = getSharedPreferences("jarvis_prefs", MODE_PRIVATE)
        val raw = prefs.getString("scanned_notes_json", "[]") ?: "[]"
        val arr = try { JSONArray(raw) } catch (e: Exception) { JSONArray() }

        val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())
        val obj = org.json.JSONObject()
        obj.put("timestamp", timestamp)
        obj.put("text", lastRecognizedText)
        arr.put(obj)

        prefs.edit().putString("scanned_notes_json", arr.toString()).apply()
        recognizedText.text = "تم حفظ الملاحظة ✅"
        saveButton.visibility = android.view.View.GONE
    }

    override fun onDestroy() {
        super.onDestroy()
        textRecognizer.close()
    }
}
