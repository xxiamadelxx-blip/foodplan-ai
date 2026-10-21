"""M4 no-AI public-search pilot: synthetic failures never produce verified offers."""
import json
import tempfile
from pathlib import Path
import unittest
from urllib.error import HTTPError

from m4_public_search_scan import (
    parse_search_page, query_registry, run_scan, search_url,
    valid_public_sku, verify_robots_policy, classify_search_response,
    is_plausible_raw_chicken_breast_title
)

ROBOTS = b"""User-agent: *
Disallow: /ajax/
Disallow: */-*
Disallow: */?*
Allow: /search/?q=*
Crawl-delay: 2
"""
PAGE = ("""<html><script><a href="/goods/fake-9999/">Fake</a></script>
<a href="/goods/file-grudki-tsyplenka-54321/">Филе грудки цыпленка, 450 г</a>
<a href="https://vkusvill.ru/goods/file-mini-12345/">Филе Мини, 300 г</a>
<a href="/goods/file-mini-12345/">Филе Мини, 300 г</a>
<a href="https://evil.test/goods/fake-6666/">Чужой сайт</a>
<a href="/goods/file-mini-12345/?promo=true">Промо с параметрами</a>
</html>""").encode("utf-8")


class M4NoAiDiscoveryTest(unittest.TestCase):
    def test_search_queries_come_from_existing_kotlin_registry(self):
        values = query_registry()
        self.assertEqual(37, len(values))
        self.assertEqual("куриное филе", values["chicken"])
        self.assertTrue(search_url(values["chicken"]).startswith(
            "https://vkusvill.ru/search/?q="))

    def test_robots_explicit_allow_and_delay_required(self):
        self.assertEqual(2.0, verify_robots_policy(ROBOTS))
        with self.assertRaises(ValueError):
            verify_robots_policy(ROBOTS.replace(b"Allow: /search/?q=*", b""))
        with self.assertRaises(ValueError):
            verify_robots_policy(ROBOTS.replace(
                b"Disallow: /ajax/", b"Disallow: /search/"))
        with self.assertRaises(ValueError):
            verify_robots_policy(ROBOTS.replace(b"Crawl-delay: 2",b"Crawl-delay: 90"))

    def test_only_official_product_urls_are_collected_and_deduplicated(self):
        items = parse_search_page(PAGE)
        self.assertEqual({"54321", "12345"},
                         {row["retailer_product_id"] for row in items})
        self.assertEqual(2,len(items))
        self.assertEqual("Филе Мини, 300 г", items[0]["public_title"])
        self.assertIsNone(valid_public_sku("https://evil.test/goods/file-2/"))
        self.assertIsNone(valid_public_sku("/goods/file-2/?coupon=1"))
        self.assertIsNone(valid_public_sku("/ajax/price-2/"))

    def test_scanner_only_emits_unconfirmed_candidate_refs_not_prices(self):
        urls=[]
        slept=[]
        def reader(url, route):
            urls.append((url,route))
            return ROBOTS if route=="robots" else PAGE
        result=run_scan({"chicken":"куриное филе"},reader,slept.append)
        self.assertEqual(1,result["queries_attempted"])
        self.assertTrue(result["all_catalog_queries_completed"])
        self.assertEqual(2,result["search_product_refs_discovered"])
        self.assertEqual([2.0], slept)
        self.assertEqual(["robots","search"],[r for _,r in urls])
        self.assertEqual(0,result["real_address_prices_verified"])
        self.assertEqual(0,result["physical_package_labels_verified"])
        self.assertTrue(result["m4_release_gate"].startswith("FAIL_"))
        for row in result["scans"][0]["products"]:
            self.assertIsNone(row["package_price_kopeks"])
            self.assertFalse(row["physical_label_verified"])
            self.assertFalse(row["allergen_traces_verified"])
            self.assertEqual("UNKNOWN",row["address_stock"])

    def test_real_chicken_titles_distinguish_raw_breast_from_cooked_and_thighs(self):
        # Identifiers and titles from run #39626906347 public search only.
        plausible = [
            "Филе грудки цыпленка-бройлера",
            "Стейк из филе грудки цыпленка-бройлера, 450 г",
            "Филе грудки цыпленка-бройлера на зерновом откорме, 700 г"
        ]
        wrong = [
            "Окорочок цыпленка-бройлера",
            "Бедро цыпленка-бройлера бескостное (филе бедра)",
            "Куриное филе запеченное с овощами, 210 г",
            "Филе куриной грудки запеченное, 160 г",
            "Куриное филе гриль, 120 г",
            "Филе грудки цыпленка-бройлера для запекания в маринаде, 750 г",
            "Филе куриной грудки с прованскими травами, 182 г",
            "Филе куриной грудки с перцем и чесноком, 182 г"
        ]
        for title in plausible:
            self.assertTrue(is_plausible_raw_chicken_breast_title(title), title)
        for title in wrong:
            self.assertFalse(is_plausible_raw_chicken_breast_title(title), title)

    def test_challenge_html_with_valid_product_links_cannot_pass_200_gate(self):
        blocked = b"<html><h1>Access denied CAPTCHA</h1>" + PAGE + b"</html>"
        parsed = parse_search_page(blocked)
        self.assertGreater(len(parsed), 0)
        self.assertEqual("ACCESS_GATE_NOT_SEARCH_EVIDENCE",
                         classify_search_response(blocked, parsed))
        result = run_scan({"chicken":"куриное филе", "rice":"рис"},
            lambda u, route: ROBOTS if route=="robots" else blocked, lambda _:None)
        self.assertEqual(1, result["queries_attempted"])
        self.assertEqual(0, result["search_product_refs_discovered"])
        self.assertEqual(0, result["unique_public_sku_ids_discovered"])
        self.assertFalse(result["all_catalog_queries_completed"])

    def test_chicken_discovery_never_creates_verified_composition_or_prices(self):
        result = run_scan({"chicken":"куриное филе"},
            lambda u, route: ROBOTS if route=="robots" else PAGE, lambda _:None)
        row = result["scans"][0]
        self.assertEqual(2, result["unique_public_sku_ids_discovered"])
        self.assertEqual(0, result["confirmed_ingredient_to_sku_equivalences"])
        self.assertEqual(0, result["real_address_prices_verified"])
        for sku in row["products"]:
            self.assertIn("title_only_raw_breast_candidate", sku)
            self.assertIsNone(sku["package_price_kopeks"])
            self.assertFalse(sku["composition_verified"])
            self.assertFalse(sku["physical_label_verified"])

    def test_http_forbidden_fails_pilot_without_retry(self):
        calls=[]
        def reader(url,route):
            calls.append(route)
            if route=="robots":
                return ROBOTS
            raise HTTPError(url,429,"Too Many Requests",{},None)
        result=run_scan({"chicken":"куриное филе","rice":"рис"},
                        reader,lambda _:None)
        self.assertEqual(["robots","search"],calls)
        self.assertEqual("PUBLIC_SEARCH_UNAVAILABLE",result["scans"][0]["status"])
        self.assertEqual("HTTP_429",result["scans"][0]["error_type"])
        self.assertEqual(1,result["queries_attempted"])
        self.assertFalse(result["all_catalog_queries_completed"])

    def test_access_gate_200_html_stops_scan_without_fabricating_skus(self):
        calls=[]
        def reader(url,route):
            calls.append(route)
            if route=="robots":
                return ROBOTS
            return b"<html><title>Access denied CAPTCHA</title></html>"
        result=run_scan({"chicken":"куриное филе","rice":"рис"},
                        reader,lambda _:None)
        self.assertEqual(["robots","search"],calls)
        self.assertEqual("ACCESS_GATE_NOT_SEARCH_EVIDENCE",result["scans"][0]["status"])
        self.assertEqual(0,result["search_product_refs_discovered"])
        self.assertFalse(result["all_catalog_queries_completed"])
        self.assertEqual(0,result["real_address_prices_verified"])

    def test_access_gate_after_pilot_stops_remaining_queries(self):
        calls=[]
        def reader(url,route):
            if route=="robots":
                return ROBOTS
            calls.append(url)
            if len(calls)==1:
                return PAGE
            return b"<html><title>Verify you are human</title></html>"
        result=run_scan({"chicken":"куриное филе","rice":"рис","oats":"овсяные хлопья"},
                        reader,lambda _:None)
        self.assertEqual(2,result["queries_attempted"])
        self.assertEqual(2,len(calls))
        self.assertEqual("ACCESS_GATE_NOT_SEARCH_EVIDENCE",result["scans"][1]["status"])
        self.assertFalse(result["all_catalog_queries_completed"])

    def test_full_scan_has_chicken_pilot_first_and_enforces_delay(self):
        order=[]
        sleeps=[]
        def reader(url,route):
            if route=="search":
                order.append(url)
                return PAGE
            return ROBOTS
        result=run_scan({"rice":"рис","chicken":"куриное филе","oats":"овсяные хлопья"},
                        reader,sleeps.append)
        self.assertEqual(3,result["queries_attempted"])
        self.assertEqual(3,len(sleeps))
        self.assertEqual(6,result["search_product_refs_discovered"])
        self.assertTrue(result["all_catalog_queries_completed"])
        self.assertIn("%D0%BA%D1%83%D1%80",order[0])
        self.assertTrue(all(delay>=2.0 for delay in sleeps))

    def test_output_keeps_hashes_not_html_or_fabricated_safety(self):
        with tempfile.TemporaryDirectory() as folder:
            file=Path(folder)/"out.json"
            run_scan({"chicken":"куриное филе"},
                     lambda u,route: ROBOTS if route=="robots" else PAGE,
                     lambda _:None,output=file)
            obj=json.loads(file.read_text(encoding="utf-8"))
            self.assertEqual(64,len(obj["scans"][0]["source_html_sha256"]))
            self.assertEqual(64,len(obj["robots_sha256"]))
            self.assertNotIn("<html>", file.read_text(encoding="utf-8"))


if __name__ == "__main__":
    unittest.main()
