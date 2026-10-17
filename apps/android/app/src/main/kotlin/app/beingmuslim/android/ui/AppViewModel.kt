package app.beingmuslim.android.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import app.beingmuslim.android.citycatalog.BundledCityCatalog
import app.beingmuslim.android.citycatalog.City
import app.beingmuslim.android.citycatalog.CityCatalog
import app.beingmuslim.android.prayer.CalculationSettings
import app.beingmuslim.android.prayer.DailyPrayerTimes
import app.beingmuslim.android.prayer.PrayerTimeCalculator
import app.beingmuslim.android.selection.SelectionStore
import app.beingmuslim.android.selection.UserSelection
import java.time.Instant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** What the app is showing: a load, the first-run setup, or today's times. */
sealed interface AppUiState {

    data object Loading : AppUiState

    /** The bundled catalog could not be read; nothing can be shown. */
    data object CatalogUnavailable : AppUiState

    /** First run, or a stored selection that is no longer usable. */
    data class Setup(val catalog: CityCatalog) : AppUiState

    /**
     * Today's times for the user's city and settings. [today] is null when the
     * sun neither rises nor sets there on the day — an unavailable state, not an
     * error.
     */
    data class Times(
        val city: City,
        val settings: CalculationSettings,
        val today: DailyPrayerTimes?,
    ) : AppUiState
}

/**
 * Orchestrates the app: loads the offline catalog once, reads the persisted
 * selection, and calculates today's times. All real logic lives in the domain
 * (catalog, calculation, [SelectionStore]); this only wires it to the UI and
 * keeps heavy work off the main thread.
 */
class AppViewModel(application: Application) : AndroidViewModel(application) {

    private val store = SelectionStore(application)
    private val _state = MutableStateFlow<AppUiState>(AppUiState.Loading)
    val state: StateFlow<AppUiState> = _state.asStateFlow()

    private var catalog: CityCatalog? = null

    init {
        viewModelScope.launch {
            val loaded = withContext(Dispatchers.IO) {
                runCatching { BundledCityCatalog.load(getApplication<Application>().assets) }.getOrNull()
            }
            if (loaded == null) {
                _state.value = AppUiState.CatalogUnavailable
                return@launch
            }
            catalog = loaded
            val selection = store.load()
            _state.value = if (selection == null) {
                AppUiState.Setup(loaded)
            } else {
                stateFor(selection, loaded)
            }
        }
    }

    /** Records the first-run choices and shows the times they produce. */
    fun completeSetup(cityId: Long, settings: CalculationSettings) {
        val loaded = catalog ?: return
        viewModelScope.launch {
            store.save(UserSelection(cityId, settings))
            _state.value = stateFor(UserSelection(cityId, settings), loaded)
        }
    }

    private suspend fun stateFor(selection: UserSelection, catalog: CityCatalog): AppUiState {
        // A city id that is no longer in the catalog falls back to setup rather
        // than showing nothing (a catalog change is a data migration).
        val city = catalog.byId(selection.cityId) ?: return AppUiState.Setup(catalog)
        val today = withContext(Dispatchers.Default) {
            PrayerTimeCalculator.forInstant(city, Instant.now(), selection.settings)
        }
        return AppUiState.Times(city, selection.settings, today)
    }
}
