package app.beingmuslim.android.citycatalog

/**
 * A city in the bundled offline [CityCatalog] — the place prayer-time and qibla
 * calculations are performed for (`GLOSSARY.md`, *City catalog*).
 *
 * @param id the stable GeoNames identifier, used to persist a selection.
 * @param name the city's English (ASCII) name.
 * @param bengaliName the Bengali name where the catalog records one, else null.
 * @param latitude coordinates in decimal degrees, north and east positive.
 * @param longitude coordinates in decimal degrees, north and east positive.
 * @param countryCode the ISO 3166-1 alpha-2 country code.
 * @param timezone the IANA time-zone identifier, e.g. `Asia/Dhaka`. All
 *   calculations derive daylight-saving behaviour from it rather than from a
 *   fixed offset.
 */
data class City(
    val id: Long,
    val name: String,
    val bengaliName: String?,
    val latitude: Double,
    val longitude: Double,
    val countryCode: String,
    val timezone: String,
)
