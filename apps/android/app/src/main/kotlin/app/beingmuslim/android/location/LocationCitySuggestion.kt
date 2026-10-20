package app.beingmuslim.android.location

import app.beingmuslim.android.citycatalog.City
import app.beingmuslim.android.citycatalog.CityCatalog

/**
 * A device location reduced to what a city suggestion needs. The coordinates
 * are used once and never stored, logged or sent (`docs/PRODUCT_REQUIREMENTS.md`
 * §6).
 *
 * @param ageMillis how long ago the fix was taken.
 */
data class LocationFix(
    val latitude: Double,
    val longitude: Double,
    val ageMillis: Long,
)

/** The outcome of the optional location shortcut (issue #6). */
sealed interface CitySuggestion {

    /** The catalog city nearest the accepted fix, including its timezone. */
    data class Found(val city: City) : CitySuggestion

    /** The user did not grant the optional coarse permission. */
    data object Denied : CitySuggestion

    /** Permission was granted, but no usable fix was available. */
    data object Unavailable : CitySuggestion
}

/**
 * Maps a permission result and a fix to what the city step should do. Only a
 * granted permission and a fix no older than [MAX_LOCATION_AGE_MILLIS] can
 * suggest a city; a stale fix is not silently treated as current.
 */
fun suggestCity(
    permissionGranted: Boolean,
    fix: LocationFix?,
    catalog: CityCatalog,
): CitySuggestion {
    if (!permissionGranted) return CitySuggestion.Denied
    if (fix == null || fix.ageMillis !in 0..MAX_LOCATION_AGE_MILLIS) {
        return CitySuggestion.Unavailable
    }
    return catalog.nearest(fix.latitude, fix.longitude)
        ?.let { CitySuggestion.Found(it) }
        ?: CitySuggestion.Unavailable
}

/** How old a fix may be and still suggest a city. */
const val MAX_LOCATION_AGE_MILLIS: Long = 10 * 60 * 1000L
