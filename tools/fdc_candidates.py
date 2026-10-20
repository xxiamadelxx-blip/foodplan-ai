#!/usr/bin/env python3
"""Extract review candidates from the OFFICIAL USDA FDC zip, never auto-verify foods.

The USDA description may not identify Russian product preparation (dry/raw/cooked).
Human review and source cross-checks are mandatory before editing app nutrient fields.
No paid API, download the CSV archive from https://fdc.nal.usda.gov/download-datasets/.
"""
import argparse
import csv
import io
import json
import re
import zipfile
from pathlib import Path

FDC_RELEASE = "Foundation Foods April 2026"
FDC_ARCHIVE_SOURCE = (
    "https://fdc.nal.usda.gov/fdc-datasets/"
    "FoodData_Central_foundation_food_csv_2026-04-30.zip"
)
# FoodData Central nutrient IDs; energy accepts standard and two modern variants.
NUTRIENT_IDS = {"protein_g": 1003, "fat_g": 1004, "carbs_g": 1005}
CALORIE_IDS = (2047, 2048, 1008)
MAX_ARCHIVE_MEMBER = 100_000_000
MAX_CANDIDATES = 50


def _rows(archive: zipfile.ZipFile, filename: str):
    matches = [
        member for member in archive.infolist()
        if Path(member.filename).name.casefold() == filename.casefold()
        and not member.is_dir()
    ]
    if len(matches) != 1:
        raise ValueError(f"Expected one {filename}, found {len(matches)}")
    member = matches[0]
    if member.file_size > MAX_ARCHIVE_MEMBER:
        raise ValueError(f"Archive table {filename} exceeds safe bound")
    with archive.open(member) as payload:
        yield from csv.DictReader(io.TextIOWrapper(payload, encoding="utf-8-sig"))


def build_candidates(archive_path: Path, contains: str, limit: int = 20):
    if len(contains.strip()) < 3:
        raise ValueError("Use a descriptive ingredient term with at least 3 characters")
    if not 1 <= limit <= MAX_CANDIDATES:
        raise ValueError("Candidate limit outside allowed range")
    with zipfile.ZipFile(archive_path) as archive:
        # Match whole words; naive substring search for "rice" matches "PRICE".
        token = re.compile(r"(?<![a-z])" + re.escape(contains.casefold()) + r"(?![a-z])")
        foods = []
        for row in _rows(archive, "food.csv"):
            name = row.get("description", "").strip()
            if token.search(name.casefold()):
                fdc_id = row.get("fdc_id", "").strip()
                if fdc_id.isdigit() and name:
                    foods.append({"fdc_id": int(fdc_id), "description": name})
        food_ids = {str(f["fdc_id"]) for f in foods}
        nutrients = {fdc: {} for fdc in food_ids}
        wanted = set(NUTRIENT_IDS.values()) | set(CALORIE_IDS)
        for row in _rows(archive, "food_nutrient.csv"):
            food_id = row.get("fdc_id", "")
            if food_id not in food_ids:
                continue
            try:
                nutrient_id = int(row.get("nutrient_id", ""))
                amount = float(row.get("amount", ""))
            except (ValueError, TypeError):
                continue
            if nutrient_id in wanted and 0 <= amount < 10000:
                nutrients[food_id][nutrient_id] = amount
        # Only complete macro/energy records proceed to the review queue.
        # Still NOT VERIFIED as suitable for an app food.
        foods = [
            food for food in foods
            if all(nutrient_id in nutrients[str(food["fdc_id"])]
                for nutrient_id in NUTRIENT_IDS.values())
            and any(nutrient_id in nutrients[str(food["fdc_id"])] for nutrient_id in CALORIE_IDS)
        ][:limit]
    for food in foods:
        mapped = nutrients[str(food["fdc_id"])]
        macros = {
            key: mapped.get(nutrient_id)
            for key, nutrient_id in NUTRIENT_IDS.items()
        }
        energy = next((mapped[i] for i in CALORIE_IDS if i in mapped), None)
        food.update({
            "nutrition_per_100g": {"energy_kcal": energy, **macros},
            "provenance": {
                "dataset": FDC_RELEASE,
                "archive": FDC_ARCHIVE_SOURCE,
                "food_url": f"https://fdc.nal.usda.gov/food-details/{food['fdc_id']}/nutrients",
            },
            "review_status": "CANDIDATE_NOT_VERIFIED",
            "imported_as_app_food": False,
            "notes": "Match raw/dry/cooked food state, edible portion, macros and Russian labels manually."
        })
    return foods


def main():
    p = argparse.ArgumentParser(description=__doc__)
    p.add_argument("archive", type=Path)
    p.add_argument("--contains", required=True, help="English ingredient search phrase")
    p.add_argument("--limit", type=int, default=20)
    p.add_argument("--output", type=Path, required=True)
    args = p.parse_args()
    candidates = build_candidates(args.archive, args.contains, args.limit)
    args.output.parent.mkdir(parents=True, exist_ok=True)
    args.output.write_text(json.dumps(candidates, ensure_ascii=False, indent=2) + "\n",
                           encoding="utf-8")
    print(f"Found {len(candidates)} UNSOURCED-IN-APP candidates for human review")
    # No automated assignment of nutrients or allergen labels to recipes.


if __name__ == "__main__":
    main()
