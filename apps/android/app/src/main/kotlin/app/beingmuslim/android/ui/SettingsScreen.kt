package app.beingmuslim.android.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.beingmuslim.android.R
import app.beingmuslim.android.citycatalog.CityCatalog
import app.beingmuslim.android.prayer.CalculationSettings
import app.beingmuslim.android.prayer.Prayer
import app.beingmuslim.android.prayer.PrayerOffsets
import app.beingmuslim.android.selection.UserSelection

/**
 * The user changing their city or calculation choices after first run. The form
 * starts at the choices the user already made, and nothing is saved until they
 * save, so opening this screen never changes anything (issue #5). Their
 * per-prayer offsets are edited here too, but stay separate from the choices
 * that produce the raw calculation (issue #8).
 */
@Composable
fun SettingsScreen(
    catalog: CityCatalog,
    selection: UserSelection,
    onSave: (cityId: Long, settings: CalculationSettings, offsets: PrayerOffsets) -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // The system back gesture leaves without changing anything, as Cancel does.
    BackHandler(onBack = onCancel)

    var method by rememberSaveable { mutableStateOf(selection.settings.method) }
    var asrMethod by rememberSaveable { mutableStateOf(selection.settings.asrJuristicMethod) }
    var rule by rememberSaveable { mutableStateOf(selection.settings.highLatitudeRule) }
    var selectedCityId by rememberSaveable { mutableStateOf(selection.cityId) }
    // Kept as the domain type through a Saver so a configuration change keeps
    // exactly the offsets the user has set.
    var offsets by rememberSaveable(stateSaver = PrayerOffsetsSaver) {
        mutableStateOf(selection.offsets)
    }

    Scaffold(modifier = modifier.fillMaxSize()) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = stringResource(R.string.settings_title),
                style = MaterialTheme.typography.headlineSmall,
            )
            Text(
                text = stringResource(R.string.settings_intro),
                style = MaterialTheme.typography.bodyMedium,
            )

            LanguageChoice()

            CalculationChoiceForm(
                catalog = catalog,
                method = method,
                asrMethod = asrMethod,
                rule = rule,
                selectedCityId = selectedCityId,
                onMethodChange = { method = it },
                onAsrMethodChange = { asrMethod = it },
                onRuleChange = { rule = it },
                onCityChange = { selectedCityId = it },
            )

            PrayerOffsetsForm(
                offsets = offsets,
                onChange = { prayer, minutes -> offsets = offsets.with(prayer, minutes) },
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = onCancel,
                    modifier = Modifier.weight(1f),
                ) {
                    Text(stringResource(R.string.settings_cancel))
                }
                Button(
                    onClick = {
                        onSave(selectedCityId, CalculationSettings(method, asrMethod, rule), offsets)
                    },
                    modifier = Modifier.weight(1f),
                ) {
                    Text(stringResource(R.string.settings_save))
                }
            }
        }
    }
}

/**
 * The user's own per-prayer minute adjustments (issue #8). Each prayer is moved
 * on its own; the copy says these are the user's adjustment, not the
 * calculation, and clearing them restores the raw time.
 */
@Composable
private fun PrayerOffsetsForm(
    offsets: PrayerOffsets,
    onChange: (Prayer, Int) -> Unit,
) {
    Text(
        text = stringResource(R.string.settings_offsets_heading),
        style = MaterialTheme.typography.titleMedium,
    )
    Text(
        text = stringResource(R.string.settings_offsets_help),
        style = MaterialTheme.typography.bodySmall,
    )
    Prayer.entries.forEach { prayer ->
        val minutes = offsets[prayer]
        val prayerName = stringResource(prayer.labelRes())
        val decreaseDescription = stringResource(R.string.settings_offset_decrease, prayerName)
        val increaseDescription = stringResource(R.string.settings_offset_increase, prayerName)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(text = prayerName, style = MaterialTheme.typography.bodyLarge)
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(
                    onClick = { onChange(prayer, minutes - 1) },
                    enabled = minutes > -PrayerOffsets.MAX_MINUTES,
                    modifier = Modifier.semantics {
                        contentDescription = decreaseDescription
                    },
                ) {
                    Text(stringResource(R.string.settings_offset_decrease_amount))
                }
                Text(
                    text = if (minutes == 0) {
                        stringResource(R.string.settings_offset_none)
                    } else {
                        stringResource(R.string.offset_minutes, minutes)
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .widthIn(min = 64.dp)
                        // Announce the new value when a stepper changes it.
                        .semantics { liveRegion = LiveRegionMode.Polite },
                )
                TextButton(
                    onClick = { onChange(prayer, minutes + 1) },
                    enabled = minutes < PrayerOffsets.MAX_MINUTES,
                    modifier = Modifier.semantics {
                        contentDescription = increaseDescription
                    },
                ) {
                    Text(stringResource(R.string.settings_offset_increase_amount))
                }
            }
        }
    }
    if (!offsets.isEmpty) {
        TextButton(onClick = { Prayer.entries.forEach { onChange(it, 0) } }) {
            Text(stringResource(R.string.settings_offsets_clear))
        }
    }
}

/** Saves offsets in prayer order, so a configuration change keeps them. */
private val PrayerOffsetsSaver = listSaver<PrayerOffsets, Int>(
    save = { offsets -> Prayer.entries.map { offsets[it] } },
    restore = { values ->
        Prayer.entries.fold(PrayerOffsets()) { offsets, prayer ->
            offsets.with(prayer, values[prayer.ordinal])
        }
    },
)
