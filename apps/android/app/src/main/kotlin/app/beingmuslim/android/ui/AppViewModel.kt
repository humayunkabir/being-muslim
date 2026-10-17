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
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
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
 *
 * "Today" is the city's local date, and it moves: times are recomputed when the
 * app returns to the foreground ([refreshToday]) and again at the city's next
 * local midnight, so a session is never left showing yesterday's times under
 * today's heading.
 */
class AppViewModel(application: Application) : AndroidViewModel(application) {

    private val store = SelectionStore(application)
    private val _state = MutableStateFlow<AppUiState>(AppUiState.Loading)
    val state: StateFlow<AppUiState> = _state.asStateFlow()

    private var catalog: CityCatalog? = null
    private var selection: UserSelection? = null
    private var rolloverJob: Job? = null

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
            val stored = store.load()
            if (stored == null) {
                _state.value = AppUiState.Setup(loaded)
            } else {
                showTimes(stored, loaded)
            }
        }
    }

    /** Records the first-run choices and shows the times they produce. */
    fun completeSetup(cityId: Long, settings: CalculationSettings) {
        val loaded = catalog ?: return
        viewModelScope.launch {
            val chosen = UserSelection(cityId, settings)
            store.save(chosen)
            showTimes(chosen, loaded)
        }
    }

    /**
     * Recomputes today's times for the current selection. The UI calls this when
     * the app returns to the foreground, so a session resumed after midnight
     * shows the new day rather than yesterday's times.
     */
    fun refreshToday() {
        val loaded = catalog ?: return
        val current = selection ?: return
        viewModelScope.launch { showTimes(current, loaded) }
    }

    private suspend fun showTimes(chosen: UserSelection, catalog: CityCatalog) {
        // A city id that is no longer in the catalog falls back to setup rather
        // than showing nothing (a catalog change is a data migration).
        val city = catalog.byId(chosen.cityId) ?: run {
            _state.value = AppUiState.Setup(catalog)
            return
        }
        selection = chosen
        _state.value = AppUiState.Times(city, chosen.settings, todayAt(city, chosen.settings))
        scheduleRollover(city, chosen.settings)
    }

    private suspend fun todayAt(city: City, settings: CalculationSettings): DailyPrayerTimes? =
        withContext(Dispatchers.Default) {
            PrayerTimeCalculator.forInstant(city, Instant.now(), settings)
        }

    /** Recomputes at the city's next local midnight so an open app rolls over. */
    private fun scheduleRollover(city: City, settings: CalculationSettings) {
        // Runs in viewModelScope, so it is cancelled with the ViewModel.
        rolloverJob?.cancel()
        rolloverJob = viewModelScope.launch {
            val zone = ZoneId.of(city.timezone)
            val now = Instant.now()
            val nextMidnight = now.atZone(zone).toLocalDate().plusDays(1).atStartOfDay(zone).toInstant()
            delay(Duration.between(now, nextMidnight).toMillis().coerceAtLeast(0))
            refreshToday()
        }
    }
}
