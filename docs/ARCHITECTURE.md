# Architecture and Repository Structure

**Status:** Baseline — last reviewed 2026-10-18. The MVP scope is decided (`PRODUCT_REQUIREMENTS.md` §2); the product name and brand remain open (§14). **Owns:** repository layout, naming, module boundaries, dependency direction, concurrency and state, data/backend boundaries, configuration and secrets, testing strategy, ADRs, build hygiene. **Defers:** product behavior and content → `PRODUCT_REQUIREMENTS.md`; visual and permission UX → `DESIGN_STANDARDS.md`.

## 1. Architectural goals

The repository holds two independently buildable native apps implementing one coherent product: iOS in Swift + SwiftUI, Android in Kotlin + Jetpack Compose, plus documentation, platform-neutral contracts, and design-token sources where sharing has a concrete benefit. Optimize for independent native development, testability, clear dependency direction, and a maintainable delivery pace — not for hypothetical future scale.

## 2. Repository layout

A **target** layout, not a checklist. Anything marked *(when needed)* does not exist yet and is added only when its boundary is real (`AGENTS.md` rules 1 and 6, ADR 0001).

```text
repository/
├── AGENTS.md, README.md, GLOSSARY.md
├── .gitignore, .editorconfig
├── .githooks/                      # versioned git hooks (see README)
├── .agents/, .opencode/            # vendored agent skills + command wrappers
├── apps/
│   ├── ios/                        # Swift + SwiftUI: BeingMuslim.xcodeproj, App/, Resources/, Tests/
│   │   └── AGENTS.md               # iOS conventions + build/test commands
│   └── android/                    # Kotlin + Compose: Gradle root, app/, gradlew
│       └── AGENTS.md               # Android conventions + build/test commands
├── shared/                         # (when needed) contracts/, design-tokens/, documentation/
├── docs/                           # spec docs, AGENT_SKILLS.md, agents/, DECISIONS/
└── tools/                          # (when needed) reproducible tooling only
```

Within the apps, the iOS `Core/` and `DesignSystem/`, the Android `core/`, `design-system/`, and `build-logic/`, and the whole `shared/` tree are *(when needed)* and are added only when their boundary is real. Use standard Xcode and Gradle layouts inside the app directories; do not add a custom build orchestrator while ordinary platform tooling suffices. Generated output (e.g. `apps/android/app/build/`) and local caches (`graft/`, `.codegraph/`) are ignored, never committed.

### 2.1 Naming

Naming follows the host platform, so one concept appears under different casing:

- **iOS source folders:** PascalCase (`App/`, `Resources/`, `Core/`, `DesignSystem/`).
- **Android module directories:** kebab-case (`app/`, `core/`, `design-system/`); Kotlin packages lowercase.
- **`shared/`:** lowercase kebab-case (`contracts/`, `design-tokens/`).

### 2.2 Design system and shared tokens

The design system is **per app**: `apps/ios/DesignSystem/` and `apps/android/design-system/`, each mapping design intent to its own native resources (`DESIGN_STANDARDS.md` §2). `shared/design-tokens/` holds only the platform-neutral **source definitions** for values both apps must keep in agreement; when it exists it is the source of truth and native resources are derived from it. Token categories and intent: `DESIGN_STANDARDS.md` §4. Create none of these speculatively — extract a per-app design system once a repeated pattern exists, and shared tokens only when both apps must agree on the same values.

## 3. Dependency direction

Conceptual responsibilities, used where they are useful: **presentation** (screens, UI state, navigation, interaction), **domain** (business rules, types, use cases needing isolation or reuse), **data** (remote/local retrieval, serialization, persistence, caching, repositories), and **platform integration** (notifications, location, background work, secure storage, widgets, OS lifecycle).

Not every feature needs four layers; small features may stay in the app target or an existing package until complexity makes a boundary valuable. Rules: avoid circular dependencies; UI must not own low-level networking or persistence; don't leak transport/database representations across the app without reason; prefer constructor or native dependency injection when it improves testability; don't add a DI framework, service locator, database, cache, or networking library before a concrete requirement; don't split every type or feature into its own module; keep platform-specific code in its native app.

## 4. Native concurrency and state

- **iOS:** Swift structured concurrency (`async`/`await`) with explicit task lifetimes; appropriate actor isolation; UI-bound mutations on `MainActor`; no unstructured tasks without a clear lifecycle owner.
- **Android:** Kotlin coroutines and structured concurrency; lifecycle-aware collection of `Flow`/`StateFlow`; no `GlobalScope`, no blocking the main thread, no state with ambiguous ownership.
- **Both:** make state ownership and transitions explicit; separate transient UI state from durable settings or user data; one source of truth; handle cancellation, lifecycle changes, retries, and offline/failure states; use controlled clocks and schedulers in time-dependent tests.

## 5. Data and backend boundaries

Depend on documented contracts, not assumptions about backend internals. When a backend is introduced, document: authentication/authorization expectations; API versioning and error format; pagination and filtering; time-zone and locale handling; data ownership, retention, deletion, and export; retry and idempotency for writes; offline and synchronization behavior if supported.

Never place private server credentials or privileged API keys in either app; assume clients and their configuration can be inspected. *Which* data may be collected, retained, or exported is a product decision (`PRODUCT_REQUIREMENTS.md` §10); this section and §7 cover only how it is wired and stored.

## 6. Islamic content and calculation boundaries

Religious content and religiously significant calculations are not generic display strings. Provenance, review, calculation configuration, localization, and correction: `PRODUCT_REQUIREMENTS.md` §3–§7, §12. Here: keep these independently testable where implemented — Quran text and translation source metadata; Hadith text, collection/reference, grading metadata when applicable, and attribution; prayer-time calculation method and parameters; qibla direction calculation and location-permission behavior; Hijri calendar source or conversion convention; user-selected language, locale, calculation method, and juristic settings.

Do not silently substitute a source, calculation convention, or translation, and do not imply religious authority or scholarly endorsement without evidence.

## 7. Configuration, secrets, and persistence

- Separate development, test, and production configuration.
- Keep signing credentials, tokens, private keys, and local environment files out of version control (enforced by `.githooks/`; see `README.md`).
- Use Keychain on Apple platforms and Android Keystore-backed mechanisms, with least privilege.
- Document migrations for durable schema changes; define cache-invalidation and offline semantics when caching is introduced.
- Do not add persistence simply because it might be useful later.
- Never store or log credentials, tokens, private user content, or precise location (`PRODUCT_REQUIREMENTS.md` §10).

## 8. Testing strategy

Use the native test frameworks first; add specialized libraries only when a clear need exists. Prioritize: religious-content parsing and source/reference integrity; prayer-time and qibla boundaries including location/time-zone edge cases; domain rules and state transitions; serialization and API error handling; persistence migrations and synchronization if present; permission-denied, no-location, offline, empty, and failure states; accessibility and critical journeys.

Tests are deterministic with no live network access. Religious-content fixtures record their source and version; fixture review and versioning follow `PRODUCT_REQUIREMENTS.md` §12–§13.

## 9. Architecture decision records

For decisions that are difficult or costly to reverse, add a short record in `docs/DECISIONS/` (e.g. `0001-repository-layout-and-identifiers.md`) with status, context, decision, alternatives considered, and consequences — the existing entry also records a date. Record a decision when it is made; do not write speculative records. `docs/agents/domain.md` covers how the agent skills consume these.

## 10. Build and repository hygiene

- Keep iOS buildable through Xcode and Android through Gradle, independently.
- Establish formatter, lint, and test commands early and document them next to the code (`apps/ios/AGENTS.md`, `apps/android/AGENTS.md`).
- Commit appropriate lockfiles and wrapper files.
- Exclude build output, local caches, machine-specific files, and secrets.
- Verify each app independently before adding extra tooling.
