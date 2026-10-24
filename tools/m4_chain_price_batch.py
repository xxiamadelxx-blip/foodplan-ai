#!/usr/bin/env python3
"""Bounded official-store price collection for M4.

One shared robots.txt GET across the run. If the host rejects requests with
403/429, immediately stop instead of hammering 33 SKUs. Each required product
still gets an explicit NOT_VERIFIED JSON row; the independent week gate must
remain UNKNOWN and the CI source availability gate must fail.
No request bypass, unofficial API, fallback vendor, synthetic value or cache.
"""
import argparse
from datetime import datetime, timezone
import json
from pathlib import Path
import time
from urllib.error import HTTPError, URLError

from m4_live_chain_price_probe import (
    AGENT, PRODUCTS, ROOT, fetch_once, probe,
)

ROBOT_URL = ROOT + "/robots.txt"
BLOCK_HTTP_CODES = frozenset((401, 403, 429))


def unknown(product_id, reason):
    return {
        "source": "MAGNIT_OFFICIAL_PUBLIC_PRODUCT_PAGE",
        "checked_at_utc": datetime.now(timezone.utc).isoformat(),
        "status": "NOT_VERIFIED",
        "source_url": PRODUCTS[product_id]["url"],
        "ingredient_id": product_id,
        "price_kind": "APPROXIMATE_CHAIN_PUBLIC_PRICE",
        "checkout_price_verified": False,
        "address_required": False,
        "reason": reason,
    }


def collect(products, output_dir, fetch=fetch_once, pause=time.sleep, delay=2.0):
    requested = tuple(products)
    if not requested or len(set(requested)) != len(requested):
        raise ValueError("INVALID_PRODUCT_BATCH")
    if any(p not in PRODUCTS for p in requested):
        raise ValueError("UNKNOWN_PRODUCT_IN_BATCH")
    if not 0 <= delay <= 60:
        raise ValueError("INVALID_REQUEST_DELAY")
    output_dir = Path(output_dir)
    output_dir.mkdir(parents=True, exist_ok=True)

    cached_robots = None
    host_blocked = None
    http_gets = 0

    def guarded_fetch(url):
        nonlocal cached_robots, host_blocked, http_gets
        if host_blocked:
            raise ValueError("HOST_ACCESS_ALREADY_BLOCKED")
        if url == ROBOT_URL and cached_robots is not None:
            return cached_robots
        http_gets += 1
        try:
            response = fetch(url)
        except HTTPError as err:
            # Exact 404 can be an isolated missing SKU; do not block other
            # products. Host-wide 401/403/429 ARE an access refusal.
            if err.code in BLOCK_HTTP_CODES:
                host_blocked = f"HOST_ACCESS_REFUSED_HTTP_{err.code}"
            raise
        except URLError:
            # Cannot establish robots policy; no SKU may be fetched.
            if url == ROBOT_URL:
                host_blocked = "ROBOTS_UNAVAILABLE"
            raise
        if url == ROBOT_URL:
            cached_robots = response
        return response

    records = {}
    for i, product_id in enumerate(requested):
        if host_blocked is not None:
            row = unknown(product_id, host_blocked)
        else:
            row = probe(fetch=guarded_fetch, product=product_id)
            if host_blocked is not None:
                # Never expose a verified price after the network access
                # circuit has entered the blocked state.
                row.pop("visible_price_kopeks", None)
                row.pop("pack_grams", None)
                row.pop("pack_unit", None)
                row.pop("pack_quantity", None)
                row.pop("purchase_count_from_grams_proven", None)
                row.pop("source_sha256", None)
                row["status"] = "NOT_VERIFIED"
                row["reason"] = host_blocked
        records[product_id] = row
        (output_dir / f"{product_id}.json").write_text(
            json.dumps(row, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
        if host_blocked is None and i < len(requested) - 1:
            pause(delay)

    summary = {
        "status": "HOST_ACCESS_BLOCKED" if host_blocked else
                  "COMPLETED_WITH_SOURCE_OBSERVATIONS",
        "checked_at_utc": datetime.now(timezone.utc).isoformat(),
        "requested_products": len(requested),
        "verified_product_prices": sum(
            row["status"] == "VERIFIED_PUBLIC_CHAIN_PRICE"
            for row in records.values()),
        "unknown_product_prices": sum(
            row["status"] != "VERIFIED_PUBLIC_CHAIN_PRICE"
            for row in records.values()),
        "robots_gets": int(cached_robots is not None),
        "network_gets": http_gets,
        "blocked_reason": host_blocked,
        "complete_week_price_approved": False,
        "no_product_price_fabricated": True,
        "checkout_price_verified": False,
    }
    (output_dir / "probe-summary.json").write_text(
        json.dumps(summary, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    return summary


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--output-dir", type=Path, required=True)
    parser.add_argument("--delay", type=float, default=2.0)
    opts = parser.parse_args()
    report = collect(tuple(PRODUCTS), opts.output_dir, delay=opts.delay)
    print("M4 public chain batch:", report["status"],
          "fresh verified prices", report["verified_product_prices"],
          "of", report["requested_products"],
          "source calls", report["network_gets"],
          "weekly total UNKNOWN unless downstream independent gate proves otherwise")


if __name__ == "__main__":
    main()
