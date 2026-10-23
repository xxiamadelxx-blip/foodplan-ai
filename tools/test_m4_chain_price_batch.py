"""Independent offline Gauntlet for batch pricing: never bypass access gates."""
import json
from pathlib import Path
import tempfile
import unittest
from urllib.error import HTTPError

from m4_chain_price_batch import collect, ROBOT_URL
from m4_live_chain_price_probe import PRODUCTS

ALLOW = b"User-agent: *\nAllow: /product/\n"
DENY = b"User-agent: *\nDisallow: /product/\n"


def http_error(url, code):
    return HTTPError(url, code, "refused", {}, None)


class M4BatchAccessTests(unittest.TestCase):
    def setUp(self):
        self.folder = tempfile.TemporaryDirectory()
        self.addCleanup(self.folder.cleanup)
        self.path = Path(self.folder.name)

    def test_one_robots_fetch_for_two_independent_verified_skus(self):
        calls = []
        def allowed(url):
            calls.append(url)
            if url == ROBOT_URL:
                return ALLOW
            if url == PRODUCTS["rice"]["url"]:
                return "<h1>Рис длиннозерный 800г</h1><p>49,99 ₽</p>".encode()
            if url == PRODUCTS["buckwheat"]["url"]:
                return "<h1>Крупа Гречневая ядрица 900г</h1><p>129,99 ₽</p>".encode()
            raise AssertionError("Unexpected URL")
        summary = collect(("rice", "buckwheat"), self.path, fetch=allowed, delay=0)
        self.assertEqual(3, len(calls))
        self.assertEqual(1, calls.count(ROBOT_URL))
        self.assertEqual(2, summary["verified_product_prices"])
        self.assertEqual("COMPLETED_WITH_SOURCE_OBSERVATIONS", summary["status"])
        self.assertEqual(4999, json.loads((self.path / "rice.json").read_text())[
            "visible_price_kopeks"])
        self.assertEqual(12999, json.loads((self.path / "buckwheat.json").read_text())[
            "visible_price_kopeks"])
        self.assertIs(summary["complete_week_price_approved"], False)

    def test_blocked_robots_stops_all_product_fetches_and_writes_unknown(self):
        calls = []
        def unavailable(url):
            calls.append(url)
            raise http_error(url, 403)
        ids = ("rice", "egg", "oil", "buckwheat")
        report = collect(ids, self.path, fetch=unavailable, delay=0)
        self.assertEqual([ROBOT_URL], calls)
        self.assertEqual("HOST_ACCESS_BLOCKED", report["status"])
        self.assertEqual("HOST_ACCESS_REFUSED_HTTP_403", report["blocked_reason"])
        self.assertEqual(0, report["verified_product_prices"])
        self.assertEqual(4, report["unknown_product_prices"])
        self.assertEqual(1, report["network_gets"])
        for key in ids:
            row = json.loads((self.path / f"{key}.json").read_text())
            self.assertEqual("NOT_VERIFIED", row["status"])
            self.assertNotIn("visible_price_kopeks", row)
            self.assertNotIn("pack_grams", row)
            self.assertFalse(row["checkout_price_verified"])

    def test_sku_rate_limit_quarantines_following_skus(self):
        calls = []
        def rate_limited(url):
            calls.append(url)
            if url == ROBOT_URL:
                return ALLOW
            raise http_error(url, 429)
        report = collect(("rice", "egg", "oil"), self.path,
                         fetch=rate_limited, delay=0)
        self.assertEqual([ROBOT_URL, PRODUCTS["rice"]["url"]], calls)
        self.assertEqual("HOST_ACCESS_REFUSED_HTTP_429", report["blocked_reason"])
        self.assertEqual(2, report["network_gets"])
        self.assertEqual(3, report["unknown_product_prices"])
        self.assertEqual("HOST_ACCESS_REFUSED_HTTP_429",
                         json.loads((self.path / "oil.json").read_text())["reason"])

    def test_isolated_sku_404_does_not_claim_global_block(self):
        calls = []
        def listing_missing(url):
            calls.append(url)
            if url == ROBOT_URL:
                return ALLOW
            if url == PRODUCTS["rice"]["url"]:
                raise http_error(url, 404)
            if url == PRODUCTS["buckwheat"]["url"]:
                return "<h1>Крупа гречневая ядрица 900г</h1><p>119,99 ₽</p>".encode()
            raise AssertionError("Unexpected")
        report = collect(("rice", "buckwheat"), self.path,
                         fetch=listing_missing, delay=0)
        self.assertEqual(3, len(calls))
        self.assertEqual("COMPLETED_WITH_SOURCE_OBSERVATIONS", report["status"])
        self.assertEqual(1, report["verified_product_prices"])
        self.assertEqual("NOT_VERIFIED",
                         json.loads((self.path / "rice.json").read_text())["status"])
        self.assertEqual("VERIFIED_PUBLIC_CHAIN_PRICE",
                         json.loads((self.path / "buckwheat.json").read_text())["status"])

    def test_robots_denial_never_calls_sku(self):
        calls = []
        def blocked_policy(url):
            calls.append(url)
            if url == ROBOT_URL:
                return DENY
            raise AssertionError("No disallowed SKU may be requested")
        result = collect(("rice", "buckwheat"), self.path,
                         fetch=blocked_policy, delay=0)
        self.assertEqual([ROBOT_URL], calls)
        self.assertEqual(0, result["verified_product_prices"])
        self.assertEqual(2, result["unknown_product_prices"])

    def test_bad_batch_rejected_without_creating_network_observations(self):
        with self.assertRaisesRegex(ValueError, "INVALID_PRODUCT_BATCH"):
            collect(("rice", "rice"), self.path)
        with self.assertRaisesRegex(ValueError, "UNKNOWN_PRODUCT_IN_BATCH"):
            collect(("unlisted",), self.path)
        self.assertEqual([], list(self.path.glob("*.json")))


if __name__ == "__main__":
    unittest.main()
