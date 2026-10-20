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

        assertEquals(1, catalog.all().size)
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

        assertEquals(2, catalog.all().size)
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
    fun aPrefixMatchRankedLateStillOutranksEarlierSubstringMatches() {
        // Every city but the last only *contains* "ha"; the prefix match sits
        // last, past twice the limit, and must still come first.
        val dhakaHa = "1185241\tDhaka\t\t23.7\t90.4\tBD\tAsia/Dhaka"
        val bhairab = "1209000\tBhairab\t\t24.05\t90.98\tBD\tAsia/Dhaka"
        val thakurgaon = "1336144\tThakurgaon\t\t26.03\t88.47\tBD\tAsia/Dhaka"
        val shahjadpur = "1337176\tShahjadpur\t\t24.13\t89.60\tBD\tAsia/Dhaka"
        val hazaribagh = "1340000\tHazaribagh\t\t23.74\t90.37\tBD\tAsia/Dhaka"

        val catalog = catalogOf(dhakaHa, bhairab, thakurgaon, shahjadpur, hazaribagh)

        assertEquals(
            listOf("Hazaribagh", "Dhaka"),
            catalog.search("ha", limit = 2).map { it.name },
        )
    }

    @Test
    fun resolvesCoordinatesToTheClosestCityWhateverTheCatalogOrder() {
        // A device fix between Dhaka and Barishal: Barishal is nearer and sits
        // later in the catalog, so catalog order must not decide the answer.
        val catalog = catalogOf(dhaka, chattogram, barishal)

        assertEquals("Dhaka", catalog.nearest(23.7104, 90.40744)!!.name)
        assertEquals("Barishal", catalog.nearest(22.9, 90.4)!!.name)
    }

    @Test
    fun anEmptyCatalogHasNoNearestCity() {
        assertNull(catalogOf().nearest(23.7104, 90.40744))
    }

    @Test
    fun aPositionThatIsNotFiniteHasNoNearestCity() {
        assertNull(catalogOf(dhaka).nearest(Double.NaN, 90.40744))
        assertNull(catalogOf(dhaka).nearest(23.7104, Double.POSITIVE_INFINITY))
    }

    @Test
    fun resolvesCoordinatesAcrossTheDateLine() {
        // Suva sits just west of the date line (178.44E), Apia just east of it
        // (-171.75, that is 188.25E). A fix at -179.5 is ~1.07° from Suva but
        // ~8.8° from Apia, so a straight subtraction of longitudes gets it wrong.
        val suva = "2198148\tSuva\t\t-18.1416\t178.4419\tFJ\tPacific/Fiji"
        val apia = "4035413\tApia\t\t-13.8333\t-171.75\tWS\tPacific/Apia"

        assertEquals("Suva", catalogOf(suva, apia).nearest(-18.0, -179.5)!!.name)
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsARowWithTheWrongNumberOfColumns() {
        CityCatalog.parse(sequenceOf("1185241\tDhaka\tঢাকা\t23.7104\t90.40744\tBD"))
    }
}
