# Product Requirements — Islamic Mobile App

**Status:** MVP scope recorded — last reviewed 2026-10-18. The MVP feature set and status are in §2; the product name and brand remain open (§14). **Owns:** product principles, feature scope, content provenance and calculation rules, localization of content, AI content, data/account obligations, notification behavior, content governance, release gates. **Defers:** how content and permissions are presented → `DESIGN_STANDARDS.md`; module layout and secure storage → `ARCHITECTURE.md`.

This records product-specific quality requirements for an Islamic mobile app. The MVP scope is decided in §2, and the decisions behind it are hardened into ADRs where they are costly to reverse (`docs/DECISIONS/`). Requirements below are accepted for the MVP, deferred, or out of scope as each row and section states.

## 1. Product principles

1. Religious content accuracy, attribution, and respectful presentation are critical product quality.
2. Be transparent about the source, method, and limitations of content and calculations.
3. Support different languages, locales, and legitimate user preferences without silently imposing one convention.
4. Minimize personal data collection, especially precise location and devotional activity.
5. Make core experiences useful without an account unless one is genuinely necessary.
6. Do not imply scholarly authority, official endorsement, or guaranteed correctness without evidence.
7. Keep Islamic content and calculation behavior testable and independently reviewable.

## 2. Scope and feature status

The MVP scope and the decisions behind it are recorded in the MVP specification (GitHub issue #1) and hardened into ADRs where they are costly to reverse (`docs/DECISIONS/`). Status vocabulary: **MVP**, **Later**, **Out of scope**. Do not add a feature just because it is common in other Islamic apps.

The MVP is **Android first, iOS following** (`DECISIONS/0002`), and its core works **fully offline and without an account** (`DECISIONS/0003`). **No religious content ships** in this version: every content row below is deferred, and anything that ships later must first clear the content policy (`DECISIONS/0004`). Calculation sources are named and their parameters disclosed; no convention is imposed silently (§5).

| Feature | User problem | Status | Platforms | Content / calculation source | Data & permissions | Deferral reason | Review owner |
| --- | --- | --- | --- | --- | --- | --- | --- |
| Prayer times | Know when to pray for a chosen location | MVP | Android first; iOS follows | BatoulApps *Adhan* (MIT), offline; calculation method and Asr juristic method chosen by the user, parameters per §5 | City chosen from the bundled offline catalog; no permission required; device location optional | — | Maintainer (calculation reference cases) |
| Qibla direction | Face the qibla | MVP | Android first; iOS follows | Great-circle bearing from the city's coordinates (§6); live device compass optional | City coordinates; original heading from device sensors when present; no network | — | Maintainer |
| Hijri calendar | Track Hijri dates | MVP | Android first; iOS follows | Platform Umm al-Qura conversion, labelled *calculated*; user ±1-day offset (§7) | Date only; no permission | — | Maintainer |
| Reminders | Be prompted to observe a practice | MVP | Android first; iOS follows | Local scheduling only, no push infrastructure (§11) | Notification permission, requested in context; no account | — | Maintainer |
| Tasbih | Count dhikr without the app attributing content to a source | MVP | Android first; iOS follows | User-entered label and target; count only, no dhikr corpus | Local storage only; no permission | — | Maintainer |
| Settings | Control language, location, calculation method, and offsets | MVP | Android first; iOS follows | — | Local preferences; settings sync only if an optional account is used | — | Maintainer |
| Account & sync (optional) | Carry settings and the tasbih total across devices | MVP (optional) | Android first; iOS follows | Appwrite Cloud; Apple and Google sign-in (§10) | OAuth identity; settings and tasbih total only — no location and no devotional timestamps | — | Maintainer |
| Report a problem | Report a wrong time or other problem with the context attached | MVP | Android first; iOS follows | User-initiated report carrying location, method and date (§5) | User-initiated only; no background collection | — | Maintainer |
| Quran reading | Read the Arabic text with reliable attribution | Later | Both | Source not identified, see §3 | Bundled or downloaded content; no account | No publisher, edition, license, attribution and named reviewer have yet cleared `DECISIONS/0004` | TBD (named Arabic reviewer required) |
| Translations | Understand the Quran in a chosen language | Later | Both | Source not identified, see §3 | Content only | As Quran reading; a translation also needs a language reviewer | TBD (translator + language reviewer) |
| Duas | Access reported supplications with sources | Later | Both | Source not identified, see §4 | Content only | As Quran reading; wordings must stay separate (§4) | TBD (named reviewer required) |
| Hadith | Read hadith with grading and attribution | Later | Both | Source not identified, see §4 | Content only | As Quran reading; grading rules per §4 | TBD (named reviewer required) |
| Audio (recitation, adhan) | Listen to a recitation or adhan | Later | Both | Recording source not identified | Bundled or streamed audio; storage | No rights-clear recording source; recorded adhan audio is also deferred | TBD (named reviewer required) |
| Bookmarks | Return to saved content | Out of scope | Both | — | Local data only | Depends on content that does not ship in the MVP | — |

Deferred and excluded **without their own row**, for the record: AI-generated content (§9), social/community features, mosque finder, Zakat calculator, 99 Names, AR qibla, streaks or other engagement mechanics, ads, and paid tiers — all excluded by the MVP decision (issue #1, "Out of scope"). Locales beyond Bengali and English are out of scope for the MVP.

### 2.1 MVP acceptance criteria

Feature-level acceptance criteria for the MVP. The full user-story set and the per-ticket breakdown live in the MVP specification (issue #1) and its sub-issues; the tests that cover them are named in `docs/ARCHITECTURE.md` §8.

**Prayer times**
- Today's five times are shown for the chosen city and method, computed offline, and match the documented reference cases within the agreed tolerance.
- The chosen calculation method, Asr juristic method and high-latitude rule are visible on the times screen and never change silently, and a time the rule produced is disclosed as such.
- Every displayed time is reproducible from recorded inputs — method, parameters, location, timezone, date and offsets.
- Tomorrow's times are available, and the city and method can be changed after setup (issue #5).

**Qibla direction**
- The bearing is computed from the chosen city's coordinates with no permission required, and is relative to true north.
- A live compass is shown only where the device supports one, with calibration and approximate-reading states, and the app says plainly when it cannot.

**Hijri calendar**
- The Hijri date is shown alongside the Gregorian date and labelled as calculated (Umm al-Qura); a user ±1-day adjustment is available.
- A calculated date is never presented as a moon sighting.

**Reminders**
- Reminders are per-prayer and off by default; the notification permission is requested only when a user enables one.
- Quiet hours are configurable; notification text reveals no devotional detail; the app states that exact delivery depends on the OS.
- The scheduling policy is pure and testable in the domain layer.

**Tasbih**
- The user types a label and sets a target; each count gives haptic feedback and the counter can be reset and reviewed.
- The total survives restarts and works offline; the app attributes no dhikr text.

**Settings**
- Language (Bengali or English), city, calculation method, Asr juristic method, high-latitude rule, and per-prayer offsets are viewable and changeable; choices survive restarts.
- Offsets are shown as the user's own adjustment, separate from the raw result, and can be removed.

**Account & sync (optional)**
- Every MVP feature works logged out and when the sync service is unreachable; sign-in is via Apple or Google.
- The synced payload is settings and the tasbih total only — no location and no devotional timestamps; in-app account deletion and data export are available, and what leaves the device is disclosed.

**Report a problem**
- A report carries the location, method and date so that a wrong time can be investigated.

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

Code-level separation and test boundaries: `ARCHITECTURE.md` §6, §8. Library, version, license, the offered methods and their parameters, and the reference cases and tolerance: `docs/PRAYER_TIMES.md`. Acceptance tests: multiple methods, time zones, daylight-saving boundaries, high-latitude cases, and permission-denied/manual-location paths.

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

Bearing formula, true-north correction, compass states and known limits: `docs/QIBLA.md`. Test boundaries: `ARCHITECTURE.md` §6, §8.

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

Track these explicitly rather than letting implementation choices become accidental product policy. Decisions that are difficult to reverse move into `docs/DECISIONS/` as an ADR when they are made. Only what is **genuinely still open** is listed here; the MVP scope and the decisions settled while producing it now live in §2 and `docs/DECISIONS/`.

- **Final product name and brand.** The working name "Being Muslim" and the `app.beingmuslim.*` identifier namespace are provisional (`DECISIONS/0001`).
- **Launch markets, store accounts, and release signing** for the first public Android release.
