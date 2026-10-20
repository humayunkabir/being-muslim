package app.beingmuslim.android.location

import app.beingmuslim.android.citycatalog.CityCatalog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * How a permission result and a device fix become a city suggestion (issue #6):
 * denial is distinct from an unavailable fix, only a recent fix is used, and the
 * chosen city carries the timezone the times are calculated in.
 */
class LocationCitySuggestionTest {

    private val header = "# id\tname\tbengali_name\tlatitude\tlongitude\tcountry_code\ttimezone"
    private val dhaka = "1185241\tDhaka\tঢাকা\t23.7104\t90.40744\tBD\tAsia/Dhaka"
    private val chattogram = "1205733\tChattogram\tচট্টগ্রাম\t22.3384\t91.83168\tBD\tAsia/Dhaka"

    private val catalog = CityCatalog.parse(sequenceOf(header, dhaka, chattogram))

    @Test
    fun aFreshFixSelectsTheNearestCityIncludingItsTimezone() {
        val result = suggestCity(
            permissionGranted = true,
            fix = LocationFix(23.72, 90.41, ageMillis = 0),
            catalog = catalog,
        )

        assertTrue(result is CitySuggestion.Found)
        val city = (result as CitySuggestion.Found).city
        assertEquals("Dhaka", city.name)
        assertEquals("Asia/Dhaka", city.timezone)
    }

    @Test
    fun aFixAtTheAgeLimitIsStillUsed() {
        val result = suggestCity(
            permissionGranted = true,
            fix = LocationFix(22.34, 91.83, ageMillis = MAX_LOCATION_AGE_MILLIS),
            catalog = catalog,
        )

        assertTrue(result is CitySuggestion.Found)
        assertEquals("Chattogram", (result as CitySuggestion.Found).city.name)
    }

    @Test
    fun aStaleFixIsUnavailable() {
        val result = suggestCity(
            permissionGranted = true,
            fix = LocationFix(23.72, 90.41, ageMillis = MAX_LOCATION_AGE_MILLIS + 1),
            catalog = catalog,
        )

        assertEquals(CitySuggestion.Unavailable, result)
    }

    @Test
    fun aMissingFixIsUnavailable() {
        assertEquals(CitySuggestion.Unavailable, suggestCity(true, null, catalog))
    }

    @Test
    fun aFixWithAFutureTimestampIsUnavailable() {
        val result = suggestCity(true, LocationFix(23.72, 90.41, ageMillis = -1), catalog)

        assertEquals(CitySuggestion.Unavailable, result)
    }

    @Test
    fun aNonFiniteFixIsUnavailable() {
        val result = suggestCity(true, LocationFix(Double.NaN, 90.41, ageMillis = 0), catalog)

        assertEquals(CitySuggestion.Unavailable, result)
    }

    @Test
    fun deniedPermissionIsReportedAsDenied() {
        val result = suggestCity(
            permissionGranted = false,
            fix = LocationFix(23.72, 90.41, ageMillis = 0),
            catalog = catalog,
        )

        assertEquals(CitySuggestion.Denied, result)
    }
}
