package app.beingmuslim.android.tasbih

/**
 * Encodes and decodes a [TasbihCounter] to and from flat string key/values, the
 * same shape as [app.beingmuslim.android.selection.UserSelectionCodec]. Pure, so
 * the persisted form and its handling of older or damaged values are covered by
 * ordinary JVM unit tests.
 *
 * Decoding is lenient: anything unreadable falls back to the default counter, so
 * a value this build cannot read loses the count rather than the feature.
 */
object TasbihCodec {

    /** Preference keys. Stable — changing one silently discards a stored count. */
    const val LABEL = "label"
    const val TARGET = "target"
    const val COUNT = "count"

    /** Every key [decode] reads, for a caller assembling the stored values. */
    val KEYS = listOf(LABEL, TARGET, COUNT)

    fun encode(counter: TasbihCounter): Map<String, String> = mapOf(
        LABEL to counter.label,
        TARGET to counter.target?.toString().orEmpty(),
        COUNT to counter.count.toString(),
    )

    fun decode(values: Map<String, String?>): TasbihCounter =
        TasbihCounter(
            label = values[LABEL].orEmpty(),
            count = values[COUNT]?.toIntOrNull()?.coerceAtLeast(0) ?: 0,
        ).withTarget(values[TARGET]?.toIntOrNull())
}
