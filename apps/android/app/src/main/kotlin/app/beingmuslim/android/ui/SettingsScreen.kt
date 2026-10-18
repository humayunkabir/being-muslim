package app.beingmuslim.android.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.beingmuslim.android.R
import app.beingmuslim.android.citycatalog.CityCatalog
import app.beingmuslim.android.prayer.CalculationSettings
import app.beingmuslim.android.selection.UserSelection

/**
 * The user changing their city or calculation choices after first run. The form
 * starts at the choices the user already made, and nothing is saved until they
 * save, so opening this screen never changes anything (issue #5).
 */
@Composable
fun SettingsScreen(
    catalog: CityCatalog,
    selection: UserSelection,
    onSave: (cityId: Long, settings: CalculationSettings) -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // The system back gesture leaves without changing anything, as Cancel does.
    BackHandler(onBack = onCancel)

    var method by rememberSaveable { mutableStateOf(selection.settings.method) }
    var asrMethod by rememberSaveable { mutableStateOf(selection.settings.asrJuristicMethod) }
    var selectedCityId by rememberSaveable { mutableStateOf(selection.cityId) }

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

            CalculationChoiceForm(
                catalog = catalog,
                method = method,
                asrMethod = asrMethod,
                selectedCityId = selectedCityId,
                onMethodChange = { method = it },
                onAsrMethodChange = { asrMethod = it },
                onCityChange = { selectedCityId = it },
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = onCancel,
                    modifier = Modifier.weight(1f),
                ) {
                    Text(stringResource(R.string.settings_cancel))
                }
                Button(
                    onClick = { onSave(selectedCityId, CalculationSettings(method, asrMethod)) },
                    modifier = Modifier.weight(1f),
                ) {
                    Text(stringResource(R.string.settings_save))
                }
            }
        }
    }
}
