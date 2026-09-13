package com.jarvisx.app

import android.graphics.Color
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.jarvisx.app.data.LocalDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object ThemeManager {

    val COLORS = linkedMapOf(
        "\u0623\u0635\u0641\u0631" to "#FFD700",
        "\u0628\u0646\u0641\u0633\u062C\u064A" to "#9C27B0",
        "\u0648\u0631\u062F\u064A" to "#FF69B4",
        "\u0628\u0631\u062A\u0642\u0627\u0644\u064A" to "#FF9800",
        "\u0623\u0632\u0631\u0642" to "#2196F3",
        "\u0623\u0633\u0648\u062F (\u0627\u0641\u062A\u0631\u0627\u0636\u064A)" to "#0D1117"
    )

    /** يطبّق اللون فورًا على عناصر الواجهة المعطاة (شريط الحالة، الأزرار، العناوين...) */
    fun applyTo(activity: AppCompatActivity, hex: String, root: View? = null) {
        val color = Color.parseColor(hex)
        activity.window.statusBarColor = color
        root?.let { applyRecursively(it, color) }
    }

    private fun applyRecursively(view: View, color: Int) {
        when (view) {
            is Button -> view.setBackgroundColor(color)
            is TextView -> if (view.tag == "themed_title") view.setTextColor(color)
        }
        if (view is android.view.ViewGroup) {
            for (i in 0 until view.childCount) applyRecursively(view.getChildAt(i), color)
        }
    }

    /** يحفظ الاختيار محليًا (Room) — لا يحتاج نت إطلاقًا */
    fun saveChoice(activity: AppCompatActivity, hex: String) {
        CoroutineScope(Dispatchers.IO).launch {
            val dao = LocalDatabase.getInstance(activity).familyConfigDao()
            val current = dao.get()
            if (current != null) {
                dao.save(current.copy(themeColorHex = hex))
            }
        }
    }
}
