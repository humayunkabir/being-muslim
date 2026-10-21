package app.beingmuslim.android.ui

import android.content.Context
import android.text.format.DateFormat
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.util.Date
import java.util.TimeZone

/** Formats [instant] for the device locale, in [zone]. */
internal fun formatTime(context: Context, instant: Instant, zone: ZoneId): String {
    val format = DateFormat.getTimeFormat(context)
    format.timeZone = TimeZone.getTimeZone(zone)
    return format.format(Date.from(instant))
}

/** Formats [date] for the device locale, in [zone] — how a day is headed on the times screen. */
internal fun formatDate(context: Context, date: LocalDate, zone: ZoneId): String {
    val format = DateFormat.getMediumDateFormat(context)
    format.timeZone = TimeZone.getTimeZone(zone)
    // Midday, so a daylight-saving transition at midnight cannot move the date.
    return format.format(Date.from(date.atTime(12, 0).atZone(zone).toInstant()))
}

/** Formats a wall-clock time in the device's 12/24-hour preference. */
internal fun formatClock(context: Context, time: LocalTime): String {
    val at = time.atDate(LocalDate.of(2000, 1, 1)).atZone(ZoneId.systemDefault()).toInstant()
    return formatTime(context, at, ZoneId.systemDefault())
}
