# Repository Engineering Rules

## Mission and platform choices

Build a high-quality Islamic mobile product as a Git monorepo with two independently buildable, fully native apps:

- **iOS:** Swift + SwiftUI, native Apple frameworks, Swift Package Manager where appropriate.
- **Android:** Kotlin + Jetpack Compose, Android Jetpack libraries, Gradle.
- **UI:** separate native implementations; do not force pixel-identical layouts or interactions.
- **Shared:** product requirements, terminology, API/data contracts, design-token sources, and docs — only where sharing has a concrete benefit.

Do not introduce Flutter, React Native, Kotlin Multiplatform, or another cross-platform UI framework without explicit approval from the project owner. The product name and final feature scope are intentionally not fixed here.

## Non-negotiable rules

1. Prefer the simplest architecture that preserves useful boundaries.
2. Keep presentation, business rules, data access, and platform integration separate where the codebase warrants it; do not create empty or speculative layers.
3. Prefer immutable state, explicit dependencies, predictable data flow, and lifecycle-aware asynchronous work.
4. Use native platform conventions and APIs unless a documented reason justifies otherwise.
5. Build accessibility, privacy, security, error handling, and testing into features from the start.
6. Avoid speculative abstractions, unnecessary dependencies, unrelated refactors, and premature infrastructure.
7. Never invent APIs or assume dependency behavior; inspect existing code and authoritative documentation.
8. Preserve existing user work. Make the smallest complete, reviewable change.
9. Never claim a build, test, lint, or validation passed unless it was actually run successfully.
10. Treat Islamic content accuracy and source attribution as product-critical; follow `docs/PRODUCT_REQUIREMENTS.md`.

## Specification documents

Three documents are authoritative within their own scope, and each declares what it owns and defers: `docs/PRODUCT_REQUIREMENTS.md` (product behavior, content provenance, release gates), `docs/DESIGN_STANDARDS.md` (UI, accessibility, permission UX), and `docs/ARCHITECTURE.md` (code structure and boundaries). Use them to guide structure, not as a mandate to create every directory immediately.

## Build and repository expectations

- Keep iOS buildable with Xcode and Android buildable with Gradle, each independently, using normal native project layouts and workflows.
- Add a directory or module only when it has a defined responsibility or the native toolchain requires it.
- Keep secrets, signing credentials, local machine settings, generated output, and caches out of version control.
- Do not introduce a custom build orchestrator while standard Xcode and Gradle workflows suffice.

## Platform standards

- **iOS:** Swift + SwiftUI by default (UIKit only for a concrete native need); Apple's Human Interface Guidelines and Swift API Design Guidelines; respect Dynamic Type, safe areas, semantic colors, system appearance, and navigation; structured concurrency with explicit task lifetimes and `MainActor` for UI updates; no force-unwrapping in production paths, no unsafe shared mutable state.
- **Android:** Kotlin + Compose by default (Views only when a native requirement justifies them); Material Design and official Kotlin conventions; respect insets, font scaling, navigation, and system settings; structured concurrency, lifecycle-aware collection, immutable state (`val`); no `GlobalScope`, no blocking the main thread, no state shared across unrelated components.

Concurrency and state details: `docs/ARCHITECTURE.md` §4. UI, tokens, typography, and interaction: `docs/DESIGN_STANDARDS.md`. Share product intent and behavior, not UI implementation; platform navigation, typography, controls, gestures, and motion may differ when that improves native usability.

## Quality requirements

- Validate untrusted input; handle expected failures explicitly.
- Never log credentials, tokens, private user content, or sensitive payloads. Use platform-appropriate secure storage and least privilege.
- Keep expensive work off the UI thread.
- Meet the accessibility bar in `docs/DESIGN_STANDARDS.md` §7 (screen readers, text scaling, contrast, target sizes, reduced motion).
- Write deterministic tests for business rules, parsing, state transitions, failure paths, and integration boundaries; no live network in ordinary unit tests.
- Before adding a dependency, evaluate maintenance, compatibility, security, licensing, and transitive cost.
- Do not suppress warnings or weaken tests to hide a problem.

## OpenCode workflow

Before changing code: (1) inspect the repo and read applicable `AGENTS.md` files; (2) identify the target platform, existing conventions, and relevant requirements across the three specification documents; (3) inspect the APIs and dependencies involved; (4) make the smallest complete change.

After changing code: (1) review the full diff for unintended changes; (2) run relevant formatter, lint/static-analysis, tests, and native build checks where practical; (3) report exactly which checks ran and their outcomes; (4) state failures and unverified assumptions plainly.

Ask for clarification before a material product or architecture decision that cannot be safely inferred. Do not turn a request for one change into unrelated cleanup.

## Definition of done

- Requirements and platform conventions are met.
- Error, loading, empty, and failure states are considered where applicable.
- Accessibility, privacy, and security implications are reviewed.
- Appropriate tests are added or updated.
- Relevant checks are actually run and results reported honestly.
- Documentation is updated when behavior or architecture changes.
- No secrets, unrelated changes, or unexplained warnings are introduced.

## Agent skills

- **Issue tracker:** GitHub issues in `humayunkabir/being-muslim`, driven with the `gh` CLI. See `docs/agents/issue-tracker.md`.
- **Triage labels:** the five canonical roles, verbatim: `needs-triage`, `needs-info`, `ready-for-agent`, `ready-for-human`, `wontfix`. See `docs/agents/triage-labels.md`.
- **Domain docs:** single-context — one `GLOSSARY.md` at the repo root, extended by `/domain-modeling` as terms are resolved; ADRs stay in `docs/DECISIONS/`. See `docs/agents/domain.md`.

## Graft — repo context graph

Indexed by `graft` (local, regenerable, uncommitted cache). Get context from the graph before grepping or opening source files; usage rules in `docs/agents/graft.md`. After big code changes, run `graft build`.
