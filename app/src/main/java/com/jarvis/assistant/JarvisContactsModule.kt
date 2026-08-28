package com.jarvis.assistant

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.ContactsContract
import android.provider.Telephony
import android.telephony.SmsManager
import androidx.core.app.ActivityCompat

// \u0648\u062D\u062F\u0629 \u062C\u0647\u0627\u062A \u0627\u0644\u0627\u062A\u0635\u0627\u0644 \u0648\u0627\u0644\u0631\u0633\u0627\u0626\u0644: \u0627\u0644\u0627\u062A\u0635\u0627\u0644\u060C \u0642\u0631\u0627\u0621\u0629 \u0648\u0634\u0631\u062D \u0648\u0625\u0631\u0633\u0627\u0644 SMS
// nextPermissionRequestCode: \u0631\u0645\u0632 \u0637\u0644\u0628 \u0627\u0644\u0635\u0644\u0627\u062D\u064A\u0627\u062A \u0627\u0644\u0645\u0648\u062C\u0648\u062F \u0623\u0635\u0644\u0627\u064B \u0641\u064A MainActivity (REQ_PERMISSIONS/REQ_CONTACTS)\u060C \u0646\u0645\u0631\u0631\u0647 \u0644\u062A\u062C\u0646\u0651\u0628 \u0627\u0632\u062F\u0648\u0627\u062C\u064A\u0629
class JarvisContactsModule(
    private val activity: Activity,
    private val speak: (String) -> Unit,
    private val askAi: (String) -> Unit,
    private val requestPermission: (String, Int) -> Unit,
    private val reqPermissionsCode: Int,
    private val reqContactsCode: Int
) {

    fun execute(intent: JarvisIntent): Boolean {
        return when (intent.type) {
            JarvisIntentType.CONTACT_CALL -> { callContact(intent.argument); true }
            JarvisIntentType.SMS_READ -> { readMessages(); true }
            JarvisIntentType.SMS_EXPLAIN -> { explainLastMessage(); true }
            JarvisIntentType.SMS_SEND -> { sendSmsFromArgument(intent.argument); true }
            else -> false
        }
    }

    // \u064A\u062F\u0648\u0631 \u0639\u0644\u0649 \u0631\u0642\u0645 \u0647\u0627\u062A\u0641 \u062C\u0647\u0629 \u0627\u062A\u0635\u0627\u0644 \u0639\u0646 \u0627\u0633\u0645\u060C \u0623\u0648 null \u0625\u0630\u0627 \u0645\u0627\u0644\u0642\u0627\u0634
    private fun lookupContactNumber(name: String): String? {
        if (ActivityCompat.checkSelfPermission(activity, android.Manifest.permission.READ_CONTACTS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            return null
        }
        val cursor = activity.contentResolver.query(
            ContactsContract.Contacts.CONTENT_URI,
            null,
            "${ContactsContract.Contacts.DISPLAY_NAME} LIKE ?",
            arrayOf("%$name%"),
            null
        )
        cursor?.use { c ->
            if (c.moveToFirst()) {
                val contactId = c.getString(c.getColumnIndexOrThrow(ContactsContract.Contacts._ID))
                val phoneCursor = activity.contentResolver.query(
                    ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                    null,
                    "${ContactsContract.CommonDataKinds.Phone.CONTACT_ID} = ?",
                    arrayOf(contactId),
                    null
                )
                phoneCursor?.use { pc ->
                    if (pc.moveToFirst()) {
                        return pc.getString(
                            pc.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.NUMBER)
                        )
                    }
                }
            }
        }
        return null
    }

    private fun callContact(name: String) {
        if (name.isBlank()) {
            speak("\u0642\u0644\u064A \u0645\u064A\u0646 \u0628\u062F\u0643 \u0623\u062A\u0635\u0644 \u0641\u064A\u0647")
            return
        }
        if (ActivityCompat.checkSelfPermission(activity, android.Manifest.permission.READ_CONTACTS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermission(android.Manifest.permission.READ_CONTACTS, reqContactsCode)
            speak("\u0628\u062F\u064A \u0625\u0630\u0646 \u0642\u0631\u0627\u0621\u0629 \u062C\u0647\u0627\u062A \u0627\u0644\u0627\u062A\u0635\u0627\u0644 \u0623\u0648\u0644\u060C \u062C\u0631\u0628 \u0645\u0631\u0629 \u062A\u0627\u0646\u064A\u0629")
            return
        }
        val number = lookupContactNumber(name)
        if (number == null) {
            speak("\u0645\u0627 \u0644\u0642\u064A\u062A $name \u0641\u064A \u062C\u0647\u0627\u062A \u0627\u0644\u0627\u062A\u0635\u0627\u0644")
            return
        }
        if (ActivityCompat.checkSelfPermission(activity, android.Manifest.permission.CALL_PHONE)
            != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermission(android.Manifest.permission.CALL_PHONE, reqPermissionsCode)
            speak("\u062E\u0644\u064A\u0646\u064A \u0646\u0637\u0644\u0628 \u0635\u0644\u0627\u062D\u064A\u0629 \u0627\u0644\u0627\u062A\u0635\u0627\u0644 \u0623\u0648\u0644")
            return
        }
        try {
            activity.startActivity(Intent(Intent.ACTION_CALL, Uri.parse("tel:$number")))
            speak("\u0646\u062A\u0635\u0644 \u0628\u0640 $name")
        } catch (e: Exception) {
            speak("\u0645\u0627 \u0642\u062F\u0631\u062A \u0646\u062A\u0635\u0644")
        }
    }

    private fun readRecentSms(count: Int = 5): List<Pair<String, String>> {
        val results = mutableListOf<Pair<String, String>>()
        if (ActivityCompat.checkSelfPermission(activity, android.Manifest.permission.READ_SMS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            return results
        }
        val uri = Telephony.Sms.CONTENT_URI
        val projection = arrayOf(Telephony.Sms.ADDRESS, Telephony.Sms.BODY)
        activity.contentResolver.query(uri, projection, null, null, "${Telephony.Sms.DATE} DESC")?.use { cursor ->
            val addressIndex = cursor.getColumnIndex(Telephony.Sms.ADDRESS)
            val bodyIndex = cursor.getColumnIndex(Telephony.Sms.BODY)
            var read = 0
            while (cursor.moveToNext() && read < count) {
                val address = cursor.getString(addressIndex) ?: "\u063A\u064A\u0631 \u0645\u0639\u0631\u0648\u0641"
                val body = cursor.getString(bodyIndex) ?: ""
                results.add(address to body)
                read++
            }
        }
        return results
    }

    private fun readMessages() {
        val messages = readRecentSms(3)
        if (messages.isEmpty()) {
            speak("\u0645\u0627\u0643\u0627\u0634 \u0631\u0633\u0627\u0626\u0644 \u0623\u0648 \u0645\u0627\u0639\u0646\u062F\u0643\u0634 \u0635\u0644\u0627\u062D\u064A\u0629 \u0642\u0631\u0627\u0621\u0629 \u0627\u0644\u0631\u0633\u0627\u0626\u0644")
            return
        }
        val summary = messages.joinToString(". ") { (from, body) -> "\u0645\u0646 $from: $body" }
        speak(summary)
    }

    private fun explainLastMessage() {
        val messages = readRecentSms(1)
        if (messages.isEmpty()) {
            speak("\u0645\u0627\u0643\u0627\u0634 \u0631\u0633\u0627\u0626\u0644 \u0623\u0648 \u0645\u0627\u0639\u0646\u062F\u0643\u0634 \u0635\u0644\u0627\u062D\u064A\u0629 \u0642\u0631\u0627\u0621\u0629 \u0627\u0644\u0631\u0633\u0627\u0626\u0644")
            return
        }
        val (from, body) = messages[0]
        askAi("\u0627\u0634\u0631\u062D\u0644\u064A \u0628\u0627\u062E\u062A\u0635\u0627\u0631 \u0647\u0627\u0630\u0647 \u0627\u0644\u0631\u0633\u0627\u0644\u0629 \u0648\u0634\u0648 \u0627\u0644\u0645\u0642\u0635\u0648\u062F \u0645\u0646\u0647\u0627 (\u0645\u0646 $from): $body")
    }

    // argument \u0645\u062A\u0648\u0642\u0639: "\u0627\u0633\u0645_\u0627\u0644\u0634\u062E\u0635|\u0646\u0635_\u0627\u0644\u0631\u0633\u0627\u0644\u0629"
    private fun sendSmsFromArgument(argument: String) {
        val parts = argument.split("|", limit = 2)
        if (parts.size != 2 || parts[0].isBlank() || parts[1].isBlank()) {
            speak("\u0642\u0648\u0644\u064A: \u0627\u0628\u0639\u062B \u0631\u0633\u0627\u0644\u0629 \u0644\u0641\u0644\u0627\u0646 \u062A\u0642\u0648\u0644 \u0627\u0644\u0646\u0635")
            return
        }
        sendSmsToContact(parts[0].trim(), parts[1].trim())
    }

    private fun sendSmsToContact(name: String, message: String) {
        val number = lookupContactNumber(name)
        if (number == null) {
            speak("\u0645\u0627 \u0644\u0642\u064A\u062A $name \u0641\u064A \u062C\u0647\u0627\u062A \u0627\u0644\u0627\u062A\u0635\u0627\u0644")
            return
        }
        if (ActivityCompat.checkSelfPermission(activity, android.Manifest.permission.SEND_SMS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermission(android.Manifest.permission.SEND_SMS, reqPermissionsCode)
            speak("\u062E\u0644\u064A\u0646\u064A \u0646\u0637\u0644\u0628 \u0635\u0644\u0627\u062D\u064A\u0629 \u0627\u0644\u0631\u0633\u0627\u0644\u0629 \u0623\u0648\u0644")
            return
        }
        try {
            val smsManager = activity.getSystemService(SmsManager::class.java)
            smsManager.sendTextMessage(number, null, message, null, null)
            speak("\u0628\u0639\u062B\u062A \u0627\u0644\u0631\u0633\u0627\u0644\u0629 \u0644\u0640 $name")
        } catch (e: Exception) {
            speak("\u0645\u0627 \u0642\u062F\u0631\u062A \u0646\u0628\u0639\u062B \u0627\u0644\u0631\u0633\u0627\u0644\u0629")
        }
    }
}
