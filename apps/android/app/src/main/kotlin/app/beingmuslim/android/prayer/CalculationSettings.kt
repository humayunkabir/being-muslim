package app.beingmuslim.android.prayer

/**
 * The calculation choices that determine how prayer times are computed
 * (`docs/PRODUCT_REQUIREMENTS.md` §5). Both are explicit user choices and are
 * never changed silently.
 */
data class CalculationSettings(
    val method: CalculationMethodChoice,
    val asrJuristicMethod: AsrJuristicMethod,
)
