package com.lenterosa.app.offline

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import com.lenterosa.app.network.AndroidNetworkStatus
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Si el teléfono tiene internet ahora, y cuando cambia (RF-10). */
interface Connectivity {
    val online: StateFlow<Boolean>
}

@Singleton
class AndroidConnectivity @Inject constructor(@ApplicationContext context: Context) : Connectivity {

    private val status = AndroidNetworkStatus(context)
    private val _online = MutableStateFlow(status.hasInternet())
    override val online: StateFlow<Boolean> = _online.asStateFlow()

    init {
        context.getSystemService(ConnectivityManager::class.java)?.registerDefaultNetworkCallback(
            object : ConnectivityManager.NetworkCallback() {
                override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) {
                    _online.value = capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                        capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
                }

                override fun onLost(network: Network) {
                    _online.value = false
                }
            },
        )
    }
}
