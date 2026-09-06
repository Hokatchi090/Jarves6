package com.jarvis.assistant

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint

/**
 * فلاتر ألوان بسيطة (تدرّج لوني، ليست فلاتر وجه متحركة) — تُطبَّق على أي Bitmap.
 */
enum class CameraFilter(val label: String) {
    NORMAL("عادي"),
    BW("أبيض وأسود"),
    SEPIA("سيبيا"),
    VINTAGE("فينتاج"),
    COOL("بارد"),
    WARM("دافئ"),
    VIVID("حيوي"),
    INVERT("عكس الألوان");

    fun colorMatrix(): ColorMatrix = when (this) {
        NORMAL -> ColorMatrix()
        BW -> ColorMatrix().apply { setSaturation(0f) }
        SEPIA -> ColorMatrix(
            floatArrayOf(
                0.393f, 0.769f, 0.189f, 0f, 0f,
                0.349f, 0.686f, 0.168f, 0f, 0f,
                0.272f, 0.534f, 0.131f, 0f, 0f,
                0f, 0f, 0f, 1f, 0f
            )
        )
        VINTAGE -> ColorMatrix(
            floatArrayOf(
                0.9f, 0.1f, 0.1f, 0f, 10f,
                0.05f, 0.85f, 0.1f, 0f, 10f,
                0.1f, 0.1f, 0.7f, 0f, 20f,
                0f, 0f, 0f, 1f, 0f
            )
        )
        COOL -> ColorMatrix(
            floatArrayOf(
                0.9f, 0f, 0f, 0f, 0f,
                0f, 0.95f, 0f, 0f, 5f,
                0f, 0f, 1.15f, 0f, 15f,
                0f, 0f, 0f, 1f, 0f
            )
        )
        WARM -> ColorMatrix(
            floatArrayOf(
                1.15f, 0f, 0f, 0f, 15f,
                0f, 1.02f, 0f, 0f, 5f,
                0f, 0f, 0.85f, 0f, 0f,
                0f, 0f, 0f, 1f, 0f
            )
        )
        VIVID -> ColorMatrix().apply { setSaturation(1.6f) }
        INVERT -> ColorMatrix(
            floatArrayOf(
                -1f, 0f, 0f, 0f, 255f,
                0f, -1f, 0f, 0f, 255f,
                0f, 0f, -1f, 0f, 255f,
                0f, 0f, 0f, 1f, 0f
            )
        )
    }

    companion object {
        /** يطبّق الفلتر على صورة ويرجع نسخة جديدة (لا يعدّل الأصلية) */
        fun apply(bitmap: Bitmap, filter: CameraFilter): Bitmap {
            if (filter == NORMAL) return bitmap
            val result = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(result)
            val paint = Paint().apply {
                colorFilter = ColorMatrixColorFilter(filter.colorMatrix())
            }
            canvas.drawBitmap(bitmap, 0f, 0f, paint)
            return result
        }
    }
}
