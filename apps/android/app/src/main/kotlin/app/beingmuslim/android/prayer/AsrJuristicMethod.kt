package app.beingmuslim.android.prayer

import com.batoulapps.adhan.Madhab

/**
 * The juristic method used for the Asr calculation (`GLOSSARY.md`, *Juristic
 * method*). A user-chosen setting that is never changed silently.
 *
 * @param adhan the Adhan madhab this choice maps to.
 */
enum class AsrJuristicMethod(val adhan: Madhab) {
    /** The majority (Shafi'i, Maliki, Hanbali) convention: shadow length 1×. */
    STANDARD(Madhab.SHAFI),

    /** The Hanafi convention: shadow length 2×, which places Asr later. */
    HANAFI(Madhab.HANAFI),
}
