package app.beingmuslim.android.prayer

import app.beingmuslim.android.citycatalog.City
import com.batoulapps.adhan.CalculationParameters
import com.batoulapps.adhan.Coordinates
import com.batoulapps.adhan.PrayerTimes
import com.batoulapps.adhan.data.CalendarUtil
import com.batoulapps.adhan.data.DateComponents
import com.batoulapps.adhan.data.TimeComponents
import com.batoulapps.adhan.internal.SolarTime
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.util.Calendar
import java.util.Date

/**
 * Computes the five daily prayer times for a [City] with BatoulApps *Adhan*
 * (§5 in `docs/PRODUCT_REQUIREMENTS.md`; library, version and parameter
 * recording in `docs/PRAYER_TIMES.md`).
 *
 * The domain holds no UI, location, notification, or networking imports: it
 * takes a city, a date or instant, and the user's [CalculationSettings], and
 * returns plain times. Nothing here reads the device clock — the current time
 * is always passed in.
 *
 * Times are unavailable when the sun neither rises nor sets — a polar day or
 * night — in which case [forDate] returns null rather than an invented time.
 */
object PrayerTimeCalculator {

    /**
     * The five times at [city] on the local [date], or null when they cannot be
     * computed for that date (polar day or night).
     */
    fun forDate(city: City, date: LocalDate, settings: CalculationSettings): DailyPrayerTimes? {
        val zone = ZoneId.of(city.timezone)
        val calculated = PrayerTimes(
            Coordinates(city.latitude, city.longitude),
            DateComponents(date.year, date.monthValue, date.dayOfMonth),
            parameters(settings),
        )

        val fajr = calculated.fajr ?: return null
        val dhuhr = calculated.dhuhr ?: return null
        val asr = calculated.asr ?: return null
        val maghrib = calculated.maghrib ?: return null
        val isha = calculated.isha ?: return null

        return DailyPrayerTimes(
            date = date,
            zone = zone,
            times = mapOf(
                Prayer.FAJR to fajr.asPrayerInstant(),
                Prayer.DHUHR to dhuhr.asPrayerInstant(),
                Prayer.ASR to asr.asPrayerInstant(),
                Prayer.MAGHRIB to maghrib.asPrayerInstant(),
                Prayer.ISHA to isha.asPrayerInstant(),
            ),
            boundedByHighLatitudeRule = boundedByHighLatitudeRule(city, date, settings, calculated),
        )
    }

    /**
     * The times at [city] for the local date that [now] falls on, and for the
     * day after. "Tomorrow" is the city's next local date, not 24 hours later,
     * so both days are right across a daylight-saving change and when the device
     * is in a different zone from the city.
     *
     * The two days are computed independently, so a polar day or night on one
     * leaves the other as computed.
     */
    fun forInstantAndNextDay(
        city: City,
        now: Instant,
        settings: CalculationSettings,
    ): TwoDayPrayerTimes {
        val today = now.atZone(ZoneId.of(city.timezone)).toLocalDate()
        return TwoDayPrayerTimes(
            today = forDate(city, today, settings),
            tomorrow = forDate(city, today.plusDays(1), settings),
        )
    }

    private fun parameters(settings: CalculationSettings): CalculationParameters =
        settings.method.adhan.parameters.apply {
            madhab = settings.asrJuristicMethod.adhan
            highLatitudeRule = settings.highLatitudeRule.adhan
        }

    /**
     * The prayers whose time the user's high-latitude rule determined, rather than
     * true twilight (issue #7).
     *
     * Adhan substitutes the rule where the twilight angle is not reached, and also
     * where the twilight falls beyond the rule's bound, but it does not report
     * which happened. So the raw twilight is recomputed here with the library's own
     * solar calculation — carrying the same minute adjustments and rounding — and
     * compared with the time Adhan returned: a difference means the rule supplied
     * that time. Where the bound and true twilight round to the same minute the two
     * are indistinguishable in the time shown, and this reports true twilight.
     *
     * This mirrors `PrayerTimes` and `CalculationParameters` in adhan 1.2.1, pinned
     * in `docs/PRAYER_TIMES.md`; revisit it when that library is upgraded.
     */
    private fun boundedByHighLatitudeRule(
        city: City,
        date: LocalDate,
        settings: CalculationSettings,
        calculated: PrayerTimes,
    ): Set<Prayer> {
        if (settings.method.usesItsOwnHighLatitudeRule) return emptySet()

        val parameters = parameters(settings)
        val components = DateComponents(date.year, date.monthValue, date.dayOfMonth)
        val solar = SolarTime(components, Coordinates(city.latitude, city.longitude))

        val fajrBounded = adjustedTwilight(
            depressionAngle = parameters.fajrAngle,
            solar = solar,
            components = components,
            isMorning = false,
            adjustmentMinutes = parameters.adjustments.fajr + parameters.methodAdjustments.fajr,
        ) != calculated.fajr?.asPrayerInstant()

        // A method with a fixed Isha interval (Umm al-Qura, Qatar) takes Isha from
        // Maghrib, so neither the angle nor the rule can apply to it.
        val ishaBounded = parameters.ishaInterval == 0 && adjustedTwilight(
            depressionAngle = parameters.ishaAngle,
            solar = solar,
            components = components,
            isMorning = true,
            adjustmentMinutes = parameters.adjustments.isha + parameters.methodAdjustments.isha,
        ) != calculated.isha?.asPrayerInstant()

        return buildSet {
            if (fajrBounded) add(Prayer.FAJR)
            if (ishaBounded) add(Prayer.ISHA)
        }
    }

    /**
     * The instant true twilight at [depressionAngle] gives on [components], with
     * the same minute adjustments and rounding Adhan applies — the time
     * `PrayerTimes` returns where the high-latitude rule does not intervene. Null
     * where the sun never reaches the angle, which means the rule supplies it.
     */
    private fun adjustedTwilight(
        depressionAngle: Double,
        solar: SolarTime,
        components: DateComponents,
        isMorning: Boolean,
        adjustmentMinutes: Int,
    ): Instant? = TimeComponents.fromDouble(solar.hourAngle(-depressionAngle, isMorning))
        ?.dateComponents(components)
        ?.let { CalendarUtil.roundedMinute(CalendarUtil.add(it, adjustmentMinutes, Calendar.MINUTE)) }
        ?.asPrayerInstant()

    /**
     * Adhan computes times in the city's zone but leaves sub-second noise on the
     * minute it has already rounded to; drop it so a time is a clean minute.
     */
    private fun Date.asPrayerInstant(): Instant = toInstant().truncatedTo(ChronoUnit.SECONDS)
}
