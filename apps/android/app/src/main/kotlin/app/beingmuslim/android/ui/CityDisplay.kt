package app.beingmuslim.android.ui

import app.beingmuslim.android.citycatalog.City
import java.util.Locale

/**
 * The name to show for a city in the current UI language: the Bengali name where
 * the catalog records one and the app is in Bengali, otherwise the English name.
 *
 * City names are catalog data — which is why they are bundled in both scripts —
 * not translatable UI strings, so they deliberately do not come from
 * `strings.xml`.
 */
fun City.displayName(language: String = Locale.getDefault().language): String =
    if (language == "bn" && !bengaliName.isNullOrBlank()) bengaliName else name
