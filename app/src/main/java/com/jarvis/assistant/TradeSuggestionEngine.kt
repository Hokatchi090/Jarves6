package com.jarvis.assistant

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * محرك اقتراحات — **لا ينفذ أي صفقة أبداً**. وظيفته الوحيدة:
 *  1. تطبيق HalalScreener على قائمة أسهم مراقبة.
 *  2. تطبيق إشارة توقيت بسيطة (متوسطات متحركة) كمبرر لتوقيت الاقتراح.
 *  3. تسجيل كل ذلك في TradeSuggestionDao مع الأسباب، بحالة PENDING.
 *
 * تنفيذ الصفقة الفعلي يبقى دائماً يدوياً من طرف المستخدم داخل تطبيق
 * الوسيط الرسمي (انظر SuggestionApprovalActivity) — لا يوجد هنا أي
 * اتصال بمفاتيح API لحساب تداول حقيقي.
 */
interface PriceHistoryProvider {
    /** يرجع أسعار الإغلاق الأخيرة (الأقدم أولاً). */
    suspend fun getRecentClosingPrices(ticker: String, days: Int): List<Double>?
}

class TradeSuggestionEngine(
    private val halalScreener: HalalScreener,
    private val priceProvider: PriceHistoryProvider,
    private val dao: TradeSuggestionDao
) {

    suspend fun evaluateWatchlist(tickers: List<String>) {
        for (ticker in tickers) {
            evaluateTicker(ticker)
        }
    }

    suspend fun evaluateTicker(ticker: String) = withContext(Dispatchers.Default) {
        val screening = halalScreener.screen(ticker)

        val (action, timingReason) = if (screening.isHalal) {
            computeTimingSignal(ticker)
        } else {
            "انتظار" to "لم يتم فحص التوقيت لأن السهم غير مطابق شرعياً."
        }

        dao.insert(
            TradeSuggestionEntity(
                ticker = ticker,
                timestampMillis = System.currentTimeMillis(),
                isHalal = screening.isHalal,
                halalReason = screening.reason,
                timingReason = timingReason,
                suggestedAction = action,
                approvalStatus = "PENDING"
            )
        )
    }

    /** إشارة بسيطة: تقاطع متوسط متحرك قصير (10 أيام) مع متوسط متحرك طويل (30 يوماً). */
    private suspend fun computeTimingSignal(ticker: String): Pair<String, String> {
        val prices = priceProvider.getRecentClosingPrices(ticker, 30)
        if (prices == null || prices.size < 30) {
            return "انتظار" to "بيانات الأسعار التاريخية غير كافية لحساب إشارة موثوقة."
        }

        val shortMa = prices.takeLast(10).average()
        val longMa = prices.average()

        return if (shortMa > longMa) {
            "شراء" to "المتوسط المتحرك لـ10 أيام (${"%.2f".format(shortMa)}) أعلى من متوسط 30 يوماً " +
                "(${"%.2f".format(longMa)})، وهو ما يُقرأ تقليدياً كزخم صاعد قصير المدى."
        } else {
            "انتظار" to "المتوسط المتحرك لـ10 أيام (${"%.2f".format(shortMa)}) لا يزال دون متوسط 30 يوماً " +
                "(${"%.2f".format(longMa)})، لا توجد إشارة زخم صاعد حالياً."
        }
    }
}
