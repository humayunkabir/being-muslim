package app.beingmuslim.android.ui

import app.beingmuslim.android.citycatalog.City

/**
 * The name to show for a city in the app's current language: the Bengali name
 * where the catalog records one and the app is in Bengali, otherwise the
 * English name.
 *
 * City names are catalog data — which is why they are bundled in both scripts —
 * not translatable UI strings, so they deliberately do not come from
 * `strings.xml`. The language is passed in rather than read from
 * `Locale.getDefault()`, which follows the device, not the app (issue #14).
 */
fun City.displayName(language: Language): String =
    if (language == Language.BENGALI && !bengaliName.isNullOrBlank()) bengaliName else name
