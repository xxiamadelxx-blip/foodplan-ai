import csv
import io
import json
import tempfile
import unittest
import zipfile
from pathlib import Path

from verify_fdc_evidence import check, extract_kotlin_ingredients


def csv_bytes(rows):
    stream = io.StringIO(newline="")
    writer = csv.DictWriter(stream, fieldnames=list(rows[0]))
    writer.writeheader()
    writer.writerows(rows)
    return stream.getvalue().encode("utf-8")


class FoundationSourceAuditTests(unittest.TestCase):
    """Fictitious local data, never represented as genuine USDA evidence."""

    def setUp(self):
        self.tmp = tempfile.TemporaryDirectory()
        self.addCleanup(self.tmp.cleanup)
        self.root = Path(self.tmp.name)
        self.archive = self.root / "synthetic.zip"
        self.evidence = self.root / "evidence.json"
        self.kotlin = self.root / "Ingredients.kt"
        self.write_sources()

    def write_sources(self, actual_energy=100.0, actual_description="Sample, raw"):
        with zipfile.ZipFile(self.archive, "w") as z:
            z.writestr("food.csv", csv_bytes([
                {"fdc_id": "123", "description": actual_description}
            ]))
            z.writestr("food_nutrient.csv", csv_bytes([
                {"fdc_id": "123", "nutrient_id": "2047", "amount": str(actual_energy)},
                {"fdc_id": "123", "nutrient_id": "1003", "amount": "15"},
                {"fdc_id": "123", "nutrient_id": "1004", "amount": "5"},
                {"fdc_id": "123", "nutrient_id": "1005", "amount": "2"}
            ]))
        self.evidence.write_text(json.dumps({
            "dataset": "USDA FoodData Central",
            "release": "Foundation Foods April 2026",
            "status": "SOURCE_VALUES_VERIFIED_NOT_RUSSIAN_SKU_OR_ALLERGEN_CLEARANCE",
            "foods": [{
                "ingredient_id": "example", "fdc_id": 123,
                "description": "Sample, raw", "per_100g": {
                    "energy_kcal": 100, "protein_g": 15, "fat_g": 5, "carbs_g": 2
                }, "allergen_reviewed": False
            }]
        }), encoding="utf-8")
        self.kotlin.write_text(
            'package com.example\nval foods=listOf(\n'
            'FoodIngredient("example","Sample",100.0,15.0,5.0,2.0,1000,'
            ' nutrientSourceReference='
            '"https://fdc.nal.usda.gov/food-details/123/nutrients")\n'
            ').associateBy { it.id }\n', encoding="utf-8"
        )

    def test_fixture_passes_only_with_matching_external_data(self):
        self.assertEqual(1, check(self.archive, self.evidence, self.kotlin))

    def test_usda_energy_mismatch_is_detected(self):
        self.write_sources(actual_energy=101)
        with self.assertRaisesRegex(ValueError, "fixture kcal"):
            check(self.archive, self.evidence, self.kotlin)

    def test_fdcs_description_is_exactly_required(self):
        self.write_sources(actual_description="Sample, cooked")
        with self.assertRaisesRegex(ValueError, "description changed"):
            check(self.archive, self.evidence, self.kotlin)

    def test_kotlin_values_are_checked_separately_from_fixture(self):
        source = self.kotlin.read_text().replace("100.0,15.0", "130.0,15.0")
        self.kotlin.write_text(source, encoding="utf-8")
        with self.assertRaisesRegex(ValueError, "Kotlin kcal"):
            check(self.archive, self.evidence, self.kotlin)

    def test_undocumented_kotlin_source_is_rejected(self):
        source = self.kotlin.read_text().replace(
            "123/nutrients", "999/nutrients"
        )
        self.kotlin.write_text(source, encoding="utf-8")
        with self.assertRaisesRegex(ValueError, "different FDC ID"):
            check(self.archive, self.evidence, self.kotlin)

    def test_no_food_ingredient_parsed_is_an_error(self):
        with self.assertRaisesRegex(ValueError, "No FoodIngredient"):
            extract_kotlin_ingredients("val empty = listOf()")


if __name__ == "__main__":
    unittest.main()
