package app.beingmuslim.android.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import app.beingmuslim.android.R
import app.beingmuslim.android.prayer.Prayer
import app.beingmuslim.android.report.ProblemReport

/**
 * The report about a wrong time or another problem (issue #18). It shows the
 * exact inputs that will be attached, so the user can see that the city, method,
 * parameters, offsets and date are what is sent, and that nothing about their
 * account, device or movements is (`docs/PRODUCT_REQUIREMENTS.md` §2.1, §10).
 *
 * The destination and what happens next are stated before sending. Sending needs
 * a connection; a failed attempt keeps the report here to retry, so being offline
 * never silently loses it.
 */
@Composable
fun ReportScreen(
    report: ProblemReport,
    sendState: ReportSendState,
    onNoteChange: (String) -> Unit,
    onSend: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BackHandler(onBack = onBack)

    val language = currentLanguage()
    val city = report.city
    val parameters = report.settings.method.adhan.parameters
    val canEdit = sendState.isReportEditable

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
                text = stringResource(R.string.report_title),
                style = MaterialTheme.typography.headlineSmall,
            )
            Text(
                text = stringResource(R.string.report_destination),
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                text = stringResource(R.string.report_needs_connection),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            Text(
                text = stringResource(R.string.report_attached_heading),
                style = MaterialTheme.typography.titleMedium,
            )
            SummaryLine(
                stringResource(
                    R.string.report_attached_location,
                    city.displayName(language),
                    countryName(city.countryCode, language),
                    city.latitude,
                    city.longitude,
                    city.timezone,
                ),
            )
            SummaryLine(
                if (parameters.ishaInterval > 0) {
                    stringResource(
                        R.string.report_attached_method_interval,
                        stringResource(report.settings.method.labelRes()),
                        parameters.fajrAngle,
                        pluralStringResource(
                            R.plurals.report_minutes_after_maghrib,
                            parameters.ishaInterval,
                            parameters.ishaInterval,
                        ),
                    )
                } else {
                    stringResource(
                        R.string.report_attached_method_angles,
                        stringResource(report.settings.method.labelRes()),
                        parameters.fajrAngle,
                        parameters.ishaAngle,
                    )
                },
            )
            SummaryLine(
                stringResource(
                    R.string.report_attached_asr,
                    stringResource(report.settings.asrJuristicMethod.labelRes()),
                ),
            )
            SummaryLine(
                stringResource(
                    R.string.report_attached_high_latitude,
                    stringResource(report.settings.highLatitudeRule.labelRes()),
                ),
            )
            SummaryLine(
                stringResource(
                    R.string.report_attached_offsets,
                    offsetsSummary(report) ?: stringResource(R.string.report_attached_offsets_none),
                ),
            )
            // Dates and raw times are the day records the report carries.
            val context = LocalContext.current
            report.days.today?.let { day ->
                SummaryLine(
                    stringResource(
                        R.string.report_attached_today,
                        formatDate(context, day.date, day.zone),
                        day.date.toString(),
                    ),
                )
            }
            report.days.tomorrow?.let { day ->
                SummaryLine(
                    stringResource(
                        R.string.report_attached_tomorrow,
                        formatDate(context, day.date, day.zone),
                        day.date.toString(),
                    ),
                )
            }

            Text(
                text = stringResource(R.string.report_privacy),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            OutlinedTextField(
                value = report.note,
                onValueChange = onNoteChange,
                label = { Text(stringResource(R.string.report_note_label)) },
                supportingText = {
                    Text(
                        stringResource(
                            R.string.report_note_help,
                            ProblemReport.MAX_NOTE_LENGTH,
                        ),
                    )
                },
                minLines = 3,
                enabled = canEdit,
                modifier = Modifier.fillMaxWidth(),
            )

            when (sendState) {
                ReportSendState.FAILED -> Text(
                    text = stringResource(R.string.report_failed),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                    // The sending button disables itself, so the outcome is
                    // announced to a screen reader rather than left to be found.
                    modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                )

                ReportSendState.SENT -> Text(
                    text = stringResource(R.string.report_sent),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                )

                ReportSendState.NOT_SENT, ReportSendState.SENDING -> Unit
            }

            if (sendState != ReportSendState.SENT) {
                Button(
                    onClick = onSend,
                    enabled = canEdit,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        stringResource(
                            if (sendState == ReportSendState.SENDING) {
                                R.string.report_sending
                            } else if (sendState == ReportSendState.FAILED) {
                                R.string.report_retry
                            } else {
                                R.string.report_send
                            },
                        ),
                    )
                }
            }

            TextButton(onClick = onBack) {
                Text(stringResource(R.string.report_back))
            }
        }
    }
}

@Composable
private fun SummaryLine(text: String) {
    Text(text = text, style = MaterialTheme.typography.bodyMedium)
}

/** The user's non-zero adjustments, in prayer order, or null when there are none. */
@Composable
private fun offsetsSummary(report: ProblemReport): String? {
    val items = Prayer.entries.mapNotNull { prayer ->
        val minutes = report.offsets[prayer]
        if (minutes == 0) {
            null
        } else {
            stringResource(
                R.string.report_offset_item,
                stringResource(prayer.labelRes()),
                stringResource(R.string.offset_minutes, minutes),
            )
        }
    }
    return items.takeIf { it.isNotEmpty() }?.joinToString(", ")
}
