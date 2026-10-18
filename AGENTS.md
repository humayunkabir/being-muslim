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

Keep secrets, signing credentials, local machine settings, generated output, and caches out of version control — this must hold on every change. Independent Xcode and Gradle builds, repository layout, and build hygiene are otherwise owned by `docs/ARCHITECTURE.md` §1–§2 and §10.

## Platform standards

Platform conventions and build/test commands sit next to the code, in `apps/ios/AGENTS.md` (Swift + SwiftUI) and `apps/android/AGENTS.md` (Kotlin + Jetpack Compose). OpenCode loads each of those only when work touches that directory, so anything that must apply everywhere belongs here in the root file instead.

Share product intent and behavior, not UI implementation; platform navigation, typography, controls, gestures, and motion may differ when that improves native usability.

## Quality requirements

- Validate untrusted input; handle expected failures explicitly.
- Never log credentials, tokens, private user content, or sensitive payloads. Use platform-appropriate secure storage and least privilege.
- Keep expensive work off the UI thread.
- Meet the accessibility bar in `docs/DESIGN_STANDARDS.md` §7.
- Write deterministic tests — no live network in ordinary unit tests. Priorities: `docs/ARCHITECTURE.md` §8.
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

- **Issue tracker:** GitHub (`humayunkabir/being-muslim`) via the `gh` CLI. See `docs/agents/issue-tracker.md`.
- **Triage labels:** the five canonical roles. See `docs/agents/triage-labels.md`.
- **Domain docs:** single-context. See `docs/agents/domain.md`.
- **Skill precedence:** these repo skills win when an installed plugin ships a
  duplicate — `tdd` over a plugin's TDD skill, `code-review` over a plugin's
  review skill, `writing-for-agents` over a plugin's skill-authoring skill.
  The plugins installed for this project, and what each adds, are listed in
  `docs/AGENT_SKILLS.md`.

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
