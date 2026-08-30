package com.jarvis.assistant

import android.app.admin.DeviceAdminReceiver

/**
 * MyDeviceAdminReceiver
 * مطلوب لتفعيل قفل الجهاز الفوري (Panic Lockdown).
 * المسار: app/src/main/java/com/jarvis/assistant/MyDeviceAdminReceiver.kt
 *
 * بعد إضافة هذا الملف، أضف في AndroidManifest.xml داخل <application>:
 *
 * <receiver
 *     android:name=".MyDeviceAdminReceiver"
 *     android:permission="android.permission.BIND_DEVICE_ADMIN">
 *     <meta-data
 *         android:name="android.app.device_admin"
 *         android:resource="@xml/device_admin_policies" />
 *     <intent-filter>
 *         <action android:name="android.app.action.DEVICE_ADMIN_ENABLED" />
 *     </intent-filter>
 * </receiver>
 *
 * وأنشئ ملف res/xml/device_admin_policies.xml بالمحتوى:
 *
 * <?xml version="1.0" encoding="utf-8"?>
 * <device-admin>
 *     <uses-policies>
 *         <force-lock />
 *     </uses-policies>
 * </device-admin>
 */
class MyDeviceAdminReceiver : DeviceAdminReceiver()
