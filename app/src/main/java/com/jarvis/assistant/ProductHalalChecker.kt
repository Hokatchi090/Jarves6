package com.jarvis.assistant

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.IOException

/**
 * يتحقق من حالة "حلال / حرام / مشبوه" لمنتج عبر باركوده،
 * باستخدام قاعدة بيانات Open Food Facts المجانية (لا تحتاج مفتاح API)
 * بالإضافة إلى قائمة محلية للمكونات المحرّمة الشائعة.
 *
 * ملاحظة شرعية: هذا فحص أولي آلي بالاعتماد على قائمة المكونات المعلنة فقط،
 * وليس بديلاً عن شهادة حلال معتمدة. النتيجة "مشبوه" تعني أن المكوّن يحتاج
 * تحققاً بشرياً إضافياً (مثل مصدر الجيلاتين أو الإنزيمات).
 */
class ProductHalalChecker(
    private val client: OkHttpClient = OkHttpClient()
) {

    enum class Status { HALAL, HARAM, MASHBOOH, UNKNOWN }

    data class Result(
        val status: Status,
        val productName: String?,
        val reason: String,
        val flaggedIngredients: List<String> = emptyList()
    )

    // مكونات مؤكد حرمتها
    private val haramKeywords = listOf(
        "pork", "lard", "bacon", "ham", "gelatin (pork)", "khinzir",
        "alcohol", "ethanol", "wine", "beer", "rum", "e120" // e120 = cochineal/carmine (خلاف فقهي، لكن نضعه كتحذير)
    )

    // مكونات تحتاج تحققاً إضافياً (مصدرها غير محدد: نباتي أم حيواني؟)
    private val mashboohKeywords = listOf(
        "gelatin", "rennet", "enzyme", "emulsifier", "mono- and diglycerides",
        "e471", "e472", "lipase", "pepsin", "whey" // whey غالباً حلال لكن يعتمد على المنفحة المستخدمة
    )

    suspend fun checkByBarcode(barcode: String): Result = withContext(Dispatchers.IO) {
        try {
            val url = "https://world.openfoodfacts.org/api/v2/product/$barcode.json"
            val request = Request.Builder().url(url).build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext Result(
                        status = Status.UNKNOWN,
                        productName = null,
                        reason = "تعذر الوصول لقاعدة بيانات المنتجات (رمز الحالة: ${response.code})"
                    )
                }
                val body = response.body?.string() ?: return@withContext unknownNoData()
                val json = JSONObject(body)
                if (json.optInt("status") != 1) {
                    return@withContext unknownNoData()
                }
                val product = json.getJSONObject("product")
                val name = product.optString("product_name", null)
                val ingredientsText = product.optString("ingredients_text", "").lowercase()

                if (ingredientsText.isBlank()) {
                    return@withContext Result(
                        status = Status.UNKNOWN,
                        productName = name,
                        reason = "لا توجد قائمة مكونات معلنة لهذا المنتج في قاعدة البيانات."
                    )
                }

                analyzeIngredients(ingredientsText, name)
            }
        } catch (e: IOException) {
            Result(
                status = Status.UNKNOWN,
                productName = null,
                reason = "خطأ في الاتصال بالشبكة: ${e.message ?: "غير معروف"}"
            )
        }
    }

    private fun unknownNoData() = Result(
        status = Status.UNKNOWN,
        productName = null,
        reason = "المنتج غير موجود في قاعدة بيانات Open Food Facts."
    )

    private fun analyzeIngredients(ingredientsText: String, productName: String?): Result {
        val foundHaram = haramKeywords.filter { ingredientsText.contains(it) }
        if (foundHaram.isNotEmpty()) {
            return Result(
                status = Status.HARAM,
                productName = productName,
                reason = "يحتوي على مكوّن محرّم صراحة.",
                flaggedIngredients = foundHaram
            )
        }

        val foundMashbooh = mashboohKeywords.filter { ingredientsText.contains(it) }
        if (foundMashbooh.isNotEmpty()) {
            return Result(
                status = Status.MASHBOOH,
                productName = productName,
                reason = "يحتوي على مكوّن مصدره غير محدد (قد يكون حيوانياً أو نباتياً) — يحتاج تحققاً إضافياً أو شهادة حلال.",
                flaggedIngredients = foundMashbooh
            )
        }

        return Result(
            status = Status.HALAL,
            productName = productName,
            reason = "لم يُعثر على أي مكوّن محرّم أو مشبوه في القائمة المعلنة."
        )
    }
}
