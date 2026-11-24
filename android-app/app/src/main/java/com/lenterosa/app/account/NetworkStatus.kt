package com.lenterosa.app.account

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

/** Indica si el teléfono tiene internet, para distinguir "sin internet" de "el servidor no responde". */
fun interface NetworkStatus {
    fun hasInternet(): Boolean
}

class AndroidNetworkStatus @Inject constructor(@ApplicationContext private val context: Context) : NetworkStatus {

    override fun hasInternet(): Boolean {
        val connectivity = context.getSystemService(ConnectivityManager::class.java) ?: return false
        val capabilities = connectivity.getNetworkCapabilities(connectivity.activeNetwork) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }
}
