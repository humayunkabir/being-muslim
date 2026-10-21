# Release: Google Play closed beta

**Status:** Prepared 2026-10-22; **not published**. The repository side is
complete, but the account, signing key, store assets, and physical-device checks
need a person. **Owns:** release signing, the upload build, the store listing and
declarations, and the closed-track runbook. **Defers:** whether the build may be
represented as verified → `PRODUCT_REQUIREMENTS.md` §13 and
`docs/ACCESSIBILITY.md`; what the app stores → `docs/PRIVACY.md`; backend
identifiers → `docs/APPWRITE.md`; name and placeholder-asset acceptance →
`DECISIONS/0009`.

## What ships

| | |
| --- | --- |
| App | Being Muslim (`app.beingmuslim.android`) |
| Version | `0.1.0` (`versionCode` 1) |
| Track | Closed testing |
| `minSdk` / `targetSdk` | 26 / 37 |
| Formats | Signed AAB for Play; signed APK for a local install |
| Privacy policy | <https://humayunkabir.github.io/being-muslim/> |

The public page is built from `docs/index.md`; `docs/_config.yml` keeps the rest
of `docs/` off the site.

## Blocked on a person

These are release gates, not code tasks. Do not represent the build as verified
while they are open.

- [ ] **#39 sign-off** — TalkBack, real permission prompts, Bengali and Arabic
      reader review, dynamic-palette contrast, compass hardware, and the Dhaka
      timetable comparison. This is the live gate from
      `PRODUCT_REQUIREMENTS.md` §13.
- [ ] **Play developer account** — identity verification, and (for a new personal
      account) the closed-test requirement described below.
- [ ] **Upload keystore** — create, back up, and keep out of git.
- [ ] **Store graphics** — 512×512 icon, 1024×500 feature graphic, and at least
      two phone screenshots. The placeholder launcher icon is accepted for the
      closed beta only (`DECISIONS/0009`).
- [ ] **Third-party notices** — Adhan (MIT) and the Appwrite SDK (BSD-3-Clause)
      notices are not yet surfaced in a distributed notice. Add an About/notices
      surface before distributing outside internal testing.
- [ ] **Quran listening is not in this build** (#33–#37). The streaming
      disclosures, data-safety answers, and offline checks in #38 apply only
      once it ships; the answers below are for the current feature set.

## Signing

The upload key is per-developer. `apps/android/app/build.gradle.kts` reads it
from an untracked `apps/android/keystore.properties`, or from environment
variables when that file is absent.

Create the key once in a location **outside the repository** and back it up
offline; losing it means an upload-key reset:

```sh
keytool -genkeypair -v \
  -keystore /secure/outside/repo/upload-keystore.jks \
  -alias being-muslim \
  -keyalg RSA -keysize 4096 -validity 10000
```

`apps/android/keystore.properties` (already gitignored):

```properties
storeFile=/secure/outside/repo/upload-keystore.jks
storePassword=…
keyAlias=being-muslim
keyPassword=…
```

A relative `storeFile` is resolved from `apps/android/`, so the example uses an
absolute path to keep the key out of the working tree. The environment
equivalents are `ANDROID_KEYSTORE_FILE`, `ANDROID_KEYSTORE_PASSWORD`,
`ANDROID_KEY_ALIAS`, and `ANDROID_KEY_PASSWORD`.

Modern `keytool` creates a PKCS12 keystore, which uses one password for both
`storePassword` and `keyPassword`; set them to the same value (or pass
`-storetype JKS` to `keytool` for separate passwords). Getting this wrong fails
the build with `Given final block not properly padded`.

Never commit the keystore, its passwords, or `keystore.properties`. If the key
is compromised or lost, use **Play Console → App integrity → Upload key reset**;
enrol in Play App Signing and let Google hold the app-signing key.

## Build and verify

Run from `apps/android`:

```sh
./gradlew clean test lint bundleRelease assembleRelease
```

Outputs:

- `app/build/outputs/bundle/release/app-release.aab` — upload this.
- `app/build/outputs/apk/release/app-release.apk` — install on a device.

Confirm the APK is signed and uses the upload key:

```sh
$ANDROID_HOME/build-tools/36.0.0/apksigner verify --print-certs \
  app/build/outputs/apk/release/app-release.apk
```

Install and launch it on a physical device:

```sh
adb install -r app/build/outputs/apk/release/app-release.apk
```

Bump `versionCode` for every upload; Play rejects a reused one. Keep
`versionName` in step with the release record.

## Play Console setup

1. Create the app: name **Being Muslim**, default language **English (United
   States)**, app, free, not a game.
2. Complete **App content**: privacy policy URL above, account deletion URL
   `https://humayunkabir.github.io/being-muslim/#delete-your-account`, ads
   **No**, app access **All functionality is available without special access**
   (the account is optional), content rating questionnaire, target audience, and
   data safety.
3. Register the release certificate for sign-in. The release build is signed
   with a key the debug OAuth client does not know, so Google sign-in fails until
   the Play App Signing SHA-1 from **Play Console → App integrity** is added to
   the Android OAuth client in Google Cloud. Register the matching SHA-256 with
   the Appwrite Android platform (`docs/APPWRITE.md`).
4. Fill the store listing from the copy below and upload the graphics.
5. Create a **Closed testing** track, upload the AAB, add testers, and roll out.
6. Send testers the opt-in URL, collect feedback, and keep the build installed
   for the full test period.
7. Bump the version, fix or explicitly defer the findings, and only then move to
   #21 (public 1.0).

New personal developer accounts must run a closed test with at least 12 testers
for 14 continuous days before they can apply for production access. Confirm the
current requirement in Play Console rather than relying on this note.

## Store listing copy

**App name** (30 characters max): `Being Muslim`

**Short description** (80 characters max):

```text
Prayer times, qibla, Hijri date, reminders and tasbih — works offline.
```

**Full description**:

```text
Prayer times, the qibla, the Hijri date, reminders, and a tasbih counter — calculated on your phone and usable without an account or a connection.

• Prayer times for the city you choose, with ten calculation methods, the Asr juristic method, a high-latitude rule, and your own per-prayer offsets.
• The qibla bearing for that city, with a live compass when your device has one.
• Today and tomorrow at a glance, and a calculated Hijri date with a ±1-day adjustment.
• Optional prayer reminders with quiet hours.
• A simple tasbih counter with your own label and target.
• English and Bengali.
• An optional account carries your settings and tasbih total to another device. Every feature works signed out.

City data from GeoNames (https://www.geonames.org/), licensed under CC BY 4.0. Prayer times are calculated with BatoulApps Adhan (MIT). Calculated times vary by method and local authority; the app shows the parameters it used and lets you report a wrong time.

Being Muslim does not replace qualified religious guidance. The name and icon are provisional during the closed beta.
```

**Category:** Lifestyle. **Contact email:** the developer email on the account.
**Privacy policy:** <https://humayunkabir.github.io/being-muslim/>.

Bengali listing copy waits for the Bengali review in #39.

## Graphics checklist

| Asset | Requirement | Status |
| --- | --- | --- |
| App icon | 512×512 PNG, 32-bit | Placeholder for beta (`DECISIONS/0009`) |
| Feature graphic | 1024×500 PNG/JPEG | **Missing** |
| Phone screenshots | 2–8, 16:9 or 9:16, 320–3840 px | **Missing** |
| Tablet screenshots | Optional | Not planned for beta |

Capture screenshots from the release build on a device, with realistic data and
no personal information in the status bar.

## Data safety answers

Answer for the current build. Re-check after adding listening or any new network
feature.

| Question | Answer |
| --- | --- |
| Collects or shares required data? | **Yes** |
| Personal info → Name | Collected, not shared, optional, account management, encrypted in transit, deletable |
| Personal info → Email address | Collected, not shared, optional, account management, encrypted in transit, deletable |
| Location → Approximate location | Collected, not shared, optional, app functionality — only the selected city's coordinates inside a report the user chooses to send; device location never leaves the phone |
| App activity → Other user-generated content | Collected, not shared, optional, app functionality — synced settings and tasbih total, and the note in a problem report |
| App info and performance → Other app performance data | Collected, not shared, optional, app functionality — app and calculation-library versions in a problem report |
| Device or other IDs | **Not collected** |
| Analytics, ads, or tracking | **None** |
| Data shared with third parties | **None** |
| Encrypted in transit | **Yes** (HTTPS) |
| Users can request deletion | **Yes** — in-app account deletion and the privacy-policy contact for reports |

The app requests `ACCESS_COARSE_LOCATION` (nearest-city suggestion, on device),
`POST_NOTIFICATIONS` (local reminders), and `INTERNET` (optional account and
reports). WorkManager merges `WAKE_LOCK`, `ACCESS_NETWORK_STATE`,
`RECEIVE_BOOT_COMPLETED`, and `FOREGROUND_SERVICE` for local reminder
scheduling. The merged manifest has no background location, no precise
location, and no advertising ID.

## After installing the release build

Check on the physical device, not only the emulator:

- the app installs, launches, and shows the release signature from above;
- prayer times match the selected city and method, including tomorrow;
- the qibla dial and its calibration/failure states work;
- a reminder can be enabled and the notification permission denial is explained;
- the language switches between English and Bengali;
- signing in, exporting, and deleting an account work, or the offline error is
  clear;
- a problem report can be sent and its listed payload contains no device or
  account identifiers;
- in airplane mode, every core feature still works and the account/report paths
  fail with a clear message.

Record the installed `versionCode` in the #20 thread when the track goes live.
