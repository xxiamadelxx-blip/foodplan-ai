#!/usr/bin/env python3
"""Gate a persistently signed INTERNAL QA APK. Never declares public release.

The pinned signer fingerprint is passed from a GitHub Actions secret.
No signing keys, passwords or unsigned release builds are embedded here.
"""
import argparse
from datetime import datetime, timezone
import hashlib
import json
from pathlib import Path
import re
import subprocess
from zipfile import ZipFile, BadZipFile


def inspect_signed_candidate(apk: Path, aapt: Path, apksigner: Path, pinned_sha256: str):
    expected = pinned_sha256.lower().replace(":", "").strip()
    if not re.fullmatch(r"[0-9a-f]{64}", expected):
        raise ValueError("MISSING_PINNED_SIGNER_FINGERPRINT")
    if not apk.is_file() or apk.stat().st_size < 1_000_000:
        raise ValueError("MISSING_SIGNED_APK")
    try:
        with ZipFile(apk) as archive:
            if archive.testzip() is not None:
                raise ValueError("CORRUPT_APK")
            if not {"AndroidManifest.xml", "classes.dex"}.issubset(archive.namelist()):
                raise ValueError("NOT_ANDROID_APK")
    except BadZipFile as error:
        raise ValueError("CORRUPT_APK") from error

    badging = subprocess.run(
        [str(aapt), "dump", "badging", str(apk)],
        capture_output=True, text=True, timeout=30, check=False
    )
    if badging.returncode:
        raise ValueError("AAPT_REJECTED_APK")
    pkg = re.search(r"^package: name='([^']+)' versionCode='([0-9]+)'",
                    badging.stdout, re.MULTILINE)
    if pkg is None or pkg.group(1) != "com.foodplan.ai" or int(pkg.group(2)) < 1:
        raise ValueError("WRONG_PACKAGE_ID_OR_VERSION")
    if re.search(r"^application-debuggable\b", badging.stdout, re.MULTILINE):
        raise ValueError("UNSAFE_DEBUGGABLE_RELEASE")

    verified = subprocess.run(
        [str(apksigner), "verify", "--verbose", "--print-certs", str(apk)],
        capture_output=True, text=True, timeout=30, check=False
    )
    if verified.returncode:
        raise ValueError("APK_SIGNATURE_INVALID")
    signers = re.findall(
        r"^Signer #([0-9]+) certificate SHA-256 digest:\s*([0-9a-fA-F:]{64,95})\s*$",
        verified.stdout, re.MULTILINE
    )
    if len(signers) != 1 or signers[0][0] != "1":
        raise ValueError("UNEXPECTED_SIGNER_SET")
    actual = signers[0][1].lower().replace(":", "")
    if actual != expected:
        raise ValueError("SIGNER_FINGERPRINT_CHANGED")

    return {
        "package_name": pkg.group(1),
        "version_code": int(pkg.group(2)),
        "sha256": hashlib.sha256(apk.read_bytes()).hexdigest(),
        "bytes": apk.stat().st_size,
        "pinned_signer_sha256": expected,
        "signature_verified": True,
        "technical_gate": "PASS_SIGNED_INTERNAL_QA_CANDIDATE",
        "public_release_gate": "BLOCKED",
        "reason": "Independent M2/M3/M4/M5 and physical acceptance still required",
        "checked_at_utc": datetime.now(timezone.utc).isoformat()
    }


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--apk", required=True)
    parser.add_argument("--aapt", required=True)
    parser.add_argument("--apksigner", required=True)
    parser.add_argument("--expected-signer-sha256", required=True)
    parser.add_argument("--output", required=True)
    args = parser.parse_args()
    result = inspect_signed_candidate(
        Path(args.apk), Path(args.aapt), Path(args.apksigner),
        args.expected_signer_sha256
    )
    file = Path(args.output)
    file.parent.mkdir(parents=True, exist_ok=True)
    file.write_text(json.dumps(result, indent=2, ensure_ascii=False) + "\n",
                    encoding="utf-8")
    print("PASS internal QA signed APK; package:", result["package_name"])
    print("Public release:", result["public_release_gate"])


if __name__ == "__main__":
    main()
