package app.beingmuslim.android.reminder

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import app.beingmuslim.android.citycatalog.BundledCityCatalog
import app.beingmuslim.android.selection.SelectionStore
import java.time.Duration
import java.time.Instant
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * Hands the pure [ReminderSchedule] to WorkManager (issue #12). WorkManager is
 * used rather than an exact alarm: reminders must survive reboot and be
 * rescheduled without a boot receiver, and Android's own deferral under Doze and
 * battery policy is exactly the inexactness the app discloses to the user.
 *
 * [reschedule] replaces the previous set, so changing a city, a calculation
 * method, an offset, or a reminder choice can never leave a stale prompt behind.
 * [extend], called by a delivered reminder, adds the new end of the horizon
 * without cancelling the set the running worker belongs to. [ensureScheduled]
 * is the launch path: it leaves a live queue alone, so a reminder the OS has not
 * delivered yet is not cancelled and lost by merely opening the app.
 */
class ReminderScheduler(context: Context) {

    private val context = context.applicationContext
    private val workManager = WorkManager.getInstance(this.context)
    private val reminders = ReminderStore(this.context)
    private val selections = SelectionStore(this.context)

    suspend fun ensureScheduled() = withContext(Dispatchers.IO) {
        val pending = workManager.getWorkInfosByTagFlow(WORK_TAG)
            .first()
            .any { !it.state.isFinished }
        if (!pending) schedule(cancelExisting = true)
    }

    suspend fun reschedule() = withContext(Dispatchers.IO) { schedule(cancelExisting = true) }

    suspend fun extend() = withContext(Dispatchers.IO) { schedule(cancelExisting = false) }

    private suspend fun schedule(cancelExisting: Boolean) = lock.withLock {
        if (cancelExisting) workManager.cancelAllWorkByTag(WORK_TAG)

        val preferences = reminders.load()
        if (preferences.enabledPrayers.isEmpty()) return@withLock
        val selection = selections.load() ?: return@withLock
        // A catalog that cannot be read leaves the queue as it is rather than
        // crashing the caller; the next launch or settings change retries.
        val city = runCatching { BundledCityCatalog.load(context.assets) }
            .getOrNull()
            ?.byId(selection.cityId)
            ?: return@withLock

        val now = Instant.now()
        ReminderSchedule.upcoming(
            city = city,
            settings = selection.settings,
            offsets = selection.offsets,
            preferences = preferences,
            now = now,
        ).forEach { reminder ->
            val delay = Duration.between(now, reminder.at).toMillis().coerceAtLeast(0)
            val request = OneTimeWorkRequestBuilder<ReminderWorker>()
                .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                .setInputData(
                    workDataOf(
                        ReminderWorker.KEY_TAG to reminder.tag(),
                        ReminderWorker.KEY_PRAYER to reminder.prayer.name,
                        ReminderWorker.KEY_ZONE to city.timezone,
                        ReminderWorker.KEY_AT to reminder.at.epochSecond,
                    ),
                )
                .addTag(WORK_TAG)
                .build()
            // One unique request per prayer and instant: a repeated reschedule
            // replaces rather than duplicates it.
            workManager.enqueueUniqueWork(
                "${WORK_TAG}-${reminder.prayer.name}-${reminder.at.epochSecond}",
                ExistingWorkPolicy.REPLACE,
                request,
            )
        }
    }

    private fun Reminder.tag(): String = "${prayer.name}-$at"

    private companion object {
        const val WORK_TAG = "prayer_reminder_work"

        /** Serialises the app's reschedule with a worker extending the horizon. */
        val lock = Mutex()
    }
}
