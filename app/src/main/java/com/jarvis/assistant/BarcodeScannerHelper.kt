package com.jarvis.assistant

import android.graphics.Bitmap
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage

/**
 * يقرأ الباركود (EAN/UPC) من إطار الكاميرا لتحديد المنتج،
 * تمهيداً لفحصه عبر ProductHalalChecker.
 */
class BarcodeScannerHelper(
    private val onBarcodeDetected: (String) -> Unit
) {
    private val scanner by lazy { BarcodeScanning.getClient() }

    fun scan(bitmap: Bitmap, rotationDegrees: Int = 0) {
        val image = InputImage.fromBitmap(bitmap, rotationDegrees)
        scanner.process(image)
            .addOnSuccessListener { barcodes ->
                val code = barcodes
                    .filter { it.valueType == Barcode.TYPE_PRODUCT || it.rawValue != null }
                    .mapNotNull { it.rawValue }
                    .firstOrNull()
                if (code != null) onBarcodeDetected(code)
            }
    }

    fun close() {
        scanner.close()
    }
}
