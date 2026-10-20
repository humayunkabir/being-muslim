package app.beingmuslim.android.reminder

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import app.beingmuslim.android.prayer.Prayer
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import kotlinx.coroutines.CancellationException

/**
 * Delivers one scheduled reminder and extends the horizon (issue #12). It posts
 * the same neutral notification for every prayer; the tag only keeps two
 * delivered reminders from collapsing into one, and carries no devotional
 * detail itself.
 *
 * Delivery re-checks the stored preferences and the clock: a request that had
 * already started when the user switched a prayer off or turned quiet hours on
 * does not prompt anyway, and a request the OS held past [STALE_AFTER] is
 * dropped rather than arriving as a misleading late prompt. WorkManager persists
 * the request across process death and reboot, so the app needs no background
 * service of its own. Extending after delivery means reminders do not run out
 * after the initial horizon while the app stays closed.
 */
class ReminderWorker(appContext: Context, params: WorkerParameters) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val prayer = inputData.getString(KEY_PRAYER)
            ?.let { stored -> Prayer.entries.firstOrNull { it.name == stored } }
            ?: return Result.success()
        val preferences = ReminderStore(applicationContext).load()
        if (prayer !in preferences.enabledPrayers) return Result.success()

        // Fail closed on a value that cannot be read: not prompting is safer
        // than prompting inside quiet hours.
        val zone = inputData.getString(KEY_ZONE)
            ?.let { stored -> runCatching { ZoneId.of(stored) }.getOrNull() }
            ?: return extendOrRetry()

        val now = Instant.now()
        val at = Instant.ofEpochSecond(inputData.getLong(KEY_AT, now.epochSecond))
        if (now.isAfter(at.plus(STALE_AFTER))) return extendOrRetry()
        if (preferences.quietHoursEnabled &&
            preferences.quietHours.contains(now.atZone(zone).toLocalTime())
        ) {
            return extendOrRetry()
        }

        // Extend first: if the queue cannot be extended, retrying must not have
        // already posted the notification again.
        return if (extend()) {
            ReminderNotifications.show(applicationContext, inputData.getString(KEY_TAG) ?: prayer.name)
            Result.success()
        } else {
            Result.retry()
        }
    }

    /** A request that posts nothing still extends the horizon, so it does not run dry. */
    private suspend fun extendOrRetry(): Result = if (extend()) Result.success() else Result.retry()

    /** Extends without swallowing cancellation, which must unwind the worker. */
    private suspend fun extend(): Boolean = try {
        ReminderScheduler(applicationContext).extend()
        true
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (_: Exception) {
        false
    }

    companion object {
        const val KEY_TAG = "tag"
        const val KEY_PRAYER = "prayer"
        const val KEY_ZONE = "zone"
        const val KEY_AT = "at"
    }
}

/**
 * How late a reminder may still be posted. The OS may defer work under Doze or
 * battery policy; a prompt for a prayer from hours ago would be misleading, so
 * it is dropped instead.
 */
private val STALE_AFTER: Duration = Duration.ofHours(1)
