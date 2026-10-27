#!/usr/bin/env python3
"""Independently inspect an UNSIGNED, NON-debuggable Android release build.

This is a BUILD gate only, NOT an assertion that FoodPlan is ready to ship.
Signing is deliberately forbidden in untrusted PR runners and secrets are
never checked into this repository. A reusable release signing key and
external M2/M4/M5 acceptance are necessary before actual distribution.
"""
import argparse
from datetime import datetime, timezone
import hashlib
import json
from pathlib import Path
import re
import subprocess
from zipfile import ZipFile, BadZipFile


def inspect_apk(apk: Path, aapt: Path, apksigner: Path):
    if not apk.is_file() or apk.stat().st_size < 1_000_000:
        raise ValueError("MISSING_OR_IMPLAUSIBLY_SMALL_RELEASE_APK")
    try:
        with ZipFile(apk) as archive:
            if archive.testzip() is not None:
                raise ValueError("CORRUPT_APK_ZIP")
            names = set(archive.namelist())
            if "AndroidManifest.xml" not in names or "classes.dex" not in names:
                raise ValueError("NOT_AN_ANDROID_APPLICATION_APK")
    except BadZipFile:
        raise ValueError("CORRUPT_APK_ZIP")

    badging = subprocess.run(
        [str(aapt), "dump", "badging", str(apk)],
        capture_output=True, text=True, timeout=30
    )
    if badging.returncode != 0:
        raise ValueError("AAPT_BADGING_REJECTED_APK")
    output = badging.stdout
    pkg = re.search(r"^package: name='([^']+)' versionCode='([0-9]+)'", output, re.M)
    if pkg is None or pkg.group(1) != "com.foodplan.ai" or int(pkg.group(2)) < 1:
        raise ValueError("WRONG_APPLICATION_ID_OR_VERSION_CODE")
    if re.search(r"^application-debuggable\b", output, re.M):
        raise ValueError("RELEASE_BUILT_AS_DEBUGGABLE")
    signing = subprocess.run(
        [str(apksigner), "verify", str(apk)],
        capture_output=True, text=True, timeout=30
    )
    # An unsigned production-flavored APK should intentionally NOT verify;
    # a signed file in an untrusted workflow deserves investigation.
    if signing.returncode == 0:
        raise ValueError("UNEXPECTED_SIGNED_APK_IN_UNTRUSTED_PR_CI")

    digest = hashlib.sha256(apk.read_bytes()).hexdigest()
    return {
        "package_name": pkg.group(1),
        "version_code": int(pkg.group(2)),
        "bytes": apk.stat().st_size,
        "sha256": digest,
        "debuggable": False,
        "verified_signature": False,
        "installable": False,
        "technical_packaging_gate": "PASS_NON_DEBUGGABLE_UNSIGNED_BUILD",
        "release_gate": "BLOCKED",
        "outstanding_external_acceptance": [
            "M2_150_INDEPENDENT_RECIPE_REVIEWS",
            "M3_PROVEN_NUTRITIONAL_ADEQUACY",
            "M4_COMPLETE_REAL_WEEK_PRICE_COVERAGE",
            "M5_PHYSICAL_ANDROID_FORCE_STOP_REOPEN",
            "REUSABLE_SECURE_RELEASE_KEYSTORE_AND_SIGNING",
            "FINAL_USER_ACCEPTANCE"
        ],
        "checked_at_utc": datetime.now(timezone.utc).isoformat()
    }


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--apk", required=True)
    parser.add_argument("--aapt", required=True)
    parser.add_argument("--apksigner", required=True)
    parser.add_argument("--output", required=True)
    args = parser.parse_args()
    result = inspect_apk(Path(args.apk), Path(args.aapt), Path(args.apksigner))
    out = Path(args.output)
    out.parent.mkdir(parents=True, exist_ok=True)
    out.write_text(json.dumps(result, indent=2, ensure_ascii=False) + "\n",
                   encoding="utf-8")
    print("M6 unsigned packaging:", result["technical_packaging_gate"],
          result["package_name"], "sha256=" + result["sha256"])
    print("M6 actual user release:", result["release_gate"],
          "APK NOT SIGNED OR INSTALLABLE")


if __name__ == "__main__":
    main()
