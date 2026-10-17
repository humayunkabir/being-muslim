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

The Adhan default, `MIDDLE_OF_THE_NIGHT`, is passed for every method and is **not
yet user-selectable or disclosed** — that is issue #7. It bounds Fajr and Isha
where true dawn or dusk is undefined.

This is **not uniform across methods**: the Moonsighting Committee method does not
use the rule at all — Adhan applies its own seasonal twilight adjustment for that
method instead. Any future disclosure of the rule (#7) must say so.

### Offsets

**No offsets are applied by default.** User offsets (issue #8) will be stored and
shown separately from the raw result, never folded into it.

## Time, dates, and daylight saving

- Every calculation is performed for the **city's IANA time zone**, taken from
  the bundled catalog (`docs/CITY_CATALOG.md`). Daylight saving is handled by the
  zone, never by a fixed offset — a BST date and a GMT date for London come out an
  hour apart in wall time and correct in instant.
- The **local date is the city's**, not the device's: an instant that is already
  tomorrow in Dhaka computes Dhaka's tomorrow. The current time is always injected
  (`forInstant`), never read inside the domain, so tests use a fixed clock.
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

Not yet covered: the **launch market's own method**, Karachi. It is exercised
(the Hanafi-vs-Standard Asr check uses it) but not against a documented
timetable, because no BatoulApps reference file exists for a South Asian city —
that comparison needs the human check below.

## Open items

- **Not yet compared with trusted local sources** for the launch market
  (`docs/PRODUCT_REQUIREMENTS.md` §5, the §13 release gate). The reference cases
  are the library's; a South Asian method against a trusted Dhaka timetable is
  still to be checked by a human before release.
