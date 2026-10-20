package app.beingmuslim.android.reminder

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import app.beingmuslim.android.MainActivity
import app.beingmuslim.android.R

/**
 * The one notification a reminder produces (issue #12). The copy is deliberately
 * neutral: the app name is the only identifier, and no prayer name, time, or
 * other devotional detail is shown — so neither a lock screen nor a shared
 * notification reveals what the user was prompted about.
 *
 * Delivery is the operating system's to decide; the app only asks.
 */
object ReminderNotifications {

    private const val CHANNEL_ID = "prayer_reminders"
    private const val NOTIFICATION_ID = 12

    fun ensureChannel(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.reminders_channel_name),
                NotificationManager.IMPORTANCE_DEFAULT,
            ).apply {
                description = context.getString(R.string.reminders_channel_description)
                setShowBadge(false)
            },
        )
    }

    /**
     * Whether a reminder could be delivered right now: the runtime permission
     * where one exists, the app-level notification switch, and this channel's
     * own switch. Used by the screen so a revocation in system settings is shown
     * as plainly as a denied permission, and by [show] so nothing posts into a
     * setting the user has turned off.
     */
    fun canPost(context: Context): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            return false
        }
        if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) return false
        val channel = context.getSystemService(NotificationManager::class.java)
            ?.getNotificationChannel(CHANNEL_ID)
        return channel == null || channel.importance != NotificationManager.IMPORTANCE_NONE
    }

    /** Posts one neutral reminder under [tag], unless the user has not allowed it. */
    fun show(context: Context, tag: String) {
        // The same permission [canPost] checks, restated here because Android
        // Lint's flow analysis cannot follow it through another function.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        if (!canPost(context)) return

        ensureChannel(context)
        val openApp = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(context.getString(R.string.reminder_notification_title))
            .setContentText(context.getString(R.string.reminder_notification_body))
            .setContentIntent(openApp)
            .setAutoCancel(true)
            // Hidden while the device is locked, in case the content is ever not
            // as neutral as it is today.
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .build()

        NotificationManagerCompat.from(context).notify(tag, NOTIFICATION_ID, notification)
    }
}
