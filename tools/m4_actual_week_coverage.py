#!/usr/bin/env python3
"""Check an ACTUAL FoodPlan 7-day shopping export against publicly observed prices.

Never claim a complete basket when the live source only verifies a subset.
All times, prices and product IDs must be sourced from auditable JSON evidence.
The only user-relevant outcome is an indicative cost, or UNKNOWN if incomplete.
"""
import argparse
from datetime import datetime, timezone
import json
from pathlib import Path
import re

MAX_AGE_S = 7 * 24 * 60 * 60
CANONICAL_PRODUCT = re.compile(r"^https://magnit\.ru/product/[A-Za-z0-9_-]+/?$")
VALID_INGREDIENT = re.compile(r"^[a-z_]+$")


def read_week(data):
    if (not isinstance(data, dict) or data.get("schema_version") != 1 or
        data.get("provenance") != "PRODUCTION_SEVEN_DAY_PLANNER_SHOPPING_AGGREGATION" or
        data.get("days") != 7 or data.get("meals") != 21):
        raise ValueError("NOT_A_REAL_7_DAY_PLANNER_EXPORT")
    rows = data.get("ingredients")
    if (not isinstance(rows, list) or not rows or
        data.get("ingredient_count") != len(rows)):
        raise ValueError("INCORRECT_WEEK_INGREDIENT_COUNT")
    used = set()
    for row in rows:
        if not isinstance(row, dict):
            raise ValueError("CORRUPT_SHOPPING_ROW")
        ingredient = row.get("ingredient_id")
        if (not isinstance(ingredient, str) or not VALID_INGREDIENT.fullmatch(ingredient) or
            ingredient in used or type(row.get("required_grams")) is not int or
            row["required_grams"] <= 0 or
            type(row.get("pantry_used_grams")) is not int or
            row["pantry_used_grams"] < 0):
            raise ValueError("UNSAFE_SHOPPING_ROW")
        used.add(ingredient)
    return rows


def valid_quote(ingredient_id, row, now):
    if not isinstance(row, dict):
        return None
    if (row.get("status") != "VERIFIED_PUBLIC_CHAIN_PRICE" or
        row.get("ingredient_id") != ingredient_id or
        row.get("price_kind") != "APPROXIMATE_CHAIN_PUBLIC_PRICE" or
        row.get("checkout_price_verified") is not False or
        row.get("address_required") is not False or
        not isinstance(row.get("source_sha256"), str) or
        not re.fullmatch("[0-9a-f]{64}", row["source_sha256"]) or
        not isinstance(row.get("product_url"), str) or
        not CANONICAL_PRODUCT.fullmatch(row["product_url"])):
        return None
    grams = row.get("pack_grams")
    price = row.get("visible_price_kopeks")
    if (type(grams) is not int or grams < 1 or grams > 50000 or
        type(price) is not int or price < 1 or price > 10000000):
        return None
    try:
        checked = datetime.fromisoformat(row["checked_at_utc"])
    except (ValueError, KeyError, TypeError):
        return None
    if checked.tzinfo is None or not 0 <= (now - checked).total_seconds() <= MAX_AGE_S:
        return None
    return {"grams": grams, "kopeks": price,
            "url": row["product_url"], "observed_at_utc": row["checked_at_utc"]}


def week_price_coverage(week_json, observed_json, budget_rub, now=None):
    if type(budget_rub) is not int or not 100 <= budget_rub <= 100000:
        raise ValueError("INVALID_WEEKLY_BUDGET")
    now = now or datetime.now(timezone.utc)
    shopping = read_week(week_json)
    results = []
    for need in shopping:
        key = need["ingredient_id"]
        match = valid_quote(key, observed_json.get(key), now)
        if match is None:
            results.append({
                "ingredient_id": key,
                "required_grams": need["required_grams"],
                "status": "PRICE_UNKNOWN",
                "estimated_cost_kopeks": None
            })
            continue
        packs = (need["required_grams"] + match["grams"] - 1) // match["grams"]
        results.append({
            "ingredient_id": key, "required_grams": need["required_grams"],
            "status": "PUBLIC_CHAIN_PRICE_OBSERVED",
            "package_grams": match["grams"], "packages": packs,
            "price_per_package_kopeks": match["kopeks"],
            "estimated_cost_kopeks": packs * match["kopeks"],
            "product_url": match["url"],
            "observed_at_utc": match["observed_at_utc"]
        })
    missing = [r["ingredient_id"] for r in results if r["status"] == "PRICE_UNKNOWN"]
    known_total = sum(r["estimated_cost_kopeks"] or 0 for r in results)
    complete = len(missing) == 0
    return {
        "origin": "ACTUAL_7_DAY_PLANNER_WITH_INDEPENDENT_PUBLIC_PRICES",
        "days": 7, "meals": week_json["meals"],
        "needed_ingredient_count": len(results),
        "priced_ingredient_count": len(results) - len(missing),
        "missing_ingredient_ids": missing,
        "lines": results,
        "known_subtotal_kopeks": known_total,
        "full_week_total_kopeks": known_total if complete else None,
        "full_week_within_budget": known_total <= budget_rub*100 if complete else None,
        "known_subtotal_exceeds_budget": known_total > budget_rub * 100,
        "budget_rub": budget_rub,
        "status": "PASS_FULL_WEEK_PUBLIC_PRICE" if complete else "PARTIAL_FULL_WEEK_PRICE_UNKNOWN",
        "address_required": False,
        "allergy_label_verified": False,
        "price_is_indicative_not_checkout": True
    }


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--shopping", required=True)
    ap.add_argument("--prices-dir", required=True)
    ap.add_argument("--budget-rub", type=int, default=356)
    ap.add_argument("--output", required=True)
    options = ap.parse_args()
    week = json.loads(Path(options.shopping).read_text(encoding="utf-8"))
    prices = {}
    for file in Path(options.prices_dir).glob("*.json"):
        if file.stem not in {
            "rice", "buckwheat", "oats", "millet", "lentils", "flour", "pasta",
            "chicken", "carrot", "cottage_cheese", "cheese", "mushroom",
            "potato", "cucumber", "tomato", "broccoli"
        }:
            continue
        prices[file.stem] = json.loads(file.read_text(encoding="utf-8"))
    report = week_price_coverage(week, prices, options.budget_rub)
    out = Path(options.output)
    out.parent.mkdir(parents=True, exist_ok=True)
    out.write_text(json.dumps(report, ensure_ascii=False, indent=2) + "\n",
                   encoding="utf-8")
    print("Actual FoodPlan week source coverage:",
          f'{report["priced_ingredient_count"]}/{report["needed_ingredient_count"]}',
          "status:", report["status"],
          "known_subtotal_kopeks:", report["known_subtotal_kopeks"],
          "week_total:", report["full_week_total_kopeks"])


if __name__ == "__main__":
    main()
