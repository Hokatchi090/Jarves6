package com.jarvis.assistant

import android.app.AlertDialog
import android.content.Context
import android.content.SharedPreferences
import android.text.InputType
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * المحفظة الحلال: تسجيل نفقات/دخل، تصنيف، ملخص شهري، واقتراح زكاة/صدقة تلقائي.
 * أول ميزة تُفصل عن MainActivity.kt كخطوة أولى نحو تقسيم الملف لعدة كلاسات أصغر.
 *
 * activity: تُستعمل لإنشاء الحوارات والوصول إلى findViewById
 * respond: دالة الرد الصوتي/الحالة الموجودة في MainActivity (يُمرَّر بدل ربط مباشر
 *          بالكلاس، حتى يبقى HalalWalletManager مستقلًا وقابلًا لإعادة الاستخدام)
 */
class HalalWalletManager(
    private val activity: AppCompatActivity,
    private val prefs: SharedPreferences,
    private val respond: (String) -> Unit
) {
    data class WalletEntry(
        val amount: Double, val category: String, val isExpense: Boolean, val monthKey: String
    )

    private val expenseCategories = listOf("طعام", "مواصلات", "ترفيه", "كتب ودراسة", "فواتير", "أخرى")

    private fun currentMonthKey(): String =
        SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date())

    private fun loadEntries(): MutableList<WalletEntry> {
        val raw = prefs.getString("wallet_entries_json", "[]") ?: "[]"
        val list = mutableListOf<WalletEntry>()
        try {
            val arr = JSONArray(raw)
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                list.add(
                    WalletEntry(
                        obj.getDouble("amount"), obj.getString("category"),
                        obj.getBoolean("isExpense"), obj.getString("monthKey")
                    )
                )
            }
        } catch (e: Exception) { }
        return list
    }

    private fun saveEntries(list: List<WalletEntry>) {
        val arr = JSONArray()
        list.forEach {
            val obj = JSONObject()
            obj.put("amount", it.amount)
            obj.put("category", it.category)
            obj.put("isExpense", it.isExpense)
            obj.put("monthKey", it.monthKey)
            arr.put(obj)
        }
        prefs.edit().putString("wallet_entries_json", arr.toString()).apply()
    }

    fun showAddEntryDialog(isExpense: Boolean) {
        val container = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            val pad = (16 * activity.resources.displayMetrics.density).toInt()
            setPadding(pad, pad, pad, pad)
        }
        val amountInput = EditText(activity).apply {
            hint = "المبلغ"
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
        }
        container.addView(amountInput)

        var selectedCategory = if (isExpense) expenseCategories.first() else "دخل"
        val categorySpinner: Spinner? = if (isExpense) {
            Spinner(activity).apply {
                adapter = ArrayAdapter(
                    activity, android.R.layout.simple_spinner_dropdown_item, expenseCategories
                )
                onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                    override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                        selectedCategory = expenseCategories[position]
                    }
                    override fun onNothingSelected(parent: AdapterView<*>?) {}
                }
            }
        } else null
        categorySpinner?.let { container.addView(it) }

        AlertDialog.Builder(activity)
            .setTitle(if (isExpense) "إضافة نفقة" else "إضافة دخل")
            .setView(container)
            .setPositiveButton("حفظ") { _, _ ->
                val amount = amountInput.text.toString().toDoubleOrNull()
                if (amount == null || amount <= 0.0) {
                    respond("قيمة غير صحيحة")
                } else {
                    val entries = loadEntries()
                    entries.add(WalletEntry(amount, selectedCategory, isExpense, currentMonthKey()))
                    saveEntries(entries)
                    refreshSummary()
                    respond(if (isExpense) "تم تسجيل النفقة" else "تم تسجيل الدخل")
                }
            }
            .setNegativeButton("إلغاء", null)
            .show()
    }

    fun refreshSummary() {
        val month = currentMonthKey()
        val entries = loadEntries().filter { it.monthKey == month }
        val totalExpenses = entries.filter { it.isExpense }.sumOf { it.amount }
        val totalIncome = entries.filter { !it.isExpense }.sumOf { it.amount }

        activity.findViewById<TextView>(R.id.walletMonthTotal)?.text =
            "إجمالي النفقات هذا الشهر: ${"%.2f".format(totalExpenses)}  |  الدخل: ${"%.2f".format(totalIncome)}"

        val byCategory = entries.filter { it.isExpense }
            .groupBy { it.category }
            .mapValues { (_, list) -> list.sumOf { it.amount } }
            .toList().sortedByDescending { it.second }

        activity.findViewById<TextView>(R.id.walletBreakdown)?.text = if (byCategory.isEmpty()) {
            "ماكاين أي نفقات مسجّلة هذا الشهر بعد"
        } else {
            byCategory.joinToString("\n") { (cat, amount) -> "$cat: ${"%.2f".format(amount)}" }
        }

        val net = totalIncome - totalExpenses
        val savingSuggestion = if (net > 0) {
            "الفائض هذا الشهر: ${"%.2f".format(net)} — نسبة زكاة/صدقة مقترحة (2.5%): ${"%.2f".format(net * 0.025)}"
        } else {
            ""
        }
        activity.findViewById<TextView>(R.id.walletSavingSuggestion)?.text = savingSuggestion
    }
}
