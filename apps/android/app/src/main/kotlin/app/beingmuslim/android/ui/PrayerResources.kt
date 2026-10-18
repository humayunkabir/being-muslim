package app.beingmuslim.android.ui

import androidx.annotation.StringRes
import app.beingmuslim.android.R
import app.beingmuslim.android.prayer.AsrJuristicMethod
import app.beingmuslim.android.prayer.CalculationMethodChoice
import app.beingmuslim.android.prayer.HighLatitudeRuleChoice
import app.beingmuslim.android.prayer.Prayer

/**
 * Maps domain choices to their user-facing string resources. The domain stays
 * free of strings (§5, §8 in `docs/PRODUCT_REQUIREMENTS.md`); every label and
 * explanation is externalized here for translation.
 */

@StringRes
fun CalculationMethodChoice.labelRes(): Int = when (this) {
    CalculationMethodChoice.MUSLIM_WORLD_LEAGUE -> R.string.method_muslim_world_league
    CalculationMethodChoice.KARACHI -> R.string.method_karachi
    CalculationMethodChoice.UMM_AL_QURA -> R.string.method_umm_al_qura
    CalculationMethodChoice.EGYPTIAN -> R.string.method_egyptian
    CalculationMethodChoice.MOON_SIGHTING_COMMITTEE -> R.string.method_moonsighting_committee
    CalculationMethodChoice.DUBAI -> R.string.method_dubai
    CalculationMethodChoice.KUWAIT -> R.string.method_kuwait
    CalculationMethodChoice.QATAR -> R.string.method_qatar
    CalculationMethodChoice.SINGAPORE -> R.string.method_singapore
    CalculationMethodChoice.NORTH_AMERICA -> R.string.method_north_america
}

@StringRes
fun CalculationMethodChoice.descriptionRes(): Int = when (this) {
    CalculationMethodChoice.MUSLIM_WORLD_LEAGUE -> R.string.method_muslim_world_league_description
    CalculationMethodChoice.KARACHI -> R.string.method_karachi_description
    CalculationMethodChoice.UMM_AL_QURA -> R.string.method_umm_al_qura_description
    CalculationMethodChoice.EGYPTIAN -> R.string.method_egyptian_description
    CalculationMethodChoice.MOON_SIGHTING_COMMITTEE -> R.string.method_moonsighting_committee_description
    CalculationMethodChoice.DUBAI -> R.string.method_dubai_description
    CalculationMethodChoice.KUWAIT -> R.string.method_kuwait_description
    CalculationMethodChoice.QATAR -> R.string.method_qatar_description
    CalculationMethodChoice.SINGAPORE -> R.string.method_singapore_description
    CalculationMethodChoice.NORTH_AMERICA -> R.string.method_north_america_description
}

@StringRes
fun AsrJuristicMethod.labelRes(): Int = when (this) {
    AsrJuristicMethod.STANDARD -> R.string.asr_standard
    AsrJuristicMethod.HANAFI -> R.string.asr_hanafi
}

@StringRes
fun AsrJuristicMethod.descriptionRes(): Int = when (this) {
    AsrJuristicMethod.STANDARD -> R.string.asr_standard_description
    AsrJuristicMethod.HANAFI -> R.string.asr_hanafi_description
}

@StringRes
fun HighLatitudeRuleChoice.labelRes(): Int = when (this) {
    HighLatitudeRuleChoice.MIDDLE_OF_THE_NIGHT -> R.string.high_latitude_rule_middle_of_the_night
    HighLatitudeRuleChoice.SEVENTH_OF_THE_NIGHT -> R.string.high_latitude_rule_seventh_of_the_night
    HighLatitudeRuleChoice.TWILIGHT_ANGLE -> R.string.high_latitude_rule_twilight_angle
}

@StringRes
fun HighLatitudeRuleChoice.descriptionRes(): Int = when (this) {
    HighLatitudeRuleChoice.MIDDLE_OF_THE_NIGHT -> R.string.high_latitude_rule_middle_of_the_night_description
    HighLatitudeRuleChoice.SEVENTH_OF_THE_NIGHT -> R.string.high_latitude_rule_seventh_of_the_night_description
    HighLatitudeRuleChoice.TWILIGHT_ANGLE -> R.string.high_latitude_rule_twilight_angle_description
}

/**
 * The times screen's disclosure that the high-latitude rule, not true dawn or
 * dusk, produced a day's Fajr or Isha — or null when it produced neither and
 * there is nothing to disclose (issue #7).
 */
@StringRes
fun Set<Prayer>.highLatitudeRuleAppliedRes(): Int? = when {
    Prayer.FAJR in this && Prayer.ISHA in this -> R.string.times_high_latitude_applied_to_both
    Prayer.FAJR in this -> R.string.times_high_latitude_applied_to_fajr
    Prayer.ISHA in this -> R.string.times_high_latitude_applied_to_isha
    else -> null
}

@StringRes
fun Prayer.labelRes(): Int = when (this) {
    Prayer.FAJR -> R.string.prayer_fajr
    Prayer.DHUHR -> R.string.prayer_dhuhr
    Prayer.ASR -> R.string.prayer_asr
    Prayer.MAGHRIB -> R.string.prayer_maghrib
    Prayer.ISHA -> R.string.prayer_isha
}

/**
 * The prayer's Arabic name, shown beside the localized one. Not translated, and
 * the same in both locales (issue #14).
 */
@StringRes
fun Prayer.arabicRes(): Int = when (this) {
    Prayer.FAJR -> R.string.prayer_fajr_arabic
    Prayer.DHUHR -> R.string.prayer_dhuhr_arabic
    Prayer.ASR -> R.string.prayer_asr_arabic
    Prayer.MAGHRIB -> R.string.prayer_maghrib_arabic
    Prayer.ISHA -> R.string.prayer_isha_arabic
}
