import unittest

from m2_gap_manifest import inventory

STATUS = "SOURCE_VALUES_VERIFIED_NOT_RUSSIAN_SKU_OR_ALLERGEN_CLEARANCE"

def evidence(records):
    return {"status": STATUS, "foods": [
        {"ingredient_id": name, "fdc_id": fdc, "allergen_reviewed": False}
        for name, fdc in records
    ]}

class GapInventoryTest(unittest.TestCase):
    def test_count_is_driven_by_actual_catalogue_not_an_outdated_denominator(self):
        kotlin = ('FoodIngredient("a","A",10.0,0.0,0.0,0.0,100,\n'
                  ' nutrientSourceReference="https://fdc.nal.usda.gov/food-details/111/nutrients"),\n'
                  'FoodIngredient("b","B",10.0,0.0,0.0,0.0,100),\n')
        result = inventory(kotlin, [evidence([("a", 111)])])
        self.assertEqual(2, result["catalogue_ingredient_count"])
        self.assertEqual(1, result["pinned_usda_source_count"])
        self.assertEqual(["b"], result["unverified_ingredient_ids"])
        self.assertFalse(result["usda_nutrition_values_checked_by_this_script"])
        self.assertEqual(0, result["russian_sku_labels_approved"])

    def test_mismatched_food_id_or_duplicate_is_rejected(self):
        kotlin = ('FoodIngredient("rice","Rice",10.0,0.0,0.0,0.0,100,\n'
                  ' nutrientSourceReference="https://fdc.nal.usda.gov/food-details/111/nutrients")')
        with self.assertRaises(ValueError):
            inventory(kotlin, [evidence([("rice", 222)])])
        with self.assertRaises(ValueError):
            inventory(kotlin, [evidence([("rice", 111), ("rice", 111)])])

    def test_rejects_any_attempt_to_mark_usda_source_as_sku_label_approval(self):
        kotlin = 'FoodIngredient("rice","Rice",10.0,0.0,0.0,0.0,100)'
        bad = evidence([])
        bad["foods"] = [{"ingredient_id": "rice", "fdc_id": 111, "allergen_reviewed": True}]
        with self.assertRaises(ValueError):
            inventory(kotlin, [bad])

    def test_rejects_unrecognized_source_reference_even_when_evidence_is_empty(self):
        kotlin = ('FoodIngredient("rice","Rice",10.0,0.0,0.0,0.0,100,\n'
                  ' nutrientSourceReference="https://unknown/123")')
        with self.assertRaises(ValueError):
            inventory(kotlin, [evidence([])])

if __name__ == "__main__":
    unittest.main()
