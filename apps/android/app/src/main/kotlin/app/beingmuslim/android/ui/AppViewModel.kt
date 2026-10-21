package app.beingmuslim.android.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import app.beingmuslim.android.citycatalog.BundledCityCatalog
import app.beingmuslim.android.citycatalog.City
import app.beingmuslim.android.citycatalog.CityCatalog
import app.beingmuslim.android.hijri.HijriCalendar
import app.beingmuslim.android.prayer.CalculationSettings
import app.beingmuslim.android.prayer.Prayer
import app.beingmuslim.android.prayer.PrayerOffsets
import app.beingmuslim.android.prayer.PrayerTimeCalculator
import app.beingmuslim.android.prayer.TwoDayPrayerTimes
import app.beingmuslim.android.reminder.QuietHours
import app.beingmuslim.android.reminder.ReminderPreferences
import app.beingmuslim.android.reminder.ReminderScheduler
import app.beingmuslim.android.reminder.ReminderStore
import app.beingmuslim.android.selection.SelectionStore
import app.beingmuslim.android.selection.UserSelection
import app.beingmuslim.android.sync.AppwriteSyncService
import app.beingmuslim.android.sync.SyncCoordinator
import app.beingmuslim.android.sync.SyncOutcome
import app.beingmuslim.android.sync.SyncPayload
import app.beingmuslim.android.sync.SyncStateStore
import app.beingmuslim.android.sync.toSyncedSettings
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
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/** What the app is showing: a load, the first-run setup, the times, the qibla, the settings, the tasbih, or the reminders. */
sealed interface AppUiState {

    data object Loading : AppUiState

    /** The bundled catalog could not be read; nothing can be shown. */
    data object CatalogUnavailable : AppUiState

    /** First run, or a stored selection that is no longer usable. */
    data class Setup(val catalog: CityCatalog) : AppUiState

    /**
     * The user's times: the city's current local date and the day after, either
     * of which is null when the sun neither rises nor sets there that day, with
     * the ±1-day shift applied to the calculated Hijri dates. [days] stays the
     * raw calculation; [offsets] is the user's own adjustment and is applied only
     * where it is shown (issue #8).
     */
    data class Times(
        val city: City,
        val settings: CalculationSettings,
        val days: TwoDayPrayerTimes,
        val hijriDayShift: Int,
        val offsets: PrayerOffsets,
    ) : AppUiState

    /** The qibla for the user's city, or no city when none is selected. */
    data class Qibla(val city: City?) : AppUiState

    /** The counter the user is counting with, as it was left last time. */
    data class Tasbih(val counter: TasbihCounter) : AppUiState

    /**
     * The reminders the user has asked for. Each change is saved as it is made,
     * so leaving this screen never discards a choice (issue #12).
     */
    data class Reminders(val preferences: ReminderPreferences) : AppUiState

    /**
     * The user is changing their city or calculation choices. Nothing is
     * persisted until they save, so opening settings never changes anything.
     */
    data class Settings(val catalog: CityCatalog, val selection: UserSelection) : AppUiState
}

/**
 * Orchestrates the app: loads the offline catalog once, reads the persisted
 * selection and counter, and calculates today's and tomorrow's times. All real
 * logic lives in the domain (catalog, calculation, [SelectionStore],
 * [TasbihCounter]); this only wires it to the UI and keeps heavy work off the
 * main thread.
 *
 * "Today" is the city's local date, and it moves: times are recomputed when the
 * app returns to the foreground ([refreshTimes]) and again at the city's next
 * local midnight, so a session is never left showing yesterday's times under
 * today's heading.
 */
class AppViewModel(application: Application) : AndroidViewModel(application) {

    private val store = SelectionStore(application)
    private val tasbihStore = TasbihStore(application)
    private val reminderStore = ReminderStore(application)
    private val reminderScheduler = ReminderScheduler(application)
    private val syncState = SyncStateStore(application)
    private val sync = SyncCoordinator(AppwriteSyncService(application), syncState)
    private val _state = MutableStateFlow<AppUiState>(AppUiState.Loading)
    val state: StateFlow<AppUiState> = _state.asStateFlow()

    private var catalog: CityCatalog? = null
    private var selection: UserSelection? = null
    private var tasbih = TasbihCounter()
    private var reminders = ReminderPreferences()
    private var rolloverJob: Job? = null
    private var syncJob: Job? = null
    private var timesRequest = 0
    private val remindersUpdate = Mutex()

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
            reminders = reminderStore.load()
            val stored = store.load()
            if (stored == null) {
                _state.value = AppUiState.Setup(loaded)
            } else {
                showTimes(stored, loaded)
                // The account may already hold settings; the first composition
                // runs before this load finishes, so ask again now that there is
                // a selection to reconcile.
                requestSync(0)
            }
            // Re-schedule on launch only when the queue is empty, so a reminder
            // the OS has deferred is not cancelled and lost by opening the app.
            reminderScheduler.ensureScheduled()
        }
    }

    /**
     * Records the choices the user made — at first run, or in settings — and
     * shows the times they produce. This is the only way a choice is ever
     * changed, so nothing is changed on the user's behalf. The Hijri date shift
     * is the user's own and is carried over untouched. Offsets are the user's own
     * and are stored beside the calculation settings, never inside them.
     */
    fun applySelection(cityId: Long, settings: CalculationSettings, offsets: PrayerOffsets) {
        val loaded = catalog ?: return
        viewModelScope.launch {
            val chosen = UserSelection(cityId, settings, selection?.hijriDayShift ?: 0, offsets)
            store.save(chosen)
            showTimes(chosen, loaded)
            // The city, method and offsets all move reminder times.
            reminderScheduler.reschedule()
            requestSync(0)
        }
    }

    /**
     * Records the choices made in setup. A forced setup — the stored city is no
     * longer in the catalog — keeps the user's saved offsets, because nothing is
     * changed on their behalf; on first run there are none to keep.
     */
    fun completeSetup(cityId: Long, settings: CalculationSettings) {
        applySelection(cityId, settings, selection?.offsets ?: PrayerOffsets())
    }

    /**
     * Shifts the calculated Hijri dates by [delta] days — the UI offers ±1 — so
     * they match the user's own country's or community's announcement, and
     * remembers the shift. Values that would move a date further are clamped, so
     * the stored shift is always one the app supports.
     */
    fun shiftHijriDay(delta: Int) {
        val loaded = catalog ?: return
        val current = selection ?: return
        viewModelScope.launch {
            val shifted = current.copy(
                hijriDayShift = HijriCalendar.clampDayShift(current.hijriDayShift + delta),
            )
            store.save(shifted)
            showTimes(shifted, loaded)
            requestSync(0)
        }
    }

    /** Opens the settings for the current selection, changing nothing. */
    fun openSettings() {
        val loaded = catalog ?: return
        val current = selection ?: return
        _state.value = AppUiState.Settings(loaded, current)
    }

    /** Leaves settings with the choices the user already had. */
    fun dismissSettings() = showStoredSelection()

    /**
     * Recomputes today's times for the current selection. The UI calls this when
     * the app returns to the foreground, so a session resumed after midnight
     * shows the new day rather than yesterday's times.
     *
     * Only while the times are on screen: returning to another screen — the
     * settings, or the counter — must not replace it, or discard edits the user
     * has not saved.
     */
    fun refreshTimes() {
        requestSync(0)
        if (_state.value !is AppUiState.Times) return
        showStoredSelection()
    }

    /** Shows the times for the selection the user already had. */
    private fun showStoredSelection() {
        val loaded = catalog ?: return
        val current = selection ?: return
        viewModelScope.launch { showTimes(current, loaded) }
    }

    /** Opens the qibla for the city the user already chose. */
    fun openQibla() {
        val loaded = catalog ?: return
        _state.value = AppUiState.Qibla(selection?.let { loaded.byId(it.cityId) })
    }

    /** Leaves the qibla and shows the times again. */
    fun dismissQibla() = showStoredSelection()

    /** Opens the counter as the user last left it. */
    fun openTasbih() {
        _state.value = AppUiState.Tasbih(tasbih)
    }

    /** Opens the reminders, showing only what the user has already chosen. */
    fun openReminders() {
        _state.value = AppUiState.Reminders(reminders)
    }

    /** Leaves the reminders, keeping every choice made there. */
    fun dismissReminders() = showStoredSelection()

    fun setReminderEnabled(prayer: Prayer, enabled: Boolean) =
        updateReminders(reminders.withReminder(prayer, enabled))

    fun setQuietHoursEnabled(enabled: Boolean) =
        updateReminders(reminders.copy(quietHoursEnabled = enabled))

    fun setQuietHours(quietHours: QuietHours) =
        updateReminders(reminders.copy(quietHours = quietHours))

    /**
     * Shows, stores and re-schedules the reminder choices — the one place they
     * change, so what the screen shows and what the OS will deliver cannot drift.
     */
    private fun updateReminders(next: ReminderPreferences) {
        if (next == reminders) return
        reminders = next
        _state.value = AppUiState.Reminders(next)
        viewModelScope.launch {
            // Saves are serialised and each writes the latest value, so two
            // quick toggles cannot leave an older choice as the stored one.
            remindersUpdate.withLock {
                reminderStore.save(reminders)
                reminderScheduler.reschedule()
            }
        }
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
        val countChanged = next.count != tasbih.count
        tasbih = next
        _state.value = AppUiState.Tasbih(next)
        viewModelScope.launch { tasbihStore.save(next) }
        // Counting is rapid; the total is sent once the user pauses. A label or
        // target change alone is not synced and does not trigger a push.
        if (countChanged) requestSync(TASBIH_SYNC_DELAY_MS)
    }

    /**
     * Reconciles with the account's stored settings and tasbih total. Called
     * when the app appears and when the user returns to the foreground; it is
     * best-effort, so a signed-out or offline device behaves exactly as before.
     */
    fun syncNow() = requestSync(0)

    /**
     * Runs one sync attempt unless there is nothing to sync yet. Attempts are
     * cancelled and replaced by a newer one, so a burst of changes sends only
     * its latest state.
     */
    private fun requestSync(debounceMs: Long) {
        val loaded = catalog ?: return
        // The settings screen holds unsaved edits; applying a pull under them
        // would discard work the user has not chosen to keep.
        if (_state.value is AppUiState.Settings) return
        val userId = syncState.signedInUserId() ?: return
        val local = currentPayload() ?: return
        syncJob?.cancel()
        syncJob = viewModelScope.launch {
            if (debounceMs > 0) delay(debounceMs)
            val outcome = sync.sync(userId, local)
            if (outcome is SyncOutcome.Apply) {
                // A pull is only recorded once its result is what this device
                // holds, so a change made while the request was in flight is
                // uploaded next rather than silently dropped.
                val applied = applySynced(outcome.payload, local, loaded)
                if (applied != null) syncState.markSynced(userId, applied)
            }
        }
    }

    /** The payload this device would send: settings without the city, plus the tasbih total. */
    private fun currentPayload(): SyncPayload? {
        val current = selection ?: return null
        return SyncPayload(current.toSyncedSettings(currentLanguage().tag), tasbih.count)
    }

    /**
     * Applies the server's settings and total, keeping the device-local city,
     * and returns the payload the device now holds. Returns null when the local
     * snapshot changed while the sync was in flight, so that change is not lost.
     */
    private suspend fun applySynced(
        remote: SyncPayload,
        localSnapshot: SyncPayload,
        loaded: CityCatalog,
    ): SyncPayload? {
        // An open settings screen holds unsaved edits; a pull that landed after
        // it opened must not replace them.
        if (_state.value is AppUiState.Settings) return null
        val current = selection ?: return null
        if (currentPayload() != localSnapshot) return null

        val language = remote.settings.language
            ?.let { tag -> Language.entries.firstOrNull { it.tag == tag } }
        val settings = remote.settings.copy(language = language?.tag ?: currentLanguage().tag)
        val next = current.copy(
            settings = settings.calculationSettings,
            hijriDayShift = settings.hijriDayShift,
            offsets = settings.offsets,
        )
        store.save(next)
        // The canonical in-memory selection must move with the stored one, even
        // when the screen on top is not the times screen: the next screen shown
        // and the next sync both read it.
        selection = next

        val nextCounter = tasbih.copy(count = remote.tasbihTotal)
        if (nextCounter != tasbih) {
            tasbih = nextCounter
            tasbihStore.save(tasbih)
        }

        when (_state.value) {
            is AppUiState.Times -> showTimes(next, loaded)
            is AppUiState.Tasbih -> _state.value = AppUiState.Tasbih(tasbih)
            else -> Unit
        }
        // Last, because the platform applies a language change by recreating the
        // activity; everything above is already stored when that happens.
        if (language != null && language != currentLanguage()) applyLanguage(language)
        return SyncPayload(settings, tasbih.count)
    }

    private suspend fun showTimes(chosen: UserSelection, catalog: CityCatalog) {
        // Only the newest request may write the times: a slower earlier one —
        // a foreground refresh overtaken by a save — must not put the times of
        // the choice before it back on screen. All of this runs on the main
        // thread, so the count only changes between a request's start and its
        // write.
        val request = ++timesRequest
        // A city id that is no longer in the catalog falls back to setup rather
        // than showing nothing (a catalog change is a data migration).
        val city = catalog.byId(chosen.cityId) ?: run {
            _state.value = AppUiState.Setup(catalog)
            return
        }
        selection = chosen
        val days = daysAt(city, chosen.settings)
        if (request != timesRequest) return
        _state.value = AppUiState.Times(
            city = city,
            settings = chosen.settings,
            days = days,
            hijriDayShift = chosen.hijriDayShift,
            offsets = chosen.offsets,
        )
        scheduleRollover(city, chosen.settings)
    }

    private suspend fun daysAt(city: City, settings: CalculationSettings): TwoDayPrayerTimes =
        withContext(Dispatchers.Default) {
            PrayerTimeCalculator.forInstantAndNextDay(city, Instant.now(), settings)
        }

    /** Recomputes at the city's next local midnight so an open app rolls over. */
    private fun scheduleRollover(city: City, settings: CalculationSettings) {
        // Runs in viewModelScope, so it is cancelled with the ViewModel.
        rolloverJob?.cancel()
        rolloverJob = viewModelScope.launch {
            val zone = ZoneId.of(city.timezone)
            val now = Instant.now()
            delay(Duration.between(now, nextLocalMidnight(now, zone)).toMillis().coerceAtLeast(0))
            refreshTimes()
        }
    }
}

/** The city's next local midnight after [now] — when the shown day rolls over. */
internal fun nextLocalMidnight(now: Instant, zone: ZoneId): Instant =
    now.atZone(zone).toLocalDate().plusDays(1).atStartOfDay(zone).toInstant()

/** How long counting must pause before the tasbih total is sent. */
private const val TASBIH_SYNC_DELAY_MS = 2_000L
