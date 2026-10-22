#!/usr/bin/env python3
"""Independently recompute actual seven-day ingredient demand from meal CSV.

This is an integrity/consistency gate, NOT food safety, diet adequacy or an
external recipe approval. It never supplies a missing retailer price.
"""
import argparse
import csv
import hashlib
import json
from collections import defaultdict
from pathlib import Path
import math

MEAL_COLUMNS = [
    "day", "slot", "recipe_id", "recipe_name", "servings",
    "estimated_raw_ingredient_kcal", "ingredient_id_and_raw_grams"
]
SHOPPING_COLUMNS = [
    "ingredient_id", "name", "required_grams", "pantry_used_grams",
    "to_buy_grams", "pack_grams", "reference_pack_count",
    "indicative_chain_price_status", "nutrition_and_sku_certification"
]


def rows(path, columns):
    with Path(path).open(encoding="utf-8-sig", newline="") as handle:
        reader = csv.DictReader(handle, strict=True)
        if reader.fieldnames != columns:
            raise ValueError("Unexpected export schema: " + str(reader.fieldnames))
        return list(reader)


def audit(meal_file, shopping_file):
    meals = rows(meal_file, MEAL_COLUMNS)
    shopping = rows(shopping_file, SHOPPING_COLUMNS)
    if len(meals) != 21:
        raise ValueError(f"Expected 21 meals, got {len(meals)}")
    keys = {(int(row["day"]), int(row["slot"])) for row in meals}
    if keys != {(d, slot) for d in range(7) for slot in range(3)}:
        raise ValueError("Missing or duplicated day/slot in real planner export")
    raw_totals = defaultdict(float)
    for meal in meals:
        if not meal["recipe_id"] or not meal["recipe_name"]:
            raise ValueError("Missing recipe ID or name")
        servings = float(meal["servings"])
        if not math.isfinite(servings) or not (0.1 <= servings <= 20):
            raise ValueError("Invalid scaled servings")
        parts = meal["ingredient_id_and_raw_grams"].split("|")
        if not parts:
            raise ValueError("Empty ingredient list")
        per_meal = set()
        for part in parts:
            ingredient_id, separator, raw_grams = part.partition(":")
            if not separator or not ingredient_id or ingredient_id in per_meal:
                raise ValueError("Invalid or duplicate meal ingredient")
            per_meal.add(ingredient_id)
            grams = int(raw_grams)
            if grams <= 0:
                raise ValueError("Non-positive ingredient grams")
            raw_totals[ingredient_id] += grams * servings
    ids = [row["ingredient_id"] for row in shopping]
    if not shopping or len(ids) != len(set(ids)):
        raise ValueError("Empty or duplicate shopping IDs")
    if set(ids) != set(raw_totals):
        raise ValueError("Weekly basket not equal to the union of real planned meals")
    mismatch = []
    for row in shopping:
        iid = row["ingredient_id"]
        if (row["indicative_chain_price_status"] != "UNKNOWN" or
            row["nutrition_and_sku_certification"] !=
                "NOT_INGREDIENT_OR_ALLERGEN_CERTIFIED"):
            raise ValueError("Unverified price or nutrition was falsely certified")
        required = int(row["required_grams"])
        pantry = int(row["pantry_used_grams"])
        purchase = int(row["to_buy_grams"])
        pack = int(row["pack_grams"])
        count = int(row["reference_pack_count"])
        expected_grams = math.ceil(raw_totals[iid])
        # Java/Kotlin Double rounding around integer boundaries differs from
        # Python float accumulation by <1g in rare cases; fail large drift.
        if (abs(required - expected_grams) > 1 or pantry != 0 or
            required != purchase + pantry or pack <= 0 or
            count != (purchase + pack - 1) // pack):
            mismatch.append(iid)
    if mismatch:
        raise ValueError("Weekly aggregate disagrees with original meal grams: "
                         + ",".join(mismatch))
    return {
        "status": "PASS_AGGREGATION_ONLY",
        "meal_count": len(meals), "day_count": 7,
        "distinct_ingredient_count": len(ids),
        "all_store_prices": "UNKNOWN",
        "M2_independently_verified_recipes": 0,
        "whole_week_chain_price_proven": False,
        "meals_csv_sha256": hashlib.sha256(Path(meal_file).read_bytes()).hexdigest(),
        "shopping_csv_sha256": hashlib.sha256(Path(shopping_file).read_bytes()).hexdigest(),
        "ingredient_ids": sorted(ids),
    }


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("meals_csv")
    ap.add_argument("shopping_csv")
    ap.add_argument("--output", required=True)
    a = ap.parse_args()
    result = audit(a.meals_csv, a.shopping_csv)
    path = Path(a.output)
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(result, indent=2, ensure_ascii=False) + "\n",
                    encoding="utf-8")
    print("M3 actual planner and independent CSV aggregation:",
          result["status"], "meals=", result["meal_count"],
          "ingredients=", result["distinct_ingredient_count"],
          "price status UNKNOWN")


if __name__ == "__main__":
    main()
