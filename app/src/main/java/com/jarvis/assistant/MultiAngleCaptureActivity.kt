package com.jarvis.assistant

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Bundle
import android.os.Environment
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.io.File
import java.io.FileOutputStream

/**
 * "استنساخ شبه-ثلاثي الأبعاد": يوجّه المستخدم لالتقاط 8 صور حول أي شيء (زاوية كل 45°)،
 * ثم يحفظها كمجموعة واحدة يمكن استعراضها لاحقًا بالسحب (Viewer360Activity) لإحساس
 * بمشاهدة الشيء من كل الجهات. هذا ليس مجسم 3D حقيقي، بل معرض صور منظّم (راجع الشرح).
 */
class MultiAngleCaptureActivity : AppCompatActivity() {

    private lateinit var previewView: PreviewView
    private lateinit var angleLabel: TextView
    private lateinit var captureButton: TextView
    private lateinit var finishButton: TextView
    private lateinit var thumbStrip: LinearLayout
    private lateinit var filterBar: LinearLayout

    private var currentFilter = CameraFilter.NORMAL
    private var currentAngleIndex = 0
    private var sessionId = ""
    private val capturedPaths = mutableListOf<String>()

    private val angleNames = listOf(
        "الأمام", "أمام-يمين", "اليمين", "خلف-يمين",
        "الخلف", "خلف-يسار", "اليسار", "أمام-يسار"
    )

    companion object {
        private const val CAMERA_PERMISSION_REQUEST = 3004
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_multi_angle_capture)

        previewView = findViewById(R.id.angleCapturePreview)
        angleLabel = findViewById(R.id.angleCaptureLabel)
        captureButton = findViewById(R.id.angleCaptureButton)
        finishButton = findViewById(R.id.angleCaptureFinishButton)
        thumbStrip = findViewById(R.id.angleCaptureThumbStrip)
        filterBar = findViewById(R.id.angleCaptureFilterBar)

        sessionId = "scan_${System.currentTimeMillis()}"
        updateAngleLabel()
        buildFilterBar()

        captureButton.setOnClickListener { captureCurrentAngle() }
        finishButton.setOnClickListener { finishSession() }

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

    private fun buildFilterBar() {
        val density = resources.displayMetrics.density
        CameraFilter.values().forEach { filter ->
            val chip = TextView(this).apply {
                text = filter.label
                setTextColor(android.graphics.Color.parseColor("#8DEFFF"))
                textSize = 10f
                setPadding((14 * density).toInt(), (8 * density).toInt(), (14 * density).toInt(), (8 * density).toInt())
                setBackgroundResource(R.drawable.hud_glow_card)
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { marginEnd = (8 * density).toInt() }
                setOnClickListener { currentFilter = filter }
            }
            filterBar.addView(chip)
        }
    }

    private fun updateAngleLabel() {
        if (currentAngleIndex < angleNames.size) {
            angleLabel.text = "الزاوية ${currentAngleIndex + 1}/8: ${angleNames[currentAngleIndex]} — دُر حول الشيء ثم التقط"
        }
    }

    private fun captureCurrentAngle() {
        val bitmap = previewView.bitmap
        if (bitmap == null || currentAngleIndex >= angleNames.size) return

        val filtered = CameraFilter.apply(bitmap, currentFilter)
        val path = saveBitmapToSession(filtered, currentAngleIndex)
        if (path != null) {
            capturedPaths.add(path)
            addThumbnail(filtered)
            currentAngleIndex++
            if (currentAngleIndex < angleNames.size) {
                updateAngleLabel()
            } else {
                angleLabel.text = "تم! التقطت كل الزوايا الثمانية. اضغط FINISH للمعاينة"
                captureButton.isEnabled = false
            }
        }
    }

    private fun addThumbnail(bitmap: Bitmap) {
        val density = resources.displayMetrics.density
        val thumb = android.widget.ImageView(this).apply {
            setImageBitmap(bitmap)
            layoutParams = LinearLayout.LayoutParams((60 * density).toInt(), (60 * density).toInt()).apply {
                marginEnd = (6 * density).toInt()
            }
            scaleType = android.widget.ImageView.ScaleType.CENTER_CROP
        }
        thumbStrip.addView(thumb)
    }

    private fun saveBitmapToSession(bitmap: Bitmap, index: Int): String? {
        return try {
            val dir = File(getExternalFilesDir(Environment.DIRECTORY_PICTURES), sessionId)
            if (!dir.exists()) dir.mkdirs()
            val file = File(dir, "angle_$index.jpg")
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
            }
            file.absolutePath
        } catch (e: Exception) {
            null
        }
    }

    private fun finishSession() {
        if (capturedPaths.isEmpty()) return
        val intent = Intent(this, Viewer360Activity::class.java)
        intent.putStringArrayListExtra("PHOTO_PATHS", ArrayList(capturedPaths))
        startActivity(intent)
        finish()
    }
}
