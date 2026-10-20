package app.beingmuslim.android.citycatalog

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Integrity of the committed city-catalog asset, exercised through the real
 * [CityCatalog.parse]. These cases catch a corrupt, truncated, or accidentally
 * emptied asset that the inline-fixture [CityCatalogTest] would not notice.
 */
class BundledCityCatalogAssetTest {

    private val asset: File by lazy {
        val relative = "src/main/assets/${BundledCityCatalog.ASSET_NAME}"
        listOf(File(relative), File("app/$relative"))
            .firstOrNull(File::isFile)
            ?: error(
                "city catalog asset not found relative to " +
                    "'${File("").absolutePath}' — run tools/build-city-catalog.py",
            )
    }

    private val catalog: CityCatalog by lazy {
        asset.bufferedReader(Charsets.UTF_8).use { CityCatalog.parse(it.lineSequence()) }
    }

    @Test
    fun theCatalogLoadsTheFullBundledCityList() {
        assertTrue("expected a global catalog, got ${catalog.all().size}", catalog.all().size > 30_000)
    }

    @Test
    fun everyCityCarriesUsableCoordinatesAndAnIanaTimezone() {
        catalog.all().forEach { city ->
            assertEquals(
                "${city.name} has a timezone",
                true,
                city.timezone.contains('/'),
            )
            assertTrue("${city.name} latitude", city.latitude in -90.0..90.0)
            assertTrue("${city.name} longitude", city.longitude in -180.0..180.0)
            assertEquals("${city.name} country code", 2, city.countryCode.length)
        }
    }

    @Test
    fun resolvesADeviceFixToTheNearestRealCity() {
        // GeoNames' own coordinates for Dhaka: the nearest real city to a fix
        // right there, carrying the timezone the times are computed in.
        val nearest = catalog.nearest(23.7104, 90.40744)!!

        assertEquals("Dhaka", nearest.name)
        assertEquals("BD", nearest.countryCode)
        assertEquals("Asia/Dhaka", nearest.timezone)
    }

    @Test
    fun findsDhakaInBengaliAndEnglish() {
        val byEnglish = catalog.search("Dhaka").first()
        assertEquals("Dhaka", byEnglish.name)
        assertEquals("BD", byEnglish.countryCode)
        assertEquals("Asia/Dhaka", byEnglish.timezone)

        assertEquals(
            listOf("Dhaka"),
            catalog.search("ঢাকা").map { it.name },
        )
    }
}
