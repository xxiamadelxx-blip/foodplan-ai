#!/usr/bin/env python3
"""Shortlist source food identities using an ORIGINAL USDA SR Legacy archive.

The shortlist is *not* source acceptance. An editor must check preparation,
cut, fat content, edible mass, and exact description before pinning records.
"""
import argparse
import json
import math
import re
from pathlib import Path
from zipfile import ZipFile

from fdc_candidates import _rows

# Names intentionally include physical states / product types.
# "berries", "cottage_cheese" and generic meat entries require extra human
# review; mixing unlike foods into one catalog entry is not allowed.
TARGET_PATTERNS = {
    "pasta": r"^Pasta,.*\b(?:dry|uncooked)\b",
    "potato": r"^Potatoes?,.*\braw\b",
    "oats": r"^(?:Oats\b|Cereals, oats,.*\bdry\b)",
    "milk": r"^Milk, whole, 3\.25% milkfat\b",
    "berries": r"^(?:Strawberries|Blueberries|Raspberries|Blackberries),.*\bfrozen\b",
    "turkey": r"^Turkey,.*\braw\b",
    "beef": r"^Beef,.*\braw\b",
    "pork": r"^Pork,.*\braw\b",
    "oil": r"^Oil, sunflower\b",
    "cottage_cheese": r"^Cheese, cottage\b",
    "yogurt": r"^Yogurt, plain\b",
    "cheese": r"^Cheese, (?:gouda|edam)\b",
}
NUTRIENT_IDS = (1003, 1004, 1005)
CALORIE_IDS = (2047, 2048, 1008)
STATUS = "CANDIDATE_NOT_VERIFIED"


def select_foods(foods, specs=TARGET_PATTERNS, limit=12):
    if limit < 1 or limit > 50:
        raise ValueError("Review limit must be 1 to 50")
    compiled = {key: re.compile(pattern, flags=re.IGNORECASE) for key, pattern in specs.items()}
    results = {key: [] for key in specs}
    for row in foods:
        desc = row.get("description", "").strip()
        fdc = row.get("fdc_id", "")
        if not fdc.isdigit():
            continue
        for key, pattern in compiled.items():
            if len(results[key]) < limit and pattern.search(desc):
                results[key].append({"fdc_id": int(fdc), "description": desc})
    return results


def build_review(archive_path: Path, limit=12):
    with ZipFile(archive_path) as archive:
        selected = select_foods(_rows(archive, "food.csv"), limit=limit)
        ids = {str(row["fdc_id"]) for rows in selected.values() for row in rows}
        values = {fdc: {} for fdc in ids}
        for row in _rows(archive, "food_nutrient.csv"):
            fdc = row.get("fdc_id")
            if fdc not in values:
                continue
            try:
                n = int(row["nutrient_id"])
                amount = float(row["amount"])
            except (ValueError, KeyError, TypeError):
                continue
            if n in (*NUTRIENT_IDS, *CALORIE_IDS) and math.isfinite(amount) and 0 <= amount <= 10000:
                values[fdc][n] = amount
    result = {}
    for key, foods in selected.items():
        candidates = []
        for food in foods:
            fdc = str(food["fdc_id"])
            nutrients = values[fdc]
            kcal = next((nutrients[n] for n in CALORIE_IDS if n in nutrients), None)
            if kcal is None or any(n not in nutrients for n in NUTRIENT_IDS):
                continue
            candidates.append({**food,
                "per_100g": {
                    "energy_kcal": kcal, "protein_g": nutrients[1003],
                    "fat_g": nutrients[1004], "carbs_g": nutrients[1005]
                },
                "status": STATUS,
                "raw_food_or_russian_sku_equivalence_verified": False})
        result[key] = candidates
    return result


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("archive", type=Path)
    parser.add_argument("--output", type=Path, default=Path("m2_sr_legacy_candidates/targeted-review.json"))
    args = parser.parse_args()
    report = build_review(args.archive)
    args.output.parent.mkdir(parents=True, exist_ok=True)
    args.output.write_text(json.dumps({
        "dataset": "USDA SR Legacy April 2018",
        "status": STATUS,
        "recipe_verification_count": 0,
        "groups": report
    }, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    for key, candidates in report.items():
        print("M2 TARGET "+key+" = "+json.dumps(candidates, ensure_ascii=False))
    assert set(report) == set(TARGET_PATTERNS)
    print("PASS targeted 12-group candidate extraction; zero foods automatically promoted")


if __name__ == "__main__":
    main()
