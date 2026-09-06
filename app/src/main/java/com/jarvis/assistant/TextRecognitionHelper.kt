package com.jarvis.assistant

import android.graphics.Bitmap
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions

/**
 * قراءة النص من صورة (OCR) عبر ML Kit — يدعم فقط الحروف اللاتينية
 * (إنجليزي/فرنسي وأرقام)، ولا يدعم العربية حاليًا (قيود ML Kit المجاني).
 */
class TextRecognitionHelper(private val onResult: (String) -> Unit) {

    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    fun recognize(bitmap: Bitmap, rotationDegrees: Int) {
        val image = InputImage.fromBitmap(bitmap, rotationDegrees)
        recognizer.process(image)
            .addOnSuccessListener { result ->
                onResult(result.text)
            }
            .addOnFailureListener {
                onResult("")
            }
    }

    fun close() {
        recognizer.close()
    }
}
