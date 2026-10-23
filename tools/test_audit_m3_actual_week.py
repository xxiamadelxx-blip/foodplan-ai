import csv
import tempfile
import unittest
from pathlib import Path
from audit_m3_actual_week import audit, MEAL_COLUMNS, SHOPPING_COLUMNS


class ActualWeekEvidenceTest(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        self.meals = Path(self.temp.name) / "meals.csv"
        self.shopping = Path(self.temp.name) / "shopping.csv"
        self.m = [
            dict(zip(MEAL_COLUMNS, [
                str(day), str(slot), "fixture_" + str(day) + "_" + str(slot),
                "Synthetic meal", "1.0", "300.0", "25.0", "10.0",
                "26.0", "rice:100|chicken:150"
            ]))
            for day in range(7) for slot in range(3)
        ]
        self.s = [
            dict(zip(SHOPPING_COLUMNS, [
                ingredient, ingredient, str(grams), "0", str(grams),
                str(pack), str((grams + pack - 1) // pack), "UNKNOWN",
                "NOT_INGREDIENT_OR_ALLERGEN_CERTIFIED"
            ]))
            for ingredient, grams, pack in (
                ("rice", 2100, 800), ("chicken", 3150, 900)
            )
        ]

    def write(self):
        for file, cols, rows in (
            (self.meals, MEAL_COLUMNS, self.m),
            (self.shopping, SHOPPING_COLUMNS, self.s)
        ):
            with file.open("w", encoding="utf-8", newline="") as h:
                w = csv.DictWriter(h, fieldnames=cols)
                w.writeheader()
                w.writerows(rows)

    def test_reference_21_meal_aggregate(self):
        self.write()
        report = audit(self.meals, self.shopping)
        self.assertEqual("PASS_AGGREGATION_ONLY", report["status"])
        self.assertEqual(2, report["distinct_ingredient_count"])
        self.assertFalse(report["whole_week_chain_price_proven"])

    def test_seven_day_raw_macro_export_is_explicitly_not_certified(self):
        self.write()
        report = audit(self.meals, self.shopping)
        self.assertEqual(7, len(report["daily_preliminary_macros"]))
        self.assertEqual(6300.0, report["weekly_preliminary_energy_kcal"])
        self.assertEqual(900.0, sum(
            d["estimated_raw_energy_kcal"]
            for d in report["daily_preliminary_macros"][:1]))
        self.assertEqual(75.0, report["daily_preliminary_macros"][0]["estimated_raw_protein_g"])
        self.assertFalse(report["dietary_adequacy_verified"])
        self.assertFalse(report["cooked_yield_and_retention_verified"])
        self.assertIn("NOT_INDEPENDENTLY_VERIFIED", report["macronutrient_source_status"])
        self.assertEqual([], report["macro_energy_discrepancy_recipe_ids"])
        self.assertEqual(7, report["days_outside_adult_macro_reference"])
        self.assertEqual("ADULTS_ONLY",
            report["adult_macro_distribution_reference"]["population"])
        self.assertTrue(all(
            "CARBS_BELOW_ADULT_REFERENCE" in day["adult_reference_deviation_flags"]
            for day in report["daily_preliminary_macros"]))
        self.assertFalse(report["dietary_adequacy_verified"])

    def test_invalid_nonfinite_negative_and_missing_macro_rejected(self):
        for value in ("nan", "inf", "-1", "", "not-a-number"):
            with self.subTest(value=value):
                original = self.m[0]["estimated_raw_protein_g"]
                self.m[0]["estimated_raw_protein_g"] = value
                self.write()
                with self.assertRaisesRegex(ValueError, "raw macro"):
                    audit(self.meals, self.shopping)
                self.m[0]["estimated_raw_protein_g"] = original

    def test_extreme_inconsistent_macros_reported_not_silently_adjusted(self):
        self.m[0]["estimated_raw_fat_g"] = "400"
        self.write()
        report = audit(self.meals, self.shopping)
        self.assertIn("fixture_0_0", report["macro_energy_discrepancy_recipe_ids"])
        self.assertIn("FAT_ABOVE_ADULT_REFERENCE",
                      report["daily_preliminary_macros"][0]["adult_reference_deviation_flags"])
        self.assertEqual(300.0, report["daily_preliminary_macros"][0][
            "estimated_raw_energy_kcal"] / 3)
        self.assertFalse(report["dietary_adequacy_verified"])

    def test_hidden_unpriced_ingredient_rejected(self):
        self.s.pop()
        self.write()
        with self.assertRaisesRegex(ValueError, "union"):
            audit(self.meals, self.shopping)

    def test_fake_free_or_verified_price_rejected(self):
        self.s[0]["indicative_chain_price_status"] = "0 RUB CONFIRMED"
        self.write()
        with self.assertRaisesRegex(ValueError, "falsely certified"):
            audit(self.meals, self.shopping)

    def test_missing_real_day_slot_rejected(self):
        self.m.pop()
        self.write()
        with self.assertRaisesRegex(ValueError, "Expected 21"):
            audit(self.meals, self.shopping)

    def test_gram_or_pack_mismatch_rejected(self):
        self.s[1]["reference_pack_count"] = "1"
        self.write()
        with self.assertRaisesRegex(ValueError, "disagrees"):
            audit(self.meals, self.shopping)


if __name__ == "__main__":
    unittest.main()
