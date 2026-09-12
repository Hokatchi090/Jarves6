package com.jarvis.assistant

import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.IOException
import java.util.concurrent.atomic.AtomicInteger

/**
 * وحدة الأخبار المصنّفة للملخص الصباحي: تكنولوجيا، ترند اليوم/الأسبوع، كوارث،
 * وأهم الأخبار السياسية (عالمية ووطنية) — كل قسم من مصدر RSS معروف وموثوق،
 * ونذكر اسم المصدر مع كل خبر بدل ما نلفّق أي معلومة.
 *
 * ملاحظة صادقة: "فضائح حصرية" ما نقدرش نضمنها كقسم مستقل موثوق — ما كاين
 * حتى مصدر RSS عام يرجّع "فضائح حصرية" بالتحديد، فهذا القسم مدموج ضمن
 * الأخبار السياسية والعامة (أي فضيحة كبيرة بتظهر فيها بشكل طبيعي لو كانت
 * فعلاً خبر مهم عند المصادر).
 */
class JarvisNewsModule(private val client: OkHttpClient) {

    private data class Feed(val label: String, val url: String, val maxItems: Int)

    private val feeds = listOf(
        Feed("\u062A\u0643\u0646\u0648\u0644\u0648\u062C\u064A\u0627", "https://techcrunch.com/feed/", 2),
        Feed("\u062A\u0631\u0646\u062F \u0627\u0644\u064A\u0648\u0645", "https://news.google.com/rss?hl=ar&gl=EG&ceid=EG:ar", 3),
        Feed("\u0643\u0648\u0627\u0631\u062B", "https://www.gdacs.org/xml/rss.xml", 2),
        Feed("\u0633\u064A\u0627\u0633\u0629 \u0639\u0627\u0644\u0645\u064A\u0629", "http://feeds.bbci.co.uk/arabic/rss.xml", 2),
        Feed("\u0633\u064A\u0627\u0633\u0629/\u0623\u062E\u0628\u0627\u0631 \u0639\u0627\u0645\u0629", "https://www.aljazeera.net/aljazeerarss/xml", 2)
    )

    /** يجيب أهم العناوين من كل مصدر، ويرجّع نص مقسّم لأقسام جاهز للعرض/النطق */
    fun fetchCategorizedNews(callback: (String) -> Unit) {
        val results = arrayOfNulls<String>(feeds.size)
        val remaining = AtomicInteger(feeds.size)

        fun finishIfDone() {
            if (remaining.decrementAndGet() == 0) {
                val sb = StringBuilder()
                feeds.forEachIndexed { i, feed ->
                    val text = results[i]
                    if (!text.isNullOrBlank()) {
                        sb.append("${feed.label}:\n$text\n\n")
                    }
                }
                callback(sb.toString().trim().ifBlank { "\u0645\u0627\u0641\u064A\u0634 \u0623\u062E\u0628\u0627\u0631 \u0645\u062A\u0648\u0641\u0631\u0629 \u062D\u0627\u0644\u064A\u064B\u0627" })
            }
        }

        feeds.forEachIndexed { i, feed ->
            val request = Request.Builder().url(feed.url).get().build()
            client.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    finishIfDone()
                }
                override fun onResponse(call: Call, response: Response) {
                    try {
                        val body = response.body?.string() ?: ""
                        results[i] = extractTitles(body, feed.maxItems).joinToString("\n") { "\u2022 $it" }
                    } catch (e: Exception) {
                        // \u062A\u062C\u0627\u0647\u0644 \u0647\u0630\u0627 \u0627\u0644\u0645\u0635\u062F\u0631 \u0628\u0647\u062F\u0648\u0621 \u0628\u062F\u0644 \u0645\u0627 \u0646\u062E\u062A\u0631\u0639 \u0643\u0644 \u0627\u0644\u0645\u0644\u062E\u0635
                    }
                    finishIfDone()
                }
            })
        }
    }

    /** استخراج بسيط لعناوين <title> من RSS/Atom بدون مكتبة XML خارجية (نفس أسلوب الكود الموجود أصلاً) */
    private fun extractTitles(xml: String, max: Int): List<String> {
        val titles = mutableListOf<String>()
        var rest = xml
        // أول <title> عادة يكون اسم القناة نفسها، نتخطاه
        var skippedChannelTitle = false
        while (titles.size < max) {
            val itemStart = rest.indexOf("<item>").let { if (it == -1) rest.indexOf("<entry>") else it }
            if (itemStart == -1) break
            rest = rest.substring(itemStart)
            val itemEnd = rest.indexOf("</item>").let { if (it == -1) rest.indexOf("</entry>") else it }
            val itemBlock = if (itemEnd != -1) rest.substring(0, itemEnd) else rest
            val title = itemBlock.substringAfter("<title>", "").substringBefore("</title>")
                .replace("<![CDATA[", "").replace("]]>", "").trim()
            if (title.isNotBlank()) titles.add(title)
            rest = if (itemEnd != -1) rest.substring(itemEnd + 7) else ""
            if (rest.isBlank()) break
        }
        if (!skippedChannelTitle) skippedChannelTitle = true // no-op، محفوظ للوضوح فقط
        return titles
    }
}
