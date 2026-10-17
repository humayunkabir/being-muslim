# Architecture and Repository Structure

## 1. Architectural goals

The repository contains two independently buildable native apps that implement one coherent product:

- iOS: Swift + SwiftUI.
- Android: Kotlin + Jetpack Compose.
- Shared repository: product specifications, platform-neutral contracts, design-token source definitions, and documentation where useful.

The architecture should enable independent native development, testability, clear dependency direction, and a maintainable pace of delivery. It should not optimize for hypothetical future scale.

## 2. Suggested repository layout

```text
repository/
├── AGENTS.md
├── README.md
├── .gitignore
├── .editorconfig
├── apps/
│   ├── ios/
│   │   ├── <Xcode project or workspace>
│   │   ├── App/
│   │   ├── Core/                 # Create only when shared native app concerns exist
│   │   ├── DesignSystem/         # Extract when repeated UI patterns justify it
│   │   └── Tests/
│   └── android/
│       ├── settings.gradle.kts
│       ├── build.gradle.kts
│       ├── app/
│       ├── core/                 # Create only when a real boundary exists
│       ├── design-system/        # Extract when repeated UI patterns justify it
│       └── build-logic/          # Only if convention plugins become worthwhile
├── shared/
│   ├── contracts/                # Platform-neutral API/event contracts, if needed
│   ├── design-tokens/             # Source definitions only when useful to both apps
│   └── documentation/
├── docs/
│   ├── ARCHITECTURE.md
│   ├── DESIGN_STANDARDS.md
│   ├── PRODUCT_REQUIREMENTS.md
│   └── DECISIONS/
└── tools/                         # Reproducible tooling only
```

This is a target map, not a requirement to create empty folders. Use standard Xcode and Gradle layouts inside the native app directories. Do not create a custom build orchestrator unless ordinary platform tooling becomes insufficient.

## 3. Dependency direction

Use these conceptual responsibilities where they are useful:

- **Presentation:** screens, UI state, navigation, and user interaction.
- **Domain:** meaningful business rules, domain types, and use cases that need isolation or reuse.
- **Data:** remote/local data retrieval, serialization, persistence, caching, and repository implementations.
- **Platform integration:** notifications, location, background work, secure storage, widgets, and OS lifecycle integration.

Not every feature needs four layers. Small features may remain within the app target or existing feature package until complexity makes a boundary valuable.

Rules:

- Avoid circular dependencies.
- UI code must not own low-level networking or persistence implementations.
- Keep transport and database representations from leaking across the app without a reason.
- Prefer dependency injection through constructors or native mechanisms when it improves testability.
- Do not introduce a DI framework, service locator, database, cache, or networking library before there is a concrete requirement.
- Do not split every type or feature into its own module.
- Keep platform-specific code in its native app.

## 4. Native concurrency and state

### iOS

Prefer Swift structured concurrency (`async`/`await`) and explicit task lifetimes. Use appropriate actor isolation and keep UI-bound mutations on `MainActor` when required. Avoid unstructured tasks without a clear lifecycle owner.

### Android

Prefer Kotlin coroutines and structured concurrency. Use lifecycle-aware collection of `Flow`/`StateFlow` where appropriate. Avoid `GlobalScope`, blocking work on the main thread, and state with ambiguous ownership.

### Both platforms

- Make state ownership and transitions explicit.
- Separate transient UI state from durable settings or user data.
- Avoid multiple sources of truth.
- Handle cancellation, lifecycle changes, retries, and offline/failure states where applicable.
- Use controlled clocks and schedulers in tests for time-dependent behavior.

## 5. Data and backend boundaries

The mobile apps should depend on documented contracts rather than assumptions about backend internals.

When a backend is introduced, document:

- Authentication and authorization expectations.
- API versioning and error format.
- Pagination and filtering behavior where applicable.
- Time zone and locale handling.
- Data ownership, retention, deletion, and export behavior.
- Retry and idempotency rules for write operations.
- Offline and synchronization behavior, if supported.

Never place private server credentials or privileged API keys in either mobile application. Assume client binaries and client-side configuration can be inspected.

## 6. Islamic content and calculation boundaries

Religious content and religiously significant calculations must not be treated as generic display strings. Follow `PRODUCT_REQUIREMENTS.md` for provenance, review, calculation configuration, localization, and correction processes.

Keep these concerns separately testable where implemented:

- Quran text and translation source metadata.
- Hadith text, collection/reference, grading metadata when applicable, and attribution.
- Prayer-time calculation method and relevant parameters.
- Qibla direction calculation and location permission behavior.
- Hijri calendar source or conversion convention.
- User-selected language, locale, calculation method, and juristic settings.

Do not silently substitute a source, calculation convention, or translation. Do not imply religious authority or scholarly endorsement without evidence.

## 7. Configuration, secrets, and persistence

- Separate development, test, and production configuration.
- Keep signing credentials, tokens, private keys, and local environment files out of version control.
- Use Keychain on Apple platforms and Android Keystore-backed mechanisms for secrets as appropriate.
- Minimize collection and retention of location and other personal data.
- Document migrations for durable schema changes.
- Define cache invalidation and offline semantics when caching is introduced.
- Do not add persistence simply because it might be useful later.

## 8. Testing strategy

Use the native test frameworks first. Add specialized libraries only when a clear need exists.

Prioritize tests for:

- Religious content parsing and source/reference integrity.
- Prayer-time and Qibla calculation boundaries, including location/time-zone edge cases.
- Domain rules and state transitions.
- Serialization and API error handling.
- Persistence migrations and synchronization, if present.
- Permission-denied, no-location, offline, empty, and failure states.
- Accessibility and critical user journeys.

Unit tests should be deterministic and should not require live network access. Use fixtures whose source and version are recorded for religious content.

## 9. Architecture decision records

For decisions that are difficult or costly to reverse, create a short record in `docs/DECISIONS/`, for example:

```text
docs/DECISIONS/0001-native-apps.md
```

Include status, context, decision, alternatives considered, and consequences. Record a decision when it is made; do not create speculative records for decisions not yet needed.

## 10. Build and repository hygiene

- Keep iOS buildable through Xcode and Android buildable through Gradle independently.
- Establish formatter, lint, and test commands early and document them in the repository README.
- Commit appropriate lockfiles and wrapper files.
- Exclude build output, local caches, machine-specific files, and secrets.
- Verify each native app independently before adding extra tooling.
