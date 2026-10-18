# 0006 — In-app language: the platform's per-app locale, Bengali and English

- **Status:** Accepted
- **Date:** 2026-10-18

## Context

Issue #14 requires the app to run in Bengali or English at the user's choice, with
every user-facing string in both locales, dates and times formatted through
platform locale APIs, and Arabic prayer names rendered with correct shaping and
directionality beside the localized text. `docs/PRODUCT_REQUIREMENTS.md` §2 fixes
the supported locales at exactly those two, and §8 fixes the rules: reviewed
translations, platform formatting APIs, and no claiming a locale is supported
before a reviewer has read it.

Android's own per-app language API (`LocaleManager.setApplicationLocales`) is
API 33+, while this app's `minSdk` is 26, so the majority of the launch market's
devices need the AndroidX backport. The app is Jetpack Compose on a
`ComponentActivity`, and every user-facing string was already externalized to
`values/strings.xml` with the domain layer holding none.

## Decision

- **Two locales, `en` and `bn`.** English is the app's fallback locale and lives
  in `values/`; Bengali is `values-bn/`. `docs/PRODUCT_REQUIREMENTS.md` §2 keeps
  any third locale out of the MVP.
- **The platform owns the language choice.** The app reads it with
  `AppCompatDelegate.getApplicationLocales()` and sets it with
  `AppCompatDelegate.setApplicationLocales()`, backed by:
  - `autoStoreLocales` on `AppLocalesMetadataHolderService`, which persists the
    choice on API 32 and below — the API that shipped in Android 13 syncs with
    system settings on its own;
  - `android:localeConfig` + `res/xml/locale_config.xml`, which lists the app's
    languages in the per-app language screen in system settings on Android 13+.
- **The language is deliberately not stored in `SelectionStore`/`UserSelection`.**
  The platform is the single source of truth, so a choice made in system settings
  and one made in the app cannot drift apart, and choosing a language never
  looks like completing first-run setup.
- **The app must extend `AppCompatActivity` and use an AppCompat base theme.**
  That is what makes `setApplicationLocales` apply on API 32 and below with
  Compose. The base theme is `Theme.AppCompat.DayNight.NoActionBar`, which
  replaces the old `values/` + `values-night/` pair with one DayNight theme.
- **The picker is in first-run setup and in settings** (`LanguageChoice`), and
  takes effect immediately: a Bengali reader must be able to switch before
  reading the setup screen. `resourceConfigurations` limits the packaged
  locales to `en` and `bn`, so the app cannot ship or resolve a language it has
  not translated (appcompat alone bundles dozens).
- **City names follow the app's language**, read from the platform rather than
  from `Locale.getDefault()`, which follows the device and not the app.
- **Arabic prayer names are untranslated string resources** (`translatable="false"`)
  shown beside the localized name, each in its own `Text` so that the platform
  shapes it and resolves its right-to-left direction as its own paragraph,
  instead of leaving a mixed-script run to be re-ordered.
- **Bengali and Arabic copy in this change is an unreviewed draft.** Issue #14's
  acceptance criteria, and `docs/PRODUCT_REQUIREMENTS.md` §8, require a human to
  read each locale, and §13 gates the release on it. Bengali therefore is *not*
  yet a supported locale, and `values-bn/strings.xml` carries a notice saying so
  until a reviewer signs off.

## Alternatives considered

- **`LocaleManager` alone (API 33+).** Rejected: it does nothing on API 26–32,
  which is most of the launch market.
- **A hand-rolled locale: storing the language ourselves and overriding the
  activity's base context.** Rejected: it re-implements the platform's backport,
  needs its own reconciliation with the system-settings picker, and hides the
  behaviour from where future readers expect it.
- **Overriding the locale inside Compose only.** Rejected: it would leave the
  system-settings picker out of step and diverges from the platform convention
  `docs/AGENTS.md` requires unless a documented reason justifies otherwise.
- **Relying on the system-settings picker with no in-app control.** Rejected:
  issue #14 requires an in-app setting, and the picker does not exist below
  Android 13.
- **Storing the language in the existing `SelectionStore`.** Rejected: it would
  either make a language choice look like a completed first-run setup, or need
  the store reshaped to hold a partial choice.

## Consequences

- **A new dependency**: `androidx.appcompat` 1.8.0 (Apache-2.0) and its
  transitive set (fragment, core, emoji2). It is Google-maintained and the
  documented backport for this API; the cost is APK size, not behaviour.
- **The base theme is now AppCompat's**, so window-level platform accents change
  from the platform Material theme's to AppCompat's. The Compose Material 3 theme
  still drives everything the user actually sees.
- **Changing the language restarts the activity.** The `ViewModel` and
  `rememberSaveable` state survive it, so an in-progress settings edit is not
  lost, but any future state held elsewhere must be survive-restart safe or the
  picker will appear to lose it.
- **Persistence is platform code, not app code**, so it is not covered by the
  JVM tests (`docs/ARCHITECTURE.md` §8); an instrumented test would be needed for
  it. What *is* covered is that both locales stay complete and keep their format
  arguments (`LocalizationResourcesTest`).
- `autoStoreLocales` reads stored locales during startup and can trip
  `StrictMode` disk-read logging. Accepted: it is what the platform's own
  backport does.
- **Release gates still open**: a Bengali-speaking reviewer must read every
  `values-bn/` string and the Arabic prayer names, and the Bengali layouts must be
  checked at large text sizes on a device with no clipping or meaning-changing
  truncation (`docs/DESIGN_STANDARDS.md` §5, §7, §10).
