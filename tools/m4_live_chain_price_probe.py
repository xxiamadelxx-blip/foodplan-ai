#!/usr/bin/env python3
"""One normal GET of a public Magnit product page; NEVER claims local stock.

This independent source gate is not a scraper or hidden API client. It checks
robots.txt first, performs a single allowed GET without redirecting, and
records a price ONLY when title, mass and an exact visible post-heading price
all appear on the same official SKU page. A CI source-access failure is FAIL,
not a synthetic price. Nothing about the user's address or profile is sent.
"""
import argparse
from datetime import datetime, timezone
import hashlib
from html.parser import HTMLParser
import json
from pathlib import Path
import re
from urllib import robotparser
from urllib.error import HTTPError, URLError
from urllib.request import Request, build_opener, HTTPRedirectHandler
from urllib.parse import urlsplit

ROOT = "https://magnit.ru"
SKU = ROOT + "/product/3152910005-ris_dlinnozernyy_2_sort_800g_6"
BUCKWHEAT_SKU = ROOT + "/product/1342500014-grechka_yadritsa_mistral_900g"
PRODUCTS = {
    "rice": {"url": SKU, "terms": ("рис", "длиннозер"), "grams": 800},
    "buckwheat": {
        "url": BUCKWHEAT_SKU,
        "terms": ("крупа", "гречнев"),
        "grams": 900
    },
    "oats": {
        "url": ROOT + "/product/3284010204-moya_tsena_krupa_gerkules_fas_400g_6",
        "terms": ("хлопья", "овсян"), "grams": 400
    },
    "millet": {
        "url": ROOT + "/product/3139480009-krupa_psheno_1s_fas_800g_6",
        "terms": ("пшено",), "grams": 800
    },
    "lentils": {
        "url": ROOT + "/product/1000567452-premiere_of_taste_chechevitsa_krasnaya_kolot_0_45kg_p_up",
        "terms": ("чечевица",), "grams": 450
    },
    "flour": {
        "url": ROOT + "/product/1042229921-muka_pshenichnaya_khlebopekarnaya_2kg",
        "terms": ("мука", "пшенич"), "grams": 2000
    },
    "pasta": {
        "url": ROOT + "/product/1319600015-makfa_makarony_rozhki_v_s_450g_20",
        "terms": ("макароны",), "grams": 450
    },
    "chicken": {
        "url": ROOT + "/product/1000233462-file_tsb_okhl_lotok_1_kg_v_lotok_ooo_soyuzptitseprom_5",
        "terms": ("филе", "цыпленка"), "grams": 1000
    },
    "carrot": {
        "url": ROOT + "/product/1445100176-morkov_mytaya_fasovannaya_1upak",
        "terms": ("морковь",), "grams": 1000
    },
    "cottage_cheese": {
        "url": ROOT + "/product/1000091079-korovka_iz_korenovki_tvorog_5_180g_fl_p_kmkk_12",
        "terms": ("творог", "5%"), "grams": 180
    },
    "cheese": {
        "url": ROOT + "/product/8000032665-moloka_voz_syr_gauda_gold_45_180g_flp_belsyr",
        "terms": ("сыр", "гауда"), "grams": 180
    },
    "mushroom": {
        "url": ROOT + "/product/9019013117-griby_shampinony_400g",
        "terms": ("шампиньоны",), "grams": 400
    },
    "potato": {
        "url": ROOT + "/product/9072651210-kartofel_1kg",
        "terms": ("картофель",), "grams": 2000
    },
    "cucumber": {
        "url": ROOT + "/product/3412110001-ogurtsy_gladkie",
        "terms": ("огурцы",), "grams": 500
    },
    "tomato": {
        "url": ROOT + "/product/1445100286-tomaty_600g",
        "terms": ("томаты",), "grams": 600
    },
    "broccoli": {
        "url": ROOT + "/product/1000348801-premiere_of_taste_kapusta_brokkoli_bystrozamorozhennaya_400g_polietilenovaya_upakovka_ooo_agama_roya",
        "terms": ("брокколи",), "grams": 400
    },
    "cod": {
        "url": ROOT + "/product/1000239574-treska_file_600g",
        "terms": ("треска", "филе"), "grams": 600
    },
    "tofu": {
        "url": ROOT + "/product/1000237883-syr_tofu_selskiy_dom_klassicheskiy_4_8_250g",
        "terms": ("тофу",), "grams": 250
    },
    "yogurt": {
        "url": ROOT + "/product/8000094915-solnyshko_kubani_yogurt_natur_2_5_160g_pl_st_molpromkuban",
        "terms": ("йогурт", "натуральн"), "grams": 160
    },
    "salmon": {
        "url": ROOT + "/product/1000128704-pf_iz_ryby_kusok_lososya_izgot_iz_morozh_syrya_neupakovan_sp_gm",
        "terms": ("лосос",), "grams": 600
    },
    "onion": {
        "url": ROOT + "/product/9072651204-luk_repchatyy",
        "terms": ("лук", "репчатый"), "grams": 700
    },
    "garlic": {
        "url": ROOT + "/product/3412060011-chesnok",
        "terms": ("чеснок",), "grams": 100
    },
    "cabbage": {
        "url": ROOT + "/product/9072651205-kapusta_belokochannaya_1kg",
        "terms": ("капуста", "белокочанная"), "grams": 3000
    },
    // Second bounded coverage pass for *real missing ingredients* from
    // 2026-10-22 actual-week report: 33 required, 22 previously priced.
    // Only official public SKU pages with observed name + packed mass.
    "apple": {
        "url": ROOT + "/product/9039215501-yabloki_novyy_urozhay",
        "terms": ("яблок",), "grams": 800
    },
    "turkey": {
        "url": ROOT + "/product/1000261026-indilayt_file_grudki_indeyki_okhl500g_lotok_penzamolinvest_4",
        "terms": ("филе", "индейк"), "grams": 500
    },
    "lemon": {
        "url": ROOT + "/product/9072651504-limony",
        "terms": ("лимон",), "grams": 400
    },
    "pear": {
        "url": ROOT + "/product/1000521260-grushi_golden",
        "terms": ("груш",), "grams": 700
    },
    "bell_pepper": {
        "url": ROOT + "/product/1444282021-perets_zhyeltyy_1kg",
        "terms": ("перец", "сладкий"), "grams": 300
    },
    "zucchini": {
        "url": ROOT + "/product/3412080003-kabachki",
        "terms": ("кабачк",), "grams": 700
    },
}
AGENT = "FoodPlanAI-public-price-check/0.2 (read-only)"
PRICE = re.compile(r"^(\d{1,6})(?:[.,](\d{1,2}))?\s*₽$")

MAX_HTML = 3_000_000


class RetailVisibleText(HTMLParser):
    def __init__(self):
        super().__init__(convert_charrefs=True)
        self.parts = []
        self.h1_end = None
        self.hidden_tags = []
        self.heading = False
        self.h1_parts = []

    def handle_starttag(self, tag, attrs):
        attributes = dict(attrs)
        hidden = tag in {"script", "style", "template", "del", "s", "strike"}
        hidden |= attributes.get("aria-hidden") == "true"
        hidden |= "display:none" in attributes.get("style", "").replace(" ", "").lower()
        if hidden:
            self.hidden_tags.append(tag)
        if tag == "h1" and not self.hidden_tags:
            self.heading = True

    def handle_endtag(self, tag):
        if tag == "h1" and self.heading:
            self.heading = False
            self.h1_end = len(self.parts)
        if tag in self.hidden_tags:
            self.hidden_tags.remove(tag)

    def handle_data(self, data):
        if self.hidden_tags or not data.strip():
            return
        value = " ".join(data.split())
        if self.heading:
            self.h1_parts.append(value)
        self.parts.append(value)


def parse_public_sku(html, requested_url=SKU):
    case = next((config for config in PRODUCTS.values()
                 if config["url"] == requested_url), None)
    if case is None:
        raise ValueError("UNLISTED_PRODUCT")
    if not isinstance(html, bytes) or len(html) > MAX_HTML or not html.strip():
        raise ValueError("EMPTY_OR_OVERSIZED")
    if urlsplit(requested_url).netloc != "magnit.ru" or not re.fullmatch(
        r"/product/[A-Za-z0-9_-]+", urlsplit(requested_url).path
    ):
        raise ValueError("INVALID_PRODUCT_URL")
    parser = RetailVisibleText()
    parser.feed(html.decode("utf-8", errors="strict"))
    title = " ".join(parser.h1_parts).strip()
    if not title or parser.h1_end is None:
        raise ValueError("SKU_TITLE_OR_PACK_MISMATCH")
    grams = case["grams"]
    # Accept exact unit in title, e.g. 2кг for 2000 grams.
    # No density or weight conversion for loose produce/liquids.
    mass_tokens = [str(grams) + r"\s*г"]
    if grams % 1000 == 0:
        mass_tokens.append(str(grams // 1000) + r"\s*кг")
    mass_pattern = re.compile(r"(?<![\d,.])(?:" + "|".join(mass_tokens) +
                              r")(?![а-я\d])", re.IGNORECASE)
    if not mass_pattern.search(title):
        raise ValueError("SKU_TITLE_OR_PACK_MISMATCH")
    lower = title.lower().replace("ё", "е")
    if not all(term in lower for term in case["terms"]):
        raise ValueError("UNRELATED_PRODUCT")
    all_text = " ".join(parser.parts).lower()
    if any(s in all_text for s in (
        "checking your browser", "access denied", "verify you are human",
        "подозрительная активность", "доступ ограничен", "captcha"
    )):
        raise ValueError("ACCESS_GATE")
    # Only a compact block after the unique main heading can supply price.
    # Do not take unrelated product prices from "Похожие".
    following = parser.parts[parser.h1_end:parser.h1_end + 18]
    for text in following:
        if text.casefold().strip() in {
            "похожие", "похожие товары", "вам может понравиться",
            "с этим товаром покупают", "рекомендуем", "рекомендации"
        }:
            break
        match = PRICE.fullmatch(text.replace("\u200a", " ").replace("\u202f", " "))
        if match:
            rub, kop = match.groups()
            kopeks = int(rub) * 100 + int((kop or "0").ljust(2, "0"))
            if 1 <= kopeks <= 10_000_000:
                return {
                    "product_url": requested_url,
                    "product_title": title,
                    "pack_grams": grams,
                    "visible_price_kopeks": kopeks,
                    "price_kind": "APPROXIMATE_CHAIN_PUBLIC_PRICE",
                    "checkout_price_verified": False,
                    "allergens_verified": False,
                }
    raise ValueError("VISIBLE_CURRENT_PRICE_NOT_FOUND")


def fetch_once(url):
    # The default redirect handler must not silently move to another SKU.
    class RefuseRedirect(HTTPRedirectHandler):
        def redirect_request(self, request, fp, code, msg, headers, newurl):
            raise ValueError("REDIRECT_REFUSED")
    opener = build_opener(RefuseRedirect())
    request = Request(url, headers={"User-Agent": AGENT})
    with opener.open(request, timeout=15) as response:
        if response.status != 200 or response.url != url:
            raise ValueError("UNEXPECTED_RESPONSE")
        raw = response.read(MAX_HTML + 1)
        if len(raw) > MAX_HTML:
            raise ValueError("OVERSIZED_RESPONSE")
        return raw


def probe(fetch=fetch_once, product="rice"):
    if product not in PRODUCTS:
        raise ValueError("UNLISTED_PRODUCT")
    sku = PRODUCTS[product]["url"]
    result = {
        "source": "MAGNIT_OFFICIAL_PUBLIC_PRODUCT_PAGE",
        "checked_at_utc": datetime.now(timezone.utc).isoformat(),
        "status": "NOT_VERIFIED",
        "source_url": sku,
        "ingredient_id": product,
        "price_kind": "APPROXIMATE_CHAIN_PUBLIC_PRICE",
        "checkout_price_verified": False,
        "address_required": False,
    }
    try:
        robots_bytes = fetch(ROOT + "/robots.txt")
        robots = robotparser.RobotFileParser()
        robots.parse(robots_bytes.decode("utf-8").splitlines())
        if not robots.can_fetch(AGENT, sku):
            raise ValueError("ROBOTS_DENIED")
        raw = fetch(sku)
        parsed = parse_public_sku(raw, requested_url=sku)
        result.update(parsed)
        result["source_sha256"] = hashlib.sha256(raw).hexdigest()
        result["status"] = "VERIFIED_PUBLIC_CHAIN_PRICE"
    except (ValueError, UnicodeError, OSError, HTTPError, URLError) as e:
        result["reason"] = type(e).__name__ + (
            ":" + str(e)[:70] if isinstance(e, ValueError) else ""
        )
    return result


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--output", required=True)
    ap.add_argument("--product", choices=sorted(PRODUCTS), default="rice")
    args = ap.parse_args()
    result = probe(product=args.product)
    target = Path(args.output)
    target.parent.mkdir(parents=True, exist_ok=True)
    target.write_text(json.dumps(result, ensure_ascii=False, indent=2) + "\n",
                      encoding="utf-8")
    print("M4 public chain price source:", result["status"],
          result.get("reason", ""),
          "cost_kopeks:", result.get("visible_price_kopeks", "UNKNOWN"))


if __name__ == "__main__":
    main()
