import unittest
from datetime import datetime, timezone, timedelta
from m4_live_pilot_basket import quote_pilot

NOW = datetime(2026, 10, 22, 12, 0, tzinfo=timezone.utc)


def item(ingredient, grams, price, hours_ago=1):
    return {
        "status": "VERIFIED_PUBLIC_CHAIN_PRICE",
        "ingredient_id": ingredient,
        "price_kind": "APPROXIMATE_CHAIN_PUBLIC_PRICE",
        "checkout_price_verified": False,
        "address_required": False,
        "source_sha256": "fixture-only-123",
        "checked_at_utc": (NOW - timedelta(hours=hours_ago)).isoformat(),
        "pack_grams": grams,
        "visible_price_kopeks": price,
        "product_url": "https://magnit.ru/product/fixture_" + ingredient,
    }


class ChainBasketAuditTest(unittest.TestCase):
    def test_two_genuine_quote_shapes_use_whole_packages_and_accurate_budget(self):
        prices = {"rice": item("rice", 800, 4999),
                  "buckwheat": item("buckwheat", 900, 12999)}
        result = quote_pilot(prices, NOW)
        self.assertEqual("PASS_TWO_INGREDIENT_PILOT", result["status"])
        self.assertEqual(22997, result["two_item_total_kopeks"])
        self.assertEqual([2, 1], [x["packages"] for x in result["lines"]])
        self.assertFalse(result["budget_scenarios"]["100"]["two_item_cost_within_budget"])
        self.assertTrue(result["budget_scenarios"]["356"]["two_item_cost_within_budget"])
        self.assertTrue(result["not_full_7_day_basket"])
        self.assertFalse(result["physical_allergens_verified"])

    def test_missing_grocery_price_is_unknown_not_free(self):
        self.assertEqual("FAIL_MISSING_REAL_SOURCE",
            quote_pilot({"rice": item("rice", 800, 4999)}, NOW)["status"])

    def test_wrong_identity_never_priced(self):
        fake = item("rice", 800, 4999)
        fake["ingredient_id"] = "pasta"
        self.assertEqual("FAIL_UNVERIFIED_PRICE", quote_pilot(
            {"rice": fake, "buckwheat": item("buckwheat", 900, 12999)}, NOW)["status"])

    def test_stale_or_future_source_invalid(self):
        for age in (169, -1):
            prices = {"rice": item("rice", 800, 4999, hours_ago=age),
                      "buckwheat": item("buckwheat", 900, 12999)}
            self.assertEqual("FAIL_STALE_OR_INVALID",
                quote_pilot(prices, NOW)["status"])

    def test_no_address_checkout_claim_can_be_smuggled(self):
        rice = item("rice", 800, 4999)
        rice["checkout_price_verified"] = True
        price = {"rice": rice, "buckwheat": item("buckwheat", 900, 12999)}
        self.assertEqual("FAIL_UNVERIFIED_PRICE", quote_pilot(price, NOW)["status"])


if __name__ == "__main__":
    unittest.main()
