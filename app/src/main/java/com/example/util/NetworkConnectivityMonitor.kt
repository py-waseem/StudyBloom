package com.example.util

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Build
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class ConnectionType {
    WIFI,
    CELLULAR,
    ETHERNET,
    NONE
}

data class NetworkState(
    val isConnected: Boolean = true,
    val connectionType: ConnectionType = ConnectionType.WIFI,
    val isMetered: Boolean = false
) {
    val displayLabel: String
        get() = when {
            !isConnected -> "Offline Mode"
            connectionType == ConnectionType.WIFI -> "Online • Wi-Fi"
            connectionType == ConnectionType.CELLULAR -> "Online • Cellular"
            else -> "Online"
        }
}

class NetworkConnectivityMonitor(context: Context) {

    private val connectivityManager =
        context.applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    private val _networkState = MutableStateFlow(getCurrentNetworkState())
    val networkState: StateFlow<NetworkState> = _networkState.asStateFlow()

    private val scope = CoroutineScope(Dispatchers.Default)

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            updateState()
        }

        override fun onLost(network: Network) {
            updateState()
        }

        override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
            updateState()
        }
    }

    init {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                connectivityManager.registerDefaultNetworkCallback(networkCallback)
            } else {
                val request = NetworkRequest.Builder()
                    .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                    .build()
                connectivityManager.registerNetworkCallback(request, networkCallback)
            }
        } catch (_: Exception) {
            // In test environments or restricted sandboxes, default to connected state
        }
    }

    fun refresh() {
        updateState()
    }

    private fun updateState() {
        scope.launch {
            _networkState.value = getCurrentNetworkState()
        }
    }

    private fun getCurrentNetworkState(): NetworkState {
        try {
            val activeNetwork = connectivityManager.activeNetwork ?: return NetworkState(
                isConnected = false,
                connectionType = ConnectionType.NONE
            )

            val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork) ?: return NetworkState(
                isConnected = false,
                connectionType = ConnectionType.NONE
            )

            val hasInternet = capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            val type = when {
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> ConnectionType.WIFI
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> ConnectionType.CELLULAR
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> ConnectionType.ETHERNET
                else -> ConnectionType.WIFI
            }

            val isMetered = !capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED)

            return NetworkState(
                isConnected = hasInternet,
                connectionType = if (hasInternet) type else ConnectionType.NONE,
                isMetered = isMetered
            )
        } catch (_: Exception) {
            return NetworkState(isConnected = true, connectionType = ConnectionType.WIFI)
        }
    }
}
