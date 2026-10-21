package app.beingmuslim.android.sync

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** How a stored row's columns are read by both sync and export (`docs/APPWRITE.md`). */
class SyncRowTest {

    private val settings = """{"v":1,"language":"bn"}"""

    @Test
    fun aStoredRowMapsItsFields() {
        assertEquals(
            StoredRow(settings, 42),
            storedRow(mapOf("settings" to settings, "tasbihTotal" to 42)),
        )
    }

    @Test
    fun aMissingRowHasNothingToRead() {
        assertNull(storedRow(null))
    }

    @Test
    fun missingOrNegativeFieldsFallBackToTheColumnDefaults() {
        val missing = storedRow(emptyMap())
        val negative = storedRow(mapOf("settings" to settings, "tasbihTotal" to -5))

        assertNull(missing?.settings)
        assertEquals(0, missing?.tasbihTotal)
        assertEquals(0, negative?.tasbihTotal)
    }

    @Test
    fun aSettingsValueThatIsNotTextIsDropped() {
        assertNull(storedRow(mapOf("settings" to 7))?.settings)
    }
}
