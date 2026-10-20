package app.beingmuslim.android.qibla

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * The qibla bearing and the arithmetic around a live heading, tested through the
 * pure domain object (`docs/PRODUCT_REQUIREMENTS.md` §6).
 *
 * The reference directions are the values published for those cities by qibla
 * calculators using the great-circle bearing to the Kaaba, not recomputed here
 * from this implementation, so a wrong formula shows up as a mismatch.
 */
class QiblaBearingTest {

    private val tolerance = 0.2

    @Test
    fun pointsToTheKaabaFromDhaka() {
        assertEquals(277.66, QiblaBearing.from(23.7104, 90.40744)!!, tolerance)
    }

    @Test
    fun pointsToTheKaabaFromLondon() {
        assertEquals(118.99, QiblaBearing.from(51.5074, -0.1278)!!, tolerance)
    }

    @Test
    fun pointsToTheKaabaFromNewYork() {
        assertEquals(58.48, QiblaBearing.from(40.7128, -74.0060)!!, tolerance)
    }

    @Test
    fun pointsToTheKaabaFromJakarta() {
        assertEquals(295.15, QiblaBearing.from(-6.2088, 106.8456)!!, tolerance)
    }

    @Test
    fun pointsDueSouthFromAPlaceDueNorthOfTheKaaba() {
        // Directly north of Makkah the qibla is due south (180°).
        assertEquals(180.0, QiblaBearing.from(30.0, 39.826206)!!, tolerance)
    }

    @Test
    fun pointsDueNorthFromAPlaceDueSouthOfTheKaaba() {
        assertEquals(0.0, QiblaBearing.from(0.0, 39.826206)!!, tolerance)
    }

    @Test
    fun isAlwaysNormalisedToAClockwiseBearing() {
        val bearing = QiblaBearing.from(51.5074, -0.1278)!!

        assertEquals(bearing, ((bearing % 360) + 360) % 360, 1e-9)
    }

    @Test
    fun aNonFinitePositionHasNoBearing() {
        assertNull(QiblaBearing.from(Double.NaN, 90.40744))
        assertNull(QiblaBearing.from(23.7104, Double.POSITIVE_INFINITY))
    }

    @Test
    fun appliesEasternDeclinationToAMagneticHeading() {
        assertEquals(10.0, QiblaBearing.trueHeading(350.0, 20.0), 1e-9)
    }

    @Test
    fun appliesWesternDeclinationToAMagneticHeading() {
        assertEquals(350.0, QiblaBearing.trueHeading(10.0, -20.0), 1e-9)
    }

    @Test
    fun aTurnToTheRightIsPositiveAndToTheLeftIsNegative() {
        assertEquals(20.0, QiblaBearing.turnDegrees(headingDegrees = 350.0, targetDegrees = 10.0), 1e-9)
        assertEquals(-20.0, QiblaBearing.turnDegrees(headingDegrees = 10.0, targetDegrees = 350.0), 1e-9)
    }

    @Test
    fun aHalfTurnIsReportedAsOneHalfTurn() {
        // Either way round is equally short, so the sign carries no meaning.
        assertEquals(180.0, QiblaBearing.turnDegrees(headingDegrees = 0.0, targetDegrees = 180.0), 1e-9)
        assertEquals(180.0, QiblaBearing.turnDegrees(headingDegrees = 180.0, targetDegrees = 0.0), 1e-9)
    }
}
