#!/usr/bin/env python3
"""Candidates from USDA SR Legacy (April 2018), not user-facing verified foods.

SR Legacy is a separate historic dataset, NOT 2026 Foundation Foods.
No candidate is automatically added to Kotlin, a retailer SKU or a verified recipe.
"""
import argparse
import json
import re
from pathlib import Path
from zipfile import ZipFile

from fdc_candidates import _rows, NUTRIENT_IDS, CALORIE_IDS

SOURCE_URL = "https://fdc.nal.usda.gov/fdc-datasets/FoodData_Central_sr_legacy_food_csv_2018-04.zip"
RELEASE = "USDA FoodData Central SR Legacy April 2018"


def find_candidates(archive_path: Path, word: str, limit: int = 25, raw_only: bool = False):
    if len(word.strip()) < 3 or not 1 <= limit <= 50:
        raise ValueError("Specify a word of 3+ characters and limit between 1 and 50")
    token = re.compile(r"(?<![a-z])" + re.escape(word.strip().casefold()) +
                       r"(?:s|es)?(?![a-z])")
    with ZipFile(archive_path) as archive:
        foods = []
        for row in _rows(archive, "food.csv"):
            description = row.get("description", "").strip()
            if token.search(description.casefold()):
                if raw_only:
                    lower=description.casefold()
                    # The selected FDC entry must actually describe raw edible
                    # food; processed snacks, juices, flour and cooked portions are
                    # not interchangeable with raw recipe ingredients.
                    if (not re.search(r"\\b(?:raw|uncooked)\\b",lower) or
                        re.search(r"\\b(?:cooked|boiled|roasted|dried|flour|chips|juice|canned|fried|sauce)\\b",lower)):
                        continue
                fdc = row.get("fdc_id", "")
                if fdc.isdigit() and description:
                    foods.append((fdc, description))
                if not raw_only and len(foods) >= limit * 8:
                    break
        wanted = {fdc for fdc, _ in foods}
        values = {fdc: {} for fdc in wanted}
        nutrient_ids = set(NUTRIENT_IDS.values()) | set(CALORIE_IDS)
        for row in _rows(archive, "food_nutrient.csv"):
            fdc = row.get("fdc_id")
            if fdc not in wanted:
                continue
            try:
                nutrient_id = int(row["nutrient_id"])
                amount = float(row["amount"])
            except (ValueError, TypeError, KeyError):
                continue
            if nutrient_id in nutrient_ids and 0 <= amount <= 10000:
                values[fdc][nutrient_id] = amount
    candidates = []
    for fdc, description in foods:
        nutrients = values[fdc]
        if any(key not in nutrients for key in NUTRIENT_IDS.values()):
            continue
        kcal = next((nutrients[key] for key in CALORIE_IDS if key in nutrients), None)
        if kcal is None:
            continue
        candidates.append({
            "fdc_id": int(fdc),
            "description": description,
            "nutrition_per_100g": {
                "energy_kcal": kcal,
                "protein_g": nutrients[1003],
                "fat_g": nutrients[1004],
                "carbs_g": nutrients[1005]
            },
            "source": RELEASE,
            "archive_source": SOURCE_URL,
            "source_url": f"https://fdc.nal.usda.gov/food-details/{fdc}/nutrients",
            "review_status": "CANDIDATE_NOT_VERIFIED",
            "imported_as_app_food": False,
            "note": "Confirm raw/dry/drained state and Russian SKU, do not assume equivalence."
        })
        if len(candidates) >= limit:
            break
    return candidates


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("archive", type=Path)
    parser.add_argument("--contains", required=True)
    parser.add_argument("--limit", type=int, default=25)
    parser.add_argument("--raw-only", action="store_true",
                        help="Keep only entries explicitly labeled raw or uncooked")
    parser.add_argument("--output", type=Path, required=True)
    args = parser.parse_args()
    data = find_candidates(args.archive, args.contains, args.limit, args.raw_only)
    args.output.parent.mkdir(parents=True, exist_ok=True)
    args.output.write_text(json.dumps(data, ensure_ascii=False, indent=2) + "\n",
                           encoding="utf-8")
    print(f"{args.contains}: {len(data)} SR Legacy candidates only, NOT VERIFIED")


if __name__ == "__main__":
    main()
