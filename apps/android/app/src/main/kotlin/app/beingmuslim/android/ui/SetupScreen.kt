package app.beingmuslim.android.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import app.beingmuslim.android.prayer.CalculationSettings

/**
 * First-run setup: the user must choose a calculation method, an Asr juristic
 * method, and a city before any time is shown. **Nothing is pre-selected**, and
 * a user who cannot answer the method question has an explicit way forward
 * (`docs/PRODUCT_REQUIREMENTS.md` §2.1, issue #4).
 */
@Composable
fun SetupScreen(
    catalog: CityCatalog,
    onComplete: (cityId: Long, settings: CalculationSettings) -> Unit,
    modifier: Modifier = Modifier,
) {
    var method by remember { mutableStateOf<CalculationMethodChoice?>(null) }
    var asrMethod by remember { mutableStateOf<AsrJuristicMethod?>(null) }
    var city by remember { mutableStateOf<City?>(null) }
    var query by remember { mutableStateOf("") }
    var showNotSure by remember { mutableStateOf(false) }

    val cityResults = remember(query) {
        if (query.isBlank()) emptyList() else catalog.search(query)
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
                text = stringResource(R.string.setup_title),
                style = MaterialTheme.typography.headlineSmall,
            )
            Text(
                text = stringResource(R.string.setup_intro),
                style = MaterialTheme.typography.bodyMedium,
            )

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
                        onSelect = { method = choice },
                    )
                }
            }

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
                            onClick = { method = CalculationMethodChoice.MUSLIM_WORLD_LEAGUE },
                        ) {
                            Text(stringResource(R.string.setup_not_sure_use))
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
                        onSelect = { asrMethod = choice },
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
            city?.let {
                Text(
                    text = stringResource(R.string.setup_city_selected, it.name),
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
                        label = candidate.name,
                        description = candidate.countryCode,
                        selected = city?.id == candidate.id,
                        onSelect = {
                            city = candidate
                            query = ""
                        },
                    )
                }
            }

            val chosenCity = city
            val chosenMethod = method
            val chosenAsr = asrMethod
            Button(
                onClick = {
                    if (chosenCity != null && chosenMethod != null && chosenAsr != null) {
                        onComplete(
                            chosenCity.id,
                            CalculationSettings(chosenMethod, chosenAsr),
                        )
                    }
                },
                enabled = chosenCity != null && chosenMethod != null && chosenAsr != null,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.setup_continue))
            }
        }
    }
}

@Composable
private fun SectionHeading(resId: Int) {
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
