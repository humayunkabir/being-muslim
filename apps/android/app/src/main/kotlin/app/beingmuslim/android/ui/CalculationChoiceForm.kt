package app.beingmuslim.android.ui

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import app.beingmuslim.android.R
import app.beingmuslim.android.citycatalog.City
import app.beingmuslim.android.citycatalog.CityCatalog
import app.beingmuslim.android.prayer.AsrJuristicMethod
import app.beingmuslim.android.prayer.CalculationMethodChoice
import app.beingmuslim.android.prayer.HighLatitudeRuleChoice
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * The city, calculation-method, Asr-method and high-latitude-rule choices, shared
 * by first-run setup and settings (issues #4, #5 and #7). The caller holds the
 * choices, so this only ever reports a change the user made — no choice is ever
 * selected here.
 *
 * The "not sure which to choose" help is shown only while no method is chosen,
 * which is first run and nowhere else.
 */
@Composable
fun CalculationChoiceForm(
    catalog: CityCatalog,
    method: CalculationMethodChoice?,
    asrMethod: AsrJuristicMethod?,
    rule: HighLatitudeRuleChoice?,
    selectedCityId: Long?,
    onMethodChange: (CalculationMethodChoice) -> Unit,
    onAsrMethodChange: (AsrJuristicMethod) -> Unit,
    onRuleChange: (HighLatitudeRuleChoice) -> Unit,
    onCityChange: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    var query by rememberSaveable { mutableStateOf("") }
    var showNotSure by rememberSaveable { mutableStateOf(false) }

    val selectedCity = selectedCityId?.let(catalog::byId)

    val cityResults by produceState(emptyList<City>(), query, catalog) {
        // Scanning the catalog is cheap but not free; keep it off the main
        // thread so typing never blocks a frame.
        value = if (query.isBlank()) {
            emptyList()
        } else {
            withContext(Dispatchers.Default) { catalog.search(query) }
        }
    }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        SectionHeading(R.string.setup_method_heading)
        Text(
            text = stringResource(R.string.setup_method_help),
            style = MaterialTheme.typography.bodySmall,
        )
        Column(Modifier.selectableGroup()) {
            CalculationMethodChoice.entries.forEach { choice ->
                SelectableRow(
                    label = stringResource(choice.labelRes()),
                    description = stringResource(choice.descriptionRes()),
                    selected = method == choice,
                    onSelect = { onMethodChange(choice) },
                )
            }
        }

        if (method == null) {
            TextButton(onClick = { showNotSure = !showNotSure }) {
                Text(stringResource(R.string.setup_not_sure))
            }
            if (showNotSure) {
                Card {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            text = stringResource(R.string.setup_not_sure_body),
                            style = MaterialTheme.typography.bodySmall,
                        )
                        TextButton(
                            onClick = { onMethodChange(CalculationMethodChoice.MUSLIM_WORLD_LEAGUE) },
                        ) {
                            Text(stringResource(R.string.setup_not_sure_use))
                        }
                    }
                }
            }
        }

        SectionHeading(R.string.setup_asr_heading)
        Text(
            text = stringResource(R.string.setup_asr_help),
            style = MaterialTheme.typography.bodySmall,
        )
        Column(Modifier.selectableGroup()) {
            AsrJuristicMethod.entries.forEach { choice ->
                SelectableRow(
                    label = stringResource(choice.labelRes()),
                    description = stringResource(choice.descriptionRes()),
                    selected = asrMethod == choice,
                    onSelect = { onAsrMethodChange(choice) },
                )
            }
        }

        SectionHeading(R.string.setup_high_latitude_rule_heading)
        Text(
            text = stringResource(R.string.setup_high_latitude_rule_help),
            style = MaterialTheme.typography.bodySmall,
        )
        Column(Modifier.selectableGroup()) {
            HighLatitudeRuleChoice.entries.forEach { choice ->
                SelectableRow(
                    label = stringResource(choice.labelRes()),
                    description = stringResource(choice.descriptionRes()),
                    selected = rule == choice,
                    onSelect = { onRuleChange(choice) },
                )
            }
        }

        SectionHeading(R.string.setup_city_heading)
        Text(
            text = stringResource(R.string.setup_city_help),
            style = MaterialTheme.typography.bodySmall,
        )
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            label = { Text(stringResource(R.string.setup_city_search_hint)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        selectedCity?.let {
            Text(
                text = stringResource(R.string.setup_city_selected, it.displayName()),
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        if (query.isNotBlank() && cityResults.isEmpty()) {
            Text(
                text = stringResource(R.string.setup_city_no_results),
                style = MaterialTheme.typography.bodySmall,
            )
        }
        Column(Modifier.selectableGroup()) {
            cityResults.forEach { candidate ->
                SelectableRow(
                    label = candidate.displayName(),
                    description = candidate.countryCode,
                    selected = selectedCityId == candidate.id,
                    onSelect = {
                        onCityChange(candidate.id)
                        query = ""
                    },
                )
            }
        }
    }
}

@Composable
private fun SectionHeading(@StringRes resId: Int) {
    Text(
        text = stringResource(resId),
        style = MaterialTheme.typography.titleMedium,
    )
}

@Composable
private fun SelectableRow(
    label: String,
    description: String,
    selected: Boolean,
    onSelect: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(
                selected = selected,
                role = Role.RadioButton,
                onClick = onSelect,
            )
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = null)
        Spacer(Modifier.width(8.dp))
        Column {
            Text(text = label, style = MaterialTheme.typography.bodyLarge)
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
