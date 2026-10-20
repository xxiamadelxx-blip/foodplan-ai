#!/usr/bin/env python3
"""Inventory M2 ingredient provenance gaps without claiming recipe or SKU safety.

This checks exact FDC IDs referenced in Kotlin against pinned evidence metadata.
It is NOT the independent numerical USDA archive comparison, which remains
tools/verify_fdc_evidence.py in the dedicated CI jobs.
"""
import argparse
import json
import re
from pathlib import Path

RECORD = re.compile(r'FoodIngredient\("([^"]+)"')
FDC = re.compile(r'nutrientSourceReference\s*=\s*"https://fdc\.nal\.usda\.gov/food-details/(\d+)/nutrients"')


def inventory(kotlin: str, evidence_documents: list[dict]) -> dict:
    matches = list(RECORD.finditer(kotlin))
    if not matches:
        raise ValueError("No ingredient records parsed from Kotlin catalogue")
    catalogue = {}
    for index, match in enumerate(matches):
        name = match.group(1)
        if name in catalogue:
            raise ValueError(f"Duplicate ingredient: {name}")
        segment = kotlin[match.start(): matches[index + 1].start()
                         if index + 1 < len(matches) else len(kotlin)]
        fdc = FDC.findall(segment)
        if len(fdc) > 1 or ("nutrientSourceReference" in segment and len(fdc) != 1):
            raise ValueError(f"Malformed or ambiguous nutrition source for {name}")
        catalogue[name] = int(fdc[0]) if fdc else None

    pinned = {}
    for document in evidence_documents:
        if document.get("status") != "SOURCE_VALUES_VERIFIED_NOT_RUSSIAN_SKU_OR_ALLERGEN_CLEARANCE":
            raise ValueError("Evidence document has unexpected status")
        for food in document["foods"]:
            name, fdc = food["ingredient_id"], food["fdc_id"]
            if name in pinned:
                raise ValueError(f"Duplicate evidence for ingredient: {name}")
            if food.get("allergen_reviewed") is not False:
                raise ValueError(f"USDA source cannot clear package allergens: {name}")
            pinned[name] = fdc

    sourced = {name: value for name, value in catalogue.items() if value is not None}
    if sourced != pinned:
        raise ValueError(f"Unmatched source references: Kotlin-only={set(sourced.items()) - set(pinned.items())}; evidence-only={set(pinned.items()) - set(sourced.items())}")
    pending = sorted(name for name, value in catalogue.items() if value is None)
    return {
        "status": "M2_SOURCE_INVENTORY_ONLY_NOT_RECIPE_ACCEPTANCE",
        "catalogue_ingredient_count": len(catalogue),
        "pinned_usda_source_count": len(sourced),
        "missing_usda_source_count": len(pending),
        "unverified_ingredient_ids": pending,
        "usda_nutrition_values_checked_by_this_script": False,
        "russian_sku_labels_approved": 0,
        "recipe_gate": "NOT_EVALUATED_BY_THIS_SCRIPT"
    }


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--kotlin", type=Path, default=Path(
        "android/domain/src/main/kotlin/com/foodplan/domain/OfflineRecipes.kt"))
    parser.add_argument("--evidence", type=Path, nargs="+", default=[
        Path("fixtures/m2_fdc_reference_evidence.json"),
        Path("fixtures/m2_sr_legacy_evidence.json")
    ])
    parser.add_argument("--output", type=Path, default=Path(
        "m2-source-gap-report/ingredient-gaps.json"))
    args = parser.parse_args()
    result = inventory(args.kotlin.read_text(encoding="utf-8"),
                       [json.loads(path.read_text(encoding="utf-8"))
                        for path in args.evidence])
    args.output.parent.mkdir(parents=True, exist_ok=True)
    args.output.write_text(json.dumps(result, ensure_ascii=False, indent=2) + "\n",
                           encoding="utf-8")
    print(f"M2 INVENTORY: {result['pinned_usda_source_count']}/{result['catalogue_ingredient_count']} source-linked, {result['missing_usda_source_count']} gaps; NOT recipe or SKU clearance")


if __name__ == "__main__":
    main()
