import csv
import io
import tempfile
import unittest
import zipfile
from pathlib import Path

from sr_legacy_candidates import find_candidates


def csv_bytes(rows):
    output=io.StringIO(newline="")
    writer=csv.DictWriter(output,fieldnames=list(rows[0]))
    writer.writeheader()
    writer.writerows(rows)
    return output.getvalue().encode("utf-8")


class SRLegacyCandidateTests(unittest.TestCase):
    def test_historic_dataset_is_distinct_and_never_auto_verified(self):
        with tempfile.TemporaryDirectory() as folder:
            archive=Path(folder) / "synthetic_sr.zip"
            with zipfile.ZipFile(archive,"w") as z:
                z.writestr("food.csv",csv_bytes([
                    {"fdc_id":"1001","description":"Carrots, raw"},
                    {"fdc_id":"1002","description":"Carrot soup, homemade"},
                    {"fdc_id":"1003","description":"Price controls for foods"}
                ]))
                z.writestr("food_nutrient.csv",csv_bytes([
                    {"fdc_id":"1001","nutrient_id":"1008","amount":"41"},
                    {"fdc_id":"1001","nutrient_id":"1003","amount":"1"},
                    {"fdc_id":"1001","nutrient_id":"1004","amount":"0.2"},
                    {"fdc_id":"1001","nutrient_id":"1005","amount":"9.6"},
                    {"fdc_id":"1002","nutrient_id":"1003","amount":"2"}
                ]))
            candidates=find_candidates(archive,"Carrots",limit=5)
            self.assertEqual(1,len(candidates))
            self.assertEqual(1001,candidates[0]["fdc_id"])
            self.assertEqual("Carrots, raw",candidates[0]["description"])
            self.assertEqual(41,candidates[0]["nutrition_per_100g"]["energy_kcal"])
            self.assertEqual("CANDIDATE_NOT_VERIFIED",candidates[0]["review_status"])
            self.assertFalse(candidates[0]["imported_as_app_food"])
            self.assertIn("2018-04",candidates[0]["archive_source"])

    def test_raw_only_avoids_juice_soups_and_potato_chips(self):
        with tempfile.TemporaryDirectory() as folder:
            archive=Path(folder) / "raw_match.zip"
            with zipfile.ZipFile(archive,"w") as z:
                z.writestr("food.csv",csv_bytes([
                    {"fdc_id":"2","description":"Carrot juice, canned"},
                    {"fdc_id":"3","description":"Carrots, raw"},
                    {"fdc_id":"4","description":"Carrot soup, cooked"}
                ]))
                rows=[]
                for n in ("2","3","4"):
                    for k,v in ((1008,40),(1003,1),(1004,0.3),(1005,8)):
                        rows.append({"fdc_id":n,"nutrient_id":k,"amount":v})
                z.writestr("food_nutrient.csv",csv_bytes(rows))
            raw=find_candidates(archive,"Carrot",limit=5,raw_only=True)
            self.assertEqual(1,len(raw))
            self.assertEqual("Carrots, raw",raw[0]["description"])

    def test_bad_searches_rejected(self):
        with self.assertRaises(ValueError):
            find_candidates(Path("nonexistent"),"x")
        with self.assertRaises(ValueError):
            find_candidates(Path("nonexistent"),"carrot",limit=200)
