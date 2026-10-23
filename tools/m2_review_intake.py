#!/usr/bin/env python3
"""Fail-closed validation of M2 external-review CSV. This cannot certify a recipe.

The CSV is an evidence intake form; an external reviewer must supply actual
artifacts and a second reviewer must check them independently.
"""
import argparse
import csv
import hashlib
import json
from pathlib import Path

PROOF_COLUMNS = (
    "external_method_source_and_snapshot_sha256",
    "independent_reviewer_identifier",
    "second_review_identifier",
    "cooked_dish_measured_weight_and_trial_source",
    "cooking_yield_or_retention_calculation_and_source",
    "exact_sku_manufacturer_gtin_trace_label_evidence",
)
EXPECTED_COLUMNS = (
    "recipe_id", "recipe_title", "meal", "evidence_status",
    "raw_input_weight_grams_not_cooked_yield",
    "ingredient_quantities_raw_basis_and_reference_sources",
    "full_preparation_steps", "equipment", "active_minutes", "total_minutes",
    "preliminary_raw_input_energy_kcal_not_certified_cooked",
    "sourced_reference_ingredient_count", "ingredient_count",
    "reference_ingredient_allergens_not_sku_trace_clearance",
    "missing_measured_cooked_yield",
    "missing_independent_cooking_method_source",
    "missing_exact_product_allergen_trace_review",
    *PROOF_COLUMNS, "independent_verdict",
)
APPROVED = "INDEPENDENTLY_VERIFIED"
# The review queue is code-generated evidence. External reviewers may fill
# only these proof fields and the verdict; they may not edit recipes or remove
# missing-evidence warnings to manufacture apparent acceptance.
IMMUTABLE_COLUMNS = tuple(
    key for key in EXPECTED_COLUMNS
    if key not in (*PROOF_COLUMNS, "independent_verdict")
)


def read_review_csv(path):
    with Path(path).open(encoding="utf-8-sig", newline="") as handle:
        reader = csv.DictReader(handle, strict=True)
        if tuple(reader.fieldnames or ()) != EXPECTED_COLUMNS:
            raise ValueError("Review CSV schema mismatch; cannot audit unknown evidence columns")
        rows = list(reader)
    if len(rows) != 150:
        raise ValueError(f"Expected 150 authored recipes, got {len(rows)}")
    ids = [row["recipe_id"].strip() for row in rows]
    if not all(ids) or len(set(ids)) != len(ids):
        raise ValueError("Missing or duplicate recipe identifiers")
    return rows


def audit_review_csv(path, baseline_path=None):
    rows = read_review_csv(path)
    immutable_changes = set()
    if baseline_path is not None:
        baseline = {row["recipe_id"]: row for row in read_review_csv(baseline_path)}
        for row in rows:
            original = baseline.get(row["recipe_id"])
            if original is None or any(
                row[key] != original[key] for key in IMMUTABLE_COLUMNS
            ):
                immutable_changes.add(row["recipe_id"])
        if immutable_changes:
            raise ValueError("Immutable recipe facts changed relative to original "
                             "Kotlin export: " + ", ".join(sorted(immutable_changes)[:5]))
    results = []
    for row in rows:
        rid = row["recipe_id"].strip()
        reasons = []
        if row["evidence_status"] != "CURATED_DRAFT":
            reasons.append("DRAFT_IDENTITY_CHANGED")
        if not row["full_preparation_steps"].strip() or not row["ingredient_quantities_raw_basis_and_reference_sources"].strip():
            reasons.append("RECIPE_DETAILS_MISSING")
        if row["sourced_reference_ingredient_count"] != row["ingredient_count"]:
            reasons.append("RAW_INGREDIENT_SOURCES_INCOMPLETE")
        if row["missing_measured_cooked_yield"] != "true":
            # The source queue intentionally marks this unknown; never let an
            # edited flag alone act as a measurement.
            reasons.append("SOURCE_QUEUE_YIELD_FLAG_CHANGED")
        if row["missing_independent_cooking_method_source"] != "true":
            reasons.append("SOURCE_QUEUE_METHOD_FLAG_CHANGED")
        if row["missing_exact_product_allergen_trace_review"] != "true":
            reasons.append("SOURCE_QUEUE_SKU_ALLERGEN_FLAG_CHANGED")
        missing = [key for key in PROOF_COLUMNS if not row[key].strip()]
        if missing:
            reasons.append("MISSING_EXTERNAL_PROOF:" + ",".join(missing))
        first = row["independent_reviewer_identifier"].strip().casefold()
        second = row["second_review_identifier"].strip().casefold()
        if first and second and first == second:
            reasons.append("REVIEWERS_NOT_INDEPENDENT")
        method = row["external_method_source_and_snapshot_sha256"].strip()
        if method and not any(len(piece) == 64 and all(c in "0123456789abcdefABCDEF" for c in piece)
                              for piece in method.replace(":", " ").split()):
            reasons.append("METHOD_SNAPSHOT_SHA256_MISSING")
        if row["independent_verdict"] == APPROVED:
            # CSV content alone can never authenticate identities, photos,
            # measured cooking yields, or a real physical SKU.
            reasons.append("CSV_CANNOT_SELF_CERTIFY")
        elif row["independent_verdict"] not in {"PENDING_EXTERNAL_REVIEW", "REJECTED", "NEEDS_REVISION"}:
            reasons.append("UNKNOWN_VERDICT")
        results.append({"recipe_id": rid, "intake_status": "NOT_EXTERNALLY_CERTIFIED",
                        "issues": reasons, "proof_fields_filled": len(PROOF_COLUMNS)-len(missing)})
    return {
        "source_sha256": hashlib.sha256(Path(path).read_bytes()).hexdigest(),
        "authored_drafts": len(rows),
        "independently_certified": 0,
        "release_gate": "FAIL_NOT_INDEPENDENTLY_CERTIFIED",
        "records": results,
    }


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("review_csv", type=Path)
    parser.add_argument("--baseline", type=Path, default=None,
                        help="Original unedited 150-recipe Kotlin export; required for external submissions")
    parser.add_argument("--output", type=Path, required=True)
    args = parser.parse_args()
    report = audit_review_csv(args.review_csv, args.baseline)
    args.output.parent.mkdir(parents=True, exist_ok=True)
    args.output.write_text(json.dumps(report, ensure_ascii=False, indent=2)+"\n", encoding="utf-8")
    print(f"PASS intake integrity: {report['authored_drafts']} drafts, 0 certified. M2 release FAIL.")


if __name__ == "__main__":
    main()
