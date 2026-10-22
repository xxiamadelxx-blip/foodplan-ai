import unittest
from m2_targeted_review import select_foods, TARGET_PATTERNS

class TargetedReviewTest(unittest.TestCase):
    def test_rejects_related_dishes_for_raw_potato_and_whole_milk(self):
        rows = [
            {"fdc_id":"1", "description":"Potato chips, salted"},
            {"fdc_id":"2", "description":"Potatoes, flesh and skin, raw"},
            {"fdc_id":"3", "description":"Milk chocolate bar"},
            {"fdc_id":"4", "description":"Milk, whole, 3.25% milkfat, with added vitamin D"},
            {"fdc_id":"5", "description":"Pasta, dry, unenriched"},
            {"fdc_id":"6", "description":"Pasta, cooked, enriched"},
        ]
        chosen = select_foods(rows)
        self.assertEqual([2], [v["fdc_id"] for v in chosen["potato"]])
        self.assertEqual([4], [v["fdc_id"] for v in chosen["milk"]])
        self.assertEqual([5], [v["fdc_id"] for v in chosen["pasta"]])
        self.assertEqual(set(TARGET_PATTERNS), set(chosen))
    def test_limits_and_invalid_ids_fail_closed(self):
        rows = [
            {"fdc_id":"bad", "description":"Oil, sunflower"},
            {"fdc_id":"2", "description":"Oil, sunflower, linoleic"},
            {"fdc_id":"3", "description":"Oil, sunflower, high oleic"},
        ]
        self.assertEqual([2], [x["fdc_id"] for x in select_foods(rows,limit=1)["oil"]])
        with self.assertRaises(ValueError):
            select_foods(rows,limit=0)

if __name__=="__main__":
    unittest.main()
