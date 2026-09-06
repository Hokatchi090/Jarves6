package com.jarvis.assistant

import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.GestureDetector
import android.view.MotionEvent
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

/**
 * معاين شبه-3D: تسحب يمين/يسار فيتنقل بين الصور المُلتقطة من MultiAngleCaptureActivity،
 * يعطي إحساس "الدوران حول الشيء" (كصور المنتجات 360° بالمواقع)، وليس مجسم 3D حقيقي.
 */
class Viewer360Activity : AppCompatActivity() {

    private lateinit var imageView: ImageView
    private lateinit var angleIndicator: TextView
    private var photoPaths: List<String> = emptyList()
    private var currentIndex = 0
    private lateinit var gestureDetector: GestureDetector

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_viewer_360)

        imageView = findViewById(R.id.viewer360Image)
        angleIndicator = findViewById(R.id.viewer360Indicator)

        photoPaths = intent.getStringArrayListExtra("PHOTO_PATHS") ?: emptyList()
        if (photoPaths.isEmpty()) {
            angleIndicator.text = "ماكاين صور محفوظة"
            return
        }
        showImage(0)

        gestureDetector = GestureDetector(this, object : GestureDetector.SimpleOnGestureListener() {
            override fun onFling(
                e1: MotionEvent?, e2: MotionEvent, velocityX: Float, velocityY: Float
            ): Boolean {
                if (velocityX < -300) {
                    nextImage()
                    return true
                } else if (velocityX > 300) {
                    previousImage()
                    return true
                }
                return false
            }
        })

        imageView.setOnTouchListener { _, event ->
            gestureDetector.onTouchEvent(event)
            true
        }
    }

    private fun showImage(index: Int) {
        currentIndex = ((index % photoPaths.size) + photoPaths.size) % photoPaths.size
        val bitmap = BitmapFactory.decodeFile(photoPaths[currentIndex])
        imageView.setImageBitmap(bitmap)
        angleIndicator.text = "${currentIndex + 1} / ${photoPaths.size} — اسحب يمين أو يسار للدوران"
    }

    private fun nextImage() = showImage(currentIndex + 1)
    private fun previousImage() = showImage(currentIndex - 1)
}
