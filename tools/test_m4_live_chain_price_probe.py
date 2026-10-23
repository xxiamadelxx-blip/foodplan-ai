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

    def test_six_new_candidates_have_independent_exact_pack_and_title_gate(self):
        from m4_live_chain_price_probe import PRODUCTS
        cases = [
            ("apple", "Яблоки новый урожай 800г", 800),
            ("turkey", "Филе грудки индейки Индилайт 500г", 500),
            ("lemon", "Лимоны 400г", 400),
            ("pear", "Груши Голден 700г", 700),
            ("bell_pepper", "Перец сладкий желтый 300г", 300),
            ("zucchini", "Кабачки 700г", 700),
        ]
        for item, title, grams in cases:
            with self.subTest(item=item):
                html = ("<h1>" + title + "</h1>"
                        "<p>99,99 ₽</p><h2>Похожие</h2><p>19,99 ₽</p>"
                        ).encode("utf-8")
                proof = parse_public_sku(
                    html, requested_url=PRODUCTS[item]["url"])
                self.assertEqual(9999, proof["visible_price_kopeks"])
                self.assertEqual(grams, proof["pack_grams"])
                wrong = html.replace(f"{grams}г".encode(), b"10g")
                with self.assertRaisesRegex(
                    ValueError, "SKU_TITLE_OR_PACK_MISMATCH"
                ):
                    parse_public_sku(
                        wrong, requested_url=PRODUCTS[item]["url"])

    def test_three_more_realistic_raw_product_skus_require_exact_gram_pack(self):
        from m4_live_chain_price_probe import PRODUCTS
        verified_shapes = [
            ("beef", "Стейк Мираторг Black Angus Тендер мраморная говядина 260г", 260),
            ("milk", "Молоко Вкуснотеево ультрапастеризованное 3.2% 900г", 900),
            ("salmon", "Лосось стейк свежемороженый на коже Fish&More 400г", 400)
        ]
        for product_id, title, grams in verified_shapes:
            with self.subTest(product=product_id):
                url = PRODUCTS[product_id]["url"]
                price_document = (
                    "<h1>" + title + "</h1>"
                    "<p>189,99 ₽</p><del>249,99 ₽</del>"
                    "<h2>Похожие</h2><p>1,00 ₽</p>"
                ).encode("utf-8")
                price = parse_public_sku(price_document, requested_url=url)
                self.assertEqual(grams, price["pack_grams"])
                self.assertEqual(18999, price["visible_price_kopeks"])
                self.assertFalse(price["allergens_verified"])
                self.assertFalse(price["checkout_price_verified"])
                wrong_pack = price_document.replace(
                    f"{grams}г".encode("utf-8"), b"100ml"
                )
                with self.assertRaisesRegex(
                    ValueError, "SKU_TITLE_OR_PACK_MISMATCH"
                ):
                    parse_public_sku(wrong_pack, requested_url=url)
                unrelated = price_document.replace(
                    title.encode("utf-8"),
                    ("Пельмени с курицей " + str(grams) + "г").encode("utf-8")
                )
                with self.assertRaisesRegex(ValueError, "UNRELATED_PRODUCT"):
                    parse_public_sku(unrelated, requested_url=url)

    def test_egg_and_oil_are_not_gram_packaged_by_assumption(self):
        from m4_live_chain_price_probe import PRODUCTS
        self.assertEqual("piece", PRODUCTS["egg"]["unit"])
        self.assertEqual(10, PRODUCTS["egg"]["quantity"])
        self.assertEqual("ml", PRODUCTS["oil"]["unit"])
        self.assertEqual(900, PRODUCTS["oil"]["quantity"])
        for key in ("egg", "oil"):
            self.assertNotIn("grams", PRODUCTS[key])
        # Retail pack observations do NOT imply grams of edible egg or
        # a universally fixed millilitre-to-gram density for sunflower oil.

    def test_egg_carton_and_oil_bottle_prices_have_distinct_physical_units(self):
        from m4_live_chain_price_probe import PRODUCTS
        egg = parse_public_sku(
            "<h1>Куриное яйцо C1 10шт в ассортименте</h1>"
            "<p>85,39 ₽</p><h2>Похожие</h2>"
            "<p>5,99 ₽</p>".encode(),
            requested_url=PRODUCTS["egg"]["url"])
        self.assertEqual("piece", egg["pack_unit"])
        self.assertEqual(10, egg["pack_quantity"])
        self.assertEqual(8539, egg["visible_price_kopeks"])
        self.assertNotIn("pack_grams", egg)
        self.assertIs(egg["purchase_count_from_grams_proven"], False)
        oil = parse_public_sku(
            "<h1>Масло подсолнечное 900мл</h1><p>125,99 ₽</p>"
            "<h2>Похожие</h2><p>3,99 ₽</p>".encode(),
            requested_url=PRODUCTS["oil"]["url"])
        self.assertEqual("ml", oil["pack_unit"])
        self.assertEqual(900, oil["pack_quantity"])
        self.assertEqual(12599, oil["visible_price_kopeks"])
        self.assertNotIn("pack_grams", oil)

    def test_pieces_millilitres_and_grams_never_interchange(self):
        from m4_live_chain_price_probe import PRODUCTS
        egg_url = PRODUCTS["egg"]["url"]
        oil_url = PRODUCTS["oil"]["url"]
        for wrong in ["Куриное яйцо 10г", "Куриное яйцо 30шт",
                      "Куриное яйцо 10мл"]:
            with self.subTest(egg=wrong), self.assertRaisesRegex(
                ValueError, "SKU_TITLE_OR_PACK_MISMATCH"
            ):
                parse_public_sku(
                    f"<h1>{wrong}</h1><p>85,39 ₽</p>".encode(),
                    requested_url=egg_url)
        for wrong in ["Масло подсолнечное 900г",
                      "Масло подсолнечное 1л",
                      "Масло подсолнечное 450мл"]:
            with self.subTest(oil=wrong), self.assertRaisesRegex(
                ValueError, "SKU_TITLE_OR_PACK_MISMATCH"
            ):
                parse_public_sku(
                    f"<h1>{wrong}</h1><p>125,99 ₽</p>".encode(),
                    requested_url=oil_url)

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

    def test_gross_banana_and_canned_chickpeas_never_claim_edible_mass(self):
        from m4_live_chain_price_probe import PRODUCTS
        cases = [
            ("banana", "Бананы 1кг", 1000, "unpeeled_g"),
            ("chickpeas", "Нут Green Ribbon 400г", 400, "net_with_liquid_g"),
        ]
        for key, title, quantity, unit in cases:
            with self.subTest(key=key):
                url = PRODUCTS[key]["url"]
                page = (f"<h1>{title}</h1><p>59,99 ₽</p>"
                        "<h2>Похожие</h2><p>999,99 ₽</p>").encode("utf-8")
                found = parse_public_sku(page, requested_url=url)
                self.assertEqual(unit, found["pack_unit"])
                self.assertEqual(quantity, found["pack_quantity"])
                self.assertEqual(5999, found["visible_price_kopeks"])
                self.assertIs(found["purchase_count_from_grams_proven"], False)
                self.assertNotIn("pack_grams", found)
                wrong = page.replace(title.encode("utf-8"),
                    f"{title} 100г".encode("utf-8")).replace(
                    str(quantity).encode(), b"45", 1)
                with self.assertRaisesRegex(
                        ValueError, "SKU_TITLE_OR_PACK_MISMATCH"):
                    parse_public_sku(wrong, requested_url=url)
        # An unrelated canned-food gross weight cannot become drained mass.
        chickpea_url = PRODUCTS["chickpeas"]["url"]
        with self.assertRaisesRegex(ValueError, "UNRELATED_PRODUCT"):
            parse_public_sku(
                "<h1>Фасоль Green Ribbon 400г</h1><p>59,99 ₽</p>".encode(),
                requested_url=chickpea_url)


if __name__ == "__main__":
    unittest.main()
