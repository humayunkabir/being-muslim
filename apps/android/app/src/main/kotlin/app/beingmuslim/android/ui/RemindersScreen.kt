package app.beingmuslim.android.ui

import android.Manifest
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.text.format.DateFormat
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import app.beingmuslim.android.R
import app.beingmuslim.android.prayer.Prayer
import app.beingmuslim.android.reminder.QuietHours
import app.beingmuslim.android.reminder.ReminderNotifications
import app.beingmuslim.android.reminder.ReminderPreferences
import java.time.LocalTime

/**
 * The user's opt-in prayer reminders (issue #12). Every prayer is off until the
 * user turns it on here; nothing is selected on their behalf and nothing is
 * pre-checked. Turning one on for the first time explains the benefit and the
 * privacy of the message before the system permission prompt, and a denied
 * permission leaves the app fully usable with a route to allow it later.
 *
 * Quiet hours suppress reminders inside the chosen window rather than delaying
 * them, and the copy says plainly that exact delivery depends on the operating
 * system and battery policy.
 */
@Composable
fun RemindersScreen(
    preferences: ReminderPreferences,
    onPrayerChange: (Prayer, Boolean) -> Unit,
    onQuietHoursEnabledChange: (Boolean) -> Unit,
    onQuietHoursChange: (QuietHours) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BackHandler(onBack = onBack)

    val context = LocalContext.current
    var notificationsAllowed by remember { mutableStateOf(ReminderNotifications.canPost(context)) }
    // Notification access can change in system settings while this screen is open.
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        notificationsAllowed = ReminderNotifications.canPost(context)
    }
    var explainPermission by rememberSaveable { mutableStateOf(false) }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted -> notificationsAllowed = granted }

    Scaffold(modifier = modifier.fillMaxSize()) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = stringResource(R.string.reminders_title),
                style = MaterialTheme.typography.headlineSmall,
            )
            Text(
                text = stringResource(R.string.reminders_intro),
                style = MaterialTheme.typography.bodyMedium,
            )

            Text(
                text = stringResource(R.string.reminders_prayers_heading),
                style = MaterialTheme.typography.titleMedium,
            )
            Prayer.entries.forEach { prayer ->
                SwitchRow(
                    label = stringResource(prayer.labelRes()),
                    checked = prayer in preferences.enabledPrayers,
                    onCheckedChange = { enabled ->
                        onPrayerChange(prayer, enabled)
                        if (enabled && !notificationsAllowed) explainPermission = true
                    },
                )
            }
            if (preferences.enabledPrayers.isEmpty()) {
                Text(
                    text = stringResource(R.string.reminders_none_enabled),
                    style = MaterialTheme.typography.bodySmall,
                )
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            SwitchRow(
                label = stringResource(R.string.reminders_quiet_hours),
                checked = preferences.quietHoursEnabled,
                onCheckedChange = onQuietHoursEnabledChange,
            )
            Text(
                text = stringResource(R.string.reminders_quiet_hours_help),
                style = MaterialTheme.typography.bodySmall,
            )
            if (preferences.quietHoursEnabled) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = {
                            pickTime(context, preferences.quietHours.start) { picked ->
                                onQuietHoursChange(preferences.quietHours.copy(start = picked))
                            }
                        },
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(
                            stringResource(
                                R.string.reminders_quiet_hours_from,
                                formatClock(context, preferences.quietHours.start),
                            ),
                        )
                    }
                    OutlinedButton(
                        onClick = {
                            pickTime(context, preferences.quietHours.end) { picked ->
                                onQuietHoursChange(preferences.quietHours.copy(end = picked))
                            }
                        },
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(
                            stringResource(
                                R.string.reminders_quiet_hours_to,
                                formatClock(context, preferences.quietHours.end),
                            ),
                        )
                    }
                }
            }

            Text(
                text = stringResource(R.string.reminders_privacy_note),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = stringResource(R.string.reminders_delivery_note),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            if (preferences.enabledPrayers.isNotEmpty() && !notificationsAllowed) {
                Text(
                    text = stringResource(R.string.reminders_permission_denied),
                    style = MaterialTheme.typography.bodyMedium,
                )
                OutlinedButton(onClick = { context.openNotificationSettings() }) {
                    Text(stringResource(R.string.reminders_open_settings))
                }
            }

            TextButton(onClick = onBack) {
                Text(stringResource(R.string.reminders_back))
            }
        }
    }

    if (explainPermission) {
        AlertDialog(
            onDismissRequest = { explainPermission = false },
            title = { Text(stringResource(R.string.reminders_permission_title)) },
            text = { Text(stringResource(R.string.reminders_permission_body)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        explainPermission = false
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    },
                ) {
                    Text(stringResource(R.string.reminders_permission_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { explainPermission = false }) {
                    Text(stringResource(R.string.reminders_permission_later))
                }
            },
        )
    }
}

/** One labelled on/off row; the row, not the switch, carries the toggle semantics. */
@Composable
private fun SwitchRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .toggleable(value = checked, role = Role.Switch, onValueChange = onCheckedChange),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyLarge)
        Switch(checked = checked, onCheckedChange = null)
    }
}

private fun Context.openNotificationSettings() {
    startActivity(
        Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
            .putExtra(Settings.EXTRA_APP_PACKAGE, packageName),
    )
}

private fun pickTime(context: Context, initial: LocalTime, onPicked: (LocalTime) -> Unit) {
    TimePickerDialog(
        context,
        { _, hour, minute -> onPicked(LocalTime.of(hour, minute)) },
        initial.hour,
        initial.minute,
        DateFormat.is24HourFormat(context),
    ).show()
}


