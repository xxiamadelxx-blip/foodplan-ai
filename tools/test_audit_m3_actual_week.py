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
                "Synthetic meal", "1.0", "300.0", "rice:100|chicken:150"
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
