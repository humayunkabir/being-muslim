package app.beingmuslim.android.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
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
import app.beingmuslim.android.prayer.AsrJuristicMethod
import app.beingmuslim.android.prayer.CalculationMethodChoice
import app.beingmuslim.android.prayer.CalculationSettings
import app.beingmuslim.android.prayer.HighLatitudeRuleChoice

/**
 * First run: the user picks a city, a calculation method, an Asr juristic method
 * and the high-latitude rule. Nothing is pre-selected unless the user asks for a
 * location suggestion, and the "not sure" help gives a user who cannot answer
 * the method question an explicit way forward (`docs/PRODUCT_REQUIREMENTS.md`
 * §2.1, issues #4, #6 and #7).
 */
@Composable
fun SetupScreen(
    catalog: CityCatalog,
    onComplete: (cityId: Long, settings: CalculationSettings) -> Unit,
    modifier: Modifier = Modifier,
) {
    var method by rememberSaveable { mutableStateOf<CalculationMethodChoice?>(null) }
    var asrMethod by rememberSaveable { mutableStateOf<AsrJuristicMethod?>(null) }
    var rule by rememberSaveable { mutableStateOf<HighLatitudeRuleChoice?>(null) }
    // The city is held by id so the choice survives a configuration change; the
    // City itself is looked up from the catalog.
    var selectedCityId by rememberSaveable { mutableStateOf<Long?>(null) }

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

            val chosenCity = selectedCityId
            val chosenMethod = method
            val chosenAsr = asrMethod
            val chosenRule = rule
            Button(
                onClick = {
                    if (chosenCity != null && chosenMethod != null && chosenAsr != null && chosenRule != null) {
                        onComplete(
                            chosenCity,
                            CalculationSettings(chosenMethod, chosenAsr, chosenRule),
                        )
                    }
                },
                enabled = chosenCity != null && chosenMethod != null && chosenAsr != null && chosenRule != null,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.setup_continue))
            }
        }
    }
}
