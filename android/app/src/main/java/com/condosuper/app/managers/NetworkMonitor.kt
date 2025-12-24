package com.condosuper.app.managers

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class NetworkMonitor private constructor(context: Context) {
    companion object {
        @Volatile
        private var INSTANCE: NetworkMonitor? = null
        
        fun getInstance(context: Context): NetworkMonitor {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: NetworkMonitor(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    private val connectivityManager = 
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    
    private val _isConnected = MutableStateFlow(true)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()
    
    private val _connectionType = MutableStateFlow<ConnectionType?>(null)
    val connectionType: StateFlow<ConnectionType?> = _connectionType.asStateFlow()

    enum class ConnectionType {
        WIFI, MOBILE, ETHERNET, OTHER
    }

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            super.onAvailable(network)
            updateConnectionStatus()
            android.util.Log.d("NetworkMonitor", "Network connection restored")
            // Trigger upload when connection is restored
            UploadQueueManager.getInstance(context).processQueue()
        }

        override fun onLost(network: Network) {
            super.onLost(network)
            updateConnectionStatus()
            android.util.Log.d("NetworkMonitor", "Network connection lost")
        }

        override fun onCapabilitiesChanged(
            network: Network,
            networkCapabilities: NetworkCapabilities
        ) {
            super.onCapabilitiesChanged(network, networkCapabilities)
            updateConnectionStatus()
        }
    }

    init {
        val networkRequest = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
        
        connectivityManager.registerNetworkCallback(networkRequest, networkCallback)
        updateConnectionStatus()
    }

    private fun updateConnectionStatus() {
        val activeNetwork = connectivityManager.activeNetwork ?: run {
            _isConnected.value = false
            _connectionType.value = null
            return
        }

        val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork) ?: run {
            _isConnected.value = false
            _connectionType.value = null
            return
        }

        _isConnected.value = capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)

        _connectionType.value = when {
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> ConnectionType.WIFI
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> ConnectionType.MOBILE
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> ConnectionType.ETHERNET
            else -> ConnectionType.OTHER
        }
    }
}


