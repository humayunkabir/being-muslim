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

/**
 * The catalog city nearest to this fix, including its timezone, or null when
 * there is no fix or it is older than [MAX_LOCATION_AGE_MILLIS]. A stale fix is
 * not silently treated as current: the user searches for a city instead.
 */
fun LocationFix?.nearestCity(
    catalog: CityCatalog,
    maxAgeMillis: Long = MAX_LOCATION_AGE_MILLIS,
): City? {
    if (this == null || ageMillis !in 0..maxAgeMillis) return null
    return catalog.nearest(latitude, longitude)
}

/** How old a fix may be and still suggest a city. */
const val MAX_LOCATION_AGE_MILLIS: Long = 10 * 60 * 1000L
