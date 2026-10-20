#!/usr/bin/env python3
"""Validate ONE Russian producer's nutrition listing, without certifying a retail SKU.

The manufacturer's public webpage is not a dated package photo, proof of
allergen absence, or a match for another brand's cottage cheese / tvorog.
"""
import argparse
import hashlib
from html.parser import HTMLParser
import json
import re
from pathlib import Path

class VisibleText(HTMLParser):
    def __init__(self):
        super().__init__(convert_charrefs=True)
        self.parts = []
        self.hidden = 0
    def handle_starttag(self, tag, attrs):
        if tag in {"script","style"}:
            self.hidden += 1
    def handle_endtag(self, tag):
        if tag in {"script","style"}:
            self.hidden = max(0, self.hidden - 1)
    def handle_data(self, data):
        if not self.hidden and data.strip():
            self.parts.append(data.strip())

def normalized_text(html: str) -> str:
    parser = VisibleText()
    parser.feed(html)
    return re.sub(r"\s+", " ", " ".join(parser.parts))

def verify_page(html: str, evidence: dict) -> dict:
    if evidence.get("status") != "SOURCE_CANDIDATE_NOT_RETAIL_SKU_OR_ALLERGEN_CLEARANCE":
        raise ValueError("Manufacturer evidence is not safe to classify as a retail SKU")
    if evidence.get("allergen_reviewed") is not False:
        raise ValueError("A product HTML page cannot attest absence of allergen traces")
    if evidence.get("source_url") != "https://rossmol.ru/tvorog":
        raise ValueError("Unexpected manufacturer source")
    if (evidence.get("product_name") != "Творог 5%" or
        evidence.get("package_grams") != 350 or evidence.get("fat_percentage") != 5):
        raise ValueError("This verifier is only for ROSMOL 5% tvorog, 350g")
    text = normalized_text(html)
    blocks = []
    for m in re.finditer(r"Творог\s*5\s*%", text, flags=re.IGNORECASE):
        block=text[m.start():m.start()+900]
        if re.search(r"Масса нетто:\s*350\s*г",block[:350]):
            blocks.append(block)
    if not blocks:
        raise ValueError("The correct 5%, 350g package was not located on manufacturer page")
    expected = evidence["nutrition_per_100g"]
    if expected != {"energy_kcal":121,"protein_g":16,"fat_g":5,"carbs_g":3}:
        raise ValueError("Manufacturer pinned nutrition unexpectedly changed")
    rules = {
        "protein_g": r"Белки:\s*16[,.]0\s*г",
        "fat_g": r"Жиры:\s*5[,.]0\s*г",
        "carbs_g": r"Углеводы:\s*3[,.]0\s*г",
        "energy_kcal": r"Энергетическая ценность/калорийность:\s*[\d,.]+\s*кДж\s*/\s*121[,.]0\s*ккал",
    }
    for block in blocks:
        if (re.search(r"Жирность:\s*5\s*%",block) and
            "молоко нормализованное" in block and
            "закваска молочнокислых культур" in block and
            all(re.search(pattern,block) for pattern in rules.values())):
            return {
                "status": "PASS_PRODUCER_PUBLIC_PAGE_NUTRITION_ONLY",
                "source_url": evidence["source_url"],
                "product_name": "Творог 5% РОСМОЛ, 350 г",
                "per_100g": expected,
                "source_sha256": hashlib.sha256(html.encode("utf-8")).hexdigest(),
                "retail_sku_label_verified": False,
                "trace_allergen_disclosure_verified": False,
                "recipe_yield_verified": False,
            }
    raise ValueError("Composition / fat / nutrition facts mismatch for exact producer product")

def main():
    parser=argparse.ArgumentParser(description=__doc__)
    parser.add_argument("html_file",type=Path)
    parser.add_argument("--evidence",type=Path,default=Path("fixtures/m2_russian_tvorog_candidate.json"))
    parser.add_argument("--output",type=Path,default=Path("m2-ru-producer-review/tvorog-verification.json"))
    args=parser.parse_args()
    result=verify_page(args.html_file.read_text(encoding="utf-8"),
                       json.loads(args.evidence.read_text(encoding="utf-8")))
    args.output.parent.mkdir(parents=True,exist_ok=True)
    args.output.write_text(json.dumps(result,ensure_ascii=False,indent=2)+"\n",encoding="utf-8")
    print("PASS independent producer page HTML: 121 kcal; P 16g F 5g C 3g per 100g")
    print("NOT ACCEPTED: retail SKU, trace allergens or recipe yield")

if __name__=="__main__":
    main()
