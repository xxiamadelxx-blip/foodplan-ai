"""Offline negatives for the independent M4 public-chain price gate."""
import unittest
from m4_live_chain_price_probe import parse_public_sku, probe, ROOT, SKU, BUCKWHEAT_SKU

HTML = """<html><body>
<section>49,99 ₽ unrelated banner</section>
<h1>Рис длиннозерный 800г</h1>
<del>159,99 ₽</del>
<p>49,99 ₽</p>
<h2>Похожие</h2>
<p>999,99 ₽</p>
</body></html>""".encode("utf-8")


class PriceProbeTest(unittest.TestCase):
    def test_public_price_from_main_sku_heading_not_banner_old_price_or_related(self):
        result = parse_public_sku(HTML)
        self.assertEqual(4999, result["visible_price_kopeks"])
        self.assertEqual(800, result["pack_grams"])
        self.assertFalse(result["checkout_price_verified"])
        self.assertEqual("APPROXIMATE_CHAIN_PUBLIC_PRICE", result["price_kind"])

    def test_second_real_chain_sku_parses_buckwheat_separately(self):
        html = """<h1>Крупа Мистраль Гречневая ядрица 900г</h1>
        <p>129,99 ₽</p><h2>Похожие</h2>
        <p>999,99 ₽</p>""".encode("utf-8")
        price = parse_public_sku(html, requested_url=BUCKWHEAT_SKU)
        self.assertEqual(12999, price["visible_price_kopeks"])
        self.assertEqual(900, price["pack_grams"])
        with self.assertRaisesRegex(ValueError, "SKU_TITLE_OR_PACK_MISMATCH"):
            parse_public_sku(HTML, requested_url=BUCKWHEAT_SKU)

    def test_kilogram_product_matches_exact_2000_gram_package(self):
        from m4_live_chain_price_probe import PRODUCTS
        page = """<h1>Мука Пшеничная 2кг в ассортименте</h1>
        <p>54,99 ₽</p><h2>Похожие</h2><p>42,99 ₽</p>""".encode()
        price = parse_public_sku(page, requested_url=PRODUCTS["flour"]["url"])
        self.assertEqual(2000, price["pack_grams"])
        self.assertEqual(5499, price["visible_price_kopeks"])
        wrong = page.replace("2кг".encode(), "1кг".encode())
        with self.assertRaisesRegex(ValueError, "SKU_TITLE_OR_PACK_MISMATCH"):
            parse_public_sku(wrong, requested_url=PRODUCTS["flour"]["url"])

    def test_missing_price_never_inherits_unrelated_recommendation(self):
        html = """<h1>Рис длиннозерный 800г</h1><h2>Похожие</h2><p>999,99 ₽</p>""".encode()
        # A "similar" product right after the title is not a price.
        with self.assertRaisesRegex(ValueError, "VISIBLE_CURRENT_PRICE_NOT_FOUND"):
            parse_public_sku(html)

    def test_wrong_title_cannot_borrow_price(self):
        with self.assertRaisesRegex(ValueError, "SKU_TITLE_OR_PACK_MISMATCH"):
            parse_public_sku("""<h1>Рис длиннозерный 400г</h1><p>49,99 ₽</p>""".encode())
        with self.assertRaisesRegex(ValueError, "UNRELATED_PRODUCT"):
            parse_public_sku("""<h1>Гречка длиннозерная 800г</h1><p>49,99 ₽</p>""".encode())

    def test_challenge_document_is_not_product_price(self):
        with self.assertRaisesRegex(ValueError, "ACCESS_GATE"):
            parse_public_sku(HTML.replace(b"</body>", b"captcha</body>"))

    def test_no_access_no_fake_quote(self):
        calls = []
        def blocked(url):
            calls.append(url)
            if url.endswith("/robots.txt"):
                return b"User-agent: *\nDisallow: /product/\n"
            raise AssertionError("Disallowed SKU must not be accessed")
        result = probe(blocked)
        self.assertEqual("NOT_VERIFIED", result["status"])
        self.assertIn("ROBOTS_DENIED", result["reason"])
        self.assertEqual([ROOT + "/robots.txt"], calls)
        self.assertNotIn("visible_price_kopeks", result)

    def test_permitted_public_sku_can_be_read_once(self):
        calls = []
        def allowed(url):
            calls.append(url)
            if url.endswith("robots.txt"):
                return b"User-agent: *\nAllow: /product/\n"
            if url == SKU:
                return HTML
            raise AssertionError("Unexpected URL")
        result = probe(allowed)
        self.assertEqual("VERIFIED_PUBLIC_CHAIN_PRICE", result["status"])
        self.assertEqual(4999, result["visible_price_kopeks"])
        self.assertEqual([ROOT + "/robots.txt", SKU], calls)


if __name__ == "__main__":
    unittest.main()
