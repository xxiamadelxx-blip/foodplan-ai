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
from urllib.request import Request, build_opener, HTTPHandler
from urllib.parse import urlsplit

ROOT = "https://magnit.ru"
SKU = ROOT + "/product/3152910005-ris_dlinnozernyy_2_sort_800g_6"
AGENT = "FoodPlanAI-public-price-check/0.2 (read-only)"
PRICE = re.compile(r"^(\\d{1,6})(?:[.,](\\d{1,2}))?\\s*₽$")
WEIGHT = re.compile(r"(?<!\\d)(800)\\s*г(?![а-я])", re.IGNORECASE)
MAX_HTML = 3_000_000


class RetailVisibleText(HTMLParser):
    def __init__(self):
        super().__init__(convert_charrefs=True)
        self.parts = []
        self.h1_end = None
        self.hidden = 0
        self.heading = False
        self.h1_parts = []

    def handle_starttag(self, tag, attrs):
        attributes = dict(attrs)
        hidden = tag in {"script", "style", "template", "del", "s", "strike"}
        hidden |= attributes.get("aria-hidden") == "true"
        hidden |= "display:none" in attributes.get("style", "").replace(" ", "").lower()
        if hidden:
            self.hidden += 1
        if tag == "h1" and not self.hidden:
            self.heading = True

    def handle_endtag(self, tag):
        if tag == "h1" and self.heading:
            self.heading = False
            self.h1_end = len(self.parts)
        if tag in {"script", "style", "template", "del", "s", "strike"}:
            self.hidden = max(0, self.hidden - 1)

    def handle_data(self, data):
        if self.hidden or not data.strip():
            return
        value = " ".join(data.split())
        if self.heading:
            self.h1_parts.append(value)
        self.parts.append(value)


def parse_public_sku(html, requested_url=SKU):
    if not isinstance(html, bytes) or len(html) > MAX_HTML or not html.strip():
        raise ValueError("EMPTY_OR_OVERSIZED")
    if urlsplit(requested_url).netloc != "magnit.ru" or not re.fullmatch(
        r"/product/[A-Za-z0-9_-]+", urlsplit(requested_url).path
    ):
        raise ValueError("INVALID_PRODUCT_URL")
    parser = RetailVisibleText()
    parser.feed(html.decode("utf-8", errors="strict"))
    title = " ".join(parser.h1_parts).strip()
    if not title or parser.h1_end is None or not WEIGHT.search(title):
        raise ValueError("SKU_TITLE_OR_PACK_MISMATCH")
    lower = title.lower().replace("ё", "е")
    if "рис" not in lower or "длиннозер" not in lower:
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
        match = PRICE.fullmatch(text.replace("\u200a", " ").replace("\u202f", " "))
        if match:
            rub, kop = match.groups()
            kopeks = int(rub) * 100 + int((kop or "0").ljust(2, "0"))
            if 1 <= kopeks <= 10_000_000:
                return {
                    "product_url": requested_url,
                    "product_title": title,
                    "pack_grams": 800,
                    "visible_price_kopeks": kopeks,
                    "price_kind": "APPROXIMATE_CHAIN_PUBLIC_PRICE",
                    "checkout_price_verified": False,
                    "allergens_verified": False,
                }
    raise ValueError("VISIBLE_CURRENT_PRICE_NOT_FOUND")


def fetch_once(url):
    class NoRedirect(HTTPHandler):
        pass
    opener = build_opener(NoRedirect)
    # The default redirect handler must not silently move to another SKU.
    from urllib.request import HTTPRedirectHandler
    class RefuseRedirect(HTTPRedirectHandler):
        def redirect_request(self, request, fp, code, msg, headers, newurl):
            raise ValueError("REDIRECT_REFUSED")
    opener.add_handler(RefuseRedirect())
    request = Request(url, headers={"User-Agent": AGENT})
    with opener.open(request, timeout=15) as response:
        if response.status != 200 or response.url != url:
            raise ValueError("UNEXPECTED_RESPONSE")
        raw = response.read(MAX_HTML + 1)
        if len(raw) > MAX_HTML:
            raise ValueError("OVERSIZED_RESPONSE")
        return raw


def probe(fetch=fetch_once):
    result = {
        "source": "MAGNIT_OFFICIAL_PUBLIC_PRODUCT_PAGE",
        "checked_at_utc": datetime.now(timezone.utc).isoformat(),
        "status": "NOT_VERIFIED",
        "source_url": SKU,
        "price_kind": "APPROXIMATE_CHAIN_PUBLIC_PRICE",
        "checkout_price_verified": False,
        "address_required": False,
    }
    try:
        robots_bytes = fetch(ROOT + "/robots.txt")
        robots = robotparser.RobotFileParser()
        robots.parse(robots_bytes.decode("utf-8").splitlines())
        if not robots.can_fetch(AGENT, SKU):
            raise ValueError("ROBOTS_DENIED")
        raw = fetch(SKU)
        parsed = parse_public_sku(raw)
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
    args = ap.parse_args()
    result = probe()
    target = Path(args.output)
    target.parent.mkdir(parents=True, exist_ok=True)
    target.write_text(json.dumps(result, ensure_ascii=False, indent=2) + "\n",
                      encoding="utf-8")
    print("M4 public chain price source:", result["status"],
          result.get("reason", ""),
          "cost_kopeks:", result.get("visible_price_kopeks", "UNKNOWN"))


if __name__ == "__main__":
    main()
