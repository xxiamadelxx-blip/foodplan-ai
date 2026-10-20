"""M2 evidence checks: independent unit regressions, NO fabricated live site access."""
import copy
import json
from pathlib import Path
import unittest

from verify_ru_retail_rice import validate_manifest, verify_public_page, visible_text

FIXTURE = Path("fixtures/m2_ru_retail_rice_candidates.json")


class RussianRetailEvidenceTests(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.fixture = json.loads(FIXTURE.read_text(encoding="utf-8"))
        cls.items = validate_manifest(cls.fixture)

    @staticmethod
    def sample_page(item):
        nutrition = item["nutrition_per_100g"]
        contents = ", ".join(item["composition"])
        return (
            "<html><script>Secret offer 0 rubles, allergens free</script>"
            "<h1>" + item["title"] + "</h1>"
            "<h3>Пищевая и энергетическая ценность в 100 г.</h3>"
            "<p>Калорийность " + str(nutrition["kcal"]) + " ккал "
            "Белки " + str(nutrition["protein_g"]) + " г "
            "Жиры " + str(nutrition["fat_g"]) + " г "
            "Углеводы " + str(nutrition["carbs_g"]) + " г</p>"
            "<h4>Вес/объем</h4><div>800 г</div>"
            "<h4>Состав</h4><p>" + contents + "</p>"
            "</html>"
        ).encode("utf-8")

    def test_cannot_promote_public_pages_to_package_or_stock_clearance(self):
        for item in self.items:
            self.assertIsNone(item["gtin"])
            self.assertIsNone(item["package_label_sha256"])
            self.assertIsNone(item["observed_address_price_kopeks"])
            self.assertFalse(item["allergen_traces_verified"])
            self.assertFalse(item["foodplan_reference_exact_match"])
        self.assertEqual(
            "PUBLIC_RETAILER_PRODUCT_CARD_NOT_ACTUAL_PACKAGE_LABEL",
            self.fixture["source_kind"]
        )

    def test_three_different_products_and_oil_containing_pilaf_variant(self):
        self.assertEqual({"54096", "56104", "56070"},
                         {item["retailer_product_id"] for item in self.items})
        oil = next(p for p in self.items if p["retailer_product_id"] == "56070")
        self.assertTrue(oil["compound_product"])
        self.assertEqual(2, len(oil["composition"]))
        self.assertIn("масло подсолнечное", oil["composition"][1])

    def test_fake_metadata_safety_clearance_is_refused(self):
        for key, value in (
            ("allergen_traces_verified", True),
            ("observed_address_price_kopeks", 10000),
            ("foodplan_reference_exact_match", True),
            ("package_label_sha256", "a" * 64),
            ("gtin", "4600000000000")
        ):
            data = copy.deepcopy(self.fixture)
            data["products"][0][key] = value
            with self.subTest(key=key):
                with self.assertRaises(ValueError):
                    validate_manifest(data)

    def test_public_page_fields_match_three_synthetic_html_samples(self):
        for item in self.items:
            with self.subTest(product=item["retailer_product_id"]):
                result = verify_public_page(self.sample_page(item), item)
                self.assertEqual(item["retailer_product_id"], result["retailer_product_id"])
                self.assertFalse(result["allergen_trace_label_verified"])
                self.assertFalse(result["address_price_verified"])
                self.assertFalse(result["exact_usda_ingredient_equivalence"])
                self.assertEqual(64, len(result["source_html_sha256"]))

    def test_wrong_nutrition_or_composition_does_not_pass(self):
        item = self.items[0]
        original = self.sample_page(item)
        for page in (
            original.replace(b"340", b"350"),
            original.replace(b"8 \xd0\xb3", b"4 \xd0\xb3"),
            original.replace("рис круглозерный шлифованный".encode(),
                             "рис длиннозерный пропаренный".encode())
        ):
            with self.assertRaises(ValueError):
                verify_public_page(page, item)

    def test_wrong_product_and_script_injection_not_evidence(self):
        first, second = self.items[0], self.items[1]
        with self.assertRaises(ValueError):
            verify_public_page(self.sample_page(first), second)
        visible = visible_text(self.sample_page(first))
        self.assertNotIn("secret offer", visible)


if __name__ == "__main__":
    unittest.main()
