package com.jarvis.assistant

import android.app.AlarmManager
import android.app.PendingIntent
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Color
import android.text.InputType
import android.view.Gravity
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import org.json.JSONArray
import org.json.JSONObject
import java.util.Calendar

/**
 * تذكيرات طبية: أدوية يومية + مواعيد أطباء/جلسات علاج.
 * يعتمد على ReminderReceiver الموجود أصلًا (نفس آلية باقي التذكيرات في التطبيق).
 */
class MedicalReminderManager(
    private val activity: AppCompatActivity,
    private val prefs: SharedPreferences,
    private val respond: (String) -> Unit
) {
    data class MedicalReminder(
        val id: Long,
        val label: String,
        val isDaily: Boolean,
        val hour: Int,
        val minute: Int,
        // فقط للمواعيد لمرة وحدة (يوم/شهر/سنة)، تُترك -1 للأدوية اليومية
        val year: Int = -1,
        val month: Int = -1,
        val day: Int = -1
    )

    private fun loadReminders(): MutableList<MedicalReminder> {
        val raw = prefs.getString("medical_reminders_json", "[]") ?: "[]"
        val list = mutableListOf<MedicalReminder>()
        try {
            val arr = JSONArray(raw)
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                list.add(
                    MedicalReminder(
                        o.getLong("id"), o.getString("label"), o.getBoolean("isDaily"),
                        o.getInt("hour"), o.getInt("minute"),
                        o.optInt("year", -1), o.optInt("month", -1), o.optInt("day", -1)
                    )
                )
            }
        } catch (e: Exception) { }
        return list
    }

    private fun saveReminders(list: List<MedicalReminder>) {
        val arr = JSONArray()
        list.forEach {
            val o = JSONObject()
            o.put("id", it.id)
            o.put("label", it.label)
            o.put("isDaily", it.isDaily)
            o.put("hour", it.hour)
            o.put("minute", it.minute)
            o.put("year", it.year)
            o.put("month", it.month)
            o.put("day", it.day)
            arr.put(o)
        }
        prefs.edit().putString("medical_reminders_json", arr.toString()).apply()
    }

    private fun scheduleAlarm(reminder: MedicalReminder) {
        val alarmManager = activity.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(activity, ReminderReceiver::class.java).apply {
            putExtra("message", reminder.label)
        }
        val pending = PendingIntent.getBroadcast(
            activity, reminder.id.toInt(), intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val calendar = Calendar.getInstance().apply {
            if (reminder.isDaily) {
                set(Calendar.HOUR_OF_DAY, reminder.hour)
                set(Calendar.MINUTE, reminder.minute)
                set(Calendar.SECOND, 0)
                if (before(Calendar.getInstance())) add(Calendar.DAY_OF_YEAR, 1)
            } else {
                set(reminder.year, reminder.month - 1, reminder.day, reminder.hour, reminder.minute, 0)
            }
        }
        if (reminder.isDaily) {
            alarmManager.setRepeating(
                AlarmManager.RTC_WAKEUP, calendar.timeInMillis, AlarmManager.INTERVAL_DAY, pending
            )
        } else {
            alarmManager.set(AlarmManager.RTC_WAKEUP, calendar.timeInMillis, pending)
        }
    }

    private fun cancelAlarm(reminder: MedicalReminder) {
        val alarmManager = activity.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(activity, ReminderReceiver::class.java)
        val pending = PendingIntent.getBroadcast(
            activity, reminder.id.toInt(), intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        alarmManager.cancel(pending)
    }

    /** يعيد جدولة كل التذكيرات المحفوظة — يُستدعى عند إقلاع التطبيق لأن المنبهات
     * لا تنجو من إعادة تشغيل الهاتف إلا إذا أعدنا تسجيلها */
    fun rescheduleAll() {
        loadReminders().forEach { scheduleAlarm(it) }
    }

    fun showAddMedicationDialog() {
        val input = EditText(activity).apply { hint = "اسم الدواء أو الجرعة" }
        AlertDialog.Builder(activity)
            .setTitle("إضافة دواء (تذكير يومي)")
            .setView(input)
            .setPositiveButton("التالي: اختر الوقت") { _, _ ->
                val label = input.text.toString().trim().ifBlank { "دواء" }
                pickTime { hour, minute ->
                    val reminder = MedicalReminder(
                        id = System.currentTimeMillis(), label = "وقت دواء: $label",
                        isDaily = true, hour = hour, minute = minute
                    )
                    val list = loadReminders()
                    list.add(reminder)
                    saveReminders(list)
                    scheduleAlarm(reminder)
                    renderList()
                    respond("تم جدولة تذكير الدواء يوميًا")
                }
            }
            .setNegativeButton("إلغاء", null)
            .show()
    }

    fun showAddAppointmentDialog() {
        val input = EditText(activity).apply { hint = "اسم الموعد (مثلاً: جلسة علاج، دكتور فلان)" }
        AlertDialog.Builder(activity)
            .setTitle("إضافة موعد")
            .setView(input)
            .setPositiveButton("التالي: اختر التاريخ والوقت") { _, _ ->
                val label = input.text.toString().trim().ifBlank { "موعد" }
                pickDateThenTime { year, month, day, hour, minute ->
                    val reminder = MedicalReminder(
                        id = System.currentTimeMillis(), label = "موعد: $label",
                        isDaily = false, hour = hour, minute = minute,
                        year = year, month = month, day = day
                    )
                    val list = loadReminders()
                    list.add(reminder)
                    saveReminders(list)
                    scheduleAlarm(reminder)
                    renderList()
                    respond("تم جدولة تذكير الموعد")
                }
            }
            .setNegativeButton("إلغاء", null)
            .show()
    }

    private fun pickTime(onPicked: (hour: Int, minute: Int) -> Unit) {
        val now = Calendar.getInstance()
        TimePickerDialog(
            activity,
            { _, hour, minute -> onPicked(hour, minute) },
            now.get(Calendar.HOUR_OF_DAY), now.get(Calendar.MINUTE), true
        ).show()
    }

    private fun pickDateThenTime(onPicked: (year: Int, month: Int, day: Int, hour: Int, minute: Int) -> Unit) {
        val now = Calendar.getInstance()
        android.app.DatePickerDialog(
            activity,
            { _, year, month, day ->
                pickTime { hour, minute -> onPicked(year, month + 1, day, hour, minute) }
            },
            now.get(Calendar.YEAR), now.get(Calendar.MONTH), now.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    fun renderList() {
        val container = activity.findViewById<LinearLayout>(R.id.medicalRemindersContainer) ?: return
        container.removeAllViews()
        val reminders = loadReminders().sortedWith(compareBy({ !it.isDaily }, { it.hour }))
        val density = activity.resources.displayMetrics.density

        reminders.forEach { reminder ->
            val row = LinearLayout(activity).apply {
                orientation = LinearLayout.HORIZONTAL
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { topMargin = (8 * density).toInt() }
                setPadding((14 * density).toInt(), (12 * density).toInt(), (14 * density).toInt(), (12 * density).toInt())
                setBackgroundResource(R.drawable.hud_glow_card)
                gravity = Gravity.CENTER_VERTICAL
            }
            val timeLabel = if (reminder.isDaily) {
                "%02d:%02d يوميًا".format(reminder.hour, reminder.minute)
            } else {
                "%02d/%02d/%d - %02d:%02d".format(reminder.day, reminder.month, reminder.year, reminder.hour, reminder.minute)
            }
            val label = TextView(activity).apply {
                text = "${reminder.label}\n$timeLabel"
                setTextColor(Color.parseColor("#D7FBFF"))
                textSize = 11f
                typeface = android.graphics.Typeface.MONOSPACE
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }
            val deleteBtn = TextView(activity).apply {
                text = "X"
                setTextColor(Color.parseColor("#FF5050"))
                textSize = 13f
                setPadding((10 * density).toInt(), 0, 0, 0)
                setOnClickListener {
                    cancelAlarm(reminder)
                    val updated = loadReminders().filter { it.id != reminder.id }
                    saveReminders(updated)
                    renderList()
                }
            }
            row.addView(label)
            row.addView(deleteBtn)
            container.addView(row)
        }
    }
}
