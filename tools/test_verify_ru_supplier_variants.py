"""Synthetic regression tests for independent Russian multi-supplier evidence checker."""
import copy
import json
from pathlib import Path
import unittest

from verify_ru_supplier_variants import check_supplier_pages, validate_manifest, verify_public_page

PATH = Path("fixtures/m2_ru_multi_supplier_candidates.json")
KOTLIN = Path("android/domain/src/main/kotlin/com/foodplan/domain/RetailerVariantRiskRegistry.kt")


class SupplierAmbiguityTests(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.source = json.loads(PATH.read_text(encoding="utf-8"))
        cls.items = validate_manifest(cls.source, KOTLIN.read_text(encoding="utf-8"))

    @staticmethod
    def sample_page(item):
        rows = []
        for variant in item["variants"]:
            n = variant["nutrition_per_100g"]
            rows.append(
                variant["manufacturer_hint"] + ": белки " + str(n["protein_g"]) +
                " г, жиры " + str(n["fat_g"]) + " г, углеводы " +
                str(n["carbs_g"]) + " г; " + str(n["kcal"]) + " ккал"
            )
        traces = ""
        if item["retailer_product_id"] == "1248":
            traces = (
                "<h3>Аллергены по производителям</h3>"
                "Торговая Компания: Злаки, содержащие глютен; "
                "ЭНДАКСИ: Злаки, содержащие глютен, Орехи; "
                "ЭЛИТПАК: Злаки, содержащие глютен"
                "<h3>Условия хранения</h3>"
            )
        if item["retailer_product_id"] == "17699":
            traces = (
                '<h3>Аллергены по производителям</h3>'
                'Мельничный комплекс "Роса": Злаки, содержащие глютен; '
                'СЕЛТИНГ: Злаки, содержащие глютен; '
                'Торговая Компания: Злаки, содержащие глютен; '
                '<h3>Условия хранения</h3>'
            )
        return (
            "<html><script>allergens absent; zero calories</script>"
            "<h1>" + item["public_title"] + "</h1>"
            "<h3>Пищевая и энергетическая ценность в 100 г.</h3>"
            + "<p>".join(rows) +
            "<h3>Вес/объем</h3>400 г" + traces + "</html>"
        ).encode("utf-8")

    def test_manifest_keeps_supplier_unknown_and_has_conflicting_macros(self):
        self.assertEqual({"1248", "15544", "17699"},
                         {p["retailer_product_id"] for p in self.items})
        for p in self.items:
            self.assertIsNone(p["actual_manufacturer"])
            self.assertIsNone(p["photo_sha256"])
            self.assertFalse(p["exact_package_label_verified"])
            n={tuple(v["nutrition_per_100g"].values()) for v in p["variants"]}
            self.assertGreaterEqual(len(n),2)

    def test_each_synthetic_page_recovers_specific_supplier_lines(self):
        for p in self.items:
            with self.subTest(p=p["retailer_product_id"]):
                result = verify_public_page(self.sample_page(p),p)
                self.assertEqual(len(p["variants"]), result["checked_supplier_variants"])
                self.assertIsNone(result["selected_physical_supplier"])
                self.assertFalse(result["allergen_safe_or_trace_free"])

    def test_missing_nut_trace_and_wrong_supplier_macro_are_rejected(self):
        oats=self.items[0]
        page=self.sample_page(oats)
        for invalid in (
            page.replace("Орехи".encode(), "Без орехов".encode()),
            page.replace("361.4".encode(), "342".encode()),
            page.replace("торговая компания".encode(), "другая компания".encode())
        ):
            with self.assertRaises(ValueError):
                verify_public_page(invalid,oats)

    def test_buckwheat_independent_source_rejects_missing_supplier_or_trace(self):
        buckwheat = next(item for item in self.items
                         if item["retailer_product_id"] == "17699")
        raw=self.sample_page(buckwheat)
        for invalid in (
            raw.replace(b"335.4", b"399.9"),
            raw.replace("Мельничный комплекс".encode(),
                        "Посторонний завод".encode()),
            raw.replace("Злаки, содержащие глютен".encode(),
                        "Информация неизвестна".encode())
        ):
            with self.assertRaises(ValueError):
                verify_public_page(invalid,buckwheat)
        changed=copy.deepcopy(self.source)
        buck=next(item for item in changed["products"]
                 if item["retailer_product_id"] == "17699")
        buck["variants"][3]["declared_facility_traces"]=[]
        with self.assertRaises(ValueError):
            validate_manifest(changed)

    def test_fake_clearance_and_unknown_supplier_absence_fail(self):
        for key,value in (
            ("actual_manufacturer","ЭНДАКСИ"),
            ("photo_sha256","a"*64),
            ("exact_package_label_verified",True),
            ("address_price_kopeks",5400)
        ):
            d=copy.deepcopy(self.source)
            d["products"][0][key]=value
            with self.subTest(key=key),self.assertRaises(ValueError):
                validate_manifest(d)
        d=copy.deepcopy(self.source)
        d["products"][1]["variants"][1]["declared_facility_traces"]=[]
        with self.assertRaises(ValueError):
            validate_manifest(d)

    def test_supplier_failures_do_not_hide_other_supplier_checks(self):
        def reader(item):
            if item["retailer_product_id"] == "1248":
                return b"<html>Temporary unavailable retailer page</html>"
            if item["retailer_product_id"] == "17699":
                raise OSError("temporary DNS or site access failure")
            return self.sample_page(item)
        results = check_supplier_pages(self.items, reader)
        self.assertEqual(3, len(results))
        verified = [r for r in results
                    if r["status"] == "PASS_MULTI_SUPPLIER_AMBIGUITY_CONFIRMED"]
        failed = [r for r in results
                  if r["status"] == "UNVERIFIED_SUPPLIER_PUBLIC_PAGE"]
        self.assertEqual(["15544"], [r["id"] for r in verified])
        self.assertEqual({"1248", "17699"}, {r["id"] for r in failed})
        for row in failed:
            self.assertIsNone(row["public_page_sha256"])
            self.assertFalse(row["allergen_safe_or_trace_free"])
            self.assertFalse(row["retail_address_stock_or_price_confirmed"])


if __name__ == "__main__":
    unittest.main()
