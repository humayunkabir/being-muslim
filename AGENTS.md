# Repository Engineering Rules

## Mission and platform choices

Build a high-quality Islamic mobile product as a Git monorepo with two independently buildable, fully native applications:

- **iOS:** Swift, SwiftUI, native Apple frameworks, and Swift Package Manager where appropriate.
- **Android:** Kotlin, Jetpack Compose, Android Jetpack libraries, and Gradle.
- **UI:** Separate native implementations; do not force pixel-identical layouts or interactions.
- **Shared assets:** Product requirements, terminology, API/data contracts, design-token source definitions, and documentation only where sharing has a concrete benefit.

Do not introduce Flutter, React Native, Kotlin Multiplatform, or another cross-platform UI framework without explicit approval from the project owner.

The product name and final feature scope are intentionally not fixed in these engineering rules.

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

## Repository and build expectations

Use the structure in `docs/ARCHITECTURE.md` as a guide, not a mandate to create every directory immediately.

- Keep iOS independently buildable with Xcode and Android independently buildable with Gradle.
- Use normal native project layouts and build workflows.
- Add a directory or module only when it has a defined responsibility or is required by the native toolchain.
- Keep secrets, signing credentials, local machine settings, generated output, and caches out of version control.
- Do not introduce a custom build orchestrator when standard Xcode and Gradle workflows are sufficient.

## Native implementation standards

### iOS

- Use Swift and SwiftUI by default; use UIKit when a native capability or concrete limitation warrants it.
- Follow Apple's Human Interface Guidelines and Swift API Design Guidelines.
- Respect Dynamic Type, safe areas, semantic colors, accessibility, system appearance, and expected navigation behavior.
- Prefer structured concurrency, explicit task lifetimes, and appropriate actor isolation. Keep UI updates on `MainActor` when required.
- Avoid force-unwrapping in ordinary production paths and unsafe shared mutable state.

### Android

- Use Kotlin and Jetpack Compose by default; use Android Views when justified by a native integration or concrete limitation.
- Follow Material Design guidance and official Kotlin coding conventions.
- Respect system insets, font scaling, accessibility, navigation, and system settings.
- Prefer structured concurrency, explicit coroutine scopes, lifecycle-aware collection, and immutable state (`val` by default).
- Avoid `GlobalScope`, unnecessary blocking calls, and mutable state shared across unrelated components.

### Cross-platform consistency

Share product intent and behavior, not UI implementation. Platform-specific navigation, typography, controls, gestures, and transitions may differ when that improves native usability.

## Quality requirements

- Validate untrusted input and handle expected failures explicitly.
- Never log credentials, tokens, private user content, or sensitive payloads.
- Use platform-appropriate secure storage and least privilege.
- Keep expensive work off the UI thread.
- Support screen readers, text scaling, adequate contrast, usable target sizes, and relevant reduced-motion preferences.
- Write deterministic tests for important business rules, parsing, state transitions, failure paths, and integration boundaries.
- Avoid real network access in ordinary unit tests; use controlled dependencies.
- Evaluate dependency maintenance, compatibility, security, licensing, and transitive dependencies before adding packages.
- Do not suppress warnings or weaken tests merely to hide a problem.

## OpenCode workflow

Before changing code:

1. Inspect the repository and read applicable `AGENTS.md` files.
2. Identify the target platform, existing conventions, and relevant requirements.
3. Inspect the APIs and dependencies involved.
4. Make the smallest complete change.

After changing code:

1. Review the full diff for unintended changes.
2. Run relevant formatter, lint/static-analysis, tests, and native build checks where practical.
3. Report exactly which checks ran and their outcomes.
4. State failures and unverified assumptions plainly.

Ask for clarification before making a material product or architecture decision that cannot be safely inferred. Do not turn a request for one change into unrelated cleanup.

## Definition of done

- Requirements and platform conventions are met.
- Error, loading, empty, and failure states are considered where applicable.
- Accessibility, privacy, and security implications are reviewed.
- Appropriate tests are added or updated.
- Relevant checks are actually run and results reported honestly.
- Documentation is updated when behavior or architecture changes.
- No secrets, unrelated changes, or unexplained warnings are introduced.

## Agent skills

### Issue tracker

Issues and specs live as GitHub issues in `humayunkabir/being-muslim`, driven with the `gh` CLI. See `docs/agents/issue-tracker.md`.

### Triage labels

The five canonical triage roles, labelled verbatim: `needs-triage`, `needs-info`, `ready-for-agent`, `ready-for-human`, `wontfix`. See `docs/agents/triage-labels.md`.

### Domain docs

Single-context: one `GLOSSARY.md` at the repo root; ADRs stay in `docs/DECISIONS/`. See `docs/agents/domain.md`.

<!-- graft:start -->
## Graft — repo context graph

This repo is indexed in `graft/`: small linked markdown nodes that explain each
system and carry exact file:line spans, kept in sync with the code through git.

For ANY task here — understanding how something works, finding where code lives,
or scoping a change — get context from the graph before grepping or opening
source files. Re-ask freely (it's cheap) and reuse literal identifiers you
already have (symbol, error string, file name) as the query. New to this repo?
Run `graft map` first — a token-budgeted orientation (dir clusters, hubs,
hotspots), no LLM, no key.

- Run `graft ask "<your question>" --source` → ranked nodes with the relevant
  code spans inlined (each hit's ≤8-line crux by default; `--full` for whole
  definitions when the crux isn't enough). Match the tool to the task shape:
  for understanding or editing, the top node IS the answer — cite its
  `covers:` file:line spans and edit straight from `--source`. For
  exhaustive tasks ("every occurrence / every caller of this pattern"), ranked
  results are top-N, not complete — run `graft grep "<literal>"` instead
  (exhaustive over indexed files, grouped by enclosing symbol), falling back
  to raw `grep -rn` only for unindexed files.
- `graft skeleton <file>` → every definition's signature + span, ~10× cheaper
  than reading the file; use it to skim an API surface.
- `graft callers <symbol>` gives precomputed, exact edges — who calls this.
  Add `--direction out` for what it calls, or `--depth N` to walk
  transitively for the full blast radius. For structural questions, skip
  ranking and use this directly.
- Or browse: `graft/INDEX.md` lists every node; follow the links.
- Monorepos and folders of multiple repos rank fairly across sub-projects —
  hits carry `[scope/]` labels naming which one they're from. Narrow with
  `graft ask "<task>" --in <scope>/` once you know where you're working.

If a returned span is truncated ("+N more lines"), open the file at that exact
range before finalizing. Only open source files when a node genuinely lacks a
needed detail, and then at the exact file:line the node points to — never
re-read whole files.

After big code changes, refresh the graph with `graft build` (deterministic,
no API key, $0).
<!-- graft:end -->
