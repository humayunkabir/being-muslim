package app.beingmuslim.android.prayer

import com.batoulapps.adhan.HighLatitudeRule

/**
 * The convention applied where a location's latitude leaves true dawn or dusk
 * undefined (`GLOSSARY.md`, *High-latitude rule*). A visible, user-chosen setting
 * that is never changed silently (`docs/PRODUCT_REQUIREMENTS.md` §5); what each
 * one bounds, and where it applies at all, is recorded in `docs/PRAYER_TIMES.md`.
 *
 * @param adhan the Adhan rule this choice maps to.
 */
enum class HighLatitudeRuleChoice(val adhan: HighLatitudeRule) {
    MIDDLE_OF_THE_NIGHT(HighLatitudeRule.MIDDLE_OF_THE_NIGHT),

    SEVENTH_OF_THE_NIGHT(HighLatitudeRule.SEVENTH_OF_THE_NIGHT),

    TWILIGHT_ANGLE(HighLatitudeRule.TWILIGHT_ANGLE),
}
