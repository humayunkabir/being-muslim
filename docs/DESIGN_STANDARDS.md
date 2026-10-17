# Native Design Standards

## 1. Design intent

Create a calm, trustworthy, accessible Islamic product. The interface should make daily practices and useful information easy to reach without overwhelming the user or implying that the app replaces qualified religious guidance.

The product name, logo, color palette, typography, and final visual identity are not decided by this document. Do not invent a permanent brand system before those decisions are made.

## 2. Native first

### iOS

- Follow Apple's Human Interface Guidelines.
- Use SwiftUI and native controls by default.
- Respect safe areas, system navigation, Dynamic Type, semantic colors, system appearance, and platform interaction patterns.
- Use UIKit only when it is a better fit for a concrete native requirement.
- Support VoiceOver and the accessibility APIs.

Reference: https://developer.apple.com/design/human-interface-guidelines/

### Android

- Follow Material Design guidance while preserving the product's own identity.
- Use Jetpack Compose and native platform patterns by default.
- Respect system insets, system bars, back navigation, font scaling, and Android accessibility semantics.
- Support TalkBack and relevant Android accessibility settings.

Reference: https://m3.material.io/

Do not force the two apps to look pixel-identical. Shared brand principles should produce a coherent product, while layout, controls, navigation, and motion may follow platform conventions.

## 3. Visual hierarchy and tone

- Prefer clear hierarchy, legible text, restrained decoration, and consistent spacing.
- Make the primary task on a screen obvious.
- Use color to communicate meaning consistently, not as the only way to convey it.
- Avoid clutter, unnecessary streak pressure, shame-based messaging, or manipulative engagement patterns.
- Do not use sacred text as decorative filler or place it in layouts that may crop, distort, or truncate it.
- Treat Arabic text as content requiring correct shaping, directionality, and typography—not as a decorative glyph.
- Distinguish Quran text, translations, transliteration, explanatory notes, and user-generated text visually and semantically.

## 4. Semantic design tokens

If shared tokens are useful, define semantic intent rather than binding every component to a raw color or pixel value. Examples of token categories:

- `color.background`, `color.surface`, `color.primary`, `color.secondary`, `color.error`
- `text.primary`, `text.secondary`, `text.onPrimary`
- `spacing.compact`, `spacing.standard`, `spacing.comfortable`
- `shape.control`, `shape.card`
- `motion.standard`, `motion.reduced`

These names are illustrative, not a finalized token contract. Map them independently to native resources. Support light and dark appearance where appropriate, and do not assume a palette before brand work is approved.

## 5. Typography and multilingual content

- Respect system text-size settings and avoid fixed-height text containers that clip content.
- Choose fonts with suitable Arabic glyph coverage and shaping behavior.
- Test Arabic, Bengali, and English if those languages are in the product's supported scope.
- Handle right-to-left layout direction explicitly; do not merely right-align text.
- Keep Arabic script, translation, transliteration, and references distinguishable.
- Avoid line breaks or truncation that change the meaning of religious content.
- Verify diacritics, ligatures, punctuation, and reference formatting with source fixtures.

## 6. Components and interaction

- Prefer standard platform components for familiar interactions.
- Create reusable components only after a real repeated pattern exists.
- Provide visible and accessible loading, success, disabled, empty, error, and permission-denied states where relevant.
- Ensure interactive controls have usable target sizes and clear labels.
- Do not rely on color, position, or icons alone to communicate an action.
- Avoid custom gestures that conflict with system navigation.
- Respect reduced-motion preferences and avoid motion that interferes with reading or worship.

## 7. Accessibility and inclusion

- Support screen readers, text scaling, adequate contrast, and keyboard/switch navigation where relevant.
- Provide meaningful accessibility labels and state descriptions.
- Do not announce decorative elements as useful content.
- Make forms understandable and errors actionable.
- Respect language and locale preferences.
- Make location-based features usable when permission is denied, with an understandable manual alternative where feasible.
- Test with real accessibility tools on both platforms.

## 8. Privacy-sensitive UX

- Explain why location or notification permission is requested at the point it becomes useful.
- Request permissions in context, not automatically at first launch without a clear need.
- Offer a usable experience when optional permissions are denied.
- Make settings and controls easy to find.
- Avoid exposing private content in notification previews by default where that could surprise users.
- Do not use religious habits or personal devotional activity for manipulative prompts.

## 9. Content integrity in the UI

- Display source and reference information near religious content when appropriate.
- Visually distinguish original text from translation, transliteration, commentary, and generated content.
- Never present AI-generated religious explanations as Quran, Hadith, or established scholarly rulings.
- Show the selected prayer calculation method and relevant settings in an understandable way.
- Provide a clear correction/reporting path for content errors.
- See `PRODUCT_REQUIREMENTS.md` for required content provenance and review rules.

## 10. Design review checklist

Before shipping a screen, review:

- [ ] Native navigation and platform conventions are respected.
- [ ] Content hierarchy is clear and the main action is understandable.
- [ ] Dynamic text, Arabic shaping, RTL, and supported locales are checked.
- [ ] Screen reader labels, contrast, target sizes, and reduced motion are considered.
- [ ] Loading, empty, error, and permission-denied states exist where relevant.
- [ ] Sacred text is complete, correctly attributed, and not treated as decoration.
- [ ] Permission prompts explain a real user benefit.
- [ ] Light/dark appearance and smaller/larger screens are tested where applicable.
