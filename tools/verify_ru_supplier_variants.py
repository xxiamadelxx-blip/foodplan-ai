#!/usr/bin/env python3
"""Audit named-manufacturer variations under one Russian public retailer product ID.

The browser product card is not a selected physical package or verified allergy label.
Fail if publisher cannot be read or the per-supplier values drift.
"""
import argparse
import hashlib
import json
from pathlib import Path
import re
from urllib.request import Request, urlopen

from verify_ru_retail_rice import visible_text

MANIFEST = Path("fixtures/m2_ru_multi_supplier_candidates.json")
EXPECTED_URLS = {
    "1248": "https://vkusvill.ru/goods/gerkules-ovsyanye-khlopya-1248/",
    "15544": "https://vkusvill.ru/goods/yaytso-kurinoe-s1-15544/",
    "17699": "https://vkusvill.ru/goods/krupa-grechnevaya-yadritsa-17699/",
}


def validate_manifest(data, kotlin_registry=None):
    if (data.get("schema_version") != 1 or
        data.get("retailer") != "ВкусВилл" or
        data.get("status") != "MULTI_SUPPLIER_PUBLIC_PRODUCT_CARD_NO_SELECTED_PACKAGE"):
        raise ValueError("Manufacturer ambiguity source type invalid")
    items = data["products"]
    if {p["retailer_product_id"] for p in items} != set(EXPECTED_URLS):
        raise ValueError("Missing, duplicate or added unreviewed product")
    for p in items:
        pid = p["retailer_product_id"]
        if (p["url"] != EXPECTED_URLS[pid] or
            p["ingredient_id"] != {"1248":"oats", "15544":"egg", "17699":"buckwheat"}[pid] or
            p["declared_manufacturer_count"] < len(p["variants"]) or
            p["actual_manufacturer"] is not None or
            p["exact_package_label_verified"] is not False or
            p["photo_sha256"] is not None or
            p["address_price_kopeks"] is not None or
            p["availability"] != "UNKNOWN" or
            p["release_status"] != "BLOCK_GENERIC_MACROS_PENDING_PRODUCER_LABEL"):
            raise ValueError("Public manufacturer card falsely promoted: " + pid)
        values = set()
        for v in p["variants"]:
            n = v["nutrition_per_100g"]
            values.add(tuple(n[k] for k in ("kcal", "protein_g", "fat_g", "carbs_g")))
        if len(values) < 2:
            raise ValueError("No independent per-supplier macro discrepancy: " + pid)
    oats = next(p for p in items if p["retailer_product_id"] == "1248")
    if (oats["declared_manufacturer_count"] != 3 or
        [v["manufacturer_hint"] for v in oats["variants"]] !=
        ["торговая компания", "эндакси", "элитпак"] or
        oats["variants"][1]["declared_facility_traces"] !=
        ["gluten_cereals", "tree_nuts"]):
        raise ValueError("Oats manufacturer and traces evidence changed")
    eggs = next(p for p in items if p["retailer_product_id"] == "15544")
    if eggs["declared_manufacturer_count"] != 8:
        raise ValueError("Eggs supplier count changed")
    # Missing a retailer-provided warning means UNKNOWN, not "none".
    if eggs["variants"][1]["declared_facility_traces"] is not None:
        raise ValueError("Unlisted egg supplier traces cannot be marked absent")
    buckwheat = next(p for p in items if p["retailer_product_id"] == "17699")
    if (buckwheat["declared_manufacturer_count"] != 4 or
        [v["manufacturer_hint"] for v in buckwheat["variants"]] !=
        ["торговая компания", 'мельничный комплекс "роса"', "селтинг", "элитпак"] or
        any(v["declared_facility_traces"] != ["gluten_cereals"]
            for v in buckwheat["variants"][:3]) or
        buckwheat["variants"][3]["declared_facility_traces"] is not None):
        raise ValueError("Buckwheat supplier label uncertainty changed")
    if kotlin_registry:
        for p in items:
            if p["url"] not in kotlin_registry:
                raise ValueError("Kotlin shopping risk guard missed " + p["url"])
    return items


def _number_pattern(number):
    raw = str(number)
    if "." in raw:
        whole, fraction = raw.split(".")
        return re.escape(whole) + r"[,.]" + re.escape(fraction)
    return re.escape(raw) + r"(?:[.,]0)?"


def verify_public_page(page_bytes, item):
    text = visible_text(page_bytes)
    if item["public_title"].casefold().replace("ё", "е") not in text:
        raise ValueError("Wrong public item title")
    start = text.find("пищевая и энергетическая ценность в 100 г")
    end = text.find("вес/объем", start)
    if start < 0 or end < 0 or end - start > 2400:
        raise ValueError("No complete per-supplier nutrition section")
    section = text[start:end]
    for variant in item["variants"]:
        name = variant["manufacturer_hint"]
        index = section.find(name)
        if index < 0:
            raise ValueError("Supplier absent from nutrition section: " + name)
        boundary = section.find("ккал", index)
        if boundary < 0 or boundary - index > 320:
            raise ValueError("Supplier has no tightly scoped KБЖУ: " + name)
        supplier_text = section[index:boundary + len("ккал")]
        n = variant["nutrition_per_100g"]
        for key, russian in (
            ("protein_g", "белки"),
            ("fat_g", "жиры"),
            ("carbs_g", "углеводы")
        ):
            pattern = russian + r"\s*[-:]?\s*" + _number_pattern(n[key]) + r"\s*г\b"
            if not re.search(pattern, supplier_text):
                raise ValueError(f"{name} {key} mismatch")
        if not re.search(_number_pattern(n["kcal"]) + r"\s*ккал\b", supplier_text):
            raise ValueError(name + " energy mismatch")
    if item["retailer_product_id"] in {"1248", "17699"}:
        s = text.find("аллергены по производителям", end)
        e = text.find("условия хранения", s)
        if s < 0 or e < 0 or e-s > 1500:
            raise ValueError("Manufacturer-specific trace notices missing")
        traces = text[s:e]
        if item["retailer_product_id"] == "1248":
            if ("злаки, содержащие глютен" not in traces or
                "эндакси" not in traces or "орехи" not in traces):
                raise ValueError("Published gluten / nuts trace notices not confirmed")
        else:
            if ("злаки, содержащие глютен" not in traces or
                'мельничный комплекс "роса"' not in traces or
                "селтинг" not in traces or "торговая компания" not in traces):
                raise ValueError("Buckwheat publicly declared manufacturer traces changed")
    return {
        "id": item["retailer_product_id"],
        "source_url": item["url"],
        "public_page_sha256": hashlib.sha256(page_bytes).hexdigest(),
        "checked_supplier_variants": len(item["variants"]),
        "status": "PASS_MULTI_SUPPLIER_AMBIGUITY_CONFIRMED",
        "selected_physical_supplier": None,
        "allergen_safe_or_trace_free": False,
        "retail_address_stock_or_price_confirmed": False
    }


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--manifest", type=Path, default=MANIFEST)
    parser.add_argument("--kotlin", type=Path, default=Path(
        "android/domain/src/main/kotlin/com/foodplan/domain/RetailerVariantRiskRegistry.kt"))
    parser.add_argument("--live", action="store_true")
    parser.add_argument("--output", type=Path,
                        default=Path("m2-supplier-review/multiple-suppliers.json"))
    args = parser.parse_args()
    items = validate_manifest(
        json.loads(args.manifest.read_text(encoding="utf-8")),
        args.kotlin.read_text(encoding="utf-8"))
    checks = []
    if args.live:
        for item in items:
            request = Request(item["url"], headers={
                "User-Agent": "FoodPlanEvidenceAudit/0.1 (+public-read-only)"
            })
            with urlopen(request, timeout=20) as response:
                if response.status != 200:
                    raise ValueError("Retailer returned non-200 response")
                raw = response.read(6_000_001)
                if len(raw) > 6_000_000:
                    raise ValueError("Public page unexpectedly large")
            checks.append(verify_public_page(raw, item))
    report = {
        "manifest": "PASS_MULTI_SUPPLIER_GUARD",
        "public_pages_independently_checked": len(checks),
        "checks": checks,
        "package_labels_or_recipe_yields_verified": 0,
        "release_gate": "FAIL_NOT_ENOUGH_EVIDENCE"
    }
    args.output.parent.mkdir(parents=True, exist_ok=True)
    args.output.write_text(json.dumps(report, ensure_ascii=False, indent=2) + "\n",
                           encoding="utf-8")
    print("PASS multi-supplier item identifiers:", len(items), "/", len(EXPECTED_URLS))
    print("Public pages independently checked:", len(checks), "/", len(EXPECTED_URLS))
    print("M2 FAIL: supplier on actual purchased pack unknown; labels not cleared")


if __name__ == "__main__":
    main()
