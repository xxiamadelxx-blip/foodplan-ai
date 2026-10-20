"""Offline fixture tests: no claim that fixture is the official USDA dataset."""
import csv
import io
import tempfile
import unittest
import zipfile
from pathlib import Path

from fdc_candidates import build_candidates


def csv_bytes(rows):
    out = io.StringIO(newline="")
    writer = csv.DictWriter(out, fieldnames=list(rows[0]))
    writer.writeheader()
    writer.writerows(rows)
    return out.getvalue().encode("utf-8")


class FdcCandidateImporterTests(unittest.TestCase):
    def test_reports_provenance_but_never_verifies_without_manual_review(self):
        with tempfile.TemporaryDirectory() as folder:
            archive = Path(folder) / "fixture.zip"
            with zipfile.ZipFile(archive, "w") as z:
                z.writestr("FDC/food.csv", csv_bytes([
                    {"fdc_id": "123456", "description": "Egg, chicken, raw"},
                    {"fdc_id": "987654", "description": "Rice, uncooked"}
                ]))
                z.writestr("FDC/food_nutrient.csv", csv_bytes([
                    {"fdc_id": "123456", "nutrient_id": "1003", "amount": "12.60"},
                    {"fdc_id": "123456", "nutrient_id": "1004", "amount": "9.50"},
                    {"fdc_id": "123456", "nutrient_id": "1005", "amount": "0.70"},
                    {"fdc_id": "123456", "nutrient_id": "2047", "amount": "143.0"},
                    {"fdc_id": "987654", "nutrient_id": "1003", "amount": "7.0"}
                ]))
            matches = build_candidates(archive, "egg", limit=5)
            self.assertEqual(1, len(matches))
            food = matches[0]
            self.assertEqual(123456, food["fdc_id"])
            self.assertEqual(143.0, food["nutrition_per_100g"]["energy_kcal"])
            self.assertEqual(12.6, food["nutrition_per_100g"]["protein_g"])
            self.assertEqual("CANDIDATE_NOT_VERIFIED", food["review_status"])
            self.assertFalse(food["imported_as_app_food"])
            self.assertIn("123456", food["provenance"]["food_url"])

    def test_word_boundaries_and_incomplete_nutrients_never_enter_candidate_queue(self):
        with tempfile.TemporaryDirectory() as folder:
            archive=Path(folder) / "fixture.zip"
            with zipfile.ZipFile(archive, "w") as z:
                z.writestr("food.csv", csv_bytes([
                    {"fdc_id": "2", "description": "PRICE chopped parmesan"},
                    {"fdc_id": "3", "description": "Rice, raw"},
                    {"fdc_id": "4", "description": "Rice, cooked"}
                ]))
                z.writestr("food_nutrient.csv", csv_bytes([
                    {"fdc_id": "2", "nutrient_id": "1003", "amount": "10"},
                    {"fdc_id": "2", "nutrient_id": "1004", "amount": "4"},
                    {"fdc_id": "2", "nutrient_id": "1005", "amount": "10"},
                    {"fdc_id": "2", "nutrient_id": "2047", "amount": "200"},
                    {"fdc_id": "3", "nutrient_id": "1003", "amount": "7"},
                    {"fdc_id": "3", "nutrient_id": "1004", "amount": "1"},
                    {"fdc_id": "3", "nutrient_id": "1005", "amount": "78"},
                    {"fdc_id": "3", "nutrient_id": "2047", "amount": "352"},
                    {"fdc_id": "4", "nutrient_id": "1003", "amount": "2"}
                ]))
            food=build_candidates(archive,"rice",limit=10)
            self.assertEqual(1,len(food))
            self.assertEqual("Rice, raw",food[0]["description"])
            self.assertEqual(3,food[0]["fdc_id"])

    def test_rejects_unsafe_ambiguous_requests(self):
        with self.assertRaises(ValueError):
            build_candidates(Path("unused.zip"), "x")
        with self.assertRaises(ValueError):
            build_candidates(Path("unused.zip"), "chicken", limit=10000)


if __name__ == "__main__":
    unittest.main()
