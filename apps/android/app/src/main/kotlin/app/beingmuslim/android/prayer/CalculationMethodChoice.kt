package app.beingmuslim.android.prayer

import com.batoulapps.adhan.CalculationMethod

/**
 * A calculation method the user can choose — the named set of parameters used
 * to compute prayer times (`GLOSSARY.md`, *Calculation method*). The listing is
 * the subset of Adhan's methods the product offers; the angles and adjustments
 * each one carries are recorded in `docs/PRAYER_TIMES.md`.
 *
 * @param adhan the Adhan method this choice maps to.
 */
enum class CalculationMethodChoice(val adhan: CalculationMethod) {
    MUSLIM_WORLD_LEAGUE(CalculationMethod.MUSLIM_WORLD_LEAGUE),
    KARACHI(CalculationMethod.KARACHI),
    UMM_AL_QURA(CalculationMethod.UMM_AL_QURA),
    EGYPTIAN(CalculationMethod.EGYPTIAN),
    MOON_SIGHTING_COMMITTEE(CalculationMethod.MOON_SIGHTING_COMMITTEE),
    DUBAI(CalculationMethod.DUBAI),
    KUWAIT(CalculationMethod.KUWAIT),
    QATAR(CalculationMethod.QATAR),
    SINGAPORE(CalculationMethod.SINGAPORE),
    NORTH_AMERICA(CalculationMethod.NORTH_AMERICA);

    /**
     * Whether the method derives Fajr and Isha itself at high latitude instead of
     * using the user's [HighLatitudeRuleChoice] — true for the Moonsighting
     * Committee method, which applies its own seasonal twilight adjustment
     * (`docs/PRAYER_TIMES.md`). Where true, the times screen must say the rule was
     * not used (issue #7).
     */
    val usesItsOwnHighLatitudeRule: Boolean
        get() = this == MOON_SIGHTING_COMMITTEE
}
