# 0002 — Android ships first; iOS follows

- **Status:** Accepted
- **Date:** 2026-10-18

## Context

The repository holds two independently buildable native apps — Swift + SwiftUI on
iOS, Kotlin + Jetpack Compose on Android — implementing one product
(`docs/ARCHITECTURE.md` §1). They deliberately do not share UI code
(`AGENTS.md`), so every feature is built twice, natively.

The product is not yet proven, and the working name, brand, and final feature
scope are still open (`docs/PRODUCT_REQUIREMENTS.md` §14). Building both
platforms in lockstep doubles the cost of every product change while the product
is still settling, and the MVP spec (issue #1) covers a substantial breadth of
features.

## Decision

- The **Android app ships to the public first**. iOS follows once the Android
  product settles, implementing the same product intent natively.
- Android is the **reference implementation of product behaviour** for the MVP:
  when the two platforms could diverge on behaviour, Android's settled behaviour
  is the one carried across, adjusted only for native platform convention.
- Both apps continue to build and test **independently** (`AGENTS.md`,
  `docs/ARCHITECTURE.md` §10). Neither becomes a prerequisite for the other.
- What is shared is **intent, not implementation**: the specification documents,
  the glossary, the Android-first MVP breakdown, verification rules, and any
  data contracts — never UI code.
- iOS parity is tracked (the MVP sub-issues name both platforms) but is **not a
  release gate** for the Android release.

## Alternatives considered

- **iOS first.** Rejected: no product reason privileged iOS, and the launch
  markets (Bengali- and English-speaking) skew Android-heavy.
- **Simultaneous release.** Rejected: doubles the cost of every still-moving
  product decision while the scope is unproven.
- **A cross-platform UI framework** (Flutter, React Native, Kotlin Multiplatform)
  to build once. Rejected by `AGENTS.md`: the product is meant to be fully native
  on each platform.

## Consequences

- The MVP breakdown and the sub-issues are Android-only; iOS parity work follows
  as its own tickets once the Android product settles.
- Feature work is written to a platform-free domain layer inside each app target,
  so the behaviour worth carrying across is the part that is cheapest to port
  (`docs/ARCHITECTURE.md` §3, §5, and the "No new modules" decision in issue #1).
- Divergence between the apps is an accepted, tracked risk rather than an
  accident; it is bounded by the shared specification documents.
- **The iOS app is not part of the first public release**, and its committed
  state stays a shell until parity work begins.
