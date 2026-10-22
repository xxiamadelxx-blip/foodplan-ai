"""Offline negatives for the independent M4 public-chain price gate."""
import unittest
from m4_live_chain_price_probe import parse_public_sku, probe, ROOT, SKU

HTML = b"""<html><body>
<section>49,99 â½ unrelated banner</section>
<h1>\xd0\xa0\xd0\xb8\xd1\x81 \xd0\xb4\xd0\xbb\xd0\xb8\xd0\xbd\xd0\xbd\xd0\xbe\xd0\xb7\xd0\xb5\xd1\x80\xd0\xbd\xd1\x8b\xd0\xb9 800\xd0\xb3</h1>
<del>159,99 â½</del>
<p>49,99 â½</p>
<h2>\xd0\x9f\xd0\xbe\xd1\x85\xd0\xbe\xd0\xb6\xd0\xb8\xd0\xb5</h2>
<p>999,99 â½</p>
</body></html>"""


class PriceProbeTest(unittest.TestCase):
    def test_public_price_from_main_sku_heading_not_banner_old_price_or_related(self):
        result = parse_public_sku(HTML)
        self.assertEqual(4999, result["visible_price_kopeks"])
        self.assertEqual(800, result["pack_grams"])
        self.assertFalse(result["checkout_price_verified"])
        self.assertEqual("APPROXIMATE_CHAIN_PUBLIC_PRICE", result["price_kind"])

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
