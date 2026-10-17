# Native Design Standards

**Status:** Baseline — last reviewed 2026-10-17. Brand, palette, and typography remain deliberately undecided. **Owns:** design intent, visual hierarchy, semantic tokens, typography and multilingual layout, components and interaction, accessibility, permission and privacy UX, and content presentation. **Defers:** content provenance and calculation rules → `PRODUCT_REQUIREMENTS.md`; module layout and token wiring → `ARCHITECTURE.md`.

## 1. Design intent

Create a calm, trustworthy, accessible Islamic product. Make daily practices and useful information easy to reach without overwhelming the user or implying the app replaces qualified religious guidance.

The name, logo, palette, typography, and final visual identity are not decided here. Do not invent a permanent brand system before those decisions are made.

## 2. Native first

**iOS** — Follow Apple's Human Interface Guidelines. Use SwiftUI and native controls by default; UIKit only when a concrete native requirement fits it better. Respect safe areas, system navigation, Dynamic Type, semantic colors, system appearance, and platform interaction patterns. Support VoiceOver. Reference: https://developer.apple.com/design/human-interface-guidelines/

**Android** — Follow Material Design while preserving the product's own identity. Use Jetpack Compose and native platform patterns by default. Respect system insets, system bars, back navigation, font scaling, and Android accessibility semantics. Support TalkBack. Reference: https://m3.material.io/

Do not force the two apps to look pixel-identical: shared brand principles should yield a coherent product, while layout, controls, navigation, and motion may follow platform conventions. The design system is per app (`ARCHITECTURE.md` §2.2).

## 3. Visual hierarchy and tone

- Prefer clear hierarchy, legible text, restrained decoration, and consistent spacing; make the primary task on a screen obvious.
- Use color to communicate meaning consistently, never as the only carrier of meaning.
- Avoid clutter, streak pressure, shame-based messaging, and manipulative engagement patterns.
- Never use sacred text as decorative filler or in layouts that may crop, distort, or truncate it.
- Treat Arabic text as content needing correct shaping, directionality, and typography — not as a decorative glyph.
- Distinguish Quran text, translations, transliteration, explanatory notes, and user-generated text, visually and semantically.

## 4. Semantic design tokens

Express design intent as **semantic** tokens, not raw values bound into components. Define intent here; map it to native resources per app (`ARCHITECTURE.md` §2.2). Illustrative categories (not a finalized contract):

- `color.background`, `color.surface`, `color.primary`, `color.secondary`, `color.error`
- `text.primary`, `text.secondary`, `text.onPrimary`
- `spacing.compact`, `spacing.standard`, `spacing.comfortable`
- `shape.control`, `shape.card`
- `motion.standard`, `motion.reduced`

Name tokens by role and intent, not appearance or a component. Support light and dark appearance where appropriate. Do not assume a palette before brand work is approved; current values are placeholders. When both apps must agree on the same values, keep the source definitions platform-neutral under `shared/design-tokens/` and derive native resources from them.

## 5. Typography and multilingual content

- Respect system text-size settings; avoid fixed-height text containers that clip content.
- Choose fonts with suitable Arabic glyph coverage and shaping.
- Test every supported locale (e.g. Arabic, Bengali, English if in scope).
- Handle right-to-left layout explicitly; do not merely right-align text.
- Keep Arabic script, translation, transliteration, and references distinguishable.
- Avoid line breaks or truncation that change the meaning of religious content.
- Verify diacritics, ligatures, punctuation, and reference formatting against source fixtures.

Locale support and translation traceability: `PRODUCT_REQUIREMENTS.md` §8.

## 6. Components and interaction

- Prefer standard platform components for familiar interactions; create reusable components only after a real repeated pattern exists.
- Provide visible, accessible loading, success, disabled, empty, error, and permission-denied states where relevant.
- Ensure interactive controls have usable target sizes and clear labels; never rely on color, position, or an icon alone to convey an action.
- Avoid custom gestures that conflict with system navigation.
- Respect reduced-motion preferences; avoid motion that interferes with reading or worship.

## 7. Accessibility and inclusion

Release gate: `PRODUCT_REQUIREMENTS.md` §13.

- Support screen readers, text scaling, adequate contrast, and keyboard/switch navigation where relevant.
- Provide meaningful accessibility labels and state descriptions; do not announce decorative elements as useful content.
- Make forms understandable and errors actionable; respect language and locale preferences.
- Make location-based features usable when permission is denied, with an understandable manual alternative where feasible.
- Test with real accessibility tools on both platforms.

## 8. Permission and privacy UX

Scope: *when and how* the UI asks for and explains access. Data policy: `PRODUCT_REQUIREMENTS.md` §6, §10, §11; secret storage: `ARCHITECTURE.md` §7.

- Explain why a location or notification permission is requested at the point it becomes useful; request it in context, not automatically at first launch.
- Offer a usable experience when optional permissions are denied.
- Make settings and controls easy to find.
- Avoid exposing private content in notification previews by default where that could surprise users.
- Do not use religious habits or devotional activity for manipulative prompts.

## 9. Content integrity in the UI

Provenance, licensing, review, and correction rules: `PRODUCT_REQUIREMENTS.md` §3–§7, §12. Here: how that integrity is shown.

- Display source and reference information near religious content when appropriate.
- Visually distinguish original text from translation, transliteration, commentary, and generated content.
- Never present AI-generated religious explanations as Quran, Hadith, or established scholarly rulings.
- Show the selected prayer calculation method and relevant settings understandably.
- Provide a clear correction/reporting path for content errors.

## 10. Design review checklist

Before shipping a screen:

- [ ] Native navigation and platform conventions are respected.
- [ ] Content hierarchy is clear and the main action is understandable.
- [ ] Dynamic text, Arabic shaping, RTL, and supported locales are checked.
- [ ] Screen reader labels, contrast, target sizes, and reduced motion are considered.
- [ ] Loading, empty, error, and permission-denied states exist where relevant.
- [ ] Sacred text is complete, correctly attributed, and not treated as decoration.
- [ ] Permission prompts explain a real user benefit.
- [ ] Light/dark appearance and smaller/larger screens are tested where applicable.
