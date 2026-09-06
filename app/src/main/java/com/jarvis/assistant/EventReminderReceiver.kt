package com.jarvis.assistant

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import org.json.JSONArray
import java.util.Calendar

/**
 * يشتغل يوميًا (مجدول عبر AlarmManager من MainActivity)، يفحص قائمة المناسبات
 * المحفوظة (أعياد ميلاد، مناسبات) ويطلق إشعارًا لكل مناسبة تصادف تاريخ اليوم.
 */
class EventReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val prefs = context.getSharedPreferences("jarvis_prefs", Context.MODE_PRIVATE)
        val raw = prefs.getString("yearly_events_json", "[]") ?: "[]"

        val today = Calendar.getInstance()
        val todayDay = today.get(Calendar.DAY_OF_MONTH)
        val todayMonth = today.get(Calendar.MONTH) + 1

        val matches = mutableListOf<String>()
        try {
            val arr = JSONArray(raw)
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                if (obj.getInt("day") == todayDay && obj.getInt("month") == todayMonth) {
                    matches.add(obj.getString("name"))
                }
            }
        } catch (e: Exception) {
            return
        }

        if (matches.isEmpty()) return

        val channelId = "jarvis_events"
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId, "مناسبات جارفس", NotificationManager.IMPORTANCE_HIGH
            )
            manager.createNotificationChannel(channel)
        }

        matches.forEach { name ->
            val notification = NotificationCompat.Builder(context, channelId)
                .setContentTitle("جارفس يذكّرك")
                .setContentText("اليوم مناسبة: $name 🎉")
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setAutoCancel(true)
                .build()
            manager.notify((name + todayDay + todayMonth).hashCode(), notification)
        }
    }
}
