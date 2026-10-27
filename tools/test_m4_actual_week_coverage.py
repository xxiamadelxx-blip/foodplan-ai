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

    def test_retailer_units_are_visible_without_claiming_grams_or_full_budget(self):
        # Real recipe needs use RAW edible grams; the official products
        # sell eggs by pieces and oil by volume. These are not equivalent.
        mixed = dict(WEEK, ingredient_count=3, ingredients=[
            {"ingredient_id": "rice", "required_grams": 900,
             "pantry_used_grams": 0},
            {"ingredient_id": "egg", "required_grams": 300,
             "pantry_used_grams": 0},
            {"ingredient_id": "oil", "required_grams": 120,
             "pantry_used_grams": 0},
        ])
        egg = source("egg", 600, 8539)
        del egg["pack_grams"]
        egg.update(pack_unit="piece", pack_quantity=10,
                   purchase_count_from_grams_proven=False)
        oil = source("oil", 900, 12599)
        del oil["pack_grams"]
        oil.update(pack_unit="ml", pack_quantity=900,
                   purchase_count_from_grams_proven=False)
        quote = week_price_coverage(mixed, {
            "rice": source("rice", 800, 4999), "egg": egg, "oil": oil,
        }, 63218, NOW)
        self.assertEqual(3, quote["observed_pack_price_count"])
        self.assertEqual(1, quote["priced_ingredient_count"])
        self.assertEqual(["egg", "oil"], quote["unit_conversion_unknown_ids"])
        self.assertEqual(9998, quote["known_subtotal_kopeks"])
        self.assertIsNone(quote["full_week_total_kopeks"])
        self.assertIsNone(quote["full_week_within_budget"])
        for row in quote["lines"][1:]:
            self.assertEqual("PURCHASE_UNIT_CONVERSION_UNKNOWN", row["status"])
            self.assertIsNone(row["estimated_cost_kopeks"])
            self.assertIsNone(row["packages"])
            self.assertGreater(row["price_per_package_kopeks"], 0)
        self.assertEqual("piece", quote["lines"][1]["retailer_pack_unit"])
        self.assertEqual("ml", quote["lines"][2]["retailer_pack_unit"])

    def test_unit_evidence_must_not_be_mislabeled_as_grams(self):
        mixed = dict(WEEK, ingredient_count=3, ingredients=[
            {"ingredient_id": "rice", "required_grams": 900,
             "pantry_used_grams": 0},
            {"ingredient_id": "egg", "required_grams": 300,
             "pantry_used_grams": 0},
            {"ingredient_id": "oil", "required_grams": 120,
             "pantry_used_grams": 0},
        ])
        egg = source("egg", 600, 8539)
        egg.update(pack_unit="piece", pack_quantity=10,
                   purchase_count_from_grams_proven=False)
        oil = source("oil", 900, 12599)
        del oil["pack_grams"]
        oil.update(pack_unit="litre", pack_quantity=1,
                   purchase_count_from_grams_proven=False)
        q = week_price_coverage(mixed, {
            "rice": source("rice", 800, 4999), "egg": egg, "oil": oil,
        }, 100000, NOW)
        self.assertEqual(1, q["priced_ingredient_count"])
        self.assertEqual(1, q["observed_pack_price_count"])
        self.assertIsNone(q["full_week_total_kopeks"])

    def test_unsupported_conversion_proof_must_not_self_certify(self):
        mixed = dict(WEEK, ingredient_count=3, ingredients=[
            {"ingredient_id": "rice", "required_grams": 900,
             "pantry_used_grams": 0},
            {"ingredient_id": "egg", "required_grams": 300,
             "pantry_used_grams": 0},
            {"ingredient_id": "oil", "required_grams": 120,
             "pantry_used_grams": 0},
        ])
        egg = source("egg", 600, 8539)
        del egg["pack_grams"]
        egg.update(pack_unit="piece", pack_quantity=10,
                   purchase_count_from_grams_proven=True,
                   edible_grams_per_piece=60)
        q = week_price_coverage(mixed, {
            "rice": source("rice", 800, 4999), "egg": egg,
        }, 356, NOW)
        self.assertEqual(1, q["priced_ingredient_count"])
        self.assertEqual(0, len(q["unit_conversion_unknown_ids"]))
        self.assertIn("egg", q["missing_ingredient_ids"])

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

    def test_peel_and_brine_packages_have_prices_but_unknown_edible_yield(self):
        mixed = dict(WEEK, ingredient_count=3, ingredients=[
            {"ingredient_id": "rice", "required_grams": 900,
             "pantry_used_grams": 0},
            {"ingredient_id": "banana", "required_grams": 210,
             "pantry_used_grams": 0},
            {"ingredient_id": "chickpeas", "required_grams": 250,
             "pantry_used_grams": 0},
        ])
        banana = source("banana", 1000, 14999)
        del banana["pack_grams"]
        banana.update(pack_unit="unpeeled_g", pack_quantity=1000,
                      purchase_count_from_grams_proven=False)
        chickpeas = source("chickpeas", 400, 5999)
        del chickpeas["pack_grams"]
        chickpeas.update(pack_unit="net_with_liquid_g", pack_quantity=400,
                         purchase_count_from_grams_proven=False)
        actual = week_price_coverage(mixed, {
            "rice": source("rice", 800, 4999),
            "banana": banana, "chickpeas": chickpeas
        }, 63218, NOW)
        self.assertEqual(3, actual["observed_pack_price_count"])
        self.assertEqual(1, actual["priced_ingredient_count"])
        self.assertEqual(["banana", "chickpeas"],
                         actual["unit_conversion_unknown_ids"])
        self.assertIsNone(actual["full_week_total_kopeks"])
        self.assertIsNone(actual["full_week_within_budget"])
        self.assertEqual([None, None], [
            actual["lines"][1]["packages"], actual["lines"][2]["packages"]])
        self.assertEqual([14999, 5999], [
            actual["lines"][1]["price_per_package_kopeks"],
            actual["lines"][2]["price_per_package_kopeks"]])

        # A 400 g canned chickpea pack is NOT 400 g drained chickpeas.
        bogus = dict(chickpeas, pack_grams=400)
        wrong = week_price_coverage(mixed, {"chickpeas": bogus}, 100000, NOW)
        self.assertNotIn("chickpeas", [
            row["ingredient_id"] for row in wrong["lines"]
            if row["status"] == "PUBLIC_CHAIN_PRICE_OBSERVED"
        ])
        wrong_qty = dict(banana, pack_quantity=1500)
        unsafe = week_price_coverage(mixed, {"banana": wrong_qty}, 100000, NOW)
        self.assertEqual("PRICE_UNKNOWN", unsafe["lines"][1]["status"])


if __name__ == "__main__":
    unittest.main()
