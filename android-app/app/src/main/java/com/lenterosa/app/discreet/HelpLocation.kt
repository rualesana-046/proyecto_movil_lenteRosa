package com.lenterosa.app.discreet

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.os.SystemClock
import androidx.core.content.ContextCompat
import androidx.core.location.LocationManagerCompat
import androidx.core.os.CancellationSignal
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.Executor
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull

/** Ubicación para pedir ayuda: precisa si hay permiso; si no, aproximada (y se avisa). Nunca se guarda en la API. */
sealed interface HelpFix {
    data class Found(val lat: Double, val lng: Double, val precise: Boolean) : HelpFix
    data object Unavailable : HelpFix
}

fun interface HelpLocation {
    suspend fun current(timeoutMillis: Long): HelpFix
}

class AndroidHelpLocation @Inject constructor(@ApplicationContext private val context: Context) : HelpLocation {

    @SuppressLint("MissingPermission", "InlinedApi") // Los permisos se comprueban justo antes; FUSED solo desde API 31.
    override suspend fun current(timeoutMillis: Long): HelpFix {
        val precise = granted(Manifest.permission.ACCESS_FINE_LOCATION)
        if (!precise && !granted(Manifest.permission.ACCESS_COARSE_LOCATION)) return HelpFix.Unavailable
        val manager = context.getSystemService(LocationManager::class.java) ?: return HelpFix.Unavailable
        if (!LocationManagerCompat.isLocationEnabled(manager)) return HelpFix.Unavailable
        val providers = listOfNotNull(
            LocationManager.FUSED_PROVIDER.takeIf { Build.VERSION.SDK_INT >= Build.VERSION_CODES.S },
            LocationManager.GPS_PROVIDER.takeIf { precise },
            LocationManager.NETWORK_PROVIDER,
        ).filter { manager.allProviders.contains(it) && manager.isProviderEnabled(it) }
        val provider = providers.firstOrNull() ?: return HelpFix.Unavailable
        val fresh = withTimeoutOrNull(timeoutMillis) {
            suspendCancellableCoroutine { continuation ->
                val cancel = CancellationSignal()
                continuation.invokeOnCancellation { cancel.cancel() }
                LocationManagerCompat.getCurrentLocation(manager, provider, cancel, Executor { it.run() }) { location ->
                    if (continuation.isActive) continuation.resume(location)
                }
            }
        }
        val location = fresh ?: recentLastKnown(manager, providers)
        return location?.let { HelpFix.Found(it.latitude, it.longitude, precise) } ?: HelpFix.Unavailable
    }

    @SuppressLint("MissingPermission")
    private fun recentLastKnown(manager: LocationManager, providers: List<String>): Location? = providers
        .mapNotNull { manager.getLastKnownLocation(it) }
        .filter { SystemClock.elapsedRealtimeNanos() - it.elapsedRealtimeNanos <= 5L * 60 * 1_000_000_000 }
        .maxByOrNull { it.elapsedRealtimeNanos }

    private fun granted(permission: String) =
        ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
}
