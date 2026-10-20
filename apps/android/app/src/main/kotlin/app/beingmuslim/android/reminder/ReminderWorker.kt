package app.beingmuslim.android.reminder

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import app.beingmuslim.android.prayer.Prayer
import java.time.Instant
import java.time.ZoneId

/**
 * Delivers one scheduled reminder and extends the horizon (issue #12). It posts
 * the same neutral notification for every prayer; the tag only keeps two
 * delivered reminders from collapsing into one, and carries no devotional
 * detail itself.
 *
 * The stored preferences are read again at delivery, so a request that had
 * already started when the user switched a prayer off or turned quiet hours on
 * does not prompt anyway. WorkManager persists the request across process death
 * and reboot, so the app needs no background service of its own. Extending after
 * delivery means reminders do not run out after the initial horizon while the
 * app stays closed.
 */
class ReminderWorker(appContext: Context, params: WorkerParameters) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val prayer = inputData.getString(KEY_PRAYER)
            ?.let { stored -> Prayer.entries.firstOrNull { it.name == stored } }
            ?: return Result.success()
        val preferences = ReminderStore(applicationContext).load()
        if (prayer !in preferences.enabledPrayers) return Result.success()

        val zone = inputData.getString(KEY_ZONE)
            ?.let { stored -> runCatching { ZoneId.of(stored) }.getOrNull() }
        if (zone != null && preferences.quietHoursEnabled &&
            preferences.quietHours.contains(Instant.now().atZone(zone).toLocalTime())
        ) {
            return Result.success()
        }

        ReminderNotifications.show(applicationContext, inputData.getString(KEY_TAG) ?: prayer.name)
        // Retry rather than end the horizon silently; the notification is
        // replaced, not duplicated, because its tag is stable.
        return if (runCatching { ReminderScheduler(applicationContext).extend() }.isSuccess) {
            Result.success()
        } else {
            Result.retry()
        }
    }

    companion object {
        const val KEY_TAG = "tag"
        const val KEY_PRAYER = "prayer"
        const val KEY_ZONE = "zone"
    }
}
