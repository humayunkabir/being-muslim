package app.beingmuslim.android.location

import app.beingmuslim.android.citycatalog.CityCatalog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * How a device fix becomes a city suggestion (issue #6): only a recent fix is
 * used, and the chosen city carries the timezone the times are calculated in.
 * The position never leaves this function.
 */
class LocationCitySuggestionTest {

    private val header = "# id\tname\tbengali_name\tlatitude\tlongitude\tcountry_code\ttimezone"
    private val dhaka = "1185241\tDhaka\tঢাকা\t23.7104\t90.40744\tBD\tAsia/Dhaka"
    private val chattogram = "1205733\tChattogram\tচট্টগ্রাম\t22.3384\t91.83168\tBD\tAsia/Dhaka"

    private val catalog = CityCatalog.parse(sequenceOf(header, dhaka, chattogram))

    @Test
    fun aFreshFixSelectsTheNearestCityIncludingItsTimezone() {
        val city = LocationFix(23.72, 90.41, ageMillis = 0).nearestCity(catalog)

        assertEquals("Dhaka", city?.name)
        assertEquals("Asia/Dhaka", city?.timezone)
    }

    @Test
    fun aFixAtTheAgeLimitIsStillUsed() {
        val city = LocationFix(22.34, 91.83, ageMillis = MAX_LOCATION_AGE_MILLIS)
            .nearestCity(catalog)

        assertEquals("Chattogram", city?.name)
    }

    @Test
    fun aStaleFixIsNotUsed() {
        val city = LocationFix(23.72, 90.41, ageMillis = MAX_LOCATION_AGE_MILLIS + 1)
            .nearestCity(catalog)

        assertNull(city)
    }

    @Test
    fun aMissingFixIsNotUsed() {
        assertNull((null as LocationFix?).nearestCity(catalog))
    }

    @Test
    fun aFixWithAFutureTimestampIsNotUsed() {
        assertNull(LocationFix(23.72, 90.41, ageMillis = -1).nearestCity(catalog))
    }

    @Test
    fun aNonFiniteFixIsNotUsed() {
        assertNull(LocationFix(Double.NaN, 90.41, ageMillis = 0).nearestCity(catalog))
    }
}
