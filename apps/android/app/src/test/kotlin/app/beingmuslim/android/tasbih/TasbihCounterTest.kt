package app.beingmuslim.android.tasbih

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * The tasbih counter's transitions: what one more count, a reset, and a changed
 * target do to the label, the target and the count
 * (`docs/PRODUCT_REQUIREMENTS.md` §2.1). The app supplies no dhikr text; every
 * label here is typed by the user.
 */
class TasbihCounterTest {

    @Test
    fun countingStopsAtTheTarget() {
        val counted = TasbihCounter(label = "evening", target = 2).counted().counted()

        assertEquals(2, counted.count)
        assertEquals(counted, counted.counted())
    }

    @Test
    fun resetReturnsToZeroAndKeepsTheLabelAndTheTarget() {
        val counted = TasbihCounter(label = "evening", target = 33).counted().counted().counted()

        val reset = counted.reset()

        assertEquals(0, reset.count)
        assertEquals("evening", reset.label)
        assertEquals(33, reset.target)
    }

    @Test
    fun aTargetThatIsNotACountIsTreatedAsNotSet() {
        val named = TasbihCounter(label = "evening")

        assertNull(named.withTarget(0).target)
        assertNull(named.withTarget(-3).target)
        assertFalse(named.withTarget(0).canCount)
    }

    @Test
    fun loweringTheTargetBelowTheCountKeepsWhatHasBeenCounted() {
        val counted = TasbihCounter(label = "evening", target = 5).counted().counted().counted()

        val lowered = counted.withTarget(2)

        assertEquals(2, lowered.target)
        assertEquals(3, lowered.count)
        assertFalse(lowered.canCount)
    }

    @Test
    fun clearingTheTargetKeepsWhatHasBeenCounted() {
        val counted = TasbihCounter(label = "evening", target = 5).counted().counted()

        val cleared = counted.withTarget(null)

        assertNull(cleared.target)
        assertEquals(2, cleared.count)
        assertFalse(cleared.canCount)
    }

    @Test
    fun retypingTheTargetDoesNotUndoTheCounting() {
        var counted = TasbihCounter(label = "evening", target = 100)
        repeat(50) { counted = counted.counted() }

        // Replacing the target means clearing the field and typing a new one,
        // which passes through every prefix on the way.
        counted = counted.withTarget(null)
        counted = counted.withTarget(1)
        counted = counted.withTarget(15)
        counted = counted.withTarget(150)

        assertEquals(150, counted.target)
        assertEquals(50, counted.count)
    }

    @Test
    fun countingDoesNothingUntilTheSessionIsNamedAndGivenATarget() {
        val unnamed = TasbihCounter(target = 5)
        val untargeted = TasbihCounter(label = "evening")
        val blanklyNamed = TasbihCounter(label = "  ", target = 5)

        assertEquals(unnamed, unnamed.counted())
        assertEquals(untargeted, untargeted.counted())
        assertEquals(blanklyNamed, blanklyNamed.counted())
    }
}
