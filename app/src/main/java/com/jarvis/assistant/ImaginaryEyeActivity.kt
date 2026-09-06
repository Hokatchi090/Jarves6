package com.jarvis.assistant

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * "العين التخيلية": تصوّر أي شيء وتحوّله لصورة بأسلوب فني (فان جوخ، بيكاسو، أو
 * أي لوحة تختارها كمرجع في assets/style_reference.jpg)، عبر TensorFlow Lite
 * بالكامل على الجهاز، بدون إنترنت. الأداء بطيء على الهواتف الاقتصادية —
 * لقطة واحدة كل بضع ثوانٍ، وليس فيديو حي سلس.
 */
class ImaginaryEyeActivity : AppCompatActivity() {

    private lateinit var previewView: PreviewView
    private lateinit var resultImage: ImageView
    private lateinit var captureButton: TextView
    private lateinit var statusText: TextView

    private var styleHelper: StyleTransferHelper? = null

    companion object {
        private const val CAMERA_PERMISSION_REQUEST = 3003
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_imaginary_eye)

        previewView = findViewById(R.id.eyePreviewView)
        resultImage = findViewById(R.id.eyeResultImage)
        captureButton = findViewById(R.id.eyeCaptureButton)
        statusText = findViewById(R.id.eyeStatusText)

        try {
            styleHelper = StyleTransferHelper(this)
            if (styleHelper?.isReady() != true) {
                statusText.text = "\u0645\u0627 \u0644\u0642\u064A\u062A\u0634 \u0645\u0644\u0641\u0627\u062A \u0627\u0644\u0646\u0645\u0648\u0630\u062C \u0641\u064A assets/ \u0631\u0627\u062C\u0639 \u062A\u0639\u0644\u064A\u0645\u0627\u062A \u0627\u0644\u062A\u062B\u0628\u064A\u062A"
                captureButton.isEnabled = false
            }
        } catch (e: Exception) {
            statusText.text = "\u062A\u0639\u0630\u0651\u0631 \u062A\u062D\u0645\u064A\u0644 \u0627\u0644\u0646\u0645\u0648\u0630\u062C: ${e.message}"
            captureButton.isEnabled = false
        }

        captureButton.setOnClickListener { captureAndStylize() }

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

    private fun captureAndStylize() {
        val bitmap = previewView.bitmap
        val helper = styleHelper
        if (bitmap == null || helper == null) {
            statusText.text = "\u062A\u0639\u0630\u0651\u0631 \u0627\u0644\u062A\u0642\u0627\u0637 \u0627\u0644\u0635\u0648\u0631\u0629"
            return
        }
        statusText.text = "\u062C\u0627\u0631\u064A \u0627\u0644\u0631\u0633\u0645... (\u0642\u062F \u064A\u0623\u062E\u0630 \u0628\u0636\u0639 \u062B\u0648\u0627\u0646\u064D)"
        resultImage.visibility = android.view.View.GONE

        lifecycleScope.launch {
            val styled: Bitmap? = withContext(Dispatchers.Default) {
                try {
                    helper.applyStyle(bitmap)
                } catch (e: Exception) {
                    null
                }
            }
            if (styled != null) {
                resultImage.setImageBitmap(styled)
                resultImage.visibility = android.view.View.VISIBLE
                statusText.text = "\u062A\u0645! \u0647\u0630\u0627 \u0627\u0644\u0639\u0627\u0644\u0645 \u0628\u0623\u0633\u0644\u0648\u0628 \u0641\u0646\u064A"
            } else {
                statusText.text = "\u0641\u0634\u0644 \u0627\u0644\u062A\u062D\u0648\u064A\u0644\u060C \u062C\u0631\u0651\u0628 \u0645\u0631\u0629 \u0623\u062E\u0631\u0649"
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        styleHelper?.close()
    }
}
