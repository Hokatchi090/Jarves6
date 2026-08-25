package com.jarvis.assistant

import android.content.Context
import android.content.pm.PackageManager
import android.os.BatteryManager

// \u064A\u0641\u062D\u0635 \u0627\u0644\u062A\u0637\u0628\u064A\u0642\u0627\u062A \u0627\u0644\u0645\u062B\u0628\u062A\u0629 \u0641\u0639\u0644\u064A\u0627\u064B \u0648\u064A\u062D\u062F\u062F \u0623\u064A \u0648\u0627\u062D\u062F \u0645\u0646\u0647\u0627 \u0639\u0646\u062F\u0647 \u0635\u0644\u0627\u062D\u064A\u0627\u062A \u062D\u0633\u0627\u0633\u0629 \u0645\u0645\u0646\u0648\u062D\u0629
// \u0644\u0627 \u064A\u062F\u0639\u064A \u0643\u0634\u0641 \"\u0627\u062E\u062A\u0631\u0627\u0642\" \u2014 \u0641\u0642\u0637 \u064A\u0639\u0631\u0636 \u0645\u0639\u0644\u0648\u0645\u0627\u062A \u0635\u0644\u0627\u062D\u064A\u0627\u062A \u062D\u0642\u064A\u0642\u064A\u0629 \u0645\u0646 \u0646\u0638\u0627\u0645 \u0623\u0646\u062F\u0631\u0648\u064A\u062F \u0646\u0641\u0633\u0647
class SecurityScanner(private val context: Context) {

    data class RiskyApp(val appName: String, val packageName: String, val riskyPermissions: List<String>)

    // \u0635\u0644\u0627\u062D\u064A\u0627\u062A \u0623\u0646\u062F\u0631\u0648\u064A\u062F \u0627\u0644\u062D\u0633\u0627\u0633\u0629 (dangerous protection level) \u0627\u0644\u0623\u0643\u062B\u0631 \u0623\u0647\u0645\u064A\u0629 \u0644\u0644\u062E\u0635\u0648\u0635\u064A\u0629
    private val sensitivePermissions = mapOf(
        android.Manifest.permission.READ_SMS to "\u0642\u0631\u0627\u0621\u0629 \u0627\u0644\u0631\u0633\u0627\u0626\u0644",
        android.Manifest.permission.SEND_SMS to "\u0625\u0631\u0633\u0627\u0644 \u0631\u0633\u0627\u0626\u0644",
        android.Manifest.permission.READ_CONTACTS to "\u062C\u0647\u0627\u062A \u0627\u0644\u0627\u062A\u0635\u0627\u0644",
        android.Manifest.permission.CAMERA to "\u0627\u0644\u0643\u0627\u0645\u064A\u0631\u0627",
        android.Manifest.permission.RECORD_AUDIO to "\u0627\u0644\u0645\u0627\u064A\u0643",
        android.Manifest.permission.ACCESS_FINE_LOCATION to "\u0627\u0644\u0645\u0648\u0642\u0639 \u0627\u0644\u062F\u0642\u064A\u0642",
        android.Manifest.permission.READ_CALL_LOG to "\u0633\u062C\u0644 \u0627\u0644\u0645\u0643\u0627\u0644\u0645\u0627\u062A",
        android.Manifest.permission.CALL_PHONE to "\u0625\u062C\u0631\u0627\u0621 \u0645\u0643\u0627\u0644\u0645\u0627\u062A"
    )

    // \u064A\u0641\u062D\u0635 \u0643\u0644 \u0627\u0644\u062A\u0637\u0628\u064A\u0642\u0627\u062A \u0627\u0644\u0645\u062B\u0628\u062A\u0629 (\u063A\u064A\u0631 \u0623\u0646\u0638\u0645\u0629 \u0627\u0644\u062A\u0634\u063A\u064A\u0644) \u0648\u064A\u0631\u062C\u0639 \u0627\u0644\u0644\u064A \u0639\u0646\u062F\u0647\u0627 \u0635\u0644\u0627\u062D\u064A\u0627\u062A \u062D\u0633\u0627\u0633\u0629 \u0645\u0645\u0646\u0648\u062D\u0629 \u0641\u0639\u0644\u064A\u0627\u064B
    fun scanInstalledApps(): List<RiskyApp> {
        val pm = context.packageManager
        val results = mutableListOf<RiskyApp>()
        val apps = pm.getInstalledApplications(PackageManager.GET_META_DATA)

        for (appInfo in apps) {
            val isSystemApp = (appInfo.flags and android.content.pm.ApplicationInfo.FLAG_SYSTEM) != 0
            if (isSystemApp) continue

            try {
                val packageInfo = pm.getPackageInfo(appInfo.packageName, PackageManager.GET_PERMISSIONS)
                val requested = packageInfo.requestedPermissions ?: continue
                val granted = mutableListOf<String>()

                for (i in requested.indices) {
                    val permName = requested[i]
                    val label = sensitivePermissions[permName] ?: continue
                    val isGranted = pm.checkPermission(permName, appInfo.packageName) == PackageManager.PERMISSION_GRANTED
                    if (isGranted) granted.add(label)
                }

                if (granted.isNotEmpty()) {
                    val label = pm.getApplicationLabel(appInfo).toString()
                    results.add(RiskyApp(label, appInfo.packageName, granted))
                }
            } catch (e: Exception) {
                continue
            }
        }
        return results.sortedByDescending { it.riskyPermissions.size }
    }

    // \u0645\u0624\u0634\u0631\u0627\u062A \u062D\u0627\u0644\u0629 \u0627\u0644\u062C\u0647\u0627\u0632 \u0627\u0644\u062D\u0642\u064A\u0642\u064A\u0629 \u0644\u0648\u0636\u0639 \u0627\u0644\u062F\u0641\u0627\u0639 (\u0628\u0637\u0627\u0631\u064A\u0629/\u0630\u0627\u0643\u0631\u0629/\u0648\u0636\u0639 \u0627\u0644\u062A\u0637\u0648\u064A\u0631)
    fun isDeveloperOptionsEnabled(): Boolean {
        return android.provider.Settings.Global.getInt(
            context.contentResolver,
            android.provider.Settings.Global.ADB_ENABLED, 0
        ) == 1
    }

    fun getMemoryUsagePercent(): Int {
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as android.app.ActivityManager
        val memInfo = android.app.ActivityManager.MemoryInfo()
        am.getMemoryInfo(memInfo)
        val used = memInfo.totalMem - memInfo.availMem
        return ((used.toDouble() / memInfo.totalMem.toDouble()) * 100).toInt()
    }

    fun getBatteryPercent(): Int {
        val bm = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
        return bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
    }
}
