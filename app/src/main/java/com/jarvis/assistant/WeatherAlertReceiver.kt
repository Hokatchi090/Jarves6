package com.jarvis.assistant

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat

/**
 * يشتغل بشكل دوري (مجدول عبر AlarmManager من MainActivity) ويفحص توقعات
 * الساعة الجاية. لو فيه احتمال مطر/عاصفة قوي، يطلق إشعار تحذير قبل نزول
 * المطر بساعة تقريبًا.
 */
class WeatherAlertReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val prefs = context.getSharedPreferences("jarvis_prefs", Context.MODE_PRIVATE)
        val lat = prefs.getFloat("last_lat", 0f).toDouble()
        val lon = prefs.getFloat("last_lon", 0f).toDouble()
        if (lat == 0.0 && lon == 0.0) return

        val weatherModule = JarvisWeatherModule(context) { lat to lon }
        weatherModule.checkUpcomingRainAlert { alertText ->
            if (alertText != null) {
                postAlertNotification(context, alertText)
            }
        }
    }

    private fun postAlertNotification(context: Context, text: String) {
        val channelId = "jarvis_weather_alert"
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId, "تنبيهات الطقس", NotificationManager.IMPORTANCE_HIGH
            )
            manager.createNotificationChannel(channel)
        }

        val openIntent = Intent(context, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        val pending = PendingIntent.getActivity(
            context, 0, openIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(context, channelId)
            .setContentTitle("تنبيه طقس \uD83C\uDF27\uFE0F")
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentIntent(pending)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        manager.notify(9003, notification)
    }
}
