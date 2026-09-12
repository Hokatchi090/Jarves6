package com.jarvis.assistant

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.Date

/**
 * وحدة اللياقة: خطوات + سعرات محروقة + مسافة مقطوعة، باستخدام حسّاس عداد
 * الخطوات (TYPE_STEP_COUNTER) اللي يرجّع مجموع تراكمي منذ إقلاع الجهاز.
 * نحفظ "خط أساس" (baseline) لكل يوم في SharedPreferences باش نحسب خطوات
 * اليوم الحالي فقط = القيمة الحالية - خط أساس اليوم.
 */
class JarvisFitnessModule(
    private val context: Context,
    private val speak: (String) -> Unit
) : SensorEventListener {

    companion object {
        private const val PREFS = "jarvis_fitness"
        private const val KEY_BASELINE = "baseline_steps"
        private const val KEY_BASELINE_DATE = "baseline_date"
        private const val KEY_LAST_RAW = "last_raw_steps"

        // ثوابت تقريبية شائعة الاستخدام لتقدير السعرات والمسافة من عدد الخطوات
        private const val KCAL_PER_STEP = 0.045       // تقريبًا لشخص متوسط الوزن
        private const val STRIDE_METERS = 0.762       // متوسط طول الخطوة بالمتر
    }

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val stepSensor: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)

    private fun prefs() = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    private fun todayKey() = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

    /** يبدأ الاستماع لحسّاس الخطوات — ينادى من onResume في MainActivity */
    fun start() {
        if (stepSensor == null) return
        sensorManager.registerListener(this, stepSensor, SensorManager.SENSOR_DELAY_NORMAL)
    }

    /** يوقف الاستماع — ينادى من onPause */
    fun stop() {
        sensorManager.unregisterListener(this)
    }

    fun isAvailable(): Boolean = stepSensor != null

    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type != Sensor.TYPE_STEP_COUNTER) return
        val raw = event.values[0].toInt()
        val p = prefs()
        val savedDate = p.getString(KEY_BASELINE_DATE, "")
        if (savedDate != todayKey()) {
            // يوم جديد: نصفّر العدّاد بأخذ القيمة الحالية كخط أساس جديد
            p.edit()
                .putInt(KEY_BASELINE, raw)
                .putString(KEY_BASELINE_DATE, todayKey())
                .apply()
        }
        p.edit().putInt(KEY_LAST_RAW, raw).apply()
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    /** خطوات اليوم الحالي فقط */
    fun stepsToday(): Int {
        val p = prefs()
        if (p.getString(KEY_BASELINE_DATE, "") != todayKey()) return 0
        val baseline = p.getInt(KEY_BASELINE, 0)
        val raw = p.getInt(KEY_LAST_RAW, baseline)
        return (raw - baseline).coerceAtLeast(0)
    }

    fun caloriesToday(): Int = (stepsToday() * KCAL_PER_STEP).toInt()

    fun distanceKmToday(): Double {
        val meters = stepsToday() * STRIDE_METERS
        return meters / 1000.0
    }

    /** ملخص جاهز للنطق أو العرض */
    fun summaryText(): String {
        if (!isAvailable()) {
            return "\u062C\u0647\u0627\u0632\u0643 \u0645\u0627 \u0641\u064A\u0647\u0634 \u062D\u0633\u0651\u0627\u0633 \u0639\u062F\u0651\u0627\u062F \u062E\u0637\u0648\u0627\u062A"
        }
        val steps = stepsToday()
        val kcal = caloriesToday()
        val km = String.format(Locale.US, "%.2f", distanceKmToday())
        return "\u062E\u0637\u0648\u0627\u062A \u0627\u0644\u064A\u0648\u0645: $steps \u2014 \u0633\u0639\u0631\u0627\u062A: $kcal kcal \u2014 \u0645\u0633\u0627\u0641\u0629: $km \u0643\u0645"
    }

    fun speakSummary() {
        speak(summaryText())
    }
}
