package com.jarvis.assistant

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject

/**
 * تصفية شرعية مبسطة للأسهم على أساس منهجية شبيهة بـ AAOIFI:
 *  1. استبعاد القطاعات المحرّمة (كحول، قمار، بنوك ربوية تقليدية، تبغ، سلاح، ترفيه للبالغين...).
 *  2. نسبة الدين إلى القيمة السوقية < 33%.
 *  3. نسبة النقد + الأوراق المالية المدرة للفائدة إلى القيمة السوقية < 33%.
 *  4. نسبة الذمم المدينة إلى القيمة السوقية < 49%.
 *
 * هذا فحص آلي تقريبي لأغراض الفرز الأولي فقط، وليس فتوى أو بديلاً عن مرجع شرعي معتمد
 * (مثل Zoya أو Islamicly) قبل اتخاذ أي قرار استثماري فعلي.
 *
 * يعتمد على مزوّد بيانات مالية قابل للتبديل (FinancialDataProvider) بدل ربطه بمزوّد
 * مدفوع واحد؛ الافتراضي هنا يستخدم Financial Modeling Prep (له باقة مجانية محدودة).
 */
interface FinancialDataProvider {
    suspend fun getFundamentals(ticker: String): CompanyFundamentals?
}

data class CompanyFundamentals(
    val ticker: String,
    val sector: String,
    val marketCap: Double,
    val totalDebt: Double,
    val cashAndSecurities: Double,
    val accountsReceivable: Double
)

class HalalScreener(
    private val dataProvider: FinancialDataProvider
) {

    data class ScreeningResult(
        val ticker: String,
        val isHalal: Boolean,
        val reason: String
    )

    private val excludedSectors = listOf(
        "alcoholic beverages", "brewers", "distillers",
        "gambling, resorts & casinos", "casinos & gaming",
        "banks", "banking", "insurance", "consumer finance", "financial services",
        "tobacco", "aerospace & defense", "adult entertainment", "pork products"
    )

    suspend fun screen(ticker: String): ScreeningResult {
        val data = dataProvider.getFundamentals(ticker)
            ?: return ScreeningResult(ticker, false, "تعذر جلب البيانات المالية لهذا السهم.")

        val sectorLower = data.sector.lowercase()
        val excludedMatch = excludedSectors.firstOrNull { sectorLower.contains(it) }
        if (excludedMatch != null) {
            return ScreeningResult(
                ticker, false,
                "القطاع (${data.sector}) مستبعد شرعياً (يطابق: $excludedMatch)."
            )
        }

        if (data.marketCap <= 0.0) {
            return ScreeningResult(ticker, false, "بيانات القيمة السوقية غير متوفرة أو غير صالحة.")
        }

        val debtRatio = data.totalDebt / data.marketCap
        val cashRatio = data.cashAndSecurities / data.marketCap
        val receivablesRatio = data.accountsReceivable / data.marketCap

        val failures = mutableListOf<String>()
        if (debtRatio >= 0.33) failures.add("نسبة الدين للقيمة السوقية ${"%.1f".format(debtRatio * 100)}% (الحد 33%)")
        if (cashRatio >= 0.33) failures.add("نسبة النقد/الفوائد للقيمة السوقية ${"%.1f".format(cashRatio * 100)}% (الحد 33%)")
        if (receivablesRatio >= 0.49) failures.add("نسبة الذمم المدينة للقيمة السوقية ${"%.1f".format(receivablesRatio * 100)}% (الحد 49%)")

        return if (failures.isEmpty()) {
            ScreeningResult(
                ticker, true,
                "القطاع مقبول، ونسب الدين/النقد/الذمم كلها ضمن الحدود الشرعية المعتمدة."
            )
        } else {
            ScreeningResult(ticker, false, "تجاوز حدود التصفية المالية: ${failures.joinToString("; ")}")
        }
    }
}

/**
 * تنفيذ افتراضي لمزوّد البيانات عبر Financial Modeling Prep.
 * يتطلب مفتاح API مجاني يُوضع في local.properties باسم FMP_API_KEY
 * (على غرار GEMINI_API_KEY الموجود مسبقاً في المشروع) — لا يلمس أي حساب مصرفي أو وسيط للمستخدم.
 */
class FmpFinancialDataProvider(
    private val apiKey: String,
    private val client: OkHttpClient = OkHttpClient()
) : FinancialDataProvider {

    override suspend fun getFundamentals(ticker: String): CompanyFundamentals? =
        withContext(Dispatchers.IO) {
            try {
                val profileUrl = "https://financialmodelingprep.com/api/v3/profile/$ticker?apikey=$apiKey"
                val balanceUrl = "https://financialmodelingprep.com/api/v3/balance-sheet-statement/$ticker?limit=1&apikey=$apiKey"

                val profileJson = fetchJsonArray(profileUrl)?.optJSONObject(0) ?: return@withContext null
                val balanceJson = fetchJsonArray(balanceUrl)?.optJSONObject(0)

                CompanyFundamentals(
                    ticker = ticker,
                    sector = profileJson.optString("sector", "unknown"),
                    marketCap = profileJson.optDouble("mktCap", 0.0),
                    totalDebt = balanceJson?.optDouble("totalDebt", 0.0) ?: 0.0,
                    cashAndSecurities = balanceJson?.optDouble("cashAndShortTermInvestments", 0.0) ?: 0.0,
                    accountsReceivable = balanceJson?.optDouble("netReceivables", 0.0) ?: 0.0
                )
            } catch (e: Exception) {
                null
            }
        }

    private fun fetchJsonArray(url: String): JSONArray? {
        val request = Request.Builder().url(url).build()
        client.newCall(request).execute().use { response ->
            val body = response.body?.string() ?: return null
            if (!response.isSuccessful) return null
            return try { JSONArray(body) } catch (e: Exception) { null }
        }
    }
}
