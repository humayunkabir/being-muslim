package app.beingmuslim.android.prayer

import app.beingmuslim.android.citycatalog.City
import com.batoulapps.adhan.CalculationParameters
import com.batoulapps.adhan.Coordinates
import com.batoulapps.adhan.PrayerTimes
import com.batoulapps.adhan.data.DateComponents
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit
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
        }

    /**
     * Adhan computes times in the city's zone but leaves sub-second noise on the
     * minute it has already rounded to; drop it so a time is a clean minute.
     */
    private fun Date.asPrayerInstant(): Instant = toInstant().truncatedTo(ChronoUnit.SECONDS)
}
