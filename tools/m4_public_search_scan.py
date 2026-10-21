#!/usr/bin/env python3
"""M4 read-only Russian grocery discovery. No AI, login, cookies or hidden APIs.

Uses only /search/?q= URLs when the retailer's robots.txt expressly allows
that route. Product-detail links are REFERENCES, never followed by the scanner.
Neither search prices nor snippets are address-specific offer evidence.
"""
import argparse
import hashlib
from html.parser import HTMLParser
import json
from pathlib import Path
import re
import time
from urllib.error import HTTPError, URLError
from urllib.parse import quote, urljoin, urlsplit
from urllib.request import Request, urlopen

DOMAIN = "vkusvill.ru"
BASE = "https://vkusvill.ru"
ROBOTS_URL = BASE + "/robots.txt"
USER_AGENT = "FoodPlanAI-public-search-audit/0.1 (read-only)"
SEARCH_ROUTE = "/search/?q="
QUERY_REGISTRY = Path(
    "android/domain/src/main/kotlin/com/foodplan/domain/NeutralRetailQueries.kt"
)
SKU_PATH = re.compile(r"^/goods/([a-z0-9-]+)-(\d+)/?$")


def query_registry(path=QUERY_REGISTRY):
    source = Path(path).read_text(encoding="utf-8")
    definitions = dict(re.findall(
        r'^\s*"([a-z_]+)"\s+to\s+\("[^"]+"\s+to\s+"([^"]+)"\),?\s*$',
        source, flags=re.MULTILINE
    ))
    if len(definitions) != 37 or "chicken" not in definitions:
        raise ValueError("NeutralRetailQueries is not exactly the supported 37-food catalog")
    if any(not id or not query or len(query) > 90 for id, query in definitions.items()):
        raise ValueError("Unsafe or empty neutral search query")
    return definitions


def search_url(query):
    if not query or len(query) > 90 or any(ord(ch) < 32 for ch in query):
        raise ValueError("Invalid anonymous grocery query")
    return BASE + SEARCH_ROUTE + quote(query, safe="")


def verify_robots_policy(raw):
    """A conservative allowlist, not an attempt to work around robots directives."""
    rules = raw.decode("utf-8-sig", errors="strict").replace("\r", "")
    allowed = False
    in_general_group = False
    for line in rules.splitlines():
        line = line.split("#", 1)[0].strip()
        if not line or ":" not in line:
            continue
        key, value = line.split(":", 1)
        key, value = key.casefold().strip(), value.strip()
        if key == "user-agent":
            # A second group begins at a subsequent User-agent after directives.
            if allowed and in_general_group and value.casefold() != "*":
                in_general_group = False
            elif not in_general_group:
                in_general_group = value.casefold() == "*"
        elif key == "allow" and value == "/search/?q=*" and in_general_group:
            allowed = True
        elif key == "disallow" and value in {"/search/", "/search/?q=*"} and in_general_group:
            raise ValueError("Search is explicitly forbidden by retailer robots")
    if not allowed:
        raise ValueError("No explicit robots allowance for public /search/?q= route")
    delay_match = re.search(r"^\s*Crawl-delay:\s*(\d+(?:\.\d+)?)\s*$",
                            rules, re.MULTILINE | re.IGNORECASE)
    delay = float(delay_match.group(1)) if delay_match else 2.0
    if delay > 60:
        raise ValueError("Retailer crawl-delay exceeds supported safe scan pace")
    return max(2.0, delay)


def valid_public_sku(href):
    if not isinstance(href, str) or not href:
        return None
    target = urlsplit(urljoin(BASE, href))
    if (target.scheme != "https" or target.netloc != DOMAIN or
        target.username is not None or target.password is not None or
        target.query or target.fragment):
        return None
    matched = SKU_PATH.fullmatch(target.path)
    if not matched:
        return None
    return {"retailer_product_id": matched.group(2),
            "public_url": BASE + target.path}


class SearchResultLinks(HTMLParser):
    """Collect real, visible product anchor text without using script JSON."""
    def __init__(self):
        super().__init__(convert_charrefs=True)
        self.stack = []
        self.hidden = 0
        self.results = []

    def handle_starttag(self, tag, attrs):
        params = dict(attrs)
        if tag in ("script", "style", "template"):
            self.hidden += 1
        if tag == "a" and not self.hidden:
            url = valid_public_sku(params.get("href"))
            self.stack.append([url, []] if url else None)

    def handle_data(self, text):
        if self.hidden or not text.strip():
            return
        if self.stack and self.stack[-1] is not None:
            self.stack[-1][1].append(text)

    def handle_endtag(self, tag):
        if tag == "a" and self.stack:
            entry = self.stack.pop()
            if entry is not None:
                url, text_parts = entry
                name = " ".join(" ".join(text_parts).split())
                if len(name) >= 5 and len(name) <= 250:
                    self.results.append({**url, "public_title": name})
        if tag in ("script", "style", "template"):
            self.hidden = max(0, self.hidden - 1)


def parse_search_page(page):
    if len(page) > 6_000_000:
        raise ValueError("Search response is unexpectedly large")
    try:
        text = page.decode("utf-8")
    except UnicodeDecodeError as exc:
        raise ValueError("Search page is not UTF-8") from exc
    parser = SearchResultLinks()
    parser.feed(text)
    distinct = {}
    for row in parser.results:
        key = row["retailer_product_id"]
        if key in distinct and distinct[key]["public_title"] != row["public_title"]:
            raise ValueError("Conflicting product names for one retailer ID")
        distinct[key] = row
    return [distinct[key] for key in sorted(distinct, key=int)]


def read_public(url, route):
    parsed = urlsplit(url)
    if (parsed.scheme != "https" or parsed.netloc != DOMAIN or
        (route == "robots" and (parsed.path != "/robots.txt" or parsed.query)) or
        (route == "search" and
         (parsed.path != "/search/" or not parsed.query.startswith("q=")))):
        raise ValueError("URL not on the strictly permitted official route")
    req = Request(url, headers={"User-Agent": USER_AGENT})
    with urlopen(req, timeout=20) as response:
        dest = urlsplit(response.geturl())
        if response.status != 200 or dest.netloc != DOMAIN or dest.path != parsed.path:
            raise ValueError("Not an official successful public route response")
        return response.read(6_000_001)


def run_scan(queries, read=read_public, sleeper=time.sleep, output=None):
    """Fail-closed public pilot. Two seconds or stricter robots delay per request."""
    robots = read(ROBOTS_URL, "robots")
    delay = verify_robots_policy(robots)
    scans = []
    # Pilot is mandatory before extending a run; no concurrent scraping.
    names = ["chicken"] + sorted(set(queries)-{"chicken"})
    for idx, ingredient in enumerate(names):
        query = queries[ingredient]
        url = search_url(query)
        sleeper(delay)  # robots.txt fetch itself counts towards crawl pacing.
        raw = None
        try:
            raw = read(url,"search")
            candidates = parse_search_page(raw)
            status = "DISCOVERED_PUBLIC_SEARCH_CANDIDATES" if candidates else "EMPTY_OR_NO_PARSEABLE_CARDS"
            records = candidates[:50]
        except (HTTPError, URLError, OSError, ValueError) as exc:
            # No access bypass, user-agent rotation or retries after HTTP refusal.
            status = "PUBLIC_SEARCH_UNAVAILABLE"
            records = []
            error_type = ("HTTP_" + str(exc.code) if isinstance(exc, HTTPError)
                          else type(exc).__name__)
        row = {
            "ingredient_id":ingredient, "neutral_query":query,
            "public_search_url":url, "status":status,
            "source_html_sha256":hashlib.sha256(raw).hexdigest() if raw else None,
            "observed_sku_count":len(records),
            "products": [{**item, "package_price_kopeks":None,
                          "address_stock":"UNKNOWN","composition_verified":False,
                          "allergen_traces_verified":False, "physical_label_verified":False}
                         for item in records],
            "candidate_only":True,
        }
        if status == "PUBLIC_SEARCH_UNAVAILABLE":
            row["error_type"] = error_type
        scans.append(row)
        if idx==0 and status != "DISCOVERED_PUBLIC_SEARCH_CANDIDATES":
            break  # A failed chicken pilot cannot be called a full scan.
    summary = {
        "source":"OFFICIAL_PUBLIC_SEARCH_ONLY", "robots_sha256":hashlib.sha256(robots).hexdigest(),
        "minimum_seconds_between_requests":delay,
        "all_catalog_queries_completed":len(scans)==len(queries) and all(
            row["status"] == "DISCOVERED_PUBLIC_SEARCH_CANDIDATES" for row in scans),
        "queries_attempted":len(scans),
        "search_product_refs_discovered":sum(x["observed_sku_count"] for x in scans),
        "real_address_prices_verified":0,
        "physical_package_labels_verified":0,
        "m4_release_gate":"FAIL_UNTIL_RETAILER_AND_PACKAGE_EVIDENCE_COMPLETE",
        "scans":scans
    }
    if output is not None:
        Path(output).parent.mkdir(parents=True,exist_ok=True)
        Path(output).write_text(json.dumps(summary,ensure_ascii=False,indent=2)+"\n",
                                encoding="utf-8")
    return summary


def main():
    parser=argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--full",action="store_true",
                        help="Scan all 37 queries after mandatory chicken pilot (respect crawl delay)")
    parser.add_argument("--output",type=Path,
                        default=Path("m4-public-search-review/candidate-discovery.json"))
    args=parser.parse_args()
    all_queries=query_registry()
    queries=all_queries if args.full else {"chicken":all_queries["chicken"]}
    results=run_scan(queries,output=args.output)
    first=results["scans"][0]
    print(f"M4 public search pilot {first['status']}; "
          f"visible SKU references {first['observed_sku_count']}")
    print("M4 release FAIL: source is not real retailer address stock, package identity or price.")
    if first["status"] != "DISCOVERED_PUBLIC_SEARCH_CANDIDATES":
        raise SystemExit(1)


if __name__=="__main__":
    main()
