# Hijri date

What the app's Hijri date is, and what it is not. This is the record
`docs/PRODUCT_REQUIREMENTS.md` §7 requires a build that shows Hijri dates to
carry: whether the date is calculated, published, or observed, the convention it
follows, and how the user's adjustment works.

## What is shown

The times screen shows the Hijri date beside the Gregorian date for each day it
lists, under the label **"Hijri date: calculated, on the Umm al-Qura
convention"**, followed by copy stating that a country's or community's
announcement may differ by a day or more and that this is **not a confirmed
local sighting**.

The date is **calculated**, never observed. The label, the copy and the
adjustment are one unit: the app states what the date is, and gives the user the
one control that reconciles it with their own authority.

## The calculation

| | |
| --- | --- |
| Convention | **Umm al-Qura** (`islamic-umalqura`) |
| Implementation | The platform's `java.time.chrono.HijrahChronology` — no bundled table, no library, no data file |
| Network | None |
| Permission | None |
| Data | The chronology's own umalqura table, fixed at build/runtime data version |

`HijriCalendar.dateFor` converts a `LocalDate`; day, month and year are read back
off the chronology. The month names are the app's own string resources, not
platform locale data, so they are reviewed and translated with the rest of the UI
(`docs/PRODUCT_REQUIREMENTS.md` §7, §8).

## The ±1-day shift

A country or community that begins a month by an **announced moon sighting**
often starts it a day either side of the calculated Umm al-Qura date. The app
therefore offers the user one control, and nothing else:

- **±1 day**, and no further. The limit lives in `HijriCalendar.MAX_DAY_SHIFT`
  and is applied by `HijriCalendar.clampDayShift`, so neither the UI nor a
  stored value can move a date past it.
- The shift is **stored with the user's choices** (`UserSelection`), so it
  survives a restart, applies to **every** Hijri date the app shows — both days
  on the times screen — and is never changed on the user's behalf. Changing city
  or method keeps the user's shift.
- A value written by an older build, or one that is unparsable, reads as **no
  shift** rather than discarding the user's selection.

Worked example of what the control is for: Singapore's MUIS published **1 Ramadan
1445 as 12 March 2024** in its 2024 Islamic calendar, while the Umm al-Qura
calculated date is **11 March 2024** — the one-day difference the shift exists to
reconcile. Both dates are correct within their own convention.

## Tests

`HijriCalendarTest` (JVM, no device) covers:

- the convention itself — the platform chronology is the Umm al-Qura one the
  label names, so a platform default changing fails loudly;
- **published Umm al-Qura dates** from outside the code: 1 Ramadan 1445,
  1 Shawwal 1445 (Eid al-Fitr), 1 Muharram 1446 and 1 Ramadan 1446. Sources:
  the Umm al-Qura civil calendar for each month, and Saudi Arabia's Supreme
  Court announcement of 1 Muharram 1446 (6 July 2024 = 30 Dhu al-Hijjah 1445);
- a **month boundary of 29 days** (Sha'ban 1446 → Ramadan 1446) and of
  **30 days** (Ramadan 1445 → Shawwal 1445);
- a **year boundary** (30 Dhu al-Hijjah 1445 → 1 Muharram 1446);
- the shift across those boundaries, the case above (a local announcement a day
  after the calculation), and clamping beyond ±1.

The date fed in is the **city's** local date, already resolved from the instant
and the city's zone by the prayer-time calculator; the zone, daylight-saving and
rollover cases for that step are in `PrayerTimeCalculatorTest`
(`todayAndTomorrowFollowTheCitiesLocalCalendar`), not duplicated here.

`UserSelectionCodecTest` covers the shift surviving storage, its absence in an
older stored selection, an unparsable value, and clamping on read.

## Open items

- The umalqura table is the **platform's**, and the two platforms differ: the JVM
  tests exercise the JDK's table, while Android ships its own chronology data,
  which may disagree by a day. The ±1-day shift reconciles one day, not more, and
  a divergence this way is a reason to widen the documentation, not the shift.
- A change to Saudi Arabia's official calendar may not appear until that platform
  data is updated.
- The platform's table covers roughly **1882–2174 CE** and throws outside it. The
  app only ever converts today's and tomorrow's date.
- **Not yet checked against a trusted local source for the launch market** (the
  §13 release gate), and **iOS parity is pending** — the MVP is Android first
  (`DECISIONS/0002`).
- The month names are English and need the same review and translation as the
  rest of the UI (issue #14).
