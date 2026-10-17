# 0003 — Offline-only core, optional account on Appwrite Cloud

- **Status:** Accepted
- **Date:** 2026-10-18

## Context

Product principle 5 requires core experiences to be useful **without an account**
(`docs/PRODUCT_REQUIREMENTS.md` §1), and the MVP spec (issue #1) commits to an
app that works without a signal, an account, or a subscription, and that is free
of ads. Prayer times, qibla, the Hijri date, reminders, and tasbih are all
computable or storable locally; the bundled city catalog supplies locations with
no network.

At the same time the product wants an **optional** account so a user with more
than one device does not set things up twice, and syncing settings and the
tasbih total is genuinely useful. A backend is therefore needed, but it must
never become a dependency of a core experience.

## Decision

- **The core works offline, with no account and no network.** Every MVP feature
  is usable on a fresh install with airplane mode on: calculation, the bundled
  city catalog, the Hijri conversion, local reminders, and tasbih.
- **The account is optional**, per `PRODUCT_REQUIREMENTS.md` principle 5. Nothing
  is gated behind sign-in, and no feature degrades silently when the user stays
  logged out.
- **The backend is Appwrite Cloud**, with **Sign in with Apple** and **Sign in
  with Google**. Self-hosting Appwrite is deferred; Appwrite Cloud covers the MVP.
- **The synced payload is minimal**: settings and the tasbih total only. No
  location, no precise coordinates, no devotional timestamps, and no content.
- **Sync failure or the service being unreachable never blocks a local feature.**
  The local store is the source of truth; sync is a best-effort projection of it.
- The API contract is documented per `docs/ARCHITECTURE.md` §5 — versioning,
  error format, retention, deletion and export — and clients carry no privileged
  server credentials (`docs/ARCHITECTURE.md` §7).
- There is **no push infrastructure**. Reminders are local notifications only.

## Alternatives considered

- **Account required** for any of the MVP features. Rejected: violates product
  principles 4 and 5 and the trust requirement in the MVP spec.
- **No account at all in the MVP.** Rejected: cross-device setup was an explicit
  MVP story; the account stays optional rather than absent.
- **Self-hosted Appwrite.** Deferred, not rejected: it adds operational surface
  the MVP does not need.
- **A custom backend.** Rejected: more to build, secure, and operate than the
  MVP's minimal synced payload justifies.

## Consequences

- The local store must be able to answer every feature without the network, which
  keeps the domain layer free of networking imports
  (`docs/ARCHITECTURE.md` §3, §5).
- The synced surface is small enough to reason about for privacy, retention, and
  deletion (`docs/PRODUCT_REQUIREMENTS.md` §10).
- Because sync can fail or be absent, **sync conflict and merge behaviour** must
  be defined and tested in the domain layer, not assumed away.
- Appwrite project configuration (platforms, OAuth providers, database, and
  permissions) becomes explicit setup work and a release dependency (issue #22).
