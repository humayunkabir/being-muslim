package app.beingmuslim.android.ui

import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.beingmuslim.android.R
import app.beingmuslim.android.citycatalog.City
import app.beingmuslim.android.hijri.HijriCalendar
import app.beingmuslim.android.prayer.DailyPrayerTimes
import app.beingmuslim.android.prayer.Prayer
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.Date
import java.util.TimeZone

/**
 * The five prayer times for the user's city, today and tomorrow, with the
 * calculation method, Asr juristic method and high-latitude rule that produced
 * them shown on screen (`docs/PRODUCT_REQUIREMENTS.md` §2.1, §5). A day says so
 * where the high-latitude rule, not true dawn or dusk, produced its Fajr or Isha
 * (issue #7). Times are formatted for the device's locale and the city's time
 * zone. [onChangeSettings] opens the screen where the user changes their city or
 * method, [onShiftHijriDay] shifts the calculated Hijri dates by ±1 day (§7),
 * and [onOpenTasbih] opens the counter.
 */
@Composable
fun TimesScreen(
    state: AppUiState.Times,
    onChangeSettings: () -> Unit,
    onOpenTasbih: () -> Unit,
    onShiftHijriDay: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(modifier = modifier.fillMaxSize()) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = state.city.displayName(currentLanguage()),
                style = MaterialTheme.typography.headlineSmall,
            )
            Text(
                text = stringResource(R.string.times_title),
                style = MaterialTheme.typography.titleMedium,
            )

            DaySection(R.string.times_section_today, state.days.today, state.city, state.hijriDayShift)
            DaySection(R.string.times_section_tomorrow, state.days.tomorrow, state.city, state.hijriDayShift)

            HijriDateSection(state.hijriDayShift, onShiftHijriDay)

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            Text(
                text = stringResource(
                    R.string.times_method,
                    stringResource(state.settings.method.labelRes()),
                ),
                style = MaterialTheme.typography.bodySmall,
            )
            Text(
                text = stringResource(
                    R.string.times_asr_method,
                    stringResource(state.settings.asrJuristicMethod.labelRes()),
                ),
                style = MaterialTheme.typography.bodySmall,
            )
            Text(
                text = stringResource(
                    R.string.times_high_latitude_rule,
                    stringResource(state.settings.highLatitudeRule.labelRes()),
                ),
                style = MaterialTheme.typography.bodySmall,
            )
            if (state.settings.method.usesItsOwnHighLatitudeRule) {
                Text(
                    text = stringResource(R.string.times_high_latitude_not_used),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            Text(
                text = stringResource(R.string.city_catalog_attribution),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            TextButton(onClick = onChangeSettings) {
                Text(stringResource(R.string.times_change_settings))
            }
            // shortcut: the counter is reached from here rather than from real
            // navigation; give it its own destination when the app has one.
            TextButton(onClick = onOpenTasbih) {
                Text(stringResource(R.string.times_open_tasbih))
            }
        }
    }
}

/** One day's heading, dates and times; the heading and the reason when it has none. */
@Composable
private fun DaySection(
    @StringRes headingRes: Int,
    times: DailyPrayerTimes?,
    city: City,
    hijriDayShift: Int,
) {
    Text(
        text = stringResource(headingRes),
        style = MaterialTheme.typography.titleMedium,
    )
    if (times == null) {
        Text(
            text = stringResource(R.string.times_unavailable, city.displayName(currentLanguage())),
            style = MaterialTheme.typography.bodyMedium,
        )
        return
    }

    val context = LocalContext.current
    Text(
        text = formatDate(context, times),
        style = MaterialTheme.typography.bodyMedium,
    )
    Text(
        text = formatHijriDate(times.date, hijriDayShift),
        style = MaterialTheme.typography.bodyMedium,
    )
    Prayer.entries.forEach { prayer ->
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // The Arabic name is its own Text rather than concatenated onto the
            // label: as its own paragraph the platform shapes it and resolves
            // its right-to-left direction, with no mixed-script ambiguity. The
            // name group is weighted so that at large text sizes the name wraps
            // instead of pushing the time off the screen.
            Row(
                modifier = Modifier.weight(1f, fill = false),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(prayer.labelRes()),
                    style = MaterialTheme.typography.bodyLarge,
                )
                Text(
                    text = stringResource(prayer.arabicRes()),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                text = formatTime(context, times[prayer], times.zone),
                style = MaterialTheme.typography.bodyLarge,
            )
        }
    }

    times.boundedByHighLatitudeRule.highLatitudeRuleAppliedRes()?.let { disclosureRes ->
        Text(
            text = stringResource(disclosureRes),
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

/**
 * The Hijri date's provenance and the user's ±1-day shift of it
 * (`docs/PRODUCT_REQUIREMENTS.md` §7). The copy says what the date is — a
 * calculation on a named convention — and that a local announcement may differ,
 * so a calculated date is never taken for a confirmed sighting.
 */
@Composable
private fun HijriDateSection(dayShift: Int, onShiftHijriDay: (Int) -> Unit) {
    Text(
        text = stringResource(R.string.hijri_label),
        style = MaterialTheme.typography.bodySmall,
    )
    Text(
        text = stringResource(R.string.hijri_calculated_note),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        // Each direction is offered only while it can still move the date, so
        // the pair cannot reach a shift the app does not support.
        TextButton(
            onClick = { onShiftHijriDay(-1) },
            enabled = dayShift > -HijriCalendar.MAX_DAY_SHIFT,
        ) {
            Text(stringResource(R.string.hijri_shift_earlier))
        }
        TextButton(
            onClick = { onShiftHijriDay(1) },
            enabled = dayShift < HijriCalendar.MAX_DAY_SHIFT,
        ) {
            Text(stringResource(R.string.hijri_shift_later))
        }
    }
    if (dayShift != 0) {
        Text(
            text = stringResource(
                if (dayShift > 0) R.string.hijri_shifted_later else R.string.hijri_shifted_earlier,
            ),
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

/** Formats [date]'s Hijri date, shifted by [dayShiftDays], as "Hijri: 11 Ramadan 1445". */
@Composable
private fun formatHijriDate(date: LocalDate, dayShiftDays: Int): String {
    val hijri = HijriCalendar.dateFor(date, dayShiftDays)
    return stringResource(
        R.string.hijri_date,
        hijri.day,
        stringResource(HIJRI_MONTH_LABELS[hijri.month - 1]),
        hijri.year,
    )
}

/** The month names in Hijri calendar order, [Muharram][R.string.hijri_month_1] first. */
private val HIJRI_MONTH_LABELS = intArrayOf(
    R.string.hijri_month_1,
    R.string.hijri_month_2,
    R.string.hijri_month_3,
    R.string.hijri_month_4,
    R.string.hijri_month_5,
    R.string.hijri_month_6,
    R.string.hijri_month_7,
    R.string.hijri_month_8,
    R.string.hijri_month_9,
    R.string.hijri_month_10,
    R.string.hijri_month_11,
    R.string.hijri_month_12,
)

/** Formats [instant] for the device locale, in the city's [zone]. */
private fun formatTime(context: Context, instant: Instant, zone: ZoneId): String {
    val format = android.text.format.DateFormat.getTimeFormat(context)
    format.timeZone = TimeZone.getTimeZone(zone)
    return format.format(Date.from(instant))
}

/** Formats the local date of [times], in the city's zone. */
private fun formatDate(context: Context, times: DailyPrayerTimes): String {
    val format = android.text.format.DateFormat.getMediumDateFormat(context)
    format.timeZone = TimeZone.getTimeZone(times.zone)
    val midday = times.date.atTime(12, 0).atZone(times.zone).toInstant()
    return format.format(Date.from(midday))
}
