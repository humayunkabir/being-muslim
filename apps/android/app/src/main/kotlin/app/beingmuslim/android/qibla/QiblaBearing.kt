package app.beingmuslim.android.qibla

import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

/**
 * The qibla, and the arithmetic that turns a device's magnetic heading into a
 * true-north one.
 *
 * The bearing is the initial great-circle direction from the city's coordinates
 * to the Kaaba. It needs no sensor and no permission, so it is the direction
 * every user gets (`docs/PRODUCT_REQUIREMENTS.md` §6, issues #9 and #10).
 *
 * This is the pure domain: no Android imports and no device state, so the
 * formula and its edge cases are covered by ordinary JVM tests.
 */
object QiblaBearing {

    /**
     * The Kaaba in Makkah, in decimal degrees. The value is precise enough for a
     * direction: a 0.001° error in the target moves the bearing far less than the
     * compass can measure.
     */
    const val KAABA_LATITUDE = 21.422487
    const val KAABA_LONGITUDE = 39.826206

    /**
     * The great-circle bearing from [latitude]/[longitude] to the Kaaba, in
     * degrees clockwise from true north (`0.0..<360.0`), or null when the
     * position is not a finite pair of numbers.
     */
    fun from(latitude: Double, longitude: Double): Double? {
        if (!latitude.isFinite() || !longitude.isFinite()) return null

        val fromLatitude = Math.toRadians(latitude)
        val kaabaLatitude = Math.toRadians(KAABA_LATITUDE)
        val longitudeDelta = Math.toRadians(KAABA_LONGITUDE - longitude)

        val y = sin(longitudeDelta) * cos(kaabaLatitude)
        val x = cos(fromLatitude) * sin(kaabaLatitude) -
            sin(fromLatitude) * cos(kaabaLatitude) * cos(longitudeDelta)

        return normalise(Math.toDegrees(atan2(y, x)))
    }

    /**
     * [magneticHeadingDegrees], as the platform sensor reports it, corrected by
     * the local magnetic declination ([declinationEastDegrees], east positive)
     * so the result is relative to true north.
     */
    fun trueHeading(magneticHeadingDegrees: Double, declinationEastDegrees: Double): Double =
        normalise(magneticHeadingDegrees + declinationEastDegrees)

    /**
     * How far to turn from [headingDegrees] to face [targetDegrees]: positive to
     * the right, negative to the left, within `(-180.0, 180.0]`. A half turn is
     * reported as `180.0`, since either direction is equally short.
     */
    fun turnDegrees(headingDegrees: Double, targetDegrees: Double): Double {
        val turn = normalise(targetDegrees - headingDegrees)
        return if (turn > 180.0) turn - 360.0 else turn
    }

    private fun normalise(degrees: Double): Double = ((degrees % 360.0) + 360.0) % 360.0
}
