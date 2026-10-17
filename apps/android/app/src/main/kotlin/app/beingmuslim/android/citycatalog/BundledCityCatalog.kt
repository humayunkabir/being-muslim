package app.beingmuslim.android.citycatalog

import android.content.res.AssetManager

/**
 * Loads the [CityCatalog] bundled in the app's assets. The catalog is read
 * straight from the APK with no network and no permission (`GLOSSARY.md`, *City
 * catalog*; `docs/CITY_CATALOG.md`).
 *
 * This is the thin platform adapter around [CityCatalog.parse]: it only opens
 * the asset. Reading and parsing is proportional to the catalog's size, so call
 * [load] off the main thread.
 */
object BundledCityCatalog {

    /** The catalog asset, produced by `tools/build-city-catalog.py`. */
    const val ASSET_NAME = "city_catalog.tsv"

    fun load(assets: AssetManager): CityCatalog =
        assets.open(ASSET_NAME).bufferedReader(Charsets.UTF_8).use { reader ->
            CityCatalog.parse(reader.lineSequence())
        }
}
