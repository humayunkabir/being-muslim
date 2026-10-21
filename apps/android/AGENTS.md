# Android app — engineering rules

Kotlin + Jetpack Compose guidance for `apps/android/`. OpenCode loads this file
when work touches this directory; the repository rules in the root `AGENTS.md`
still apply and govern anything cross-cutting.

## Conventions

- Kotlin + Jetpack Compose by default, Views only when a native requirement
  justifies them; Android Jetpack libraries and Gradle.
- Material Design and official Kotlin conventions. Respect insets, font
  scaling, native navigation, and system settings.
- Structured concurrency with lifecycle-aware collection and immutable state
  (`val`); no `GlobalScope`, no blocking the main thread, no state shared across
  unrelated components. Details: `docs/ARCHITECTURE.md` §4.
- Module directories are kebab-case (`app/`; when needed `core/`,
  `design-system/`, `build-logic/`) and Kotlin packages are lowercase. The
  design system is **per app** — extract `design-system/` once a repeated
  pattern exists, never speculatively (`docs/ARCHITECTURE.md` §2.1–§2.2,
  `docs/DESIGN_STANDARDS.md` §2/§4).
- `MainActivity` stays an `AppCompatActivity` and the base theme stays an
  AppCompat descendant: that is what applies the user's chosen app language
  below Android 13 (issue #14, `docs/DECISIONS/0006`). User-facing strings live
  in `values/strings.xml` and `values-bn/strings.xml`, and a new language needs
  a `Language` entry and a `res/xml/locale_config.xml` entry too
  (`LocalizationResourcesTest` enforces this).
## Build, test, lint

Run from `apps/android`:

```sh
./gradlew assembleDebug     # build
./gradlew test              # JVM unit tests
./gradlew lint              # Android Lint
./gradlew bundleRelease     # signed AAB when an upload keystore is configured
```

Release signing reads `apps/android/keystore.properties` (gitignored) or the
`ANDROID_KEYSTORE_*` environment variables. Without them, release builds fail
before packaging instead of producing an unsigned artifact. The upload build,
store listing, and closed-beta runbook are in `docs/RELEASE.md`.

Android Lint runs as part of `./gradlew lint`. After the Appwrite SDK was added
it reports two `TrustAllX509TrustManager` warnings inside the SDK jar (the app
never calls `setSelfSigned(true)`) and one pre-existing
`AutoboxingStateCreation` hint in `SettingsScreen`; neither is a build error.
When it can reach the network it also suggests newer wrapper and dependency
versions. Toolchain (JDK, SDK packages, API levels, `ANDROID_HOME`): `README.md`
§Prerequisites. Formatter tooling (`ktlint`/`detekt`) is not set up yet — add it
when the app has code worth formatting, and record the command here.
