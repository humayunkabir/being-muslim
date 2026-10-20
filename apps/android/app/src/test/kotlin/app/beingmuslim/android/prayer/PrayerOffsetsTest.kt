package app.beingmuslim.android.prayer

import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The user's own per-prayer adjustments (issue #8): off by default, per prayer,
 * bounded, and removable.
 */
class PrayerOffsetsTest {

    @Test
    fun noPrayerIsAdjustedByDefault() {
        val offsets = PrayerOffsets()

        Prayer.entries.forEach { prayer ->
            assertEquals("$prayer has no adjustment", 0, offsets[prayer])
        }
        assertTrue(offsets.isEmpty)
    }

    @Test
    fun anAdjustmentTouchesOnlyItsPrayer() {
        val offsets = PrayerOffsets().with(Prayer.FAJR, 5)

        assertEquals(5, offsets[Prayer.FAJR])
        assertEquals(0, offsets[Prayer.DHUHR])
        assertEquals(0, offsets[Prayer.ASR])
        assertEquals(0, offsets[Prayer.MAGHRIB])
        assertEquals(0, offsets[Prayer.ISHA])
    }

    @Test
    fun settingAPrayerBackToZeroRemovesItsAdjustment() {
        val offsets = PrayerOffsets().with(Prayer.ISHA, -3).with(Prayer.ISHA, 0)

        assertEquals(0, offsets[Prayer.ISHA])
        assertTrue(offsets.isEmpty)
    }

    @Test
    fun anAdjustmentBeyondTheSupportedRangeIsClamped() {
        assertEquals(PrayerOffsets.MAX_MINUTES, PrayerOffsets().with(Prayer.DHUHR, 500)[Prayer.DHUHR])
        assertEquals(-PrayerOffsets.MAX_MINUTES, PrayerOffsets().with(Prayer.DHUHR, -500)[Prayer.DHUHR])
    }

    @Test
    fun anAdjustmentAtTheBoundIsKept() {
        val offsets = PrayerOffsets()
            .with(Prayer.ASR, PrayerOffsets.MAX_MINUTES)
            .with(Prayer.MAGHRIB, -PrayerOffsets.MAX_MINUTES)

        assertEquals(PrayerOffsets.MAX_MINUTES, offsets[Prayer.ASR])
        assertEquals(-PrayerOffsets.MAX_MINUTES, offsets[Prayer.MAGHRIB])
        assertFalse(offsets.isEmpty)
    }

    @Test
    fun anAdjustmentMovesThatPrayersTimeByItsMinutes() {
        val time = Instant.parse("2026-10-20T05:00:00Z")
        val offsets = PrayerOffsets().with(Prayer.FAJR, 5).with(Prayer.ISHA, -3)

        assertEquals(Instant.parse("2026-10-20T05:05:00Z"), offsets.adjusted(Prayer.FAJR, time))
        assertEquals(Instant.parse("2026-10-20T04:57:00Z"), offsets.adjusted(Prayer.ISHA, time))
        assertEquals(time, offsets.adjusted(Prayer.DHUHR, time))
    }
}
