package com.jarvis.assistant

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Context
import android.os.Handler
import android.os.Looper

// \u064A\u062F\u064A\u0631 \u0627\u0643\u062A\u0634\u0627\u0641 \u0623\u062C\u0647\u0632\u0629 Bluetooth \u0627\u0644\u0642\u0631\u064A\u0628\u0629 (BLE) \u0648\u0625\u062F\u0627\u0631\u062A\u0647\u0627 (\u0627\u0642\u062A\u0631\u0627\u0646/\u0641\u0635\u0644)
// \u0645\u0644\u0627\u062D\u0638\u0629: \u0627\u0644\u0627\u0633\u062A\u062F\u0639\u0627\u0621 \u064A\u062A\u062D\u0642\u0642 \u0645\u0646 \u0627\u0644\u0635\u0644\u0627\u062D\u064A\u0627\u062A \u0642\u0628\u0644 \u0627\u0644\u0628\u062F\u0621 (BLUETOOTH_SCAN/BLUETOOTH_CONNECT \u0641\u064A Android 12+)
class BluetoothManagerHelper(private val context: Context) {

    data class FoundDevice(val name: String, val address: String, val rssi: Int)

    private val bluetoothAdapter: BluetoothAdapter? by lazy {
        val manager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? android.bluetooth.BluetoothManager
        manager?.adapter
    }

    private var scanner: BluetoothLeScanner? = null
    private var scanning = false
    private val handler = Handler(Looper.getMainLooper())
    private val foundDevices = LinkedHashMap<String, FoundDevice>()

    fun isBluetoothAvailable(): Boolean = bluetoothAdapter != null
    fun isBluetoothEnabled(): Boolean = bluetoothAdapter?.isEnabled == true

    @SuppressLint("MissingPermission")
    fun startScan(durationMs: Long = 6000L, onDeviceFound: (FoundDevice) -> Unit, onFinished: (List<FoundDevice>) -> Unit) {
        if (scanning) return
        val adapter = bluetoothAdapter ?: run {
            onFinished(emptyList())
            return
        }
        if (!adapter.isEnabled) {
            onFinished(emptyList())
            return
        }
        scanner = adapter.bluetoothLeScanner
        foundDevices.clear()
        scanning = true

        val callback = object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult) {
                val device: BluetoothDevice = result.device
                val name = device.name ?: "\u062C\u0647\u0627\u0632 \u063A\u064A\u0631 \u0645\u0633\u0645\u0649"
                val found = FoundDevice(name, device.address, result.rssi)
                if (!foundDevices.containsKey(found.address)) {
                    foundDevices[found.address] = found
                    onDeviceFound(found)
                }
            }
        }

        try {
            scanner?.startScan(callback)
        } catch (e: Exception) {
            scanning = false
            onFinished(emptyList())
            return
        }

        handler.postDelayed({
            stopScan(callback)
            onFinished(foundDevices.values.toList())
        }, durationMs)
    }

    @SuppressLint("MissingPermission")
    private fun stopScan(callback: ScanCallback) {
        if (!scanning) return
        scanning = false
        try {
            scanner?.stopScan(callback)
        } catch (e: Exception) {
            // \u0627\u0644\u0645\u0627\u0633\u062D \u0642\u062F \u064A\u0641\u0634\u0644 \u0625\u0630\u0627 Bluetooth \u062A\u0642\u0641\u0644 \u0623\u062B\u0646\u0627\u0621 \u0627\u0644\u0645\u0633\u062D\u060C \u0645\u0627\u0641\u064A\u0634 \u062E\u0637\u0648\u0631\u0629
        }
    }

    fun cancelScan() {
        handler.removeCallbacksAndMessages(null)
        scanning = false
    }
}
