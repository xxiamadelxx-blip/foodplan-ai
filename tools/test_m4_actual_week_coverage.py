"""Gauntlet negatives for coverage of a real planned FoodPlan shopping week."""
import unittest
from datetime import datetime, timezone, timedelta
from m4_actual_week_coverage import week_price_coverage, read_week

NOW = datetime(2026, 10, 22, 13, tzinfo=timezone.utc)
WEEK = {
    "schema_version": 1,
    "provenance": "PRODUCTION_SEVEN_DAY_PLANNER_SHOPPING_AGGREGATION",
    "days": 7, "meals": 21, "ingredient_count": 3,
    "ingredients": [
        {"ingredient_id": "rice", "required_grams": 900, "pantry_used_grams": 0},
        {"ingredient_id": "buckwheat", "required_grams": 850, "pantry_used_grams": 0},
        {"ingredient_id": "chicken", "required_grams": 600, "pantry_used_grams": 0},
    ],
}


def source(ingredient, mass, kopeks, age_hours=1):
    return {
        "ingredient_id": ingredient,
        "status": "VERIFIED_PUBLIC_CHAIN_PRICE",
        "price_kind": "APPROXIMATE_CHAIN_PUBLIC_PRICE",
        "checkout_price_verified": False, "address_required": False,
        "source_sha256": "a" * 64,
        "product_url": "https://magnit.ru/product/realsku_" + ingredient,
        "pack_grams": mass,
        "visible_price_kopeks": kopeks,
        "checked_at_utc": (NOW - timedelta(hours=age_hours)).isoformat()
    }


class WholeWeekPriceCoverageTest(unittest.TestCase):
    def test_partial_real_week_never_claims_zero_or_complete(self):
        result = week_price_coverage(WEEK, {
            "rice": source("rice", 800, 4999),
            "buckwheat": source("buckwheat", 900, 12999),
        }, 356, NOW)
        self.assertEqual(3, result["needed_ingredient_count"])
        self.assertEqual(2, result["priced_ingredient_count"])
        self.assertEqual(["chicken"], result["missing_ingredient_ids"])
        self.assertEqual(22997, result["known_subtotal_kopeks"])
        self.assertIsNone(result["full_week_total_kopeks"])
        self.assertIsNone(result["full_week_within_budget"])
        self.assertEqual("PARTIAL_FULL_WEEK_PRICE_UNKNOWN", result["status"])

    def test_incomplete_week_can_prove_budget_is_already_over(self):
        prices = {"rice": source("rice", 800, 4999),
                  "buckwheat": source("buckwheat", 900, 12999)}
        result = week_price_coverage(WEEK, prices, 100, NOW)
        self.assertTrue(result["known_subtotal_exceeds_budget"])
        self.assertIsNone(result["full_week_total_kopeks"])
        self.assertIsNone(result["full_week_within_budget"])
        plenty = week_price_coverage(WEEK, prices, 100000, NOW)
        self.assertFalse(plenty["known_subtotal_exceeds_budget"])
        self.assertIsNone(plenty["full_week_within_budget"])

    def test_complete_source_set_enables_full_quote(self):
        full = {x: source(x, mass, price) for x, mass, price in (
            ("rice", 800, 4999), ("buckwheat", 900, 12999),
            ("chicken", 500, 25000)
        )}
        result = week_price_coverage(WEEK, full, 356, NOW)
        self.assertEqual("PASS_FULL_WEEK_PUBLIC_PRICE", result["status"])
        self.assertEqual(72997, result["full_week_total_kopeks"])
        self.assertFalse(result["full_week_within_budget"])
        self.assertEqual(2, result["lines"][2]["packages"])

    def test_stale_and_wrong_origin_quotations_rejected(self):
        bad = source("rice", 800, 4999)
        bad["product_url"] = "https://magnit.ru.evil.test/product/fake"
        result = week_price_coverage(WEEK, {
            "rice": bad, "buckwheat": source("buckwheat", 900, 12999, 169),
        }, 63218, NOW)
        self.assertEqual(0, result["priced_ingredient_count"])
        self.assertIsNone(result["full_week_total_kopeks"])

    def test_rejects_fake_seven_day_export_and_duplicate_items(self):
        wrong = dict(WEEK, days=2)
        with self.assertRaises(ValueError):
            read_week(wrong)
        bad = dict(WEEK, ingredients=WEEK["ingredients"][:2] + [WEEK["ingredients"][0]])
        with self.assertRaises(ValueError):
            read_week(bad)
        with self.assertRaises(ValueError):
            week_price_coverage(WEEK, {}, 99, NOW)

    def test_no_product_with_bad_checksum_or_false_checkout_claim(self):
        row = source("rice", 800, 4999)
        row["source_sha256"] = "fiction"
        broken = week_price_coverage(WEEK, {"rice": row}, 100000, NOW)
        self.assertEqual(0, broken["priced_ingredient_count"])
        row["source_sha256"] = "b" * 64
        row["checkout_price_verified"] = True
        broken = week_price_coverage(WEEK, {"rice": row}, 100000, NOW)
        self.assertEqual(0, broken["priced_ingredient_count"])


if __name__ == "__main__":
    unittest.main()
