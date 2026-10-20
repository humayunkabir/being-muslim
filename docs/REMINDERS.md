# Reminders

The opt-in prayer reminders (`docs/PRODUCT_REQUIREMENTS.md` §2, §2.1, §11;
issue #12). This records what the app schedules, the policy behind it, and the
limits the copy tells the user about.

## What is scheduled

Reminders are **local only**. There is no push service, no account, and no
network request; the app asks Android to deliver a notification at a time it has
already calculated offline.

- Every prayer is **off by default** and is turned on individually by the user.
- The time used is the one shown on the times screen: the raw calculated time
  plus the user's own per-prayer offset (issue #8).
- A window of the next **seven days** is scheduled at a time, today included.
  When a reminder is delivered, it extends the window again, so reminders do
  not stop while the app stays closed.
- The whole set is replaced whenever the city, calculation method, offsets, or
  reminder choices change, so a stale prompt is not left behind after a change.

The policy itself — `ReminderSchedule.upcoming` — is pure: it takes the city,
settings, offsets, preferences and the current instant, and returns the
notifications that should exist. It is unit-tested with fixed clocks and a real
city (`ReminderScheduleTest`).

## Quiet hours

Quiet hours are a user-chosen window in the **chosen city's local time** — the
same clock the times are shown on. A reminder that falls inside the window is
**skipped, not delayed**: the prompt belongs to the prayer, not to the first
moment the phone may beep. A window that crosses midnight (for example 22:00 to
06:00) wraps correctly; equal start and end means an empty window.

## Permission and delivery

- The notification permission is requested **only after** the user turns on a
  reminder, and an in-app explanation of the benefit, the neutral copy and the
  delivery limits is shown before the system prompt.
- Denying the permission leaves every other feature working. The reminders
  screen then explains how to allow notifications later and links to the
  system notification settings.
- Delivery is **not exact**. WorkManager is used rather than an exact alarm: it
  survives reboot and process death without a boot receiver or the
  `SCHEDULE_EXACT_ALARM` permission. Android may defer a reminder under Doze or
  the device's battery policy, so a reminder can arrive after the calculated
  time. The app says this in the reminder copy rather than promising a time the
  OS cannot guarantee.

## Privacy of the notification

The notification is deliberately neutral: the app name and "Tap to open the
app." No prayer name, no calculated time, no other devotional detail is shown,
including on a lock screen. It is also marked `VISIBILITY_PRIVATE`, so the
content stays hidden while the device is locked even if the copy changes later.

Reminder choices are stored locally beside the selection, in their own
preferences file. Which prayers a user asked to be prompted for is devotional
activity, so that file is excluded from platform cloud backup and
device-to-device transfer, like the tasbih counter
(`docs/ARCHITECTURE.md` §7). It is not location data and holds no timestamps.

## Adding a prayer or changing the policy

- The policy is in `apps/android/app/src/main/kotlin/app/beingmuslim/android/reminder/ReminderSchedule.kt`.
- The notification channel, worker and scheduling adapter are beside it.
- Update `ReminderScheduleTest` and this file with any change to the horizon,
  quiet-hours semantics, or what is scheduled.
