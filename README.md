# being-muslim

Native iOS and Android applications for an Islamic mobile product, built as a
single repository with two independently buildable apps.

**Status:** Early. The repository currently holds the engineering and product
documentation baseline and repository hygiene setup. The iOS and Android app
targets do not exist yet; product scope and the product name are not finalized.

## Repository layout

```text
.
├── AGENTS.md              # engineering rules and workflow expectations
├── docs/                  # product, architecture, and design standards
├── .githooks/             # versioned git hooks (see "Git hooks" below)
└── .gitignore
```

Planned, per `docs/ARCHITECTURE.md` §2 — directories are created only when they
have a defined responsibility:

```text
apps/ios/       # Swift + SwiftUI (Xcode project or workspace)
apps/android/   # Kotlin + Jetpack Compose (Gradle)
shared/         # platform-neutral contracts and design-token sources, if useful
tools/          # reproducible tooling only
```

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

## Build, test, lint

Not established yet. As each app is added, its formatter, lint, and test
commands are documented here and must be runnable from the native toolchain
(`docs/ARCHITECTURE.md` §10).

## Documentation

- `docs/PRODUCT_REQUIREMENTS.md` — product principles, feature status, content
  and calculation requirements, privacy, release gates.
- `docs/ARCHITECTURE.md` — repository layout, dependency direction, concurrency,
  configuration and secrets, testing strategy.
- `docs/DESIGN_STANDARDS.md` — design intent, semantic tokens, typography,
  accessibility, content integrity.
- `AGENTS.md` — engineering rules for contributors and coding agents.

## Secrets

Credentials, signing material, and local environment files are never committed.
Secrets belong in Keychain or Android Keystore-backed storage on device, and
platform-native secure storage or local untracked files during development.
Assume every client binary and client-side configuration value can be inspected.
