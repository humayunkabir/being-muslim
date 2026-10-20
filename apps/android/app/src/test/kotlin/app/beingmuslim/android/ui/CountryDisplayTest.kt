package app.beingmuslim.android.ui

import org.junit.Assert.assertEquals
import org.junit.Test

/** Country names come from the platform locale, so they follow the app language. */
class CountryDisplayTest {

    @Test
    fun namesACommonCountryInEnglish() {
        assertEquals("Bangladesh", countryName("BD", Language.ENGLISH))
    }

    @Test
    fun namesACommonCountryInBengali() {
        assertEquals("বাংলাদেশ", countryName("BD", Language.BENGALI))
    }

    @Test
    fun fallsBackToTheCodeWhenThePlatformHasNoName() {
        assertEquals("XX", countryName("XX", Language.ENGLISH))
    }
}
