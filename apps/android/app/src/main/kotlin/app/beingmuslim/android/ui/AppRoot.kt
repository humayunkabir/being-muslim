package app.beingmuslim.android.ui

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import app.beingmuslim.android.R

/**
 * The app shell. It shows the first-run setup until the user has chosen a
 * calculation method, an Asr juristic method, and a city, then today's and
 * tomorrow's times, with the qibla, settings, counter and account reachable from
 * there (`docs/PRODUCT_REQUIREMENTS.md` §2.1). [onOpenAccount] opens the
 * optional account screen; nothing here depends on being signed in.
 */
@Composable
fun AppRoot(
    onOpenAccount: () -> Unit,
    viewModel: AppViewModel = viewModel(),
) {
    // Recompute on return to the foreground so times roll over with the date.
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.refreshTimes() }
    // Also on entering the app: the account screen may have established a
    // session since the last attempt, and the activity is already resumed.
    LaunchedEffect(Unit) { viewModel.syncNow() }

    val state by viewModel.state.collectAsStateWithLifecycle()

    when (val current = state) {
        AppUiState.Loading -> MessageScreen(R.string.state_loading)
        AppUiState.CatalogUnavailable -> MessageScreen(R.string.state_catalog_unavailable)
        is AppUiState.Setup -> SetupScreen(
            catalog = current.catalog,
            onComplete = viewModel::completeSetup,
        )
        is AppUiState.Times -> TimesScreen(
            state = current,
            onChangeSettings = viewModel::openSettings,
            onReportProblem = viewModel::openReport,
            onOpenQibla = viewModel::openQibla,
            onOpenTasbih = viewModel::openTasbih,
            onOpenReminders = viewModel::openReminders,
            onOpenAccount = onOpenAccount,
            onShiftHijriDay = viewModel::shiftHijriDay,
        )
        is AppUiState.Qibla -> QiblaScreen(
            state = current,
            onBack = viewModel::dismissQibla,
        )
        is AppUiState.Settings -> SettingsScreen(
            catalog = current.catalog,
            selection = current.selection,
            onSave = viewModel::applySelection,
            onCancel = viewModel::dismissSettings,
        )
        is AppUiState.Reminders -> RemindersScreen(
            preferences = current.preferences,
            onPrayerChange = viewModel::setReminderEnabled,
            onQuietHoursEnabledChange = viewModel::setQuietHoursEnabled,
            onQuietHoursChange = viewModel::setQuietHours,
            onBack = viewModel::dismissReminders,
        )
        is AppUiState.Tasbih -> TasbihScreen(
            counter = current.counter,
            onLabelChange = viewModel::setTasbihLabel,
            onTargetChange = viewModel::setTasbihTarget,
            onCount = viewModel::countTasbih,
            onReset = viewModel::resetTasbih,
            onBack = viewModel::dismissTasbih,
        )
        is AppUiState.Report -> ReportScreen(
            report = current.report,
            sendState = current.sendState,
            onNoteChange = viewModel::setReportNote,
            onSend = viewModel::sendReport,
            onBack = viewModel::dismissReport,
        )
    }
}

@Composable
private fun MessageScreen(@StringRes messageRes: Int, modifier: Modifier = Modifier) {
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
