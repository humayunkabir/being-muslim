# Product Requirements — Islamic Mobile App

**Status:** Initial requirements baseline; product scope and name are not finalized.

This document records product-specific quality requirements for an Islamic mobile app. It is not a claim that any feature is already committed to the roadmap. Confirm the feature scope before implementation and mark requirements as accepted, deferred, or out of scope.

## 1. Product principles

1. Treat religious content accuracy, attribution, and respectful presentation as critical product quality.
2. Be transparent about the source, method, and limitations of content and calculations.
3. Support different languages, locales, and legitimate user preferences without silently imposing one convention.
4. Minimize personal data collection, especially precise location and devotional activity.
5. Make core experiences understandable and useful without requiring an account unless an account is genuinely necessary.
6. Do not imply scholarly authority, official endorsement, or guaranteed correctness without evidence.
7. Keep Islamic content and calculation behavior testable and independently reviewable.

## 2. Scope and feature status

Candidate capabilities may include Quran reading, translations, prayer times, Qibla, duas, Hadith, Hijri calendar, reminders, bookmarks, and settings. These are candidates only; they are not all automatically in scope.

Before implementation, maintain a feature inventory with:
- Feature name and user problem.
- MVP / later / out of scope status.
- Supported platforms and locales.
- Content provider or calculation source.
- Data and permission requirements.
- Acceptance criteria and tests.
- Review owner for religious or linguistic content where appropriate.

Do not add a feature just because it is common in other Islamic apps.

## 3. Quran content requirements

If Quran text is included:

- Record the exact Arabic text source, edition/revision if provided, license, and retrieval date/version.
- Preserve surah and ayah identifiers and source ordering.
- Do not paraphrase, silently normalize, omit, or modify the Arabic Quran text.
- If diacritics or orthographic normalization are supported, document the source and transformation; validate the output against approved fixtures.
- Keep Arabic text separate from translations, transliteration, tafsir, and commentary in both data models and UI.
- Attribute each translation and record its translator, language, edition/version, license, and source.
- Do not treat a translation as the Arabic Quran itself.
- Include source identifiers in stored content so displayed passages can be traced to their source.
- Test beginning/end boundaries, verse numbering, pagination, search results, and offline copies if applicable.
- Provide a correction/reporting route for suspected text or reference errors.

Do not ship Quran text from an unverified scrape or an unknown source.

## 4. Hadith and dua requirements

If Hadith or dua content is included:

- Record the collection/source, book/chapter or other available reference, narrator or compiler metadata when available, language, edition/version, and license.
- Preserve grading/authenticity metadata only when it is supplied by a traceable source. Identify whose grading is being shown.
- Do not invent references, grades, chains, or attributions.
- Distinguish original Arabic, translation, transliteration, commentary, and editorial notes.
- If a dua has multiple reported wordings or sources, represent them separately rather than silently merging them.
- Use a documented editorial/review process for corrections and disputed metadata.
- Avoid presenting a paraphrase as a verbatim Hadith or prophetic supplication.

## 5. Prayer-time calculation requirements

If prayer times are included:

- Identify the calculation library/provider, version, and license.
- Record the selected calculation method and configurable parameters, including Fajr/Isha angles or intervals where relevant, high-latitude rules, Asr juristic method, and any supported offsets.
- Explain that calculated times can vary by method, location, date, and local authority.
- Make the chosen method visible and changeable where supported; do not silently change it.
- Handle daylight-saving transitions, time zones, date boundaries, high latitudes, invalid/missing location, and manual location entry.
- Separate raw calculated results from user adjustments and display settings.
- Do not imply that one calculation method is universally accepted.
- Validate with documented reference cases from the chosen provider and compare representative locations/dates with trusted local sources before release.
- Define notification behavior and avoid promising exact delivery times that the operating system cannot guarantee.

Acceptance tests should include multiple methods, time zones, daylight-saving boundaries where applicable, high-latitude cases, and permission-denied/manual-location paths.

## 6. Qibla and location

If Qibla or location-based features are included:

- Request location only when needed and explain the user benefit before the permission prompt.
- Request the least precise permission that can satisfy the feature.
- Provide manual city/location selection where feasible.
- Handle denied, restricted, unavailable, stale, and approximate location states gracefully.
- Explain that compass readings may be affected by device calibration and environmental interference.
- Avoid storing precise location history unless a clearly justified feature requires it.
- If location is sent to a server, disclose why, what is sent, retention, and controls before collection.
- Do not place coordinates, location history, or other sensitive values in analytics or diagnostic logs.

## 7. Hijri calendar and dates

If Hijri dates are included:

- Document whether dates are calculated, sourced from a published calendar, or based on an announced local observation.
- Explain that Hijri dates may differ by country, authority, or moon-sighting convention.
- Do not silently present a calculated date as a confirmed local moon sighting.
- Make any supported adjustment or calendar preference discoverable.
- Test Gregorian/Hijri boundaries, local time zones, and date transitions.

## 8. Localization and directionality

For every supported locale:

- Maintain reviewed translations for navigation, settings, permission explanations, errors, and accessibility labels.
- Handle pluralization, dates, numbers, and time formatting through platform localization APIs.
- Support Arabic right-to-left layout correctly, including mixed Arabic/Latin strings and punctuation.
- Validate Bengali and English text if they are selected supported locales.
- Keep religious translations traceable to their specific source; do not use ordinary UI translation workflows to rewrite sacred source text.
- Do not claim a locale is supported until key journeys and critical content have been reviewed.

## 9. AI-generated content (if introduced)

AI features are not assumed to be part of the MVP. If introduced:

- Clearly label generated summaries or explanations as AI-generated.
- Never fabricate Quran verses, Hadith text, citations, scholarly opinions, or religious rulings.
- Show source references that can be independently opened and checked.
- If reliable grounding is unavailable, say so rather than inventing an answer.
- Do not present the model as a scholar or mufti, and direct users to qualified scholars for consequential or disputed religious questions.
- Keep generated content distinct from verified source text in data structures and UI.
- Define moderation, privacy, retention, failure, and abuse controls before release.
- Do not send private user data or precise location to an AI provider without a documented purpose and appropriate disclosure.

## 10. Privacy, security, and accounts

- Establish a data inventory before adding analytics, accounts, cloud sync, or personalization.
- Collect only data needed for an explicit product purpose.
- Explain collection, use, sharing, retention, and deletion in understandable privacy documentation.
- Avoid requiring an account for local-only functionality unless necessary.
- Provide account deletion and data export where applicable and required.
- Use platform secure storage for secrets and sensitive local credentials.
- Encrypt network traffic; never embed privileged server secrets in app binaries.
- Avoid logging private content, authentication data, precise coordinates, or devotional activity.
- Keep analytics opt-in/controls and platform policy obligations under review before release.
- Define backup, retention, and deletion behavior for locally stored and server-side data.

## 11. Notifications and reminders

If reminders are included:

- Request notification permission in context and explain the benefit.
- Let users configure reminder types and quiet hours where appropriate.
- Respect device settings and permission revocation.
- Avoid guilt-inducing or manipulative wording.
- Make clear that exact delivery may depend on operating-system scheduling and battery policies.
- Keep notification content privacy-conscious and avoid sensitive previews by default where appropriate.

## 12. Content governance and correction workflow

Before publishing a content source:

1. Identify the publisher/provider and verify provenance.
2. Confirm license and permitted use.
3. Record source version, retrieval date, and any transformations.
4. Define who reviews language, references, and religious metadata.
5. Store source identifiers with the content.
6. Test imports and transformations against known fixtures.
7. Provide a way to report errors.
8. Define how corrections are reviewed, versioned, distributed, and communicated.

If an error is confirmed, correct it with traceable version history. Do not quietly replace a source or erase relevant correction history.

## 13. Testing and release gates

A release containing religious content or calculations must meet the relevant gates:

- [ ] Source provenance and license are documented.
- [ ] References and attribution are visible where appropriate.
- [ ] Content fixtures match the approved source/version.
- [ ] Prayer-time and Qibla edge cases are tested if those features exist.
- [ ] Arabic shaping, RTL, supported locales, and accessibility are reviewed.
- [ ] Permission-denied, offline, empty, and error states are tested.
- [ ] Privacy and security implications are reviewed.
- [ ] No generated text is presented as verified religious source material.
- [ ] Known limitations are disclosed clearly.
- [ ] A correction/reporting path exists for published content.

A feature that does not meet its relevant release gates should not be represented as verified or production-ready.

## 14. Open decisions

Track these explicitly rather than letting implementation choices become accidental product policy:

- Final product name and brand.
- MVP feature list.
- Supported languages and launch markets.
- Quran text and translation sources/licenses.
- Hadith and dua sources, metadata, and review process.
- Prayer-time calculation provider and supported methods.
- Qibla calculation approach and location-permission strategy.
- Hijri calendar convention, if applicable.
- Offline access requirements.
- Account, cloud sync, analytics, and AI scope.
- Content correction ownership and support channel.
