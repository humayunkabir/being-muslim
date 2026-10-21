# 0009 — Closed beta keeps the working name and placeholder assets

- **Status:** Accepted
- **Date:** 2026-10-22

## Context

`DECISIONS/0001` recorded **Being Muslim** and `app.beingmuslim.*` as working
placeholders, and required the name to be replaced before any store upload if it
changes. It also recorded that the launcher icon is a neutral square and that
the apps are not distributable as committed.

Issue #20 publishes a closed beta to Google Play. Google fixes the
`applicationId` when an app is first uploaded, so the identifier must be chosen
before that upload, not after it. The product name, logo, palette, and final
visual identity are still open (`PRODUCT_REQUIREMENTS.md` §14,
`DESIGN_STANDARDS.md` §1).

## Decision

- The closed beta ships as **Being Muslim** with `app.beingmuslim.android`.
  Both are retained, not replaced, for this test release. The display name can
  still change later; the `applicationId` cannot, so it is kept deliberately
  neutral rather than tied to a brand decision.
- The **placeholder launcher icon** (a white square on `#FF546E7A`) is accepted
  as the app's mark for the **closed beta only**, including the 512×512 store
  icon derived from it. Beta testers are told the name and icon are provisional.
- The 1024×500 feature graphic and the screenshots are still required by Google
  Play for a published track and are produced by a person; they are not waived
  by this decision. They must use the same provisional mark.
- Play Store listing copy and all graphics must be replaced before the public
  1.0 release (issue #21).
- Bengali-language store listing copy is deferred until the Bengali review in
  issue #39 signs the locale off; until then the listing is English-only, as the
  app's Bengali strings are still marked unreviewed (`DECISIONS/0006`).
- The **iOS placeholders** recorded in `DECISIONS/0001` (the empty `AppIcon`
  and `AccentColor`, and `app.beingmuslim.ios`) are outside this release: iOS is
  not distributed (`DECISIONS/0002`). Their retention is accepted until iOS
  parity work, when they must be replaced.
- This acceptance is limited to the closed beta. It does not approve the
  placeholders for public distribution.

## Alternatives considered

- **Rename before the first upload.** Rejected: no name or brand has been
  chosen, and inventing one now would make a permanent, brand-driven
  `applicationId` for a product that is still settling.
- **Keep the acceptance implicit in `DECISIONS/0001`.** Rejected: the acceptance
  criteria for issue #20 require retention to be explicit and public, and the
  placeholder-removal requirement must survive into the public-release ticket.

## Consequences

- `PRODUCT_REQUIREMENTS.md` §14's open decision on the final name and brand
  remains open for the public release; only the beta's identifiers are settled.
- A public 1.0 upload cannot reuse this decision: it must replace the icon and
  store graphics, and record the shipped version with the content policy it was
  shipped under (issue #21).
- If the product is renamed after the beta, the Play listing's display name and
  in-app label change, but the package name and Play app record do not.
