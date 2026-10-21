# 0008 — Self-hosted Appwrite is the backend

- **Status:** Accepted
- **Date:** 2026-10-21

## Context

`0003` chose Appwrite Cloud and deferred self-hosting. A self-hosted Appwrite
instance is now running at `https://appwrite.alveo.dev/v1` with project
`being-muslim`, server API 2.5.0. The project owner confirmed on 2026-10-20 that
this instance is the production backend, replacing Appwrite Cloud. No Appwrite
Cloud project was used.

## Decision

- **The production backend is self-hosted Appwrite** at the endpoint above.
  Appwrite Cloud is not part of the deployment.
- Environment separation follows `0007`: the current `being-muslim` project is
  the development project; a staging project is created before the first
  external account; a production project is created before the public release.
  All three may live on the same self-hosted instance.
- **Operational ownership is ours**, not Appwrite's: TLS certificate renewal,
  upgrades, backups and restore tests, monitoring, disk capacity, access
  control, and security patching. The core product still works offline, so a
  backend outage affects only optional sync (`0003`).
- Pin the Android client SDK to the server's API line (2.5.x, currently
  `io.appwrite:sdk-for-android:30.0.0`). Upgrade the server and the SDK together
  after checking the compatibility note.
- The endpoint and project ID are build configuration; the administrative API
  key is a server-side secret and is never embedded in either app.

## Alternatives considered

- **Appwrite Cloud (`0003`).** Rejected now that a self-hosted instance is
  operating and the owner wants it in production. It would avoid the operational
  burden but add a vendor dependency and a second backend to keep in sync.
- **Cloud for production, self-hosted for development.** Rejected: two backends
  to configure, verify, and migrate between for no MVP benefit.

## Consequences

- Uptime, backups, certificate rotation, and security patching become our
  responsibility. A failed restore or an expired certificate is a production
  outage for optional sync.
- Data residency and retention are under our control.
- Cost shifts from a Cloud plan to server operation and maintenance time.
- The Android build config uses this endpoint; there is no Cloud fallback.
- Before the closed beta, verify at minimum: automated backups with a successful
  restore, TLS renewal, a tested upgrade path, monitoring/alerting, and a
  rotated, least-privilege admin key. If the instance cannot meet that bar,
  revisit this decision rather than shipping on an unverified backend.
