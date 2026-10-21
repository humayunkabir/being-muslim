# being-muslim

Native iOS and Android applications for an Islamic mobile product, built as a
single repository with two independently buildable apps.

**Status:** Early. The Android app implements the first MVP path — first-run
setup, then today's and tomorrow's offline prayer times for a chosen city and
method, with the city and method changeable from settings afterwards, plus
opt-in per-prayer reminders with quiet hours and an optional Apple or Google
account with a small signed-in dashboard. iOS remains a shell, parity following
the Android product (`docs/DECISIONS/0002-android-first-release-order.md`). The MVP
scope is recorded in `docs/PRODUCT_REQUIREMENTS.md` §2; the product name is
still not finalized (§14).

## Repository layout

```text
.
├── AGENTS.md, README.md, GLOSSARY.md     # engineering rules, entry point, vocabulary
├── apps/ios/                             # Swift + SwiftUI (Xcode project); AGENTS.md
├── apps/android/                         # Kotlin + Jetpack Compose (Gradle); AGENTS.md
├── backend/functions/user-sync/          # Appwrite Function: creates a user's sync row
├── backend/functions/account-delete/     # Appwrite Function: deletes a user's row and identity
├── backend/functions/report-problem/     # Appwrite Function: validates and stores problem reports
├── docs/                                 # spec docs, decisions, agent config
├── .agents/, .opencode/                  # vendored agent skills + command wrappers
├── .githooks/                            # versioned git hooks (see "Git hooks")
└── .gitignore, .editorconfig
```

The full target tree, naming conventions, and per-app design system are in `docs/ARCHITECTURE.md` §2. Directories it marks *(when needed)* — iOS `Core/`/`DesignSystem/`, Android `core/`/`design-system/`/`build-logic/`, and `shared/` — are not created yet; add them when a concrete boundary exists (`AGENTS.md` rules 1 and 6, `docs/ARCHITECTURE.md` §2.1–§2.2). Generated output (`apps/android/app/build/`) and the local `graft/`/`.codegraph/` caches are gitignored. Identifiers are placeholders (`docs/DECISIONS/0001-repository-layout-and-identifiers.md`).

## Getting started

The repository is a plain Git working tree; there is no build orchestrator.
Each app is built and run with its own native toolchain: Xcode for iOS, Gradle
for Android.

```sh
git clone https://github.com/humayunkabir/being-muslim.git
cd being-muslim
git config core.hooksPath .githooks   # required, once per clone
```

### Git hooks

`.githooks/` is versioned with the repository and must be activated per clone
with `git config core.hooksPath .githooks`. Without it, Git runs hooks from
`.git/hooks` and these checks are silently skipped.

- **pre-commit** blocks a commit when the staged changes contain a sensitive
  file type (keystore, provisioning profile, `.env`, and similar), a file that
  matches `.gitignore` (for example one staged with `git add -f`), or content
  that looks like a credential.
- **pre-push** rescans the commits actually being pushed, which also covers
  commits made with `--no-verify` or before the hooks existed.

GitHub secret scanning and push protection are also enabled on the repository.

Deliberate exceptions use `git commit --no-verify` or `git push --no-verify`.
Never bypass these checks to push a real secret; rotate the credential instead.

### Local tooling caches

`graft/` and `.codegraph/` are locally generated, gitignored indexes read by the agent tooling (`AGENTS.md`). Regenerate them per clone with `graft build` and `codegraph init`; `graft check` fails when the `graft/` graph is stale (for CI).

## Prerequisites

iOS:

- macOS with Xcode 27.1 or later (iOS 27 SDK); deployment target iOS 18.0. No other tooling — packages resolve via Swift Package Manager when they are added.
- A **physical iPhone** also needs a signing identity and a Development Team selected in *Signing & Capabilities*. The committed project has `CODE_SIGN_STYLE = Automatic` and **no** team, so it builds for the simulator but not a device (`docs/DECISIONS/0001-repository-layout-and-identifiers.md`).

Android:

- JDK 17 or later (AGP 9 requires 17; JDK 21 is fine).
- Android SDK with, at minimum, `platforms;android-37.0` (API 37 uses minor versions, so there is no plain `platforms;android-37`), `build-tools;36.0.0`, and `platform-tools`.
- `ANDROID_HOME` (or `ANDROID_SDK_ROOT`) pointing at the SDK, or `apps/android/local.properties` with `sdk.dir=...` — machine-specific and not committed.
- The Gradle wrapper (9.8.1) is committed; no separate Gradle install needed.
- API levels: `compileSdk`/`targetSdk` 37, `minSdk` 26. Release builds apply R8 and resource shrinking. A release build is signed when `apps/android/keystore.properties` (gitignored) or the matching environment variables are present; otherwise it stays unsigned. The signing setup and upload runbook are in `docs/RELEASE.md`.

## Build, test, lint

Each app builds with its own native toolchain; the exact commands and platform
conventions live next to the code, in `apps/ios/AGENTS.md` (Xcode /
`xcodebuild`) and `apps/android/AGENTS.md` (Gradle: `assembleDebug`, `test`,
`lint`).

Formatter tooling is not set up yet on either platform (no
`swift-format`/SwiftLint, no `ktlint`/`detekt`); add it when the app has code
worth formatting.

## Documentation

- `docs/PRODUCT_REQUIREMENTS.md` — product principles, feature status, content
  and calculation requirements, privacy, release gates.
- `docs/ARCHITECTURE.md` — repository layout, dependency direction, concurrency,
  configuration and secrets, testing strategy.
- `docs/DESIGN_STANDARDS.md` — design intent, semantic tokens, typography,
  accessibility, content integrity.
- `docs/ACCESSIBILITY.md` — the Android verification record for accessibility,
  locales, state coverage, and the calculation edge cases, including the gates
  still open.
- `GLOSSARY.md` — the canonical product vocabulary shared by both apps and the
  specification documents.
- `docs/CITY_CATALOG.md` — the bundled city catalog's source, license, format,
  and coverage limits.
- `docs/HIJRI_CALENDAR.md` — the calculated Hijri date's convention and the
  user's ±1-day shift.
- `docs/PRAYER_TIMES.md` — the prayer-time library, methods, parameters, and
  reference cases.
- `docs/QIBLA.md` — the qibla bearing, the true-north correction, and the
  compass states and limits.
- `docs/REMINDERS.md` — the opt-in prayer reminders, quiet hours, permission
  UX, and delivery limits.
- `docs/APPWRITE.md` — the optional account backend: endpoint and project IDs,
  platform registrations, OAuth provider state, the sync table and its
  permissions, the row-creation function, and the admin-key runbook.
- `docs/SYNC.md` — the sync contract: payload shape and versioning, the conflict
  rule, error and retry behaviour, and retention, deletion, and export.
- `docs/PRIVACY.md` — the plain-language account privacy notice: what is stored,
  what is never stored, retention, deletion, and export.
- `docs/RELEASE.md` — release signing, the Play upload build, the store listing
  and data-safety answers, and the closed-beta runbook.
- `docs/index.md` — the public privacy policy published at
  <https://humayunkabir.github.io/being-muslim/> (built by GitHub Pages; the rest
  of `docs/` is excluded by `docs/_config.yml`).
- `docs/DECISIONS/` — architecture decision records.
- `docs/AGENT_SKILLS.md` — vendored agent skills: provenance, layout, update
  procedure.
- `docs/agents/` — configuration the engineering skills read: issue tracker,
  triage labels, domain docs.
- `AGENTS.md` — engineering rules for contributors and coding agents.
- `apps/ios/AGENTS.md`, `apps/android/AGENTS.md` — per-app platform conventions
  and build/test/lint commands, loaded when work touches each app.

Each of the three specification documents opens with a short header naming what
it owns and what it defers to the others; when a rule looks duplicated, the
owning document is authoritative.

## Secrets

Never commit credentials, signing material, or local environment files — the hooks above block them. On device, secrets belong in Keychain or Android Keystore-backed storage; during development, in platform-native secure storage or local untracked files. Assume every client binary and its configuration can be inspected. See `docs/ARCHITECTURE.md` §7.
