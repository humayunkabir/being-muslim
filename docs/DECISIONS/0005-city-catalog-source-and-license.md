# 0005 — City catalog: GeoNames `cities15000` under CC BY 4.0

- **Status:** Accepted
- **Date:** 2026-10-18

## Context

Every location-dependent feature — prayer times and qibla — needs a place with
coordinates and an IANA timezone, and the MVP requires this to work **offline,
with no permission and no network** (`docs/DECISIONS/0003`). A bundled catalog is
therefore required, and its license must permit redistribution in a shipped app;
the city catalog was recorded as an explicit open dependency
(`docs/PRODUCT_REQUIREMENTS.md` §14) precisely because getting the license wrong
is expensive to reverse.

The launch markets are Bengali- and English-speaking, so a Bengali name where one
exists is valuable but not always available from a rights-clear source.

## Decision

- Bundle the **GeoNames `cities15000`** dataset (`docs/CITY_CATALOG.md`), used
  under **CC BY 4.0**, which permits redistribution, including commercially, with
  attribution.
- Ship it as a **generated, committed asset** — `city_catalog.tsv` — produced by
  `tools/build-city-catalog.py`, not as a hand-maintained list. The generator
  records exactly what was kept and dropped.
- `bengali_name` is the **shortest Bengali-script alternate name** GeoNames
  records for a place, so Bengali lookup works where the source supports it. A
  city without a Bengali name is found by its English name only.
- Rows are **sorted by population descending** so the first search match is the
  most significant place; population is not stored.
- **Attribution is required to ship**: the app must display
  "City data from GeoNames (https://www.geonames.org/), licensed under CC BY 4.0"
  in an About/data-sources surface before release; the string and surface are
  added with the city picker (issue #4).
- Cities are **not religious content** and are not held to
  `docs/DECISIONS/0004`'s review-owner bar, but their **license to redistribute**
  is held to the same standard.

## Alternatives considered

- **A curated launch-market list** hand-authored for Bangladesh and major
  English-market cities. Rejected as the primary catalog: it gives up global
  coverage, and a rights-clear external catalog was available, so the fallback
  it is defined for (`docs/PRODUCT_REQUIREMENTS.md` §14) was not needed.
- **GeoNames alternate names in full**, to widen Bengali coverage. Rejected for
  size: the alternate-names file is far larger than the catalog itself and would
  bundle every language for every place. Coverage is partial and disclosed.
- **A different gazetteer**, such as SimpleMaps World Cities. Rejected because
  its freely licensed tier does not carry the required IANA timezone, which
  GeoNames does.
- **Downloading the catalog at runtime.** Rejected: it violates the offline core
  and would make a core feature depend on the network.

## Consequences

- The bundled asset is ~1.9 MB (34,166 cities); it is compressed inside the APK
  but must be parsed off the main thread. **Load behaviour on a low-end device is
  not yet verified** and remains a release gate (`docs/CITY_CATALOG.md`).
- Bengali lookup coverage is limited by what GeoNames records (3,739 cities, 29
  in Bangladesh) and is disclosed rather than hidden.
- Localized country display names are not bundled; the catalog carries ISO
  codes, and a picker that needs country names must supply them separately.
- Regenerating the catalog is a data build step, not a code change: it re-runs
  `tools/build-city-catalog.py` against a fresh dump, and the new asset's
  checksum and retrieval date are re-recorded.
