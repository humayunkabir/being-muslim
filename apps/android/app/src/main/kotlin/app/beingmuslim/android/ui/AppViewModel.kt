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
import app.beingmuslim.android.tasbih.TasbihCounter
import app.beingmuslim.android.tasbih.TasbihStore
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

/** What the app is showing: a load, the first-run setup, the times, or the tasbih. */
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

    /** The counter the user is counting with, as it was left last time. */
    data class Tasbih(val counter: TasbihCounter) : AppUiState
}

/**
 * Orchestrates the app: loads the offline catalog once, reads the persisted
 * selection and counter, and calculates today's times. All real logic lives in
 * the domain (catalog, calculation, [SelectionStore], [TasbihCounter]); this only
 * wires it to the UI and keeps heavy work off the main thread.
 *
 * "Today" is the city's local date, and it moves: times are recomputed when the
 * app returns to the foreground ([refreshToday]) and again at the city's next
 * local midnight, so a session is never left showing yesterday's times under
 * today's heading.
 */
class AppViewModel(application: Application) : AndroidViewModel(application) {

    private val store = SelectionStore(application)
    private val tasbihStore = TasbihStore(application)
    private val _state = MutableStateFlow<AppUiState>(AppUiState.Loading)
    val state: StateFlow<AppUiState> = _state.asStateFlow()

    private var catalog: CityCatalog? = null
    private var selection: UserSelection? = null
    private var tasbih = TasbihCounter()
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
            tasbih = tasbihStore.load()
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
     *
     * Only while the times are on screen: returning to another screen must not
     * replace it.
     */
    fun refreshToday() {
        if (_state.value !is AppUiState.Times) return
        val loaded = catalog ?: return
        val current = selection ?: return
        viewModelScope.launch { showTimes(current, loaded) }
    }

    /** Opens the counter as the user last left it. */
    fun openTasbih() {
        _state.value = AppUiState.Tasbih(tasbih)
    }

    /** Leaves the counter, keeping it, and shows the times again. */
    fun dismissTasbih() {
        val loaded = catalog ?: return
        val current = selection ?: return
        viewModelScope.launch { showTimes(current, loaded) }
    }

    /** One more count, stored so it survives a restart. */
    fun countTasbih() = updateTasbih(tasbih.counted())

    /** Back to zero, keeping the label and the target. */
    fun resetTasbih() = updateTasbih(tasbih.reset())

    fun setTasbihLabel(label: String) = updateTasbih(tasbih.copy(label = label))

    fun setTasbihTarget(target: Int?) = updateTasbih(tasbih.withTarget(target))

    /**
     * Shows and stores the counter — the one place the count moves, so what the
     * screen shows and what a restart restores cannot drift apart.
     */
    private fun updateTasbih(next: TasbihCounter) {
        if (next == tasbih) return
        tasbih = next
        _state.value = AppUiState.Tasbih(next)
        viewModelScope.launch { tasbihStore.save(next) }
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
