#!/usr/bin/env python3
"""Independent two-ingredient *pilot*, using live-public source JSON only.

NOT a nutritionally complete seven-day diet or a claim of 37/37 priced items.
"""
import argparse
from datetime import datetime, timezone
import json
from pathlib import Path

PILOT_NEEDS_GRAMS = {"rice": 900, "buckwheat": 850}
BUDGETS_RUB = (100, 356, 63218, 100000)
MAX_SOURCE_AGE_SECONDS = 7 * 24 * 60 * 60


def quote_pilot(sources, now=None):
    now = now or datetime.now(timezone.utc)
    items = []
    for ingredient_id, grams_needed in PILOT_NEEDS_GRAMS.items():
        item = sources.get(ingredient_id)
        if not isinstance(item, dict):
            return {"status": "FAIL_MISSING_REAL_SOURCE", "missing": ingredient_id}
        if (item.get("status") != "VERIFIED_PUBLIC_CHAIN_PRICE" or
            item.get("ingredient_id") != ingredient_id or
            item.get("price_kind") != "APPROXIMATE_CHAIN_PUBLIC_PRICE" or
            item.get("checkout_price_verified") is not False or
            item.get("address_required") is not False or
            not item.get("source_sha256")):
            return {"status": "FAIL_UNVERIFIED_PRICE", "missing": ingredient_id}
        try:
            observed_at = datetime.fromisoformat(item["checked_at_utc"])
            age = (now - observed_at).total_seconds()
            pack = item["pack_grams"]
            kopeks = item["visible_price_kopeks"]
            if (observed_at.tzinfo is None or age < 0 or
                age > MAX_SOURCE_AGE_SECONDS or not isinstance(pack, int) or
                not isinstance(kopeks, int) or pack <= 0 or
                kopeks <= 0 or kopeks > 10_000_000):
                raise ValueError("stale or invalid public quote")
        except (ValueError, KeyError, TypeError):
            return {"status": "FAIL_STALE_OR_INVALID", "missing": ingredient_id}
        packs = (grams_needed + pack - 1) // pack
        items.append({
            "ingredient_id": ingredient_id,
            "required_grams": grams_needed,
            "pack_grams": pack,
            "packages": packs,
            "price_per_pack_kopeks": kopeks,
            "subtotal_kopeks": packs * kopeks,
            "public_url": item["product_url"],
            "observed_at_utc": item["checked_at_utc"],
        })
    subtotal = sum(x["subtotal_kopeks"] for x in items)
    return {
        "status": "PASS_TWO_INGREDIENT_PILOT",
        "not_full_7_day_basket": True,
        "verified_ingredient_count": len(items),
        "seven_day_ingredient_count_unverified": True,
        "lines": items,
        "two_item_total_kopeks": subtotal,
        "budget_scenarios": {
            str(b): {"budget_rub": b, "two_item_cost_within_budget":
                     subtotal <= b * 100} for b in BUDGETS_RUB
        },
        "physical_allergens_verified": False,
        "real_checkout_price_verified": False
    }


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--source-dir", required=True)
    parser.add_argument("--output", required=True)
    args = parser.parse_args()
    sources = {}
    for ingredient in PILOT_NEEDS_GRAMS:
        file = Path(args.source_dir) / f"{ingredient}.json"
        if file.exists():
            sources[ingredient] = json.loads(file.read_text(encoding="utf-8"))
    outcome = quote_pilot(sources)
    target = Path(args.output)
    target.parent.mkdir(parents=True, exist_ok=True)
    target.write_text(json.dumps(outcome, ensure_ascii=False, indent=2) + "\n",
                      encoding="utf-8")
    print("M4 chain-price mini-basket:", outcome["status"],
          "kopeks:", outcome.get("two_item_total_kopeks", "UNKNOWN"))
    if outcome["status"] != "PASS_TWO_INGREDIENT_PILOT":
        raise SystemExit(1)


if __name__ == "__main__":
    main()
