# 0007 — Separate Appwrite projects for development, staging, and production

- **Status:** Accepted
- **Date:** 2026-10-20

## Context

`ARCHITECTURE.md` §7 requires development, test, and production configuration to
be separate, and issue #22 asks explicitly whether a second Appwrite project is
needed for staging before anyone outside development holds an account. The
backend currently has one project (`being-muslim`), used only by developers.
Accounts, sync, and deletion (#15–#17) are about to make real user data exist.

Appwrite isolates projects completely: users, sessions, databases, tables, and
API keys do not cross a project boundary. Separate projects therefore provide
the separation without another server.

## Decision

- **Development is the existing `being-muslim` project.** Developer test
  accounts and disposable rows stay there.
- **A second project is created for staging before the first external account**
  (the closed beta, issue #20). External testers never touch development.
- **Production is a third project**, created before the public release (issue
  #21). Staging data is disposable and is not promoted into production.
- Each project gets its **own** database/table configuration and its **own**
  administrative API key. No project reuses another project's key or data.
- Configuration identifiers are recorded per project in `docs/APPWRITE.md`;
  secrets stay in local environment files or the console, never in the
  repository.
- Staging may run on the same Appwrite deployment as development (Cloud or
  self-hosted; this ADR does not decide the host, and `0003` remains
  authoritative until amended). Project isolation is the boundary; a separate
  server is not required for the MVP.

"Staging" here is the pre-release test environment. Automated tests stay
network-free (`ARCHITECTURE.md` §8), so no separate "test" project is added.

## Alternatives considered

- **One project for everything.** Rejected: test accounts and test data would
  share a user namespace and database with real users, and a mistaken key or
  migration would reach production data.
- **A second self-hosted server for staging.** Rejected for the MVP: it adds
  operational cost without adding isolation beyond what projects already give.
- **Promote staging to production.** Rejected: staging is expected to be reset
  and polluted; production data must not inherit it.

## Consequences

- Creating a project, its database/table, and its key becomes part of the
  release checklist for #20 and #21.
- The Android app needs a way to select the endpoint/project per build type
  when account support (#15) is added; those values are build configuration,
  not secrets.
- A defect found only in staging cannot be reproduced against production data,
  which is intentional. Debugging uses the development project.
- The Appwrite deployment (Cloud or self-hosted) remains a single point of
  failure for all three projects; that risk is operational and outside this
  decision.
