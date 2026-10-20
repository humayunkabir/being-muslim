# Qibla and the compass

The direction the app shows, how it is calculated, and what the live compass
does and does not claim. This is the record `docs/PRODUCT_REQUIREMENTS.md` §6
requires a build containing a qibla to carry: the bearing's source, the
true-north correction, the permission position and the known limits.

## What is shown

The qibla screen always shows the **bearing in degrees from true north**, for
the city the user selected. That number needs no sensor, no permission and no
network, so it is the direction every user gets, including one who has denied
every permission (issue #9).

Where the device has a usable compass, the screen adds a **live dial** on top of
that bearing (issue #10). The dial turns with the device so that north and the
qibla marker sit at their true-north positions; a fixed pointer at the top is
the direction the top of the phone faces. The screen also says how far to turn,
or that the user is facing the qibla, in words and degrees — direction is never
conveyed by colour or an icon alone. A reading the platform does not trust is
not used to turn the dial; the dial then stays north-up and the screen asks for
calibration.

## The bearing

| | |
| --- | --- |
| Formula | Initial great-circle bearing (forward azimuth) to the Kaaba |
| Kaaba | 21.422487° N, 39.826206° E — the Kaaba's published coordinates, to within about 100 m |
| Implementation | `QiblaBearing.from`, pure Kotlin, no library |
| Network | None |
| Permission | None |

The bearing is the direction of the great circle at the user's city, not the
rhumb line; for the distances involved the two differ by a fraction of a degree
except at high latitudes, where the great circle is the correct one. The result
is normalised to `0.0..<360.0`.

A city whose coordinates are not finite has no bearing and the screen explains
the state instead of showing a direction (issue #9). The app only opens the
qibla for a selected city, so that state is defensive.

## True north and magnetic declination

A phone's compass reports **magnetic** north. The app converts the reading to
**true** north with the local magnetic declination, east positive:

```text
true heading = magnetic heading + declination
```

The declination comes from the platform's `android.hardware.GeomagneticField`
at the selected city's coordinates and the current time. It is computed once
per collection, because it changes slowly with position and date. Without this
step the needle would be wrong by the local declination — single digits in
much of the world, but tens of degrees at high latitudes.

## Compass states

`CompassSensor` reports the platform's accuracy with every reading, and the
screen treats it as follows:

| State | What the screen shows |
| --- | --- |
| No rotation-vector sensor | The bearing and the north-up dial, and that this device has no usable compass |
| Sensor present, no reading within 5 s | The bearing and the north-up dial, and that this device has no usable compass |
| Sensor present, no reading yet | The bearing and the north-up dial, and that it is waiting for the compass |
| Accuracy `LOW` or `UNRELIABLE` | The bearing and the north-up dial, and a figure-eight calibration prompt. The dial is **not** turned by the reading: a direction the platform does not trust is worse than none |
| Accuracy `MEDIUM` | The live dial, with a note that the reading is approximate |
| Accuracy `HIGH` | The live dial, and how far to turn or that the user is facing the qibla |

The bearing stays on screen in every one of these states, so the compass is
never the only way to obtain a direction. The screen also states the
environmental limit: nearby metal or magnets and the way the phone is held
affect a compass, and it should be held flat.

## Limits

- **Only the sensor is tested by a human.** The great-circle bearing and the
  declination arithmetic are covered by JVM tests; the sensor adapter and the
  dial need a physical device, and are not claimed as verified until they have
  been checked on one.
- The rotation vector is used only for a phone held **flat**. Held upright, the
  platform's azimuth is not the direction the phone faces, which is why the
  screen tells the user to hold it flat.
- A device with a magnetometer but no rotation-vector sensor falls back to the
  geomagnetic rotation vector, then to the bearing-only state. It never
  synthesises a heading from raw accelerometer and magnetometer readings.
- Compass accuracy depends on the device's own calibration and on the
  environment. The app can report what the platform reports; it cannot correct
  for a magnet nearby.
- **Declination is taken at the selected city, not at the phone.** That is exact
  for a user who is in their chosen city and drifts only slowly with distance —
  a few degrees at most across a country — but it is wrong for a traveller who
  has moved far from the city selected in setup. The bearing itself is likewise
  for the selected city's coordinates. Re-selecting the current city fixes both;
  device location is not used to correct the compass automatically.
- **iOS parity is pending** — the MVP is Android first
  (`DECISIONS/0002`).

## Tests

`QiblaBearingTest` (JVM, no device) covers:

- the bearing from **Dhaka, London, New York and Jakarta** against the
  directions published for those cities by qibla calculators — independent
  values, not recomputed from this implementation — within 0.2°;
- the two exact cases north and south of the Kaaba (due south and due north);
- normalisation into `0.0..<360.0`, and a non-finite position having no bearing;
- eastern and western declination applied across the 0°/360° boundary;
- the signed shortest turn from a heading to the qibla, including a half turn.

The compass adapter itself is a platform boundary with no logic of its own; it
is verified by the native build and lint, and needs the physical-device check
above.
