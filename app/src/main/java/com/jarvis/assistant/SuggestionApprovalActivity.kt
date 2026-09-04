package com.jarvis.assistant

import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

/**
 * تعرض اقتراحات التداول المعلّقة (PENDING) مع سبب الحكم الشرعي وسبب التوقيت.
 *
 * مهم جداً: زر "موافقة" هنا **لا ينفذ أي صفقة**. كل ما يفعله:
 *   1. يُحدّث حالة السجل إلى APPROVED_MANUAL_EXECUTION (لأغراض الأرشفة فقط).
 *   2. يفتح تطبيق الوسيط الرسمي (أو موقعه) ليقوم المستخدم بتنفيذ الصفقة بنفسه هناك.
 * لا يوجد هنا أي اتصال بمفاتيح API لحساب حقيقي، ولا أي تخزين لبيانات حساب مصرفي.
 */
class SuggestionApprovalActivity : AppCompatActivity() {

    // ⚠️ عدّل أسماء الحزم هذه لتطابق التطبيقات الفعلية المثبتة لديك بعد التحقق من Play Store.
    private val brokerPackageCandidates = listOf(
        "com.redotpay.app",
        "com.pocketbroker.app"
    )
    private val brokerFallbackUrl = "https://www.redotpay.com/"

    private lateinit var dao: TradeSuggestionDao
    private lateinit var container: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_suggestion_approval)

        dao = JarvisFinanceDatabase.getInstance(this).tradeSuggestionDao()
        container = findViewById(R.id.suggestionsContainer)

        loadSuggestions()
    }

    private fun loadSuggestions() {
        lifecycleScope.launch {
            val pending = dao.getPending()
            container.removeAllViews()
            if (pending.isEmpty()) {
                addEmptyState()
                return@launch
            }
            pending.forEach { suggestion -> container.addView(buildSuggestionCard(suggestion)) }
        }
    }

    private fun addEmptyState() {
        val text = TextView(this).apply {
            text = "لا توجد اقتراحات معلّقة حالياً."
            setTextColor(android.graphics.Color.parseColor("#AAAAAA"))
            gravity = Gravity.CENTER
            setPadding(0, 40, 0, 0)
        }
        container.addView(text)
    }

    private fun buildSuggestionCard(s: TradeSuggestionEntity): LinearLayout {
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundResource(R.drawable.chat_card_background)
            setPadding(28, 24, 28, 24)
            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
            )
            params.setMargins(0, 0, 0, 16)
            layoutParams = params
        }

        fun label(text: String, size: Float, color: String, bold: Boolean = false) = TextView(this).apply {
            this.text = text
            textSize = size
            setTextColor(android.graphics.Color.parseColor(color))
            if (bold) setTypeface(typeface, android.graphics.Typeface.BOLD)
        }

        card.addView(label("${s.ticker} — الإجراء المقترح: ${s.suggestedAction}", 18f, "#FFFFFF", true))
        card.addView(label("الحكم الشرعي: ${if (s.isHalal) "حلال ✅" else "غير مطابق ⛔"}", 14f,
            if (s.isHalal) "#2E7D32" else "#C62828"))
        card.addView(label(s.halalReason, 12f, "#CCCCCC"))
        card.addView(label("سبب التوقيت:", 13f, "#FFFFFF", true).apply { setPadding(0, 12, 0, 0) })
        card.addView(label(s.timingReason, 12f, "#CCCCCC"))

        val buttonsRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, 20, 0, 0)
        }

        val approveBtn = Button(this).apply {
            text = "موافقة (فتح الوسيط للتنفيذ اليدوي)"
            isEnabled = s.isHalal && s.suggestedAction != "انتظار"
            setOnClickListener { onApprove(s) }
        }
        val rejectBtn = Button(this).apply {
            text = "رفض"
            setOnClickListener { onReject(s) }
        }

        buttonsRow.addView(approveBtn)
        buttonsRow.addView(rejectBtn)
        card.addView(buttonsRow)

        return card
    }

    private fun onApprove(s: TradeSuggestionEntity) {
        lifecycleScope.launch {
            dao.updateStatus(s.id, "APPROVED_MANUAL_EXECUTION")
            openBrokerAppForManualExecution(s.ticker)
            loadSuggestions()
        }
    }

    private fun onReject(s: TradeSuggestionEntity) {
        lifecycleScope.launch {
            dao.updateStatus(s.id, "REJECTED")
            loadSuggestions()
        }
    }

    /**
     * يفتح تطبيق الوسيط المثبت (إن وُجد) وإلا موقعه في المتصفح.
     * لا يُرسل أي أمر شراء/بيع بنفسه — المستخدم من يبحث عن السهم وينفّذ الصفقة يدوياً.
     */
    private fun openBrokerAppForManualExecution(ticker: String) {
        val pm = packageManager
        val installedPackage = brokerPackageCandidates.firstOrNull { pkg ->
            try { pm.getPackageInfo(pkg, 0); true } catch (e: PackageManager.NameNotFoundException) { false }
        }

        val intent = if (installedPackage != null) {
            pm.getLaunchIntentForPackage(installedPackage)
        } else {
            Intent(Intent.ACTION_VIEW, Uri.parse(brokerFallbackUrl))
        }

        if (intent != null) {
            startActivity(intent)
            Toast.makeText(
                this,
                "افتح السهم $ticker ونفّذ الصفقة بنفسك داخل تطبيق الوسيط.",
                Toast.LENGTH_LONG
            ).show()
        } else {
            Toast.makeText(this, "تعذر فتح تطبيق الوسيط. افتحه يدوياً وابحث عن $ticker.", Toast.LENGTH_LONG).show()
        }
    }
}
