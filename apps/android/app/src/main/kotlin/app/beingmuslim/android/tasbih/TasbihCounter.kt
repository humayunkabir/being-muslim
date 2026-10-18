package app.beingmuslim.android.tasbih

/**
 * A counting session: the label the user typed, the target they set, and how far
 * they have counted. The app ships no dhikr corpus and attributes no text of its
 * own — the label is the user's own words (`GLOSSARY.md`, *Tasbih*;
 * `docs/PRODUCT_REQUIREMENTS.md` §2.1).
 *
 * [target] is null until the user sets one, because no target is chosen for
 * them. Changing it never changes what has been counted: a count is the user's
 * own progress, so lowering the target past it leaves [count] where it is and
 * simply puts the session past its target.
 */
data class TasbihCounter(
    val label: String = "",
    val target: Int? = null,
    val count: Int = 0,
) {

    /** True when the session is named, has a target, and has not reached it. */
    val canCount: Boolean
        get() = label.isNotBlank() && target != null && count < target

    /** One more count, or this same counter when there is nothing left to count. */
    fun counted(): TasbihCounter = if (canCount) copy(count = count + 1) else this

    /** Back to zero, keeping the label and the target for the next session. */
    fun reset(): TasbihCounter = copy(count = 0)

    /** Sets the target to [target], or clears it when that is not a count the user could reach. */
    fun withTarget(target: Int?): TasbihCounter = copy(target = target?.takeIf { it > 0 })
}
