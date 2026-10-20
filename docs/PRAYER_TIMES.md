# Prayer-time calculation

The library and parameters behind every calculated prayer time. This is the
record `docs/PRODUCT_REQUIREMENTS.md` §5 requires a build containing
calculations to carry: library, version, license, the methods and their
parameters, and how time zones and daylight saving are handled.

## Library

| | |
| --- | --- |
| Library | BatoulApps **Adhan** — <https://github.com/batoulapps/adhan> |
| Artifact | `com.batoulapps.adhan:adhan:1.2.1` |
| License | **MIT** (Copyright © 2016 Batoul Apps). The 1.2.1 POM omits the license element; the license is the repo's `LICENSE` at tag `v1.2.1`. |
| Network | None. The library is pure computation; it makes no network or file access. |
| Runtime | Offline. Used inside the app target; nothing about it requires a permission. |

**Why this artifact.** BatoulApps publishes two distributions. `adhan2` is the
Kotlin Multiplatform rewrite, but at the time of writing it is pre-1.0 (0.0.5)
and publishes **no Android target** (jvm/js/native only), so an Android app can
only consume it by depending on the bare `-jvm` variant. `adhan:1.2.1` is the
mature, dependency-free JVM artifact and is callable from Kotlin. The choice was
confirmed while implementing issue #4; swapping to `adhan2` later is contained to
`PrayerTimeCalculator` and this file.

## Methods offered

The user picks one; nothing is pre-selected (issue #4). Each maps to an Adhan
`CalculationMethod`, whose Fajr and Isha angles and built-in adjustments are
shown below (read from the resolved library, not restated by hand).

| Choice | Fajr angle | Isha | Built-in adjustments (minutes) |
| --- | --- | --- | --- |
| Muslim World League | 18.0° | 17.0° | Dhuhr +1 |
| University of Islamic Sciences, Karachi | 18.0° | 18.0° | Dhuhr +1 |
| Umm al-Qura, Makkah | 18.5° | 90 min after Maghrib | — |
| Egyptian General Authority | 19.5° | 17.5° | Dhuhr +1 |
| Moonsighting Committee | 18.0° | 18.0° | Dhuhr +5, Maghrib +3; seasonal twilight at high latitude |
| Gulf (Dubai) | 18.2° | 18.2° | Sunrise −3, Dhuhr +3, Asr +3, Maghrib +3 |
| Kuwait | 18.0° | 17.5° | — |
| Qatar | 18.0° | 90 min after Maghrib | — |
| Singapore | 20.0° | 18.0° | Dhuhr +1 |
| North America (ISNA) | 15.0° | 15.0° | Dhuhr +1 |

These figures vary by method, location, date and local authority; **no method is
universally accepted**, and the app states which one produced a time rather than
implying a neutral truth (§5).

### Juristic method for Asr

| Choice | Convention | Shadow length | Adhan |
| --- | --- | --- | --- |
| Standard | Majority (Shafi'i, Maliki, Hanbali) | 1× | `SHAFI` |
| Hanafi | Hanafi | 2× (Asr later) | `HANAFI` |

### High-latitude rule

The user picks the rule; **nothing is pre-selected and the times screen shows
which one is in force** (issue #7). Each maps to an Adhan `HighLatitudeRule`:

| Choice | Adhan | The bound it sets |
| --- | --- | --- |
| Middle of the night | `MIDDLE_OF_THE_NIGHT` | Fajr no earlier, and Isha no later, than the middle of the night (½ of the night) |
| Seventh of the night | `SEVENTH_OF_THE_NIGHT` | Fajr no earlier than the last seventh of the night; Isha no later than the end of the first (1/7 each) |
| Twilight angle | `TWILIGHT_ANGLE` | The method's own Fajr and Isha angles, each ÷ 60, of the night |

Adhan substitutes the bound in **two** cases: where the sun never reaches the
angle at all, and where the twilight it computed falls beyond the bound. It does
not report which happened, and for `MIDDLE_OF_THE_NIGHT` that cannot be inferred
from its public output — comparing the times two rules return reports the rule as
engaged at 50–57°N in summer, where the middle-of-the-night bound is never
reached and true dawn stands. `PrayerTimeCalculator` therefore recomputes the raw
twilight with the library's own `internal.SolarTime`, carries the same minute
adjustments and rounding Adhan does, and compares it with the time Adhan
returned: a difference means the rule produced that time. This **mirrors
`PrayerTimes` in adhan 1.2.1** and is pinned by a test; revisit it, like the rest
of this file, if the library is upgraded.

The times screen discloses this per day, and only where the rule produced Fajr or
Isha; on a day where true dawn and dusk stand it says nothing about the rule.

This is **not uniform across methods**:

- **Moonsighting Committee** does not use the rule at all — Adhan applies its own
  seasonal twilight adjustment for that method instead. The times screen says so
  beside the rule, and the choice cannot change that method's times.
- **Umm al-Qura and Qatar** take Isha from a fixed interval after Maghrib, so
  neither the Isha angle nor the rule applies to Isha there.

### Offsets

Per-prayer user offsets are stored as whole minutes, from −60 to +60, and are
**off by default** (issue #8). They are held apart from the calculation settings
and applied only where a time is shown, never folded into the library parameters
or the raw result: the times screen shows the adjusted time with the calculated
time it moved beside it. Clearing an offset restores that prayer's raw time. The
stored selection carries them, so they survive a restart, and a selection stored
before offsets existed reads as no offsets at all.

## Time, dates, and daylight saving

- Every calculation is performed for the **city's IANA time zone**, taken from
  the bundled catalog (`docs/CITY_CATALOG.md`). Daylight saving is handled by the
  zone, never by a fixed offset — a BST date and a GMT date for London come out an
  hour apart in wall time and correct in instant.
- The **local date is the city's**, not the device's: an instant that is already
  tomorrow in Dhaka computes Dhaka's tomorrow. The current time is always injected
  (`forInstantAndNextDay`), never read inside the domain, so tests use a fixed
  clock. That one query returns the city's current date and the day after, each
  computed on its own date rather than as 24 hours later, so both are right across
  a daylight-saving change (issue #5).
- Adhan returns true instants; sub-second noise is dropped so a time is a clean
  minute.
- Where the sun neither rises nor sets — a polar day or night — the library
  cannot place Dhuhr or Maghrib, and the calculator returns **no times** rather
  than an invented value. The app shows an unavailable state.

## Reference cases and tolerance

Tests compare against the reference cases BatoulApps ships with the library
(`Shared/Times/*.json`), which the library validates against the official
timetables they were taken from (moonsighting.com for London, ummulqura.org.sa for
Makkah) to **±1 minute** — the tolerance reused in `PrayerTimeCalculatorTest`
(Kuwait's own file allows 2). Covered: **six methods** — Moonsighting Committee,
Umm al-Qura, Singapore, Gulf (Dubai), Kuwait and Qatar — across both Asr
conventions, a DST date and a standard-time date, large UTC offsets
(Asia/Riyadh, Asia/Dubai), a date rollover (Asia/Dhaka), and an unavailable polar
date.

**High latitude.** The library's own high-latitude rule case
(`testHighLatitudeRule` in the suite of its Kotlin port, `adhan2`; the pinned
1.2.1 artifact returns the same values) is pinned for all three rules — Fajr and
Isha 01:14 for middle of the night, 03:31 and 22:56 for seventh of the night,
02:31 and 23:50 for twilight angle — so the rule is the only thing that moves
those times. The pinned version's own suite supplies the other high-latitude
case (`testMoonsightingMethodHighLat`: Oslo, 60°N, 2016-01-01), where the
method's seasonal adjustment produced the times and the user's rule does not
apply. Around them: a day where a looser rule keeps true twilight while a tighter
one caps it (London, 2016-08-15), a low latitude where no rule binds (Dhaka), a
method with a fixed Isha interval, a method with its own rule, and an unavailable
polar date.

Not yet covered: the **launch market's own method**, Karachi. It is exercised
(the Hanafi-vs-Standard Asr check uses it) but not against a documented
timetable, because no BatoulApps reference file exists for a South Asian city —
that comparison needs the human check below.

## Open items

- **Not yet compared with trusted local sources** for the launch market
  (`docs/PRODUCT_REQUIREMENTS.md` §5, the §13 release gate). The reference cases
  are the library's; a South Asian method against a trusted Dhaka timetable is
  still to be checked by a human before release.
