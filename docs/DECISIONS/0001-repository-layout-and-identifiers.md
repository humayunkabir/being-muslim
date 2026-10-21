# 0001 — Monorepo layout and app identifiers

- **Status:** Accepted
- **Date:** 2026-10-17

## Context

The repository holds two independently buildable native apps that implement one
product (`AGENTS.md`, `docs/ARCHITECTURE.md` §1–2). No app targets existed yet,
so the first app scaffolding had to choose where the native projects live, how
they are generated, and what identifiers they use.

The product name is deliberately not finalized (`docs/PRODUCT_REQUIREMENTS.md`
§14, `docs/DESIGN_STANDARDS.md` §1). A working name and identifier namespace are
still required, because Xcode targets, bundle identifiers, and the Android
`applicationId` are baked into committed files.

## Decision

- Apps live under `apps/ios` (Swift + SwiftUI) and `apps/android`
  (Kotlin + Jetpack Compose), as the concrete realization of
  `docs/ARCHITECTURE.md` §2.
- Working name: **Being Muslim**. It is a placeholder and is expected to change;
  renames are mechanical at this size.
- Identifier namespace: **`app.beingmuslim`**, with platform suffixes:
  - iOS bundle identifier: `app.beingmuslim.ios`
  - Android `namespace` / `applicationId`: `app.beingmuslim.android`
- The iOS project is committed as a **native `.xcodeproj`**. It was produced once
  with XcodeGen, but no generator spec is committed and XcodeGen is not a
  build-time dependency. Xcode and Gradle remain the only required toolchains.
- Modularization beyond a single app module per platform (`Core/`,
  `design-system/`, `build-logic/`, shared `contracts/`) is deferred until a
  concrete boundary exists, per `AGENTS.md` rules 1 and 6.

## Alternatives considered

- **Committed XcodeGen/Tuist spec** (`project.yml` regenerated into
  `.xcodeproj`). Rejected for now: it adds a generator dependency and a second
  source of truth for a project with one target. Easy to adopt later if the
  project grows.
- **Hand-authored `.pbxproj`.** Rejected: error-prone and harder to review.
- **Neutral identifiers** (for example `com.example.*`). Rejected: a real
  namespace avoids a rename that would touch signing, entitlements, and store
  records.

## Consequences

- Renaming the product later means renaming the Xcode targets, the Gradle
  `rootProject.name`, display names, and the identifier namespace. Acceptable
  while the app is a shell.
- `app.beingmuslim.*` must be replaced before any store upload if the name
  changes.
- A future shared module or design-token source can be added under `shared/`
  without disturbing this layout.
- **Placeholder assets are committed on purpose** and must be replaced before
  any distribution: the iOS `AppIcon` has no image and `AccentColor` has no
  color, and the Android launcher icon is a neutral square. This keeps the
  projects buildable without inventing a brand identity
  (`docs/DESIGN_STANDARDS.md` §1).
- **The apps are not distributable as committed.** iOS has no Development Team
  (simulator-only builds), and the Android release build fails before packaging
  until a local upload keystore is configured (`docs/RELEASE.md`). Signing is
  per-developer/per-environment and deliberately uncommitted.
- The closed beta keeps the working name, `applicationId`, and placeholder
  launcher icon under `DECISIONS/0009`; they must be revisited before the public
  release.
