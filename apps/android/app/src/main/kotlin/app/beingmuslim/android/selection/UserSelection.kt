package app.beingmuslim.android.selection

import app.beingmuslim.android.prayer.CalculationSettings

/**
 * The user's first-run choices: the city to calculate for and the calculation
 * settings to calculate with. Persisted so they survive a restart, and never
 * changed without the user changing them (`docs/PRODUCT_REQUIREMENTS.md` §5).
 */
data class UserSelection(
    val cityId: Long,
    val settings: CalculationSettings,
)
