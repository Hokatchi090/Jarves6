package com.jarvis.assistant

import android.graphics.Bitmap
import android.graphics.Rect
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.objects.ObjectDetection
import com.google.mlkit.vision.objects.ObjectDetector
import com.google.mlkit.vision.objects.defaults.ObjectDetectorOptions

/**
 * يغلف منطق الكشف عن الأشياء عبر ML Kit.
 * يعمل في وضع الفيديو المباشر (STREAM_MODE) لأداء سلس أثناء تحليل إطارات الكاميرا.
 */
class ObjectDetectorHelper(
    private val onObjectsDetected: (List<DetectedItem>) -> Unit
) {

    data class DetectedItem(
        val label: String,
        val confidence: Float,
        val boundingBox: Rect,
        val trackingId: Int?
    )

    private val detector: ObjectDetector by lazy {
        val options = ObjectDetectorOptions.Builder()
            .setDetectorMode(ObjectDetectorOptions.STREAM_MODE)
            .enableMultipleObjects()
            .enableClassification()
            .build()
        ObjectDetection.getClient(options)
    }

    /** يحلل صورة (Bitmap) واحدة ويرجع قائمة الأشياء المكتشفة عبر الكولباك. */
    fun detectObjects(bitmap: Bitmap, rotationDegrees: Int = 0) {
        val image = InputImage.fromBitmap(bitmap, rotationDegrees)
        detector.process(image)
            .addOnSuccessListener { objects ->
                val items = objects.map { obj ->
                    val bestLabel = obj.labels.maxByOrNull { it.confidence }
                    DetectedItem(
                        label = bestLabel?.text ?: "غير معروف",
                        confidence = bestLabel?.confidence ?: 0f,
                        boundingBox = obj.boundingBox,
                        trackingId = obj.trackingId
                    )
                }
                onObjectsDetected(items)
            }
            .addOnFailureListener {
                onObjectsDetected(emptyList())
            }
    }

    fun close() {
        detector.close()
    }
}
