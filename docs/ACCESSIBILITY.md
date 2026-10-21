# Accessibility, locale and state coverage

**Status:** Verification record for issue #19 and the Android release gates in
`PRODUCT_REQUIREMENTS.md` §13. Covers a build containing calculations. Last run
2026-10-21 on Android (API 37, 1080×2400 at 420 dpi). **Owns:** what was checked,
how, what was fixed, and what remains a human gate. **Defers:** requirements →
`PRODUCT_REQUIREMENTS.md` §8 and §13; visual hierarchy and typography →
`DESIGN_STANDARDS.md` §3 and §5; concurrency and test strategy →
`ARCHITECTURE.md` §4 and §8.

The iOS app is still a shell (`DECISIONS/0002`), so nothing here covers
VoiceOver, Dynamic Type, or an iOS build.

## How this was checked

- Every Compose screen was read against `DESIGN_STANDARDS.md` §6, §7 and §10:
  labels, reading order, state coverage, target sizes, fixed-size text, colour
  use, and motion.
- Android Lint (`./gradlew lint`) reports no accessibility or usability error.
- The debug build was installed on an emulator and each screen was exercised.
  The accessibility tree was inspected with `uiautomator`; TalkBack was enabled
  to check focus grouping and heading navigation.
- Every screen was rendered at the system's maximum text size (200%), in one
  language each: Bengali on the times, settings, reminders, report and account
  screens; English on the app shell, setup, qibla and tasbih screens (the app's
  data was cleared to reach first-run setup). English and Bengali were both
  exercised on the times screen at the default size.
- JVM tests cover the calculation edge cases listed below; the full suite and
  `assembleDebug` pass.

What this cannot replace: a full TalkBack pass on a physical device (hover,
gestures, speech order), a Bengali-speaking and Arabic-reading human review, and
measurement with a contrast analyser. Those are listed as open gates, and this
document does not claim them.

## Fixed in this pass

| Problem | Fix |
|---|---|
| Each prayer row was three TalkBack stops: localised name, Arabic name, time. | The row now merges its descendants (`TimesScreen.kt`), so one stop reads the name, its Arabic, and the time together. TalkBack's focus rectangle covers the whole row. |
| The city, screen title, and "Today"/"Tomorrow" headings were plain text, so heading navigation skipped the times list. | Marked with `heading()` semantics, including every screen's title and the form sections. TalkBack announces "Heading" when it reaches them. |
| One-line radio rows (language, and country/city results) measured 40 dp tall; reminder switch rows 44 dp — under the 48 dp minimum. | `defaultMinSize(minHeight = 48.dp)` on `SelectableRow` and `SwitchRow`; the whole row, not just the control, is the target. |
| At 200% text, the two Hijri ±1-day buttons squeezed each other: the first label clipped and the second wrapped one character per line. | Each button takes half the row (`weight(1f)`) and its label wraps. |
| Setup's disabled **Continue** gave no reason, leaving a screen-reader user to guess why it could not be used. | The hint above the button is also attached as the disabled button's state description (`setup_continue_hint`, both locales). |
| The catalog failure state told the user to "report the problem", but no report path is reachable from that screen. | The copy now gives the action that works: reinstall the app. |

Reviewed and left as they are: the qibla dial is visual only, with the numeric
bearing always in text beside it; compass state changes are not a live region
(the bearing and turn instruction update continuously, and announcing every
reading would be unusable); Arabic names are merged into their prayer row rather
than given a second stop.

Reduced motion: the app defines no custom animation, transition, or auto-playing
content. Two things move: the qibla dial rotates as the sensor reading changes
(live orientation feedback, with the same information in the bearing and turn
text), and the account screen's indeterminate progress indicators, which are
Compose infinite animations and stop when the system's animations are turned off.
The remaining movement is the system ripple and the tasbih button's haptic, which
follows the system's haptics setting.

## State coverage

Every state below is reachable in the current build and explains what happened
and what to do next. "Offline" is not an error for the core app: times, qibla,
tasbih, reminders, and settings are local, and only the optional account and the
problem report use the network.

| Screen | Empty | Loading | Error / failure | Permission denied |
|---|---|---|---|---|
| App shell | — | "Loading…" | Bundled city list unreadable → reinstall instruction | — |
| Setup / settings | No country or city matches | Location suggestion "finding…" | Location unavailable → search instead | Location denied → search instead, stated before the prompt |
| Times | A polar day says times cannot be calculated, and why | — | — | — |
| Qibla | No city selected | Compass "waiting…" | No compass, timed-out compass, calibration needed, approximate reading | Not applicable (bearing needs no permission) |
| Reminders | No reminder enabled | — | Notifications blocked → open system settings | Notification permission explained before the prompt; denial leaves the app usable |
| Tasbih | Label/target not set → what to do | — | — | — |
| Report | Note is optional | Sending | Send failed (offline or server) keeps the report to retry | — |
| Account | Signed out, with "continue without an account" | Checking, signing in, exporting, deleting | Check/sign-in/sign-out/export/delete failure, each with a next step | — |

Settings intentionally discards unsaved edits on Cancel or system back; the
screen says nothing changes until Save, so this is not a hidden loss.

## Calculation edge cases

Verified by the JVM suite; the linked files carry the reference values and their
provenance.

| Case | Covered by |
|---|---|
| Daylight-saving boundaries, both directions, with each day carrying its own offset | `PrayerTimeCalculatorTest.todayAndTomorrowFollowTheCitiesLocalCalendar`, `DayRolloverTest.aDaylightSavingChangeMovesTheMidnightInUtc` |
| Date rollover: the city's next local midnight, not the device's and not +24 h; also on resume and while the app stays open | `DayRolloverTest`, `AppViewModel.refreshTimes`/`scheduleRollover` |
| High latitude: rule-supplied Fajr/Isha, the rule's disclosure, and a polar day with no times | `PrayerTimeCalculatorTest` (Oslo, Edinburgh, Longyearbyen, Dhaka) |
| Timezone change: the device's zone never decides the day or the clock | `DayRolloverTest.theDeviceZoneDoesNotDecideTheDay`, `PrayerTimeCalculatorTest.todayAndTomorrowFollowTheCitiesLocalCalendar` |
| Hijri month of 29 and 30 days, year boundary, ±1-day clamp, and a local moon-sighting announcement | `HijriCalendarTest` |
| Qibla bearings against published directions, declination in both directions, turn sign, half turn, invalid coordinates | `QiblaBearingTest` |

A time-zone change while the app is open is handled by recomputing on return to
the foreground; the device zone is never used for a city's times.

## Locales

- Every user-facing string exists in English and Bengali, with the same format
  arguments, enforced by `LocalizationResourcesTest`.
- The Arabic prayer names render correctly right-to-left beside their localised
  names at both text sizes, and are kept as their own text paragraph so the
  platform shapes them.
- Bengali renders at 200% without clipping on the times, settings, reminders,
  report and account screens, apart from the Hijri button defect fixed above.
  English does the same on setup, qibla, tasbih and the app shell.
- **Bengali is still an unreviewed draft.** `values-bn/strings.xml` says so in its
  header: it was written by an implementing agent, not a Bengali speaker. Until a
  reviewer has read every string and the Arabic prayer names, Bengali is not a
  supported locale in the release sense (`DECISIONS/0006`). The Bengali city names
  are GeoNames source data, partial by source (`CITY_CATALOG.md`), not app copy.

## Release-gate status

Against `PRODUCT_REQUIREMENTS.md` §13, for this Android build:

| Gate | Status |
|---|---|
| Source provenance and licence documented | Met for the prayer library, city catalog and Hijri convention (their docs and `DECISIONS/0004`, `0005`) |
| References and attribution visible | Met: city attribution and calculation method are on the times screen |
| Content fixtures match the approved source/version | Met: pinned Adhan reference cases, city asset test, published Hijri dates |
| Prayer-time and qibla edge cases tested | Met, as listed above |
| Arabic shaping, RTL, locales and accessibility reviewed | **Partially met**: code, runtime semantics, TalkBack grouping, 200% text and Bengali rendering checked; **Bengali and Arabic wording review open** |
| Permission-denied, offline, empty and error states tested | **Partially met**: states were reviewed in code and the reachable ones exercised on the emulator; **the location and notification permission prompts still need a physical-device check** (`CITY_CATALOG.md`) |
| Privacy and security implications reviewed | Met: `PRIVACY.md`, `APPWRITE.md`, and the no-account report path |
| No generated text presented as verified source | Met: there is no generated religious content |
| Known limitations disclosed | **Partially met**: the UI states times variation, compass limits, delivery timing and the calculated Hijri; the launch-market timetable check remains open (`PRAYER_TIMES.md`) |
| Correction/reporting path exists | Met: the report screen, usable without an account |

## Open gates

1. **A full TalkBack pass on a physical device**, including the notification and
   location permission prompts, gesture navigation, and speech order.
2. **Bengali and Arabic human review**, including a per-screen layout pass in
   both languages, then removal of the draft notice in `values-bn/strings.xml`
   (`DECISIONS/0006`).
3. **Contrast measurement** with an analyser on light and dark appearance. The app
   uses Material 3's baseline or system-generated colour schemes and no custom
   text colours (`ui/theme/Theme.kt`), so no colour is invented here; that is not
   the same as a measured pass.
4. The calculation/compass physical-device and launch-market checks already
   recorded as open in `PRAYER_TIMES.md`, `QIBLA.md`, `HIJRI_CALENDAR.md` and
   `CITY_CATALOG.md`.
