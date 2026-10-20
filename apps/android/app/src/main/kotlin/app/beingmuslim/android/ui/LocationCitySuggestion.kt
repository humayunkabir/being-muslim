package app.beingmuslim.android.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import app.beingmuslim.android.R
import app.beingmuslim.android.citycatalog.CityCatalog
import app.beingmuslim.android.location.LocationLookup
import app.beingmuslim.android.location.nearestCity
import kotlinx.coroutines.launch

/**
 * The optional "use my location" action for the city choice (issue #6). The
 * benefit is explained before the system prompt, only coarse permission is
 * requested, and denial or failure leaves the search field as the complete path.
 * The fix is used once and never stored (`docs/DESIGN_STANDARDS.md` §8).
 */
@Composable
internal fun LocationCitySuggestion(
    catalog: CityCatalog,
    onCityFound: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val lookup = remember(context) { LocationLookup(context) }
    val scope = rememberCoroutineScope()
    var status by remember { mutableStateOf(LocationSuggestionStatus.IDLE) }

    val findCity = {
        status = LocationSuggestionStatus.SEARCHING
        scope.launch {
            val city = lookup.currentFix()?.nearestCity(catalog)
            if (city == null) {
                status = LocationSuggestionStatus.UNAVAILABLE
            } else {
                onCityFound(city.id)
                status = LocationSuggestionStatus.IDLE
            }
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) findCity() else status = LocationSuggestionStatus.DENIED
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = stringResource(R.string.setup_city_location_benefit),
            style = MaterialTheme.typography.bodySmall,
        )
        OutlinedButton(
            onClick = {
                if (lookup.hasPermission()) {
                    findCity()
                } else {
                    permissionLauncher.launch(LocationLookup.PERMISSION)
                }
            },
            enabled = status != LocationSuggestionStatus.SEARCHING,
        ) {
            Text(stringResource(R.string.setup_city_use_location))
        }

        val message = when (status) {
            LocationSuggestionStatus.SEARCHING -> R.string.setup_city_location_searching
            LocationSuggestionStatus.DENIED -> R.string.setup_city_location_denied
            LocationSuggestionStatus.UNAVAILABLE -> R.string.setup_city_location_unavailable
            LocationSuggestionStatus.IDLE -> null
        }
        message?.let {
            Text(
                text = stringResource(it),
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
            )
        }
    }
}

private enum class LocationSuggestionStatus { IDLE, SEARCHING, DENIED, UNAVAILABLE }
