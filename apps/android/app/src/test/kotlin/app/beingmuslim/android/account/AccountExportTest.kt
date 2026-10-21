package app.beingmuslim.android.account

import app.beingmuslim.android.auth.AuthUser
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The export document's shape, as documented in `docs/SYNC.md` and explained to
 * the user in `docs/PRIVACY.md`: self-describing, versioned, and read-only.
 */
class AccountExportTest {

    private val user = AuthUser("user-1", "Amina", "amina@example.com")

    private val settings =
        """{"v":1,"calculationMethod":"KARACHI","asrJuristicMethod":"HANAFI",""" +
            """"highLatitudeRule":"SEVENTH_OF_THE_NIGHT","hijriDayShift":1,""" +
            """"offsets":{"FAJR":5,"DHUHR":0,"ASR":0,"MAGHRIB":0,"ISHA":-3},"language":"bn"}"""

    @Test
    fun theDocumentSaysWhatItIsAndWhatVersionItIs() {
        val export = JSONObject(AccountExport.document(user, StoredAccountData(settings, 42)))

        assertEquals("being-muslim-account", export.getString("format"))
        assertEquals(1, export.getInt("version"))
        assertEquals(
            setOf("format", "version", "account", "stored"),
            export.keys().asSequence().toSet(),
        )
    }

    @Test
    fun theAccountIsIdentifiedByItsOwnFields() {
        val account = JSONObject(AccountExport.document(user, null)).getJSONObject("account")

        assertEquals("user-1", account.getString("id"))
        assertEquals("Amina", account.getString("name"))
        assertEquals("amina@example.com", account.getString("email"))
    }

    @Test
    fun theStoredSettingsStayReadableObjects() {
        val stored = JSONObject(AccountExport.document(user, StoredAccountData(settings, 42)))
            .getJSONObject("stored")

        assertEquals("KARACHI", stored.getJSONObject("settings").getString("calculationMethod"))
        assertEquals(42, stored.getInt("tasbihTotal"))
    }

    @Test
    fun noRowExportsAnEmptyStoredSection() {
        val export = JSONObject(AccountExport.document(user, null))

        assertTrue(export.isNull("stored"))
    }

    @Test
    fun aRowWithoutSettingsStillExportsTheTotal() {
        val stored = JSONObject(AccountExport.document(user, StoredAccountData(null, 7)))
            .getJSONObject("stored")

        assertTrue(stored.isNull("settings"))
        assertEquals(7, stored.getInt("tasbihTotal"))
    }

    @Test
    fun anUnreadableSettingsValueIsKeptRatherThanDropped() {
        val stored = JSONObject(AccountExport.document(user, StoredAccountData("not json", 0)))
            .getJSONObject("stored")

        assertEquals("not json", stored.getString("settings"))
    }

    @Test
    fun theExportCarriesNoDeviceLocalData() {
        val export = JSONObject(AccountExport.document(user, StoredAccountData(settings, 42)))

        assertEquals(
            setOf("format", "version", "account", "stored"),
            export.keys().asSequence().toSet(),
        )
        assertEquals(
            setOf("id", "name", "email"),
            export.getJSONObject("account").keys().asSequence().toSet(),
        )
        assertEquals(
            setOf("settings", "tasbihTotal"),
            export.getJSONObject("stored").keys().asSequence().toSet(),
        )
    }

    @Test
    fun aStoredRowMapsItsFields() {
        assertEquals(
            StoredAccountData(settings, 42),
            storedAccountData(mapOf("settings" to settings, "tasbihTotal" to 42)),
        )
    }

    @Test
    fun aMissingRowHasNothingToExport() {
        assertNull(storedAccountData(null))
    }

    @Test
    fun missingOrNegativeFieldsFallBackToTheColumnDefaults() {
        val missing = storedAccountData(emptyMap())
        val negative = storedAccountData(mapOf("settings" to settings, "tasbihTotal" to -5))

        assertNull(missing?.settings)
        assertEquals(0, missing?.tasbihTotal)
        assertEquals(0, negative?.tasbihTotal)
    }

    @Test
    fun aSettingsValueThatIsNotTextIsDropped() {
        assertNull(storedAccountData(mapOf("settings" to 7))?.settings)
    }
}
