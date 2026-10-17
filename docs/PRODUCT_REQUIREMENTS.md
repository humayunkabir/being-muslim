# Product Requirements — Islamic Mobile App

**Status:** Initial requirements baseline — last reviewed 2026-10-17. Product scope and name are not finalized. **Owns:** product principles, feature scope, content provenance and calculation rules, localization of content, AI content, data/account obligations, notification behavior, content governance, release gates. **Defers:** how content and permissions are presented → `DESIGN_STANDARDS.md`; module layout and secure storage → `ARCHITECTURE.md`.

This records product-specific quality requirements for an Islamic mobile app. It does not commit any feature to the roadmap: confirm scope before implementing, and mark requirements accepted, deferred, or out of scope.

## 1. Product principles

1. Religious content accuracy, attribution, and respectful presentation are critical product quality.
2. Be transparent about the source, method, and limitations of content and calculations.
3. Support different languages, locales, and legitimate user preferences without silently imposing one convention.
4. Minimize personal data collection, especially precise location and devotional activity.
5. Make core experiences useful without an account unless one is genuinely necessary.
6. Do not imply scholarly authority, official endorsement, or guaranteed correctness without evidence.
7. Keep Islamic content and calculation behavior testable and independently reviewable.

## 2. Scope and feature status

Every row is a **candidate** — none is committed until scope is explicitly decided. Do not add a feature just because it is common in other Islamic apps. Status vocabulary: **Candidate — not scoped**, **MVP**, **Later**, **Out of scope**.

| Feature | User problem | Status | Platforms | Content / calculation source | Data & permissions | Review owner |
| --- | --- | --- | --- | --- | --- | --- |
| Quran reading | Read the Arabic text with reliable attribution | Candidate — not scoped | Both | TBD, see §3 | Bundled or downloaded content; no account | TBD |
| Translations | Understand the Quran in a chosen language | Candidate — not scoped | Both | TBD, see §3 | Content only | TBD (translator + language reviewer) |
| Prayer times | Know when to pray for a location | Candidate — not scoped | Both | TBD provider, see §5 | Location; notifications | TBD |
| Qibla direction | Face the qibla | Candidate — not scoped | Both | TBD, see §6 | Location; device sensors | TBD |
| Duas | Access reported supplications with sources | Candidate — not scoped | Both | TBD, see §4 | Content only | TBD |
| Hadith | Read hadith with grading and attribution | Candidate — not scoped | Both | TBD, see §4 | Content only | TBD |
| Hijri calendar | Track Hijri dates | Candidate — not scoped | Both | TBD, see §7 | Date source | TBD |
| Reminders | Be prompted to observe a practice | Candidate — not scoped | Both | Local scheduling, see §11 | Notification permission | TBD |
| Bookmarks | Return to saved content | Candidate — not scoped | Both | Local storage | Local data only | — |
| Settings | Control language, locale, and calculation method | Candidate — not scoped | Both | — | Local preferences | — |

Before a row moves off "Candidate — not scoped", record: the user problem; MVP/later/out-of-scope status with platforms and locales; content provider or calculation source; data and permission requirements; acceptance criteria and tests; and a review owner for religious or linguistic content where appropriate.

## 3. Quran content requirements

If Quran text is included:

- Record the exact Arabic source, edition/revision if provided, license, and retrieval date/version; ship only from a verified source, never an unverified scrape.
- Preserve surah and ayah identifiers and source ordering; do not paraphrase, silently normalize, omit, or modify the Arabic text.
- If diacritics or orthographic normalization are supported, document the source and transformation and validate against approved fixtures.
- Keep Arabic text separate from translations, transliteration, tafsir, and commentary in both data models and UI.
- Attribute each translation with its translator, language, edition/version, license, and source; never treat a translation as the Arabic Quran.
- Store source identifiers so displayed passages trace to their source.
- Test beginning/end boundaries, verse numbering, pagination, search results, and offline copies if applicable.
- Provide a correction/reporting route for suspected text or reference errors.

Code-level separation and tests: `ARCHITECTURE.md` §6, §8.

## 4. Hadith and dua requirements

If Hadith or dua content is included:

- Record collection/source, book/chapter or other reference, narrator/compiler metadata when available, language, edition/version, and license.
- Preserve grading/authenticity metadata only when a traceable source supplies it, and identify whose grading is shown. Never invent references, grades, chains, or attributions.
- Distinguish original Arabic, translation, transliteration, commentary, and editorial notes.
- Represent multiple reported wordings or sources separately rather than merging them.
- Use a documented editorial/review process for corrections and disputed metadata; never present a paraphrase as a verbatim Hadith or prophetic supplication.

## 5. Prayer-time calculation requirements

If prayer times are included:

- Identify the calculation library/provider, version, and license.
- Record the selected method and configurable parameters — Fajr/Isha angles or intervals, high-latitude rules, Asr juristic method, supported offsets.
- Explain that calculated times vary by method, location, date, and local authority, and never imply one method is universally accepted.
- Make the chosen method visible and changeable where supported; never change it silently.
- Handle daylight-saving transitions, time zones, date boundaries, high latitudes, invalid/missing location, and manual location entry.
- Separate raw calculated results from user adjustments and display settings.
- Do not promise exact notification delivery times the OS cannot guarantee.
- Validate against documented reference cases from the provider and compare representative locations/dates with trusted local sources before release.

Code-level separation and test boundaries: `ARCHITECTURE.md` §6, §8. Acceptance tests: multiple methods, time zones, daylight-saving boundaries, high-latitude cases, and permission-denied/manual-location paths.

## 6. Qibla and location

If Qibla or location-based features are included:

- Request location only when needed and explain the benefit before the permission prompt; request the least precise permission that satisfies the feature.
- Provide manual city/location selection where feasible.
- Handle denied, restricted, unavailable, stale, and approximate location gracefully.
- Explain that compass readings may be affected by device calibration and environmental interference.
- Avoid storing precise location history unless a clearly justified feature requires it.
- If location is sent to a server, disclose why, what is sent, retention, and controls before collection.
- Never place coordinates, location history, or other sensitive values in analytics or diagnostic logs.

Permission UX: `DESIGN_STANDARDS.md` §8.

## 7. Hijri calendar and dates

If Hijri dates are included:

- Document whether dates are calculated, from a published calendar, or from an announced local observation.
- Explain that Hijri dates may differ by country, authority, or moon-sighting convention; never present a calculated date as a confirmed local sighting.
- Make any supported adjustment or calendar preference discoverable.
- Test Gregorian/Hijri boundaries, local time zones, and date transitions.

## 8. Localization and directionality

For every supported locale:

- Maintain reviewed translations for navigation, settings, permission explanations, errors, and accessibility labels.
- Handle pluralization, dates, numbers, and time formatting through platform localization APIs.
- Support Arabic RTL layout including mixed Arabic/Latin strings and punctuation.
- Validate Bengali and English text if they are supported locales.
- Keep religious translations traceable to their specific source; do not use ordinary UI translation workflows to rewrite sacred source text.
- Do not claim a locale is supported until key journeys and critical content are reviewed.

Layout, RTL, and fonts: `DESIGN_STANDARDS.md` §5.

## 9. AI-generated content (if introduced)

AI features are not assumed to be in the MVP. If introduced:

- Clearly label generated summaries or explanations as AI-generated.
- Never fabricate Quran verses, Hadith text, citations, scholarly opinions, or rulings; show source references that can be independently opened and checked.
- If reliable grounding is unavailable, say so rather than inventing an answer.
- Do not present the model as a scholar or mufti; direct users to qualified scholars for consequential or disputed questions.
- Keep generated content distinct from verified source text in data structures and UI.
- Define moderation, privacy, retention, failure, and abuse controls before release.
- Do not send private user data or precise location to an AI provider without a documented purpose and appropriate disclosure.

## 10. Privacy, security, and accounts

Owns *what data* the product may handle. Permission UX: `DESIGN_STANDARDS.md` §8; secure storage and secrets: `ARCHITECTURE.md` §7.

- Establish a data inventory before adding analytics, accounts, cloud sync, or personalization; collect only data needed for an explicit purpose.
- Explain collection, use, sharing, retention, and deletion in understandable privacy documentation.
- Avoid requiring an account for local-only functionality unless necessary.
- Provide account deletion and data export where applicable and required.
- Use platform secure storage for secrets and sensitive local credentials; encrypt network traffic; never embed privileged server secrets in app binaries.
- Never log private content, authentication data, precise coordinates, or devotional activity.
- Keep analytics opt-in/controls and platform policy obligations under review before release.
- Define backup, retention, and deletion behavior for locally stored and server-side data.

## 11. Notifications and reminders

If reminders are included:

- Request notification permission in context and explain the benefit; respect device settings and revocation.
- Let users configure reminder types and quiet hours where appropriate.
- Avoid guilt-inducing or manipulative wording.
- Make clear that exact delivery may depend on OS scheduling and battery policies.
- Keep notification content privacy-conscious; avoid sensitive previews by default where appropriate.

Permission copy and preview privacy: `DESIGN_STANDARDS.md` §8.

## 12. Content governance and correction workflow

Before publishing a content source: (1) identify the publisher/provider and verify provenance; (2) confirm license and permitted use; (3) record source version, retrieval date, and any transformations; (4) define who reviews language, references, and religious metadata; (5) store source identifiers with the content; (6) test imports and transformations against known fixtures; (7) provide a way to report errors; (8) define how corrections are reviewed, versioned, distributed, and communicated.

If an error is confirmed, correct it with traceable version history. Do not quietly replace a source or erase relevant correction history.

## 13. Testing and release gates

A release containing religious content or calculations must meet the relevant gates:

- [ ] Source provenance and license are documented.
- [ ] References and attribution are visible where appropriate.
- [ ] Content fixtures match the approved source/version.
- [ ] Prayer-time and Qibla edge cases are tested if those features exist.
- [ ] Arabic shaping, RTL, supported locales, and accessibility are reviewed (`DESIGN_STANDARDS.md` §7, §10).
- [ ] Permission-denied, offline, empty, and error states are tested.
- [ ] Privacy and security implications are reviewed.
- [ ] No generated text is presented as verified religious source material.
- [ ] Known limitations are disclosed clearly.
- [ ] A correction/reporting path exists for published content.

A feature that does not meet its relevant gates must not be represented as verified or production-ready.

## 14. Open decisions

Track these explicitly rather than letting implementation choices become accidental product policy. Decisions that are difficult to reverse move into `docs/DECISIONS/` as an ADR.

- Final product name and brand (working name "Being Muslim" and the `app.beingmuslim.*` identifier namespace are provisional — `DECISIONS/0001-repository-layout-and-identifiers.md`).
- MVP feature list (see the §2 inventory).
- Supported languages and launch markets.
- Quran text and translation sources/licenses.
- Hadith and dua sources, metadata, and review process.
- Prayer-time calculation provider and supported methods.
- Qibla calculation approach and location-permission strategy.
- Hijri calendar convention, if applicable.
- Offline access requirements.
- Account, cloud sync, analytics, and AI scope.
- Content correction ownership and support channel.
