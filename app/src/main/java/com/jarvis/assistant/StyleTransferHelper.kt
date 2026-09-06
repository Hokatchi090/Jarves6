package com.jarvis.assistant

import android.content.Context
import android.graphics.Bitmap
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel

/**
 * "العين التخيلية": يطبّق أسلوب فني (فان جوخ، بيكاسو، أي لوحة تختارها) على صورة
 * ملتقطة، عبر نموذج Magenta Arbitrary Image Stylization (يشتغل بالكامل على
 * الجهاز، بدون إنترنت).
 *
 * يحتاج وجود 3 ملفات في assets/ (راجع تعليمات التحميل المرفقة):
 *  - magenta_predict.tflite
 *  - magenta_transfer.tflite
 *  - style_reference.jpg  (أي لوحة فنية تختارها، مثلاً "ليلة مرصعة بالنجوم")
 */
class StyleTransferHelper(context: Context) {

    private val predictInterpreter: Interpreter
    private val transferInterpreter: Interpreter
    private var styleBottleneck: Array<Array<Array<FloatArray>>>? = null

    companion object {
        private const val STYLE_IMAGE_SIZE = 256
        private const val CONTENT_IMAGE_SIZE = 384
    }

    init {
        predictInterpreter = Interpreter(loadModelFile(context, "magenta_predict.tflite"))
        transferInterpreter = Interpreter(loadModelFile(context, "magenta_transfer.tflite"))
        try {
            val styleBitmap = context.assets.open("style_reference.jpg").use {
                android.graphics.BitmapFactory.decodeStream(it)
            }
            styleBottleneck = computeStyleBottleneck(styleBitmap)
        } catch (e: Exception) {
            styleBottleneck = null
        }
    }

    private fun loadModelFile(context: Context, filename: String): MappedByteBuffer {
        val fd = context.assets.openFd(filename)
        val inputStream = FileInputStream(fd.fileDescriptor)
        val channel = inputStream.channel
        return channel.map(FileChannel.MapMode.READ_ONLY, fd.startOffset, fd.declaredLength)
    }

    private fun bitmapToInputBuffer(bitmap: Bitmap, size: Int): ByteBuffer {
        val resized = Bitmap.createScaledBitmap(bitmap, size, size, true)
        val buffer = ByteBuffer.allocateDirect(4 * size * size * 3)
        buffer.order(ByteOrder.nativeOrder())
        val pixels = IntArray(size * size)
        resized.getPixels(pixels, 0, size, 0, 0, size, size)
        for (pixel in pixels) {
            buffer.putFloat(((pixel shr 16) and 0xFF) / 255f)
            buffer.putFloat(((pixel shr 8) and 0xFF) / 255f)
            buffer.putFloat((pixel and 0xFF) / 255f)
        }
        buffer.rewind()
        return buffer
    }

    private fun computeStyleBottleneck(styleBitmap: Bitmap): Array<Array<Array<FloatArray>>> {
        val input = bitmapToInputBuffer(styleBitmap, STYLE_IMAGE_SIZE)
        // \u0634\u0643\u0644 \u0627\u0644\u0645\u062E\u0631\u062C \u0627\u0644\u0645\u0639\u062A\u0627\u062F \u0644\u0647\u0630\u0627 \u0627\u0644\u0646\u0645\u0648\u0630\u062C: 1x1x1x100
        val output = Array(1) { Array(1) { Array(1) { FloatArray(100) } } }
        predictInterpreter.run(input, output)
        return output
    }

    /** يطبّق الأسلوب الفني على صورة المحتوى، يرجع null إذا كانت ملفات النموذج ناقصة */
    fun applyStyle(contentBitmap: Bitmap): Bitmap? {
        val bottleneck = styleBottleneck ?: return null
        val contentInput = bitmapToInputBuffer(contentBitmap, CONTENT_IMAGE_SIZE)

        val outputSize = CONTENT_IMAGE_SIZE
        val output = Array(1) { Array(outputSize) { Array(outputSize) { FloatArray(3) } } }

        val inputs = arrayOf<Any>(contentInput, bottleneck)
        val outputs = mutableMapOf<Int, Any>(0 to output)
        transferInterpreter.runForMultipleInputsOutputs(inputs, outputs)

        val resultBitmap = Bitmap.createBitmap(outputSize, outputSize, Bitmap.Config.ARGB_8888)
        val pixels = IntArray(outputSize * outputSize)
        for (y in 0 until outputSize) {
            for (x in 0 until outputSize) {
                val r = (output[0][y][x][0] * 255).toInt().coerceIn(0, 255)
                val g = (output[0][y][x][1] * 255).toInt().coerceIn(0, 255)
                val b = (output[0][y][x][2] * 255).toInt().coerceIn(0, 255)
                pixels[y * outputSize + x] = (0xFF shl 24) or (r shl 16) or (g shl 8) or b
            }
        }
        resultBitmap.setPixels(pixels, 0, outputSize, 0, 0, outputSize, outputSize)
        return resultBitmap
    }

    fun isReady(): Boolean = styleBottleneck != null

    fun close() {
        predictInterpreter.close()
        transferInterpreter.close()
    }
}
