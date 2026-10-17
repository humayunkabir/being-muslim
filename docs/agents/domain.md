# Domain Docs

How the engineering skills should consume this repo's domain documentation when exploring the codebase.

## Before exploring, read these

- **`GLOSSARY.md`** at the repo root.
- **`docs/DECISIONS/`**: read ADRs that touch the area you're about to work in.

If either doesn't exist yet, **proceed silently**. Don't flag the absence; don't suggest creating them upfront. The `/domain-modeling` skill (reached via `/grill-with-docs` and `/improve-codebase-architecture`) creates them lazily when terms or decisions actually get resolved.

## File structure

Single-context repo:

```
/
├── GLOSSARY.md
├── docs/DECISIONS/
│   └── 0001-repository-layout-and-identifiers.md
└── apps/
    ├── ios/
    └── android/
```

ADRs use this repo's existing convention, not upstream's `docs/adr/`: a
`<NNNN>-<slug>.md` file in `docs/DECISIONS/`. `docs/ARCHITECTURE.md` §9 requires
**status**, **context**, **decision**, **alternatives considered** and
**consequences**; the existing entry also records a **date**. Follow its shape.

This repo is single-context by decision: iOS and Android are two implementations of
one product that deliberately share terminology, not two domains. There is no
`GLOSSARY-MAP.md` and no per-app `GLOSSARY.md`; don't introduce them.

## Use the glossary's vocabulary

When your output names a domain concept (in an issue title, a refactor proposal, a hypothesis, a test name), use the term as defined in `GLOSSARY.md`. Don't drift to synonyms the glossary explicitly avoids.

If the concept you need isn't in the glossary yet, that's a signal: either you're inventing language the project doesn't use (reconsider) or there's a real gap (note it for `/domain-modeling`).

## Flag ADR conflicts

If your output contradicts an existing ADR, surface it explicitly rather than silently overriding:

> _Contradicts `docs/DECISIONS/0001` (Monorepo layout and app identifiers), but worth reopening because…_
