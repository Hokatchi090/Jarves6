package com.jarvis.assistant

import android.app.Activity
import android.app.PendingIntent
import android.content.Intent
import android.nfc.NdefMessage
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.Ndef

// \u064A\u062F\u064A\u0631 \u0642\u0631\u0627\u0621\u0629 \u0628\u0637\u0627\u0642\u0627\u062A NFC (\u0645\u0644\u0635\u0642\u0627\u062A \u0630\u0643\u064A\u0629 \u064A\u0645\u0644\u0643\u0647\u0627 \u0627\u0644\u0645\u0633\u062A\u062E\u062F\u0645) \u0639\u0628\u0631 Foreground Dispatch
// \u0645\u0644\u0627\u062D\u0638\u0629: \u064A\u0642\u0631\u0627 \u0641\u0642\u0637 \u0628\u064A\u0627\u0646\u0627\u062A NDEF \u0627\u0644\u0645\u062E\u0632\u0651\u0646\u0629 \u0641\u064A \u0627\u0644\u0628\u0637\u0627\u0642\u0629\u060C \u0644\u0627 \u064A\u0643\u062A\u0628 \u0648\u0644\u0627 \u064A\u0633\u062A\u0646\u0633\u062E \u0623\u064A \u0628\u064A\u0627\u0646\u0627\u062A \u062D\u0645\u0627\u064A\u0629 (\u0645\u062B\u0644 \u0628\u0637\u0627\u0642\u0627\u062A \u0627\u0644\u062F\u062E\u0648\u0644)
class NfcHelper(private val activity: Activity) {

    private val nfcAdapter: NfcAdapter? by lazy { NfcAdapter.getDefaultAdapter(activity) }

    fun isNfcAvailable(): Boolean = nfcAdapter != null
    fun isNfcEnabled(): Boolean = nfcAdapter?.isEnabled == true

    fun enableForegroundDispatch() {
        val adapter = nfcAdapter ?: return
        if (!adapter.isEnabled) return
        val intent = Intent(activity, activity.javaClass).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
        val pendingIntent = PendingIntent.getActivity(
            activity, 0, intent,
            PendingIntent.FLAG_MUTABLE
        )
        try {
            adapter.enableForegroundDispatch(activity, pendingIntent, null, null)
        } catch (e: Exception) {
            // \u0642\u062F \u064A\u0641\u0634\u0644 \u0625\u0630\u0627 \u0627\u0644\u0640 Activity \u0645\u0627\u0634\u064A \u0641\u064A \u0627\u0644\u0645\u0642\u062F\u0645\u0629\u060C \u0645\u0627\u0641\u064A\u0634 \u062E\u0637\u0648\u0631\u0629
        }
    }

    fun disableForegroundDispatch() {
        val adapter = nfcAdapter ?: return
        try {
            adapter.disableForegroundDispatch(activity)
        } catch (e: Exception) {
            // \u062A\u062C\u0627\u0647\u0644 \u0623\u064A \u062E\u0637\u0623 \u0645\u0634\u0627\u0628\u0647 \u0639\u0646\u062F \u0627\u0644\u0625\u064A\u0642\u0627\u0641
        }
    }

    // \u064A\u0642\u0631\u0623 \u0645\u062D\u062A\u0648\u0649 \u0628\u0637\u0627\u0642\u0629 NFC \u0645\u0644\u0645\u0648\u0633\u0629 \u062D\u062F\u064A\u062B\u0627\u064B\u060C \u064A\u0631\u062C\u0639 \u0646\u0635 \u0645\u0642\u0631\u0648\u0621 \u0623\u0648 \u0631\u0633\u0627\u0644\u0629 \u0648\u0627\u0636\u062D\u0629 \u0625\u0630\u0627 \u0645\u0627\u0644\u0642\u0627\u0634 \u0628\u064A\u0627\u0646\u0627\u062A
    fun readTagFromIntent(intent: Intent): String? {
        if (intent.action != NfcAdapter.ACTION_TAG_DISCOVERED &&
            intent.action != NfcAdapter.ACTION_NDEF_DISCOVERED &&
            intent.action != NfcAdapter.ACTION_TECH_DISCOVERED
        ) {
            return null
        }

        val tag: Tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG) ?: return null
        val ndef = Ndef.get(tag) ?: return "\u0628\u0637\u0627\u0642\u0629 NFC \u0645\u0643\u062A\u0634\u0641\u0629 \u0628\u0635\u062D \u0628\u062F\u0648\u0646 \u0628\u064A\u0627\u0646\u0627\u062A NDEF \u0645\u062F\u0639\u0648\u0645\u0629"

        return try {
            ndef.connect()
            val message: NdefMessage? = ndef.cachedNdefMessage ?: ndef.ndefMessage
            ndef.close()
            if (message == null || message.records.isEmpty()) {
                "\u0627\u0644\u0628\u0637\u0627\u0642\u0629 \u0641\u0627\u0636\u064A\u0629"
            } else {
                message.records.joinToString(" | ") { record ->
                    try {
                        String(record.payload, Charsets.UTF_8).trim()
                    } catch (e: Exception) {
                        "(\u0628\u064A\u0627\u0646\u0627\u062A \u063A\u064A\u0631 \u0646\u0635\u064A\u0629)"
                    }
                }
            }
        } catch (e: Exception) {
            "\u062A\u0639\u0630\u0631 \u0642\u0631\u0627\u0621\u0629 \u0627\u0644\u0628\u0637\u0627\u0642\u0629: ${e.message}"
        }
    }
}
