#!/usr/bin/env python3
"""Independent M2 data check: pinned Kotlin nutrients vs the original USDA CSV ZIP.

This verifies source identity and macro values, NOT retail labels, allergens,
recipe yields, food-safety, or the similarity of every Russian store SKU.
"""
import argparse
import json
import math
import re
from pathlib import Path
from zipfile import ZipFile

from fdc_candidates import _rows

SOURCE_PATTERN = re.compile(
    r'FoodIngredient\(\s*"(?P<id>[^"]+)"\s*,\s*"[^"]+"\s*,'
    r'\s*(?P<energy>\d+(?:\.\d+)?)\s*,'
    r'\s*(?P<protein>\d+(?:\.\d+)?)\s*,'
    r'\s*(?P<fat>\d+(?:\.\d+)?)\s*,'
    r'\s*(?P<carbs>\d+(?:\.\d+)?)\s*,',
    re.MULTILINE
)
REFERENCE_PATTERN = re.compile(r'nutrientSourceReference\s*=\s*"([^"]+)"')
EXPECTED_PREFIX = "https://fdc.nal.usda.gov/food-details/"


def extract_kotlin_ingredients(text: str):
    matches = list(SOURCE_PATTERN.finditer(text))
    if not matches:
        raise ValueError("No FoodIngredient constructors parsed from Kotlin source")
    found = {}
    for i, match in enumerate(matches):
        block = text[match.start():matches[i + 1].start() if i + 1 < len(matches)
                     else text.find(').associateBy { it.id }', match.start())]
        ingredient_id = match.group("id")
        if ingredient_id in found:
            raise ValueError(f"Duplicate ingredient {ingredient_id}")
        source_match = REFERENCE_PATTERN.search(block)
        found[ingredient_id] = {
            "values": [float(match.group(x)) for x in ("energy", "protein", "fat", "carbs")],
            "source": source_match.group(1) if source_match else None,
            "allergen_marked_reviewed": "allergenCompositionReviewed=true" in block
        }
    return found


def check(
    archive_path: Path,
    evidence_path: Path,
    kotlin_path: Path,
) -> int:
    data = json.loads(evidence_path.read_text(encoding="utf-8"))
    assert data["dataset"] == "USDA FoodData Central"
    if data["release"] not in ("Foundation Foods April 2026", "SR Legacy April 2018"):
        raise ValueError("Unrecognized USDA release; use a pinned source")
    assert data["status"] == "SOURCE_VALUES_VERIFIED_NOT_RUSSIAN_SKU_OR_ALLERGEN_CLEARANCE"
    foods = data["foods"]
    if not foods or len(foods) != len({item["ingredient_id"] for item in foods}):
        raise ValueError("Empty or duplicated source mappings")
    ids = {str(f["fdc_id"]) for f in foods}
    if len(ids) != len(foods):
        raise ValueError("Duplicate FDC ID; require specific source per ingredient")

    app = extract_kotlin_ingredients(kotlin_path.read_text(encoding="utf-8"))
    linked = {name for name, food in app.items() if food["source"] is not None}
    documented = {f["ingredient_id"] for f in foods}
    if not documented.issubset(linked):
        raise ValueError("Referenced USDA source missing from Kotlin: " +
                         str(documented-linked))

    with ZipFile(archive_path) as archive:
        actual_descriptions = {
            row["fdc_id"]: row["description"].strip()
            for row in _rows(archive, "food.csv") if row.get("fdc_id") in ids
        }
        actual_nutrients = {fdc: {} for fdc in ids}
        wanted = {1003, 1004, 1005, 1008, 2047, 2048}
        for row in _rows(archive, "food_nutrient.csv"):
            fdc = row.get("fdc_id")
            if fdc not in ids:
                continue
            try:
                nutrient_id = int(row["nutrient_id"])
                amount = float(row["amount"])
            except (ValueError, TypeError, KeyError):
                continue
            if nutrient_id in wanted and math.isfinite(amount):
                actual_nutrients[fdc][nutrient_id] = amount

    for record in foods:
        ingredient_id = record["ingredient_id"]
        fdc = str(record["fdc_id"])
        expected_ref = f"{EXPECTED_PREFIX}{fdc}/nutrients"
        if actual_descriptions.get(fdc) != record["description"]:
            raise ValueError(f"{ingredient_id}: USDA description changed or FDC ID mismatched")
        values = actual_nutrients[fdc]
        energy = next((values[n] for n in (2047, 2048, 1008) if n in values), None)
        source_values = [energy, values.get(1003), values.get(1004), values.get(1005)]
        if any(value is None for value in source_values):
            raise ValueError(f"{ingredient_id}: incomplete nutrition source")
        expected_values = [
            record["per_100g"][key]
            for key in ("energy_kcal", "protein_g", "fat_g", "carbs_g")
        ]
        actual_app = app[ingredient_id]
        if actual_app["source"] != expected_ref:
            raise ValueError(f"{ingredient_id}: Kotlin links to a different FDC ID")
        if record["allergen_reviewed"] is not False or actual_app["allergen_marked_reviewed"]:
            raise ValueError(f"{ingredient_id}: USDA must never approve SKU allergens")
        for label, a, b, c in zip(
            ("kcal", "protein", "fat", "carbohydrate"),
            source_values, expected_values, actual_app["values"]
        ):
            if not math.isclose(a, b, rel_tol=0, abs_tol=0.0001):
                raise ValueError(f"{ingredient_id}: fixture {label} differs from official USDA")
            if not math.isclose(a, c, rel_tol=0, abs_tol=0.0001):
                raise ValueError(f"{ingredient_id}: Kotlin {label} differs from official USDA")
    return len(foods)


def check_complete_source_coverage(
        kotlin_path: Path, evidence_paths: list[Path],
        external_evidence_paths: list[Path] = None) -> int:
    """Reject any Kotlin source without an independently pinned source registry.

    USDA numerics are verified by check(), Russian producer facts by
    verify_ru_tvorog.py; this function only compares coverage and URLs.
    """
    app = extract_kotlin_ingredients(kotlin_path.read_text(encoding="utf-8"))
    app_usda = {name for name, food in app.items()
                if food["source"] and food["source"].startswith(EXPECTED_PREFIX)}
    app_external = {name: food["source"] for name, food in app.items()
                    if food["source"] and not food["source"].startswith(EXPECTED_PREFIX)}
    mapped = []
    for path in evidence_paths:
        records = json.loads(path.read_text(encoding="utf-8"))["foods"]
        mapped.extend(item["ingredient_id"] for item in records)
    if len(mapped) != len(set(mapped)) or app_usda != set(mapped):
        raise ValueError("Undocumented, duplicate or missing USDA source: " +
                         str({"app_only": app_usda-set(mapped),
                              "fixture_only": set(mapped)-app_usda}))
    external = {}
    for path in external_evidence_paths or []:
        doc = json.loads(path.read_text(encoding="utf-8"))
        if doc.get("status") != "PRODUCER_NUTRIENT_FACTS_VERIFIED_NOT_RETAIL_SKU_OR_ALLERGEN_CLEARANCE":
            raise ValueError("External nutrition source status is not independently approved")
        ingredient_id = doc.get("ingredient_id")
        if not ingredient_id or ingredient_id in external or ingredient_id in mapped:
            raise ValueError("Duplicate or missing non-USDA source identity")
        if doc.get("allergen_reviewed") is not False:
            raise ValueError("Manufacturer nutrient source must not clear allergens")
        external[ingredient_id] = doc["source_url"]
    if app_external != external:
        raise ValueError("Undocumented or mismatched non-USDA Kotlin sources: " +
                         str({"app_only": app_external, "fixture_only": external}))
    return len(mapped) + len(external)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("archive", type=Path, help="original USDA Foundation Foods CSV ZIP")
    parser.add_argument("--evidence", type=Path,
                        default=Path("fixtures/m2_fdc_reference_evidence.json"))
    parser.add_argument("--kotlin", type=Path,
                        default=Path("android/domain/src/main/kotlin/com/foodplan/domain/OfflineRecipes.kt"))
    parser.add_argument("--complete-with", type=Path, nargs="*", default=[],
                        help="Other pinned evidence files for complete Kotlin source coverage")
    parser.add_argument("--external-evidence", type=Path, nargs="*", default=[],
                        help="Manufacturer records independently checked by separate CI jobs")
    args = parser.parse_args()
    count = check(args.archive, args.evidence, args.kotlin)
    if args.complete_with or args.external_evidence:
        covered = check_complete_source_coverage(
            args.kotlin, [args.evidence] + args.complete_with, args.external_evidence)
        print(f"PASS all {covered} sourced Kotlin ingredients have pinned USDA evidence")
    print(f"PASS: {count} raw/packaged ingredient values matched USDA CSV and Kotlin")
    print("NOT ACCEPTED: recipe safety, product label allergens, yield and SKU equivalence")


if __name__ == "__main__":
    main()
