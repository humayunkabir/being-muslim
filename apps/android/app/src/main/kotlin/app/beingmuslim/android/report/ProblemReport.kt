package app.beingmuslim.android.report

import app.beingmuslim.android.BuildConfig
import app.beingmuslim.android.citycatalog.City
import app.beingmuslim.android.prayer.CalculationSettings
import app.beingmuslim.android.prayer.DailyPrayerTimes
import app.beingmuslim.android.prayer.Prayer
import app.beingmuslim.android.prayer.PrayerOffsets
import app.beingmuslim.android.prayer.TwoDayPrayerTimes
import java.time.LocalDate
import org.json.JSONArray
import org.json.JSONObject

/**
 * One user-initiated problem report about a calculated time
 * (`docs/PRODUCT_REQUIREMENTS.md` §2.1, §5). It holds the inputs that reproduce
 * the result — city and coordinates, method and its parameters, the juristic and
 * high-latitude choices, the user's offsets, the date the report is about, the
 * dates and the raw times — plus the user's own note. It deliberately holds
 * nothing else: no account, device, or location history (`docs/PRIVACY.md`).
 */
data class ProblemReport(
    val appVersion: String,
    val city: City,
    val settings: CalculationSettings,
    val offsets: PrayerOffsets,
    val hijriDayShift: Int,
    val date: LocalDate,
    val days: TwoDayPrayerTimes,
    val note: String = "",
) {

    /** This report with the note the user typed, bounded to what the report carries. */
    fun withNote(note: String): ProblemReport = copy(note = note.take(MAX_NOTE_LENGTH))

    companion object {
        /** The most a note may carry; the server enforces the same bound. */
        const val MAX_NOTE_LENGTH = 2000
    }
}

/**
 * The JSON the report is sent as, versioned so a future shape can be told apart
 * from this one. The server accepts exactly these keys (`docs/APPWRITE.md`), so a
 * field added here without that agreement is rejected rather than stored.
 *
 * Times are absolute instants, so daylight saving and the city's zone are
 * already resolved; the inputs that produced them travel alongside.
 */
object ProblemReportCodec {

    const val VERSION = 1

    fun encode(report: ProblemReport): String = JSONObject().apply {
        put("version", VERSION)
        put("appVersion", report.appVersion)
        // The calculation engine's version travels too: a fix in the library can
        // change a time without the app version changing.
        put("library", "adhan ${BuildConfig.ADHAN_VERSION}")
        put("date", report.date.toString())
        put("city", JSONObject().apply {
            put("name", report.city.name)
            put("countryCode", report.city.countryCode)
            put("latitude", report.city.latitude)
            put("longitude", report.city.longitude)
            put("timezone", report.city.timezone)
        })

        val parameters = report.settings.method.adhan.parameters
        put("method", report.settings.method.name)
        put("parameters", JSONObject().apply {
            put("fajrAngle", parameters.fajrAngle)
            put("ishaAngle", parameters.ishaAngle)
            put("ishaInterval", parameters.ishaInterval)
            put("adjustments", JSONObject().apply {
                put("fajr", parameters.methodAdjustments.fajr)
                put("sunrise", parameters.methodAdjustments.sunrise)
                put("dhuhr", parameters.methodAdjustments.dhuhr)
                put("asr", parameters.methodAdjustments.asr)
                put("maghrib", parameters.methodAdjustments.maghrib)
                put("isha", parameters.methodAdjustments.isha)
            })
        })
        put("asrJuristicMethod", report.settings.asrJuristicMethod.name)
        put("highLatitudeRule", report.settings.highLatitudeRule.name)
        put("hijriDayShift", report.hijriDayShift)
        put("offsets", JSONObject().apply {
            Prayer.entries.forEach { put(it.name, report.offsets[it]) }
        })
        put("days", JSONArray().apply {
            // A polar day has no times and no reportable time; the other day,
            // when it exists, still carries its own date and times.
            listOfNotNull(report.days.today, report.days.tomorrow).forEach { put(dayJson(it)) }
        })
        put("note", report.note)
    }.toString()

    private fun dayJson(day: DailyPrayerTimes): JSONObject = JSONObject().apply {
        put("date", day.date.toString())
        put("times", JSONObject().apply {
            Prayer.entries.forEach { put(it.name, day[it].toString()) }
        })
        put(
            "highLatitudeApplied",
            JSONArray(Prayer.entries.filter { it in day.boundedByHighLatitudeRule }.map { it.name }),
        )
    }
}
