package com.example.data.remote

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import java.util.concurrent.atomic.AtomicBoolean

class NetworkMonitor(context: Context, private val onConnected: () -> Unit) {
    private val manager = context.getSystemService(ConnectivityManager::class.java)
    private val wasConnected = AtomicBoolean(false)
    val isConnected: Boolean
        get() = manager?.getNetworkCapabilities(manager.activeNetwork)?.let {
            it.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                it.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
        } ?: false

    private val callback = object : ConnectivityManager.NetworkCallback() {
        override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) {
            val connected = capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
            if (connected && !wasConnected.getAndSet(true)) onConnected()
            else if (!connected) wasConnected.set(false)
        }
        override fun onLost(network: Network) { wasConnected.set(false) }
    }
    private val registered = runCatching { manager?.registerDefaultNetworkCallback(callback); manager != null }.getOrDefault(false)
    fun close() { if (registered) runCatching { manager?.unregisterNetworkCallback(callback) } }
}
