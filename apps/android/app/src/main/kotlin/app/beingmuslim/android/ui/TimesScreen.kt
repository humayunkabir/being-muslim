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
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.beingmuslim.android.R
import app.beingmuslim.android.citycatalog.City
import app.beingmuslim.android.prayer.DailyPrayerTimes
import app.beingmuslim.android.prayer.Prayer
import java.time.Instant
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
 * method, and [onOpenTasbih] opens the counter.
 */
@Composable
fun TimesScreen(
    state: AppUiState.Times,
    onChangeSettings: () -> Unit,
    onOpenTasbih: () -> Unit,
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
                text = state.city.displayName(),
                style = MaterialTheme.typography.headlineSmall,
            )
            Text(
                text = stringResource(R.string.times_title),
                style = MaterialTheme.typography.titleMedium,
            )

            DaySection(R.string.times_section_today, state.days.today, state.city)
            DaySection(R.string.times_section_tomorrow, state.days.tomorrow, state.city)

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

/** One day's heading, date and times; the heading and the reason when it has none. */
@Composable
private fun DaySection(
    @StringRes headingRes: Int,
    times: DailyPrayerTimes?,
    city: City,
) {
    Text(
        text = stringResource(headingRes),
        style = MaterialTheme.typography.titleMedium,
    )
    if (times == null) {
        Text(
            text = stringResource(R.string.times_unavailable, city.name),
            style = MaterialTheme.typography.bodyMedium,
        )
        return
    }

    val context = LocalContext.current
    Text(
        text = formatDate(context, times),
        style = MaterialTheme.typography.bodyMedium,
    )
    Prayer.entries.forEach { prayer ->
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = stringResource(prayer.labelRes()),
                style = MaterialTheme.typography.bodyLarge,
            )
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
