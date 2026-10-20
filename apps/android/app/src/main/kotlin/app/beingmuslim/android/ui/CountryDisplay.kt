package app.beingmuslim.android.ui

import java.util.Locale

/**
 * The country name for its ISO 3166-1 alpha-2 [code] in [language], from the
 * platform's locale data — the catalog stores codes only. An unknown region is
 * shown as its code rather than left blank.
 */
fun countryName(code: String, language: Language): String =
    Locale.Builder()
        .setRegion(code)
        .build()
        .getDisplayCountry(Locale.forLanguageTag(language.tag))
        .ifBlank { code }
