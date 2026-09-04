package com.jarvis.assistant

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * شاشة الكاميرا الذكية: تكشف الأشياء وتقرأ الباركود، وعند العثور على
 * منتج غذائي تفحص حالته الشرعية (حلال/حرام/مشبوه) عبر ProductHalalChecker.
 *
 * لا تُفعَّل إلا بعد منح إذن الكاميرا (CAMERA) الموجود مسبقاً في الـ Manifest.
 */
class ProductScannerActivity : AppCompatActivity() {

    private lateinit var previewView: PreviewView
    private lateinit var resultCard: android.widget.LinearLayout
    private lateinit var statusBadge: TextView
    private lateinit var productNameText: TextView
    private lateinit var reasonText: TextView
    private lateinit var detectedObjectText: TextView

    private lateinit var cameraExecutor: ExecutorService
    private val objectDetector by lazy {
        ObjectDetectorHelper { items -> onObjectsDetected(items) }
    }
    private val barcodeScanner by lazy {
        BarcodeScannerHelper { code -> onBarcodeDetected(code) }
    }
    private val halalChecker by lazy { ProductHalalChecker() }

    // لتجنّب فحص نفس الباركود عشرات المرات في الثانية
    private var lastScannedBarcode: String? = null
    private var isCheckingProduct = false

    companion object {
        private const val CAMERA_PERMISSION_REQUEST = 3001
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_product_scanner)

        previewView = findViewById(R.id.previewView)
        resultCard = findViewById(R.id.resultCard)
        statusBadge = findViewById(R.id.statusBadge)
        productNameText = findViewById(R.id.productNameText)
        reasonText = findViewById(R.id.reasonText)
        detectedObjectText = findViewById(R.id.detectedObjectText)

        cameraExecutor = Executors.newSingleThreadExecutor()

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

            val preview = androidx.camera.core.Preview.Builder().build().also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }

            val imageAnalysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .also { analysis ->
                    analysis.setAnalyzer(cameraExecutor) { imageProxy ->
                        analyzeFrame(imageProxy)
                    }
                }

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    this, CameraSelector.DEFAULT_BACK_CAMERA, preview, imageAnalysis
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }, ContextCompat.getMainExecutor(this))
    }

    @androidx.camera.core.ExperimentalGetImage
    private fun analyzeFrame(imageProxy: ImageProxy) {
        val bitmap = imageProxyToBitmap(imageProxy)
        val rotation = imageProxy.imageInfo.rotationDegrees
        if (bitmap != null) {
            objectDetector.detectObjects(bitmap, rotation)
            // لا نفحص الباركود إذا كان هناك فحص منتج جارٍ حالياً
            if (!isCheckingProduct) {
                barcodeScanner.scan(bitmap, rotation)
            }
        }
        imageProxy.close()
    }

    /** تحويل بسيط لـ ImageProxy إلى Bitmap عبر الـ PreviewView bitmap الحالي
     *  (أبسط وأخف من فك YUV يدوياً؛ كافٍ لمعدل تحليل بضع إطارات بالثانية). */
    private fun imageProxyToBitmap(imageProxy: ImageProxy): Bitmap? {
        return previewView.bitmap
    }

    private fun onObjectsDetected(items: List<ObjectDetectorHelper.DetectedItem>) {
        runOnUiThread {
            val label = items.firstOrNull()?.label
            detectedObjectText.text = if (label != null) "تم رصد: $label" else ""
        }
    }

    private fun onBarcodeDetected(barcode: String) {
        if (barcode == lastScannedBarcode || isCheckingProduct) return
        lastScannedBarcode = barcode
        isCheckingProduct = true

        lifecycleScope.launch {
            val result = halalChecker.checkByBarcode(barcode)
            showResult(result)
            isCheckingProduct = false
        }
    }

    private fun showResult(result: ProductHalalChecker.Result) {
        runOnUiThread {
            resultCard.visibility = android.view.View.VISIBLE
            val (label, color) = when (result.status) {
                ProductHalalChecker.Status.HALAL -> "✅ حلال" to "#2E7D32"
                ProductHalalChecker.Status.HARAM -> "⛔ حرام" to "#C62828"
                ProductHalalChecker.Status.MASHBOOH -> "⚠️ مشبوه — يحتاج تحققاً" to "#F9A825"
                ProductHalalChecker.Status.UNKNOWN -> "❔ غير معروف" to "#616161"
            }
            statusBadge.text = label
            statusBadge.setTextColor(android.graphics.Color.parseColor(color))
            productNameText.text = result.productName ?: "منتج غير مُعرّف"
            reasonText.text = if (result.flaggedIngredients.isNotEmpty()) {
                "${result.reason}\nالمكونات المعنية: ${result.flaggedIngredients.joinToString(", ")}"
            } else {
                result.reason
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        objectDetector.close()
        barcodeScanner.close()
        cameraExecutor.shutdown()
    }
}
