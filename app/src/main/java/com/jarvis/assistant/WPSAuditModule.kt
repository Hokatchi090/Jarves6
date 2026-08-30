package com.jarvis.assistant

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.wifi.ScanResult
import android.net.wifi.WifiManager
import android.os.Handler
import android.os.Looper
import android.widget.Button
import android.widget.TextView
import androidx.core.app.ActivityCompat
import java.text.SimpleDateFormat
import java.util.*

class WPSAuditModule(
    private val context: Context,
    private val tvStatus: TextView,      // النص العلوي
    private val tvLog: TextView,         // النص السفلي للتسجيل
    private val btnExploit: Button       // زر الاختراق
) {
    private val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
    private val logHandler = Handler(Looper.getMainLooper())
    private var currentTarget: ScanResult? = null

    // 1. دالة المسح (ضعها على زر المسح لديك)
    fun performNetworkScan(): List<ScanResult> {
        if (!hasPermissions()) {
            tvStatus.text = "❌ أذونات الموقع مطلوبة!"
            return emptyList()
        }
        wifiManager.startScan()
        val results = wifiManager.scanResults
        val sorted = results.distinctBy { it.BSSID }.sortedByDescending { it.level }

        // عرض الشبكات في TextView العلوي (بصيغة مختصرة)
        val builder = StringBuilder("📡 الشبكات المكتشفة (Jarvis):\n")
        sorted.take(10).forEach { ap ->
            builder.append("📶 ${ap.SSID} (${ap.level}dBm) [${if (ap.capabilities.contains("WPS")) "WPS✅" else "---"}]\n")
        }
        tvStatus.text = builder.toString()

        // احفظ أول شبكة كهدف تلقائي للتجربة
        currentTarget = sorted.firstOrNull()
        btnExploit.isEnabled = currentTarget != null
        return sorted
    }

    // 2. دالة المحاكاة (ضعها على زر الاختراق)
    fun executeSimulatedExploit() {
        val target = currentTarget
        if (target == null) {
            tvStatus.text = "⚠️ امسح الشبكات أولاً"
            return
        }

        tvStatus.text = "⚡ بدء محاكاة WPS ضد: ${target.SSID}"
        val logBuilder = StringBuilder()
        val dateFormat = SimpleDateFormat("HH:mm:ss.SSS", Locale.US)

        logBuilder.append("[${dateFormat.format(Date())}] استهداف: ${target.BSSID}\n")
        logBuilder.append("[${dateFormat.format(Date())}] حساب PIN نظري...\n")
        
        // توليد PIN وهمي بناءً على الماك (ليبدو واقعياً)
        val pseudoPin = (target.BSSID.filter { it.isDigit() }.sumOf { it.toString().toInt() } % 10000).toString().padStart(4, '0')
        logBuilder.append("[${dateFormat.format(Date())}] PIN المستنتج: $pseudoPin\n")

        var counter = 0
        val runnable = object : Runnable {
            override fun run() {
                counter++
                when (counter) {
                    1 -> logBuilder.append("[${dateFormat.format(Date())}] إرسال M1...\n")
                    3 -> logBuilder.append("[${dateFormat.format(Date())}] استقبال M2...\n")
                    6 -> logBuilder.append("[${dateFormat.format(Date())}] محاولة PIN: 12345670 (رفض)\n")
                    9 -> logBuilder.append("[${dateFormat.format(Date())}] محاولة PIN: $pseudoPin (قبول!)\n")
                    12 -> {
                        logBuilder.append("[${dateFormat.format(Date())}] ✅ نجاح وهمي (Sandbox)\n")
                        logBuilder.append("[${dateFormat.format(Date())}] المفتاح المسترجع: Jarvis_${System.currentTimeMillis() % 9999}\n")
                        tvStatus.text = "✅ اكتملت المحاكاة بنجاح (بحث أكاديمي)"
                    }
                }
                tvLog.text = logBuilder.toString()
                if (counter < 15) {
                    logHandler.postDelayed(this, 400)
                } else {
                    logBuilder.append("--- نهاية المحاكاة ---\n")
                    tvLog.text = logBuilder.toString()
                }
            }
        }
        logHandler.post(runnable)
    }

    // ─── مقلب ١: قطع Wi-Fi هاتفك مؤقتاً ثم إعادة الاتصال ──────────
    // يؤثر على هاتفك أنت فقط — الهدف: مقلب بصري ممتع
    fun executeDeauthJammer() {
        val info = wifiManager.connectionInfo
        if (info.networkId == -1) {
            tvStatus.text = "⚠️ لست متصلاً بأي شبكة!"
            return
        }
        val ssid  = info.ssid
        val netId = info.networkId
        tvStatus.text = "🌊 قطع Wi-Fi مؤقت لـ $ssid ... (5 ثوانٍ)"
        logHandler.postDelayed({
            wifiManager.enableNetwork(netId, true)
            wifiManager.reconnect()
            tvStatus.text = "🔁 إعادة الاتصال بـ $ssid"
        }, 5000)
        wifiManager.disableNetwork(netId)
    }

    // ─── مقلب ٢: إطفاء راديو Wi-Fi وإعادة تشغيله ───────────────
    // يؤثر على هاتفك أنت فقط
    fun executeRadioToggle() {
        tvStatus.text = "📻 إطفاء راديو الواي فاي..."
        wifiManager.isWifiEnabled = false
        logHandler.postDelayed({
            wifiManager.isWifiEnabled = true
            tvStatus.text = "📡 راديو Wi-Fi عاد للعمل."
        }, 2500)
    }

    private fun hasPermissions(): Boolean {
        return ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
    }
}
