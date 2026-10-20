import unittest
from verify_ru_tvorog import verify_page

SOURCE={
    "status":"SOURCE_CANDIDATE_NOT_RETAIL_SKU_OR_ALLERGEN_CLEARANCE",
    "allergen_reviewed":False,
    "source_url":"https://rossmol.ru/tvorog",
    "product_name":"Творог 5%",
    "package_grams":350,
    "fat_percentage":5,
    "nutrition_per_100g":{"energy_kcal":121,"protein_g":16,"fat_g":5,"carbs_g":3}
}
def page(grams="350",energy="121,0",percent="5"):
    return f"""<html><script>unsafe tokens</script><h2>Творог 5%</h2>
    <div>Масса нетто: {grams} г</div><p>Жирность: {percent}%</p>
    <p>молоко нормализованное, закваска молочнокислых культур</p>
    <p>Пищевая ценность в 100 г</p><p>Белки: 16,0 г</p>
    <p>Энергетическая ценность/калорийность: 508,0 кДж/ {energy} ккал</p>
    <p>Жиры: 5,0 г</p><p>Углеводы: 3,0 г</p></html>"""

class RussianProducerVerificationTests(unittest.TestCase):
    def test_exact_product_can_supply_only_source_provenance(self):
        record=verify_page(page(),SOURCE)
        self.assertEqual("PASS_PRODUCER_PUBLIC_PAGE_NUTRITION_ONLY",record["status"])
        self.assertFalse(record["retail_sku_label_verified"])
        self.assertFalse(record["recipe_yield_verified"])
    def test_wrong_fat_package_or_energy_is_rejected(self):
        for html in [page(grams="200"),page(percent="9"),page(energy="157,0")]:
            with self.assertRaises(ValueError):
                verify_page(html,SOURCE)
    def test_attempt_to_clear_allergens_is_rejected(self):
        with self.assertRaises(ValueError):
            verify_page(page(),{**SOURCE,"allergen_reviewed":True})

if __name__=="__main__":
    unittest.main()
