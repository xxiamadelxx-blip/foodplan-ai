import csv
import tempfile
import unittest
from pathlib import Path

from m2_review_intake import EXPECTED_COLUMNS, PROOF_COLUMNS, audit_review_csv


class M2IntakeTests(unittest.TestCase):
    def setUp(self):
        self.tmp = tempfile.TemporaryDirectory()
        self.addCleanup(self.tmp.cleanup)
        self.path = Path(self.tmp.name) / "review.csv"
        self.rows = [self.row(i) for i in range(150)]

    def row(self, i):
        r = dict.fromkeys(EXPECTED_COLUMNS, "")
        r.update(recipe_id=f"recipe-{i:03}", recipe_title=f"Блюдо {i}",
                 meal="LUNCH", evidence_status="CURATED_DRAFT",
                 raw_input_weight_grams_not_cooked_yield="350",
                 ingredient_quantities_raw_basis_and_reference_sources="rice:350g FDC",
                 full_preparation_steps="1. Cook.", active_minutes="15",
                 total_minutes="25", sourced_reference_ingredient_count="1",
                 ingredient_count="1", missing_measured_cooked_yield="true",
                 missing_independent_cooking_method_source="true",
                 missing_exact_product_allergen_trace_review="true",
                 independent_verdict="PENDING_EXTERNAL_REVIEW")
        return r

    def write(self, rows=None, fields=EXPECTED_COLUMNS):
        with self.path.open("w", newline="", encoding="utf-8") as f:
            writer = csv.DictWriter(f, fieldnames=fields)
            writer.writeheader()
            writer.writerows(self.rows if rows is None else rows)

    def test_complete_queue_stays_zero_certified(self):
        self.write()
        report = audit_review_csv(self.path)
        self.assertEqual(150, report["authored_drafts"])
        self.assertEqual(0, report["independently_certified"])
        self.assertEqual("FAIL_NOT_INDEPENDENTLY_CERTIFIED", report["release_gate"])
        self.assertIn("MISSING_EXTERNAL_PROOF", ",".join(report["records"][0]["issues"]))

    def test_fake_approval_with_filled_fields_is_never_certified(self):
        self.rows[0].update({k: "placeholder" for k in PROOF_COLUMNS})
        self.rows[0]["independent_verdict"] = "INDEPENDENTLY_VERIFIED"
        self.rows[0]["independent_reviewer_identifier"] = "person-1"
        self.rows[0]["second_review_identifier"] = "person-1"
        self.write()
        report = audit_review_csv(self.path)
        self.assertEqual(0, report["independently_certified"])
        self.assertIn("CSV_CANNOT_SELF_CERTIFY", report["records"][0]["issues"])
        self.assertIn("REVIEWERS_NOT_INDEPENDENT", report["records"][0]["issues"])
        self.assertIn("METHOD_SNAPSHOT_SHA256_MISSING", report["records"][0]["issues"])

    def test_duplicate_and_missing_rows_are_rejected(self):
        self.rows[1]["recipe_id"] = self.rows[0]["recipe_id"]
        self.write()
        with self.assertRaisesRegex(ValueError, "duplicate"):
            audit_review_csv(self.path)
        self.rows[1]["recipe_id"] = "unique"
        self.write(self.rows[:-1])
        with self.assertRaisesRegex(ValueError, "150"):
            audit_review_csv(self.path)

    def test_unknown_schema_rejected(self):
        self.write(fields=EXPECTED_COLUMNS[:-1])
        with self.assertRaisesRegex(ValueError, "schema"):
            audit_review_csv(self.path)

    def test_manually_cleared_flags_do_not_create_evidence(self):
        self.rows[0]["missing_measured_cooked_yield"] = "false"
        self.rows[0]["missing_independent_cooking_method_source"] = "false"
        self.write()
        issues = audit_review_csv(self.path)["records"][0]["issues"]
        self.assertIn("SOURCE_QUEUE_YIELD_FLAG_CHANGED", issues)
        self.assertIn("SOURCE_QUEUE_METHOD_FLAG_CHANGED", issues)


if __name__ == "__main__":
    unittest.main()
