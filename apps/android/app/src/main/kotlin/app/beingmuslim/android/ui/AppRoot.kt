package app.beingmuslim.android.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import app.beingmuslim.android.R
import app.beingmuslim.android.prayer.CalculationSettings

/**
 * The app shell. It shows the first-run setup until the user has chosen a
 * calculation method, an Asr juristic method, and a city, then today's times
 * (`docs/PRODUCT_REQUIREMENTS.md` §2.1).
 */
@Composable
fun AppRoot(viewModel: AppViewModel = viewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    when (val current = state) {
        AppUiState.Loading -> MessageScreen(R.string.state_loading)
        AppUiState.CatalogUnavailable -> MessageScreen(R.string.state_catalog_unavailable)
        is AppUiState.Setup -> SetupScreen(
            catalog = current.catalog,
            onComplete = { cityId: Long, settings: CalculationSettings ->
                viewModel.completeSetup(cityId, settings)
            },
        )
        is AppUiState.Times -> TimesScreen(current)
    }
}

@Composable
private fun MessageScreen(messageRes: Int, modifier: Modifier = Modifier) {
    Scaffold(modifier = modifier.fillMaxSize()) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = stringResource(messageRes),
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
            )
        }
    }
}
