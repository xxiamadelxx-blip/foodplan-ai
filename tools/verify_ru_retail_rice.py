#!/usr/bin/env python3
"""Verify retailer-public rice candidates without approving labels, prices or M2 recipes.

Run without --live for a strict, offline manifest gate.
Run with --live for an independent HTTP read of exact public product pages.
No bypass of access protections. A failed fetch is NOT a successful verification.
"""
import argparse
import hashlib
from html.parser import HTMLParser
import json
from pathlib import Path
import re
from urllib.request import Request, urlopen
from urllib.error import HTTPError

MANIFEST = Path("fixtures/m2_ru_retail_rice_candidates.json")
EXPECTED = {
    "54096": ("ROUND_GRAIN_POLISHED", 340, 8, 1, 72),
    "56104": ("LONG_GRAIN_PARBOILED", 350, 8, 1, 74),
    "56070": ("ROUND_GRAIN_PREMIXED_WITH_SUNFLOWER_OIL", 350, 7, 2, 74),
}
RETAIL_URL = "https://vkusvill.ru/goods/"


class VisibleText(HTMLParser):
    def __init__(self):
        super().__init__(convert_charrefs=True)
        self.parts = []
        self.hidden = 0

    def handle_starttag(self, tag, attrs):
        if tag in ("script", "style"):
            self.hidden += 1

    def handle_endtag(self, tag):
        if tag in ("script", "style"):
            self.hidden = max(0, self.hidden - 1)

    def handle_data(self, data):
        if not self.hidden and data.strip():
            self.parts.append(data)


def normalized(value):
    return re.sub(r"\s+", " ", value).casefold().replace("ё", "е").strip()


def visible_text(html_bytes):
    p = VisibleText()
    p.feed(html_bytes.decode("utf-8"))
    return normalized(" ".join(p.parts))




def classify_public_response(page_bytes, expected_title):
    """Distinguish missing item identity from temporary HTML without approving evidence."""
    if not isinstance(page_bytes, bytes) or not page_bytes.strip():
        return "EMPTY_RESPONSE"
    visible = visible_text(page_bytes)
    if normalized(expected_title) in visible:
        return "EXPECTED_PRODUCT_TITLE_PRESENT"
    restricted = (
        "access denied", "captcha", "verify you are human",
        "checking your browser", "request blocked",
        "доступ ограничен", "подозрительная активность",
        "проверка безопасности", "проверьте, что вы не робот",
    )
    if any(marker in visible for marker in restricted):
        return "ACCESS_GATE_NOT_PRODUCT_EVIDENCE"
    if ("пищевая и энергетическая ценность" in visible or
        "вес/объем" in visible or "вес / объем" in visible):
        return "PRODUCT_PAGE_IDENTITY_MISMATCH"
    return "NON_PRODUCT_HTML_OR_PLACEHOLDER"


def read_public_page_guarded(item, read_page, expected_title):
    """One retry for transport/transient neutral placeholder only.

    An access gate, explicit restriction, or different-product page is NEVER
    retried or counted as accepted evidence.
    """
    for attempt in range(2):
        try:
            raw = read_page(item)
        except HTTPError as exc:
            # 401/403/429 and all other explicit access restrictions must
            # not be retried. Only transient upstream 502/503/504 qualify.
            if attempt == 0 and exc.code in (502, 503, 504):
                continue
            raise
        except (OSError, TimeoutError, ConnectionError):
            if attempt == 0:
                continue
            raise
        if (attempt == 0 and classify_public_response(raw, expected_title)
                in {"EMPTY_RESPONSE", "NON_PRODUCT_HTML_OR_PLACEHOLDER"}):
            continue
        return raw
    raise ValueError("Public page was not returned")


def validate_manifest(data):
    if (data.get("schema_version") != 1 or
        data.get("source_kind") != "PUBLIC_RETAILER_PRODUCT_CARD_NOT_ACTUAL_PACKAGE_LABEL" or
        data.get("independent_source_snapshot_verified") is not False or
        data.get("retailer") != "ВкусВилл"):
        raise ValueError("Public-listing candidate status is not safely declared")
    rice = data["usda_comparator"]
    if (rice.get("fdc_id") != 2512381 or
        rice.get("ingredient_id") != "rice" or
        rice.get("kind") != "dry raw white long-grain rice"):
        raise ValueError("USDA comparator is not the pinned dry long-grain source")
    items = data["products"]
    if len(items) != len(EXPECTED) or {p["retailer_product_id"] for p in items} != set(EXPECTED):
        raise ValueError("Unexpected public retailer product IDs")
    for item in items:
        pid = item["retailer_product_id"]
        variant, kcal, protein, fat, carbs = EXPECTED[pid]
        if (item["variant"] != variant or
            item["net_grams"] != 800 or
            item["brand"] != "Увелка" or
            item["ingredient_id"] != "rice" or
            not item["url"].startswith(RETAIL_URL) or
            re.search(r"-" + re.escape(pid) + r"/$", item["url"]) is None):
            raise ValueError("Wrong product identity / source URL: " + pid)
        if item["nutrition_per_100g"] != {
            "kcal": kcal, "protein_g": protein, "fat_g": fat, "carbs_g": carbs
        }:
            raise ValueError("Source nutrient identity changed: " + pid)
        if (item["foodplan_reference_exact_match"] is not False or
            item["release_status"] != "PUBLIC_PAGE_CANDIDATE_ONLY" or
            item["package_label_sha256"] is not None or
            item["source_html_sha256"] is not None or
            item["gtin"] is not None or
            item["allergen_traces_verified"] is not False or
            item["observed_address_price_kopeks"] is not None or
            item["address_specific_availability"] != "UNKNOWN"):
            raise ValueError("Unverified listing was falsely promoted to full SKU evidence: " + pid)
        parts = [normalized(p) for p in item["composition"]]
        if len(parts) != (2 if pid == "56070" else 1):
            raise ValueError("Composition parts do not match the exact public card: " + pid)
        if item["compound_product"] != (pid == "56070"):
            raise ValueError("Compound-food flag wrong: " + pid)
        if "рис" not in parts[0] or (pid == "56070" and "масло подсолнечное" not in parts[1]):
            raise ValueError("Not the documented rice or rice+oil composition: " + pid)
    return items


def _macro_matches(section, name, expected):
    number = re.escape(str(expected)) + r"(?:[.,]0)?"
    return re.search(r"\b" + name + r"\s*[-:]?\s*" + number + r"\s*г\b", section) is not None


def verify_public_page(page_bytes, item):
    """Confirm identity, all four macros and ingredients on the same public page.

    Crucially, this deliberately DOES NOT verify actual package trace labels,
    physical stock, checkout price, GTIN, shelf date or food safety.
    """
    text = visible_text(page_bytes)
    if normalized(item["title"]) not in text:
        raise ValueError("Product title absent or wrong item ID: " + item["retailer_product_id"])
    start = text.find("пищевая и энергетическая ценность в 100 г")
    if start < 0:
        raise ValueError("No public nutritional panel")
    end = text.find("вес/объем", start)
    if end < 0 or end - start > 1100:
        raise ValueError("Nutrition section was incomplete or unexpectedly broad")
    nutrition = text[start:end]
    values = item["nutrition_per_100g"]
    if (re.search(r"\b" + str(values["kcal"]) + r"\s*ккал\b", nutrition) is None or
        not _macro_matches(nutrition, "белки", values["protein_g"]) or
        not _macro_matches(nutrition, "жиры", values["fat_g"]) or
        not _macro_matches(nutrition, "углеводы", values["carbs_g"])):
        raise ValueError("Product-card macros do not match pinned record")
    comp_start = text.find("состав", end)
    if comp_start < 0:
        raise ValueError("No public composition panel")
    composition = text[comp_start:comp_start+750]
    for part in item["composition"]:
        if normalized(part) not in composition:
            raise ValueError("Declared public composition mismatch: " + part)
    return {
        "retailer_product_id": item["retailer_product_id"],
        "url": item["url"],
        "status": "PUBLIC_PRODUCT_PAGE_NUTRIENTS_AND_COMPOSITION_MATCH_ONLY",
        "source_html_sha256": hashlib.sha256(page_bytes).hexdigest(),
        "exact_usda_ingredient_equivalence": False,
        "physical_package_label_verified": False,
        "allergen_trace_label_verified": False,
        "address_price_verified": False,
        "address_stock_verified": False
    }


def check_pages(items, read_page):
    """Audit every candidate, preserving negative evidence without failing open.

    A failed public fetch or mismatching page cannot be promoted to a package
    label, available stock, price or even a verified public product card.
    """
    checks = []
    for item in items:
        pid = item["retailer_product_id"]
        raw = None
        try:
            raw = read_public_page_guarded(item, read_page, item["title"])
            if len(raw) > 6_000_000:
                raise ValueError("Unexpectedly large product page")
            checks.append(verify_public_page(raw, item))
        except Exception as exc:
            # Do not save potentially private HTTP bodies or response headers.
            # The failed page content is not valid evidence.
            reason = str(exc) if isinstance(exc, ValueError) else type(exc).__name__
            checks.append({
                "retailer_product_id": pid,
                "url": item["url"],
                "status": "UNVERIFIED_PUBLIC_PAGE",
                "failure_reason": reason[:200],
                "response_classification": (
                    classify_public_response(raw, item["title"])
                    if isinstance(raw, bytes) else "TRANSPORT_OR_REQUEST_ERROR"
                ),
                "failed_response_sha256": hashlib.sha256(raw).hexdigest() if isinstance(raw, bytes) else None,
                "failed_response_byte_count": len(raw) if isinstance(raw, bytes) else None,
                "source_html_sha256": None,
                "exact_usda_ingredient_equivalence": False,
                "physical_package_label_verified": False,
                "allergen_trace_label_verified": False,
                "address_price_verified": False,
                "address_stock_verified": False
            })
    return checks


def fetch_live_page(item):
    req = Request(item["url"], headers={
        "User-Agent": "FoodPlanEvidenceAudit/0.1 (+public-read-only)"
    })
    with urlopen(req, timeout=20) as response:
        if response.status != 200:
            raise ValueError("Public listing HTTP status is not 200")
        return response.read(6_000_001)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--manifest", type=Path, default=MANIFEST)
    parser.add_argument("--live", action="store_true",
                        help="GET each official public page; fail instead of inventing access")
    parser.add_argument("--html-dir", type=Path,
                        help="Folder with independently captured {product_id}.html pages")
    parser.add_argument("--output", type=Path,
                        default=Path("m2-retail-rice-review/report.json"))
    args = parser.parse_args()
    if args.live and args.html_dir:
        parser.error("Choose only one source mode")
    items = validate_manifest(json.loads(args.manifest.read_text(encoding="utf-8")))
    if args.live:
        results = check_pages(items, fetch_live_page)
    elif args.html_dir:
        results = check_pages(items, lambda item:
            (args.html_dir / (item["retailer_product_id"] + ".html")).read_bytes())
    else:
        results = []
    passed = sum(i["status"] ==
                 "PUBLIC_PRODUCT_PAGE_NUTRIENTS_AND_COMPOSITION_MATCH_ONLY"
                 for i in results)
    result = {
        "manifest": "PASS_CANDIDATES_ONLY",
        "pages_attempted": len(results),
        "pages_verified": passed,
        "pages_failed_or_unverified": len(results) - passed,
        "items": results,
        "actual_package_labels_verified": 0,
        "complete_recipes_verified": 0,
        "retail_prices_or_stock_verified": 0,
        "release_gate": "FAIL_NOT_ENOUGH_EVIDENCE"
    }
    args.output.parent.mkdir(parents=True, exist_ok=True)
    args.output.write_text(json.dumps(result, ensure_ascii=False, indent=2) + "\n",
                           encoding="utf-8")
    print("PASS candidate identity guard: 3 distinct RU retailer product cards")
    print(f"Public product pages independently checked: {passed}/{len(results)}")
    print("M2 FAIL: no exact package labels, address prices, cooked yields or approved recipes")
    if results and passed != len(results):
        for item in results:
            if item["status"] == "UNVERIFIED_PUBLIC_PAGE":
                print("UNVERIFIED product", item["retailer_product_id"], ":",
                      item["failure_reason"], "[", item["response_classification"], "]")
        print("FAIL: one or more public product pages could not be independently verified")
        raise SystemExit(1)


if __name__ == "__main__":
    main()
