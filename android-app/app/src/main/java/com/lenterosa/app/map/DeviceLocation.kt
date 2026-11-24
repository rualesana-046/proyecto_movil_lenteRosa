package com.lenterosa.app.map

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
import com.lenterosa.app.places.GeoPoint
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.Executor
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull

/** Resultado de pedir la ubicación una sola vez. */
sealed interface LocationResult {
    data class Found(val point: GeoPoint) : LocationResult
    data object PermissionMissing : LocationResult
    data object Disabled : LocationResult
    data object Unavailable : LocationResult
}

/**
 * Ubicación bajo demanda (RNF-01): una sola lectura aproximada cuando la usuaria toca "Cerca de mí".
 * Sin seguimiento continuo y sin guardarla: se usa para buscar y se descarta.
 */
fun interface DeviceLocation {
    suspend fun currentApproximate(): LocationResult
}

class AndroidDeviceLocation @Inject constructor(@ApplicationContext private val context: Context) : DeviceLocation {

    @SuppressLint("MissingPermission") // Se comprueba el permiso justo antes de pedir la ubicación.
    override suspend fun currentApproximate(): LocationResult {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            return LocationResult.PermissionMissing
        }
        val manager = context.getSystemService(LocationManager::class.java) ?: return LocationResult.Unavailable
        if (!LocationManagerCompat.isLocationEnabled(manager)) {
            return LocationResult.Disabled
        }
        val provider = providerFor(manager) ?: return LocationResult.Disabled
        val executor = Executor { it.run() }
        val location = withTimeoutOrNull(LOCATION_TIMEOUT_MS) {
            suspendCancellableCoroutine { continuation ->
                val cancel = CancellationSignal()
                continuation.invokeOnCancellation { cancel.cancel() }
                LocationManagerCompat.getCurrentLocation(manager, provider, cancel, executor) { location ->
                    if (continuation.isActive) continuation.resume(location)
                }
            }
        }
        // Si no llega una lectura nueva a tiempo, sirve la última del teléfono solo si es reciente.
        val result = location ?: recentLastKnown(manager)
        return result?.let { LocationResult.Found(GeoPoint(it.latitude, it.longitude)) } ?: LocationResult.Unavailable
    }

    @SuppressLint("MissingPermission") // Solo se llama después de comprobar el permiso.
    private fun recentLastKnown(manager: LocationManager): Location? = manager.allProviders
        .filter { manager.isProviderEnabled(it) }
        .mapNotNull { manager.getLastKnownLocation(it) }
        .filter { SystemClock.elapsedRealtimeNanos() - it.elapsedRealtimeNanos <= MAX_LAST_KNOWN_AGE_NANOS }
        .maxByOrNull { it.elapsedRealtimeNanos }

    // FUSED_PROVIDER solo se usa desde Android 12 (API 31): la condición lo comprueba en tiempo de ejecución.
    @SuppressLint("InlinedApi")
    private fun providerFor(manager: LocationManager): String? = listOfNotNull(
        LocationManager.FUSED_PROVIDER.takeIf { Build.VERSION.SDK_INT >= Build.VERSION_CODES.S },
        LocationManager.NETWORK_PROVIDER,
        LocationManager.GPS_PROVIDER,
    ).firstOrNull { manager.allProviders.contains(it) && manager.isProviderEnabled(it) }

    private companion object {
        const val LOCATION_TIMEOUT_MS = 15_000L
        const val MAX_LAST_KNOWN_AGE_NANOS = 5L * 60 * 1_000_000_000
    }
}
