# City catalog

The bundled, offline city list that prayer-time and qibla calculations are based
on (`GLOSSARY.md`, *City catalog*). This file records its provenance, the exact
data transformation, its coverage limits, and what is still unverified. The
catalog is **generated, never hand-edited**: the generator is
`tools/build-city-catalog.py` and the committed output is the source of truth for
what ships.

## What ships

| | |
| --- | --- |
| Asset | `apps/android/app/src/main/assets/city_catalog.tsv` |
| Size | 1,970,662 bytes (34,166 cities) |
| Format | Tab-separated text, UTF-8, one city per line |
| Loader | `BundledCityCatalog.load(assets)` → `CityCatalog.parse` |
| Runtime | Read from the APK: **no network, no permission, no account** |

Format (a `#` comment line is ignored by the parser):

```text
# id  name  bengali_name  latitude  longitude  country_code  timezone
1185241  Dhaka  ঢাকা  23.7104  90.40744  BD  Asia/Dhaka
1835848  Seoul    37.566  126.9784  KR  Asia/Seoul
```

- `id` — the stable GeoNames identifier, persisted when a user picks a city.
- `name` — the city's English (ASCII) name.
- `bengali_name` — the Bengali name where the source records one, else empty.
- `latitude`, `longitude` — decimal degrees, north and east positive.
- `country_code` — ISO 3166-1 alpha-2.
- `timezone` — the IANA time-zone identifier; daylight saving is derived from it.

## Provenance

| | |
| --- | --- |
| Publisher | GeoNames — <https://www.geonames.org/> |
| Dataset | `cities15000` (places with population > 15000, or seats of administration) |
| Source file | `https://download.geonames.org/export/dump/cities15000.zip` |
| License | Creative Commons Attribution 4.0 (CC BY 4.0) — <https://creativecommons.org/licenses/by/4.0/> |
| Redistribution | Permitted, including commercial use, **with attribution** |
| Retrieved | 2026-10-18 |
| Edition | `cities15000` dump retrieved 2026-10-18; SHA-256 of `cities15000.zip` = `3068649d544bacddb18f7d80ebceb20a6970c0948c9a3553c830047f72ae6618` |

The license permits redistribution in an app. GeoNames states this data is
provided "as is" without warranty of accuracy, so the app never presents a
catalog entry as verified truth, only as the city the user chose.

**Required attribution.** CC BY 4.0 requires visible attribution; before release
the app must display, in an About/data-sources surface:

> City data from GeoNames (https://www.geonames.org/), licensed under CC BY 4.0.

Showing it in the UI is a release gate (`docs/PRODUCT_REQUIREMENTS.md` §13). The
string and its surface are added with the city picker (issue #4); it is not yet
in the app.

## How it is generated

```sh
cd /tmp
curl -O https://download.geonames.org/export/dump/cities15000.zip
unzip cities15000.zip
cd -
python3 tools/build-city-catalog.py \
    --source /tmp/cities15000.txt \
    --out apps/android/app/src/main/assets/city_catalog.tsv
```

The generator keeps every row with a name and a non-empty timezone (34,166 read,
34,166 kept, 0 dropped), takes the English name from GeoNames' ASCII name, takes
`bengali_name` as the **shortest Bengali-script alternate name** GeoNames records
for the place, and **sorts by population descending** so that the first search
match is the most significant place. Population is used only for ordering and is
not written to the asset.

## Coverage and limitations

- **Global.** Countries appear as ISO alpha-2 codes; `CityCatalog.countries()`
  lists them and `citiesIn(code)` lists a country's cities.
- **Bengali names are partial by source.** GeoNames records a Bengali-script
  alternate name for 3,739 of the 34,166 cities (29 of 139 in Bangladesh).
  Lookup works in Bengali "where the catalog supports it" — a city without a
  Bengali name is found by its English name only.
- **Country display names are not bundled.** Only codes and cities ship; localized
  country names are a picker concern and are deferred. The MVP city picker
  searches by city name in English and Bengali.
- City entries that are administrative seats rather than settlements (thanas,
  districts) are present, as GeoNames supplies them.

## Unverified on this change

- **Load behaviour and memory on a low-end device is not yet verified.** The
  asset is ~1.9 MB and parsing is proportional to its size, so it must be loaded
  off the main thread (the loader documents this). The
  `PRODUCT_REQUIREMENTS.md` §13 release gate for a build containing calculations
  requires this to be measured on a real low-end device before release; it has
  not been.
