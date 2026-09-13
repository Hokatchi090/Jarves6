package com.jarvisx.app.data

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * مصدر واحد لحالة الاتصال بالإنترنت تقرأ منه كل شاشات التطبيق
 * (أيقونة الحالة، تفعيل/تعطيل محاولات المزامنة الفورية...).
 * نديرها object باش تكون singleton بسيطة بلا Dependency Injection معقّد.
 */
object NetworkStateHolder {

    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> get() = _isConnected

    private var registered = false

    fun init(context: Context) {
        if (registered) return
        registered = true

        val cm = context.applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        // نحدد الحالة الابتدائية فورًا بدل ما ننتظر أول callback
        val active = cm.activeNetwork
        val caps = active?.let { cm.getNetworkCapabilities(it) }
        _isConnected.value = caps?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true

        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()

        cm.registerNetworkCallback(request, object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                _isConnected.value = true
            }
            override fun onLost(network: Network) {
                _isConnected.value = false
            }
            override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
                _isConnected.value = networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            }
        })
    }

    fun currentlyOnline(): Boolean = _isConnected.value
}
