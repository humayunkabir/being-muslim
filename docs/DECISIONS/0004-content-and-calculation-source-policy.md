# 0004 — No content or calculation convention ships without a recorded source

- **Status:** Accepted
- **Date:** 2026-10-18

## Context

Religious-content accuracy and attribution are critical product quality
(`docs/PRODUCT_REQUIREMENTS.md` §1). The MVP spec (issue #1) found that the
existing failure mode is worse than no app: presenting a calculated time or a
calculated Hijri date as settled fact, and shipping religious content that
cannot be traced to a verified source, with no way for a user to tell how an
answer was produced.

No Quran, Hadith, or supplication content can currently be attributed to a
verified source with a redistribution license and a named reviewer, and recorded
adhan audio is deferred for the same reason. The MVP therefore ships **no
religious content** — but calculation still involves conventions (calculation
method parameters, the Asr juristic method, high-latitude rules, and the Hijri
conversion) that are contested and must not be presented as neutral fact.

## Decision

- **No religious content ships** — Quran text or translation, tafsir, Hadith,
  duas/adhkar, or adhan audio — until, for each source, the research records the
  **publisher, exact edition or version, the quoted license with a URL, the
  redistribution terms, the required attribution, the retrieval date, the Bengali
  path and its cost, and a named review owner** for the Arabic and the Bengali
  rendering. Until then the content is deferred, not hidden: the deferral is
  recorded (`docs/PRODUCT_REQUIREMENTS.md` §2, §12).
- **Calculation sources are named, versioned, and disclosed.** The calculation
  library, its version and license, the method parameters, the Asr juristic
  method, the high-latitude rule, and the Hijri convention are recorded, shown to
  the user, and never substituted silently (`docs/PRODUCT_REQUIREMENTS.md` §5,
  §7; `docs/ARCHITECTURE.md` §6). A calculation is labelled as calculated.
- **User-supplied text is not religious content.** A tasbih label the user types
  is the user's own text; the app attributes nothing and ships no dhikr corpus.
- **Nothing may appear in the UI as a placeholder that implies content ships**
  when it does not (issue #1).
- Corrections to published content, when content eventually ships, are versioned
  and traceable — never silent (`docs/PRODUCT_REQUIREMENTS.md` §12).

## Alternatives considered

- **Ship content labelled "unverified".** Rejected: a label does not make an
  unattributable source fit to present as religious material, and it undermines
  the trust the product depends on.
- **Ship content from a convenient public source without a license review.**
  Rejected: redistribution rights and attribution obligations cannot be assumed.
- **Ship calculations without disclosing the method.** Rejected: it is the exact
  failure mode the MVP set out to fix.

## Consequences

- The MVP is content-free; the deferred content list stays **gated, not parked**
  — each item must clear the policy above before it ships.
- The maintainer is the review owner for now, with a qualified reviewer to follow;
  that limitation is disclosed in-app when content lands.
- The city catalog is held to the same bar as content for its **license to
  redistribute**, even though cities are not religious content (issue #3,
  `docs/PRODUCT_REQUIREMENTS.md` §14).
- Content-provenance plumbing is not built speculatively while no content ships
  (`AGENTS.md` rule 6); it is added when the first content source clears.
