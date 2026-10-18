package app.beingmuslim.android.tasbih

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * The persisted form of the tasbih counter — what must survive a restart.
 * Decoding is deliberately lenient, so a value written by an older build or
 * damaged on disk loses what cannot be read rather than the feature.
 */
class TasbihCodecTest {

    private val counter = TasbihCounter(label = "evening", target = 33).counted().counted()

    @Test
    fun roundTripsACounter() {
        val restored = TasbihCodec.decode(TasbihCodec.encode(counter))

        assertEquals(counter, restored)
    }

    @Test
    fun nothingStoredYetIsAnEmptyCounter() {
        assertEquals(TasbihCounter(), TasbihCodec.decode(emptyMap()))
    }

    @Test
    fun unreadableValuesFallBackToTheEmptyCounter() {
        val stored = mapOf(
            TasbihCodec.TARGET to "not-a-number",
            TasbihCodec.COUNT to "-4",
        )

        val restored = TasbihCodec.decode(stored)

        assertNull(restored.target)
        assertEquals(0, restored.count)
    }

    @Test
    fun aStoredCountBeyondItsTargetIsBroughtBackWithinIt() {
        val stored = mapOf(
            TasbihCodec.LABEL to "evening",
            TasbihCodec.TARGET to "5",
            TasbihCodec.COUNT to "9000",
        )

        val restored = TasbihCodec.decode(stored)

        assertEquals(5, restored.target)
        assertEquals(5, restored.count)
    }
}
