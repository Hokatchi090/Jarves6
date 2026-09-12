package com.jarvis.assistant

import android.content.Context
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.json.JSONObject
import java.io.IOException
import java.util.Locale

/**
 * وحدة الطقس المفصّل: حرارة الآن + توقعات 5 أيام + تحذير قبل المطر/العاصفة
 * بساعة تقريبًا + اقتراح "خُذ مظلة" صباحًا. تعتمد على Open-Meteo (مجاني، بلا مفتاح API).
 */
class JarvisWeatherModule(
    private val context: Context,
    private val getLocation: () -> Pair<Double, Double>
) {
    private val client = OkHttpClient()

    companion object {
        // أكواد الطقس (WMO) البسيطة اللي تعتبر "عاصفة/مطر قوي"
        val STORM_CODES = setOf(95, 96, 99)
        val RAIN_CODES = setOf(51, 53, 55, 61, 63, 65, 80, 81, 82)
    }

    private fun weatherLabel(code: Int): String = when (code) {
        0 -> "\u0635\u0627\u0641\u064A\u0629"
        1, 2, 3 -> "\u063A\u0627\u0626\u0645\u0629 \u062C\u0632\u0626\u064A\u064B\u0627"
        45, 48 -> "\u0636\u0628\u0627\u0628"
        in RAIN_CODES -> "\u0645\u0637\u0631"
        in STORM_CODES -> "\u0639\u0627\u0635\u0641\u0629 \u0631\u0639\u062F\u064A\u0629"
        71, 73, 75, 77, 85, 86 -> "\u062B\u0644\u062C"
        else -> "\u063A\u064A\u0631 \u0645\u062D\u062F\u062F"
    }

    /** حرارة الآن + توقعات 5 أيام (أعلى/أدنى حرارة + حالة الطقس) كنص جاهز للعرض/النطق */
    fun fetchFiveDayForecast(callback: (String) -> Unit) {
        val (lat, lon) = getLocation()
        if (lat == 0.0 && lon == 0.0) {
            callback("\u0645\u0627\u0639\u0646\u062F\u064A \u0645\u0648\u0642\u0639 \u062D\u0627\u0644\u064A \u0628\u0627\u0634 \u0646\u062C\u064A\u0628\u0644\u0643 \u0627\u0644\u0637\u0642\u0633")
            return
        }
        val url = "https://api.open-meteo.com/v1/forecast?latitude=$lat&longitude=$lon" +
            "&current_weather=true" +
            "&daily=temperature_2m_max,temperature_2m_min,precipitation_probability_max,weathercode" +
            "&timezone=auto&forecast_days=5"
        val request = Request.Builder().url(url).get().build()
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                callback("\u0645\u0627 \u0642\u062F\u0631\u062A \u0646\u062C\u064A\u0628 \u0627\u0644\u0637\u0642\u0633")
            }
            override fun onResponse(call: Call, response: Response) {
                try {
                    val json = JSONObject(response.body?.string() ?: "")
                    val current = json.getJSONObject("current_weather")
                    val nowTemp = current.getDouble("temperature").toInt()

                    val daily = json.getJSONObject("daily")
                    val dates = daily.getJSONArray("time")
                    val maxT = daily.getJSONArray("temperature_2m_max")
                    val minT = daily.getJSONArray("temperature_2m_min")
                    val rainP = daily.getJSONArray("precipitation_probability_max")
                    val codes = daily.getJSONArray("weathercode")

                    val dayNames = listOf(
                        "\u0627\u0644\u064A\u0648\u0645", "\u063A\u062F\u064B\u0627",
                        "\u0628\u0639\u062F \u063A\u062F", "\u0628\u0639\u062F 3 \u0623\u064A\u0627\u0645", "\u0628\u0639\u062F 4 \u0623\u064A\u0627\u0645"
                    )

                    val sb = StringBuilder()
                    sb.append("\u0627\u0644\u0622\u0646: $nowTemp\u00B0\n")
                    for (i in 0 until minOf(5, dates.length())) {
                        val label = dayNames.getOrElse(i) { dates.getString(i) }
                        val hi = maxT.getDouble(i).toInt()
                        val lo = minT.getDouble(i).toInt()
                        val rp = rainP.optInt(i, 0)
                        val code = codes.optInt(i, -1)
                        val cond = weatherLabel(code)
                        sb.append("$label: $lo\u00B0/$hi\u00B0 \u2014 $cond")
                        if (rp >= 30) sb.append(" (\u0627\u062D\u062A\u0645\u0627\u0644 \u0645\u0637\u0631 $rp\u066A)")
                        sb.append("\n")
                    }
                    callback(sb.toString().trim())
                } catch (e: Exception) {
                    callback("\u0645\u0627 \u0642\u062F\u0631\u062A \u0646\u062C\u064A\u0628 \u0627\u0644\u0637\u0642\u0633")
                }
            }
        })
    }

    /**
     * يفحص توقعات الساعة الجاية: لو احتمال مطر/عاصفة عالي خلال أول ساعة،
     * يرجّع نص تنبيه، وإلا null. يُستعمل من WeatherAlertReceiver كل فترة.
     */
    fun checkUpcomingRainAlert(callback: (String?) -> Unit) {
        val (lat, lon) = getLocation()
        if (lat == 0.0 && lon == 0.0) {
            callback(null)
            return
        }
        val url = "https://api.open-meteo.com/v1/forecast?latitude=$lat&longitude=$lon" +
            "&hourly=precipitation_probability,weathercode&timezone=auto&forecast_days=1"
        val request = Request.Builder().url(url).get().build()
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) = callback(null)
            override fun onResponse(call: Call, response: Response) {
                try {
                    val json = JSONObject(response.body?.string() ?: "")
                    val hourly = json.getJSONObject("hourly")
                    val times = hourly.getJSONArray("time")
                    val probs = hourly.getJSONArray("precipitation_probability")
                    val codes = hourly.getJSONArray("weathercode")

                    val nowMillis = System.currentTimeMillis()
                    val fmt = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm", Locale.US)
                    fmt.timeZone = java.util.TimeZone.getDefault()

                    for (i in 0 until times.length()) {
                        val t = fmt.parse(times.getString(i))?.time ?: continue
                        val diffMin = (t - nowMillis) / 60000.0
                        // نراقب الساعة الجاية فقط (0-70 دقيقة قدّام)
                        if (diffMin in 0.0..70.0) {
                            val prob = probs.optInt(i, 0)
                            val code = codes.optInt(i, -1)
                            if (code in STORM_CODES) {
                                callback("\u26A0\uFE0F \u0645\u062A\u0648\u0642\u0639 \u0639\u0627\u0635\u0641\u0629 \u0631\u0639\u062F\u064A\u0629 \u062E\u0644\u0627\u0644 \u0633\u0627\u0639\u0629 \u062A\u0642\u0631\u064A\u0628\u064B\u0627 \u2014 \u062F\u064A\u0631 \u0628\u0627\u0644\u0643")
                                return
                            }
                            if (prob >= 50 && (code in RAIN_CODES || prob >= 60)) {
                                callback("\u2614 \u0627\u062D\u062A\u0645\u0627\u0644 \u0645\u0637\u0631 $prob\u066A \u062E\u0644\u0627\u0644 \u0633\u0627\u0639\u0629 \u062A\u0642\u0631\u064A\u0628\u064B\u0627 \u2014 \u062E\u0648\u062F \u0645\u0638\u0644\u0629 \u0645\u0639\u0627\u0643")
                                return
                            }
                        }
                    }
                    callback(null)
                } catch (e: Exception) {
                    callback(null)
                }
            }
        })
    }
}
