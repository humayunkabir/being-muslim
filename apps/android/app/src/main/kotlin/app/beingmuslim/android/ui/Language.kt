package app.beingmuslim.android.ui

import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import app.beingmuslim.android.R

/**
 * A language the app is published in. Bengali and English are the MVP's
 * supported locales (`docs/PRODUCT_REQUIREMENTS.md` §2, §8); anything else
 * falls back to English.
 *
 * [tag] must match an entry in `res/xml/locale_config.xml` and, unless it is
 * the app's default locale, a `values-<tag>/` directory.
 * `LocalizationResourcesTest` keeps the three in step.
 */
enum class Language(val tag: String) {
    ENGLISH("en"),
    BENGALI("bn"),
}

@StringRes
fun Language.labelRes(): Int = when (this) {
    Language.ENGLISH -> R.string.language_english
    Language.BENGALI -> R.string.language_bengali
}

/**
 * The language the app is showing: the user's in-app choice, or one they made
 * in system settings, or — before either — the device's own language.
 *
 * Read from the platform rather than from `Locale.getDefault()`, which follows
 * the device and not the app. A change restarts the activity, so this needs no
 * recomposition tracking of its own.
 */
fun currentLanguage(): Language =
    supportedLanguage(AppCompatDelegate.getApplicationLocales().toLanguageTags())
        ?: supportedLanguage(LocaleListCompat.getDefault().toLanguageTags())
        ?: Language.ENGLISH

/** Switches the app's language. The platform stores the choice and restarts the app. */
fun applyLanguage(language: Language) {
    AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(language.tag))
}

/** The first tag in [languageTags] this app publishes, preferring the user's order. */
private fun supportedLanguage(languageTags: String): Language? =
    languageTags.split(',').firstNotNullOfOrNull { tag ->
        val language = tag.substringBefore('-').lowercase()
        Language.entries.firstOrNull { it.tag == language }
    }
