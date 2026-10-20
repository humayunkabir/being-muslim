package app.beingmuslim.android.ui

import java.time.Instant
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * When the times screen rolls over to the next day: the city's next local
 * midnight, not the device's and not 24 hours later (issue #5).
 */
class DayRolloverTest {

    @Test
    fun theNextMidnightIsTheCitysNextLocalDay() {
        val now = Instant.parse("2026-10-20T12:00:00Z")

        assertEquals(
            Instant.parse("2026-10-20T18:00:00Z"),
            nextLocalMidnight(now, ZoneId.of("Asia/Dhaka")),
        )
    }

    @Test
    fun aDaylightSavingChangeMovesTheMidnightInUtc() {
        // London springs forward at 01:00Z on 2026-03-29, so the next local
        // midnight is 23:00Z, an hour earlier than on a standard-time day.
        val now = Instant.parse("2026-03-29T12:00:00Z")

        assertEquals(
            Instant.parse("2026-03-29T23:00:00Z"),
            nextLocalMidnight(now, ZoneId.of("Europe/London")),
        )
    }

    @Test
    fun theDeviceZoneDoesNotDecideTheDay() {
        // 20:00Z is already the next day in Dhaka.
        val now = Instant.parse("2026-10-20T20:00:00Z")

        assertEquals(
            Instant.parse("2026-10-21T18:00:00Z"),
            nextLocalMidnight(now, ZoneId.of("Asia/Dhaka")),
        )
    }
}
