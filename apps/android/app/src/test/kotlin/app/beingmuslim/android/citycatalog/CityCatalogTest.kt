package app.beingmuslim.android.citycatalog

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Behaviour of the bundled [CityCatalog]: parsing its tab-separated form and
 * looking cities up by name in English and Bengali (`GLOSSARY.md`, *City
 * catalog*). The real asset's integrity is checked separately by
 * [BundledCityCatalogAssetTest]; these cases use a small inline fixture.
 */
class CityCatalogTest {

    private val header = "# id\tname\tbengali_name\tlatitude\tlongitude\tcountry_code\ttimezone"
    private val dhaka = "1185241\tDhaka\tঢাকা\t23.7104\t90.40744\tBD\tAsia/Dhaka"
    private val chattogram = "1205733\tChattogram\tচট্টগ্রাম\t22.3384\t91.83168\tBD\tAsia/Dhaka"
    private val barishal = "1336135\tBarishal\tবরিশাল\t22.7\t90.37\tBD\tAsia/Dhaka"
    private val hala = "1177153\tHala\t\t25.81\t68.32\tPK\tAsia/Karachi"
    private val seoul = "1835848\tSeoul\t\t37.566\t126.9784\tKR\tAsia/Seoul"

    private fun catalogOf(vararg rows: String) =
        CityCatalog.parse((listOf(header) + rows).asSequence())

    @Test
    fun parsesCityCoordinatesAndTimezone() {
        val catalog = catalogOf(dhaka)

        assertEquals(1, catalog.size())
        assertEquals(
            City(
                id = 1185241L,
                name = "Dhaka",
                bengaliName = "ঢাকা",
                latitude = 23.7104,
                longitude = 90.40744,
                countryCode = "BD",
                timezone = "Asia/Dhaka",
            ),
            catalog.byId(1185241L),
        )
    }

    @Test
    fun aCityWithoutABengaliNameHasNone() {
        assertNull(catalogOf(seoul).byId(1835848L)!!.bengaliName)
    }

    @Test
    fun ignoresTheHeaderAndBlankLines() {
        val catalog = CityCatalog.parse(sequenceOf(header, "", dhaka, "   ", seoul))

        assertEquals(2, catalog.size())
    }

    @Test
    fun anUnknownIdHasNoCity() {
        assertNull(catalogOf(dhaka).byId(999L))
    }

    @Test
    fun findsACityByItsEnglishNameRegardlessOfCase() {
        val catalog = catalogOf(dhaka, chattogram, seoul)

        assertEquals(listOf("Dhaka"), catalog.search("dHaKa").map { it.name })
    }

    @Test
    fun findsACityByItsBengaliName() {
        val catalog = catalogOf(dhaka, chattogram)

        assertEquals(listOf("Chattogram"), catalog.search("চট্ট").map { it.name })
    }

    @Test
    fun ranksNamesThatStartWithTheQueryAheadOfNamesThatOnlyContainIt() {
        // Barishal appears first in the catalog but only contains "hal";
        // Hala starts with it and must rank first.
        val catalog = catalogOf(barishal, hala)

        assertEquals(listOf("Hala", "Barishal"), catalog.search("hal").map { it.name })
    }

    @Test
    fun aBlankQueryMatchesNothing() {
        assertEquals(emptyList<City>(), catalogOf(dhaka).search("   "))
    }

    @Test
    fun reportsTheCountriesPresentInTheCatalog() {
        assertEquals(listOf("BD", "KR"), catalogOf(dhaka, chattogram, seoul).countries())
    }

    @Test
    fun listsTheCitiesOfACountryInCatalogOrder() {
        val catalog = catalogOf(dhaka, seoul, chattogram)

        assertEquals(listOf("Dhaka", "Chattogram"), catalog.citiesIn("BD").map { it.name })
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsARowWithTheWrongNumberOfColumns() {
        CityCatalog.parse(sequenceOf("1185241\tDhaka\tঢাকা\t23.7104\t90.40744\tBD"))
    }
}
