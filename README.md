# being-muslim

Native iOS and Android applications for an Islamic mobile product, built as a
single repository with two independently buildable apps.

**Status:** Early. The repository holds the engineering and product
documentation baseline, repository hygiene setup, and empty app shells for both
platforms. Product scope and the product name are still not finalized; see
`docs/PRODUCT_REQUIREMENTS.md` §14.

## Repository layout

```text
.
├── AGENTS.md              # engineering rules and workflow expectations
├── apps/
│   ├── ios/               # Swift + SwiftUI (Xcode project)
│   │   ├── BeingMuslim.xcodeproj
│   │   ├── App/           # app entry point and screens
│   │   ├── Resources/     # asset catalog
│   │   └── Tests/         # XCTest unit tests
│   └── android/           # Kotlin + Jetpack Compose (Gradle)
│       ├── app/           # application module
│       ├── gradle/        # version catalog + wrapper
│       └── gradlew
├── docs/                  # product, architecture, design standards, decisions
├── .agents/skills/        # vendored agent skills (provenance: docs/AGENT_SKILLS.md)
├── .opencode/commands/    # /slash wrappers that load those skills
├── .githooks/             # versioned git hooks (see "Git hooks" below)
└── .gitignore
```

Directories listed in `docs/ARCHITECTURE.md` §2 as `Core/`, `design-system/`,
`build-logic/`, and `shared/` are **not** created yet: they are added when a
concrete boundary exists (`AGENTS.md` rules 1 and 6).

The identifiers are placeholders and are recorded in
`docs/DECISIONS/0001-repository-layout-and-identifiers.md`.

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

## Prerequisites

iOS:

- macOS with Xcode 27.1 or later (iOS 27 SDK). No other tooling is required;
  packages are resolved with Swift Package Manager when they are added.
- Deployment target: iOS 18.0.
- Running on a **physical iPhone** additionally requires a signing identity and
  a Development Team selected in the target's *Signing & Capabilities* tab. The
  committed project has `CODE_SIGN_STYLE = Automatic` and **no** team, so it
  builds for the simulator out of the box but not for a device. See
  `docs/DECISIONS/0001-repository-layout-and-identifiers.md`.

Android:

- JDK 17 or later. AGP 9 requires JDK 17; JDK 21 is fine.
- Android SDK containing, at minimum:
  - `platforms;android-37.0` (API 37 uses minor versions, so there is no
    plain `platforms;android-37` package),
  - `build-tools;36.0.0`,
  - `platform-tools`.
- `ANDROID_HOME` (or `ANDROID_SDK_ROOT`) pointing at the SDK, or
  `apps/android/local.properties` with `sdk.dir=...`. `local.properties` is
  machine-specific and not committed.
- The Gradle wrapper (9.8.1) is committed; a separate Gradle install is not
  needed.
- API levels: `compileSdk`/`targetSdk` 37, `minSdk` 26.
- Release builds apply R8 and resource shrinking and produce an **unsigned**
  APK; a release signing config is intentionally not committed.

## Build, test, lint

### iOS

```sh
open apps/ios/BeingMuslim.xcodeproj          # build and run in Xcode
```

```sh
xcodebuild -project apps/ios/BeingMuslim.xcodeproj \
  -scheme BeingMuslim \
  -destination 'platform=iOS Simulator,name=iPhone 17' \
  build
```

```sh
xcodebuild -project apps/ios/BeingMuslim.xcodeproj \
  -scheme BeingMuslim \
  -destination 'platform=iOS Simulator,name=iPhone 17' \
  test
```

### Android

Run from `apps/android`:

```sh
./gradlew assembleDebug     # build
./gradlew test              # JVM unit tests
./gradlew lint              # Android Lint
```

Android Lint runs as part of `./gradlew lint` and currently reports no issues.
Formatter tooling is not set up yet on either platform (no
`swift-format`/SwiftLint, no `ktlint`/`detekt`); add it when the app has code
worth formatting, and document the commands here.

## Documentation

- `docs/PRODUCT_REQUIREMENTS.md` — product principles, feature status, content
  and calculation requirements, privacy, release gates.
- `docs/ARCHITECTURE.md` — repository layout, dependency direction, concurrency,
  configuration and secrets, testing strategy.
- `docs/DESIGN_STANDARDS.md` — design intent, semantic tokens, typography,
  accessibility, content integrity.
- `docs/DECISIONS/` — architecture decision records.
- `docs/AGENT_SKILLS.md` — vendored agent skills: provenance, layout, update
  procedure.
- `docs/agents/` — configuration the vendored engineering skills read (issue
  tracker, triage labels, domain docs).
- `AGENTS.md` — engineering rules for contributors and coding agents.

## Secrets

Credentials, signing material, and local environment files are never committed.
Secrets belong in Keychain or Android Keystore-backed storage on device, and
platform-native secure storage or local untracked files during development.
Assume every client binary and client-side configuration value can be inspected.
