package app.beingmuslim.android.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.os.CancellationSignal
import android.os.Looper
import android.os.SystemClock
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import kotlin.coroutines.resume
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Requests one coarse fix for the optional nearest-city suggestion.
 *
 * Coarse permission is the least precise access that satisfies the feature, and
 * no fine-location permission is declared. Only the fused or network provider is
 * used; the coordinates are handed to the caller once and never stored, logged
 * or sent (`docs/PRODUCT_REQUIREMENTS.md` §6).
 */
class LocationLookup(context: Context) {

    private val appContext = context.applicationContext
    private val manager: LocationManager? =
        appContext.getSystemService(LocationManager::class.java)

    /** Whether the optional coarse permission is currently granted. */
    fun hasPermission(): Boolean =
        ContextCompat.checkSelfPermission(appContext, PERMISSION) ==
            PackageManager.PERMISSION_GRANTED

    /**
     * One coarse fix, or null when permission is missing, no provider is
     * enabled, or none arrives within [TIMEOUT_MILLIS]. A cached fix is used as
     * a fallback; [nearestCity] rejects it if it is stale.
     */
    suspend fun currentFix(): LocationFix? {
        if (!hasPermission()) return null
        val locationManager = manager ?: return null
        val provider = enabledProvider(locationManager) ?: return null

        val location = withTimeoutOrNull(TIMEOUT_MILLIS) {
            requestCurrent(locationManager, provider)
        } ?: lastKnown(locationManager) ?: return null

        val ageMillis = (SystemClock.elapsedRealtimeNanos() - location.elapsedRealtimeNanos)
            .coerceAtLeast(0L) / NANOS_PER_MILLI
        return LocationFix(location.latitude, location.longitude, ageMillis)
    }

    private suspend fun requestCurrent(manager: LocationManager, provider: String): Location? =
        withContext(Dispatchers.Main.immediate) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                requestCurrentModern(manager, provider)
            } else {
                requestCurrentLegacy(manager, provider)
            }
        }

    // Coarse permission is checked in currentFix; the calls also catch
    // SecurityException so a revocation between the check and the call is safe.
    @SuppressLint("MissingPermission")
    @RequiresApi(Build.VERSION_CODES.R)
    private suspend fun requestCurrentModern(
        manager: LocationManager,
        provider: String,
    ): Location? = suspendCancellableCoroutine { continuation ->
        val signal = CancellationSignal()
        continuation.invokeOnCancellation { signal.cancel() }
        runCatching {
            manager.getCurrentLocation(
                provider,
                signal,
                ContextCompat.getMainExecutor(appContext),
            ) { location -> continuation.resumeIfActive(location) }
        }.onFailure { error ->
            if (error is SecurityException || error is IllegalArgumentException) {
                continuation.resumeIfActive(null)
            } else {
                throw error
            }
        }
    }

    @SuppressLint("MissingPermission")
    @Suppress("DEPRECATION")
    private suspend fun requestCurrentLegacy(
        manager: LocationManager,
        provider: String,
    ): Location? = suspendCancellableCoroutine { continuation ->
        val listener = object : LocationListener {
            override fun onLocationChanged(location: Location) {
                manager.removeUpdates(this)
                continuation.resumeIfActive(location)
            }

            @Deprecated("Deprecated in Java")
            override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) = Unit

            override fun onProviderEnabled(provider: String) = Unit

            override fun onProviderDisabled(provider: String) = Unit
        }
        continuation.invokeOnCancellation { manager.removeUpdates(listener) }
        runCatching {
            manager.requestSingleUpdate(provider, listener, Looper.getMainLooper())
        }.onFailure { error ->
            if (error is SecurityException || error is IllegalArgumentException) {
                continuation.resumeIfActive(null)
            } else {
                throw error
            }
        }
    }

    /**
     * The fused provider where the platform has one, else the network provider —
     * both work with coarse permission. A disabled provider is skipped instead of
     * leaving the user waiting for a fix that cannot arrive.
     */
    private fun enabledProvider(manager: LocationManager): String? {
        val providers = buildList {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                add(LocationManager.FUSED_PROVIDER)
            }
            add(LocationManager.NETWORK_PROVIDER)
        }
        return providers.firstOrNull { provider ->
            runCatching { manager.isProviderEnabled(provider) }.getOrDefault(false)
        }
    }

    @SuppressLint("MissingPermission")
    private fun lastKnown(manager: LocationManager): Location? {
        val providers = buildList {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                add(LocationManager.FUSED_PROVIDER)
            }
            add(LocationManager.NETWORK_PROVIDER)
        }
        return providers.firstNotNullOfOrNull { provider ->
            runCatching { manager.getLastKnownLocation(provider) }.getOrNull()
        }
    }

    private fun CancellableContinuation<Location?>.resumeIfActive(location: Location?) {
        if (isActive) resume(location)
    }

    companion object {
        /** The least precise permission that satisfies the suggestion. */
        val PERMISSION: String = Manifest.permission.ACCESS_COARSE_LOCATION

        private const val TIMEOUT_MILLIS = 8_000L
        private const val NANOS_PER_MILLI = 1_000_000L
    }
}
