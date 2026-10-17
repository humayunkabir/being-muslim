#!/usr/bin/env python3
"""Build the app's bundled city catalog from the GeoNames `cities15000` dump.

The catalog is generated, not hand-edited. This script is the reproducible
source of the bundled asset and the record of what was kept and dropped; the
data's publisher, license, and required attribution live in
`docs/CITY_CATALOG.md`.

Input — the GeoNames `cities15000` dump (CC BY 4.0):

    curl -O https://download.geonames.org/export/dump/cities15000.zip
    unzip cities15000.zip

Output — a tab-separated asset, sorted by population descending so that the
first search match is the most significant place:

    # id  name  bengali_name  latitude  longitude  country_code  timezone
    1337176  Tongi  টঙ্গী  23.89154  90.40232  BD  Asia/Dhaka

`name` is GeoNames' ASCII name. `bengali_name` is the shortest Bengali-script
alternate name GeoNames records for the place, or empty when it has none.
Population is used only for ordering and is not written to the asset.

Usage:

    python3 tools/build-city-catalog.py \\
        --source /path/to/cities15000.txt \\
        --out apps/android/app/src/main/assets/city_catalog.tsv
"""

from __future__ import annotations

import argparse
import re
import sys
from pathlib import Path

# Unicode Bengali block. GeoNames alternate names are mixed-script, so a name
# is Bengali if it contains any character in this block.
BENGALI = re.compile(r"[\u0980-\u09FF]")

COLUMNS = (
    "id",
    "name",
    "bengali_name",
    "latitude",
    "longitude",
    "country_code",
    "timezone",
)

#: 0-based indices into a GeoNames `cities15000` row.
ID, NAME, ASCII_NAME, ALTERNATES = 0, 1, 2, 3
LATITUDE, LONGITUDE, COUNTRY_CODE = 4, 5, 8
POPULATION, TIMEZONE = 14, 17


def bengali_name(alternates: str) -> str:
    """The shortest Bengali-script alternate name, or "" when there is none."""
    bengali = (a.strip() for a in alternates.split(",") if BENGALI.search(a))
    return min(bengali, key=len, default="")


def build(source: Path) -> tuple[list[list[str]], dict[str, int]]:
    rows: list[list[str]] = []
    stats = {"total": 0, "kept": 0, "dropped_no_name": 0,
             "dropped_no_timezone": 0, "with_bengali": 0}

    with source.open(encoding="utf-8") as handle:
        for line in handle:
            columns = line.rstrip("\n").split("\t")
            if len(columns) <= TIMEZONE:
                continue
            stats["total"] += 1

            name = columns[ASCII_NAME].strip() or columns[NAME].strip()
            timezone = columns[TIMEZONE].strip()
            if not name:
                stats["dropped_no_name"] += 1
                continue
            if not timezone:
                stats["dropped_no_timezone"] += 1
                continue

            bengali = bengali_name(columns[ALTERNATES])
            if bengali:
                stats["with_bengali"] += 1

            population = columns[POPULATION].strip()
            rows.append([
                population,
                columns[ID].strip(),
                name,
                bengali,
                columns[LATITUDE].strip(),
                columns[LONGITUDE].strip(),
                columns[COUNTRY_CODE].strip(),
                timezone,
            ])

    rows.sort(key=lambda row: int(row[0]) if row[0].isdigit() else 0, reverse=True)
    stats["kept"] = len(rows)
    return [row[1:] for row in rows], stats


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--source", required=True, type=Path,
                        help="path to the GeoNames cities15000.txt dump")
    parser.add_argument("--out", required=True, type=Path,
                        help="path to write the generated catalog asset")
    args = parser.parse_args()

    if not args.source.is_file():
        print(f"source not found: {args.source}", file=sys.stderr)
        print("download cities15000.zip from "
              "https://download.geonames.org/export/dump/ and unzip it",
              file=sys.stderr)
        return 1

    rows, stats = build(args.source)
    args.out.parent.mkdir(parents=True, exist_ok=True)
    with args.out.open("w", encoding="utf-8", newline="\n") as handle:
        handle.write("# " + "\t".join(COLUMNS) + "\n")
        for row in rows:
            handle.write("\t".join(row) + "\n")

    size = args.out.stat().st_size
    print(f"wrote {args.out} ({size:,} bytes)")
    print(f"  rows read        {stats['total']:,}")
    print(f"  cities kept      {stats['kept']:,}")
    print(f"  with Bengali     {stats['with_bengali']:,}")
    print(f"  dropped (name)   {stats['dropped_no_name']:,}")
    print(f"  dropped (no tz)  {stats['dropped_no_timezone']:,}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
