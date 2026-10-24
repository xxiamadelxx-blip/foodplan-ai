#!/usr/bin/env bash
# Rehearse REAL Android APK signing with a disposable CI-only certificate.
# Neither the private key nor the signed test APK is uploaded as an artifact.
set -euo pipefail

if [[ "$#" -ne 3 ]]; then
  echo "Usage: m6_ephemeral_sign_smoke.sh UNSIGNED_APK BUILD_TOOLS OUTPUT_JSON" >&2
  exit 2
fi

unsigned_apk="$1"
build_tools="$2"
output_json="$3"
test -s "$unsigned_apk"
test -x "$build_tools/zipalign"
test -x "$build_tools/apksigner"
test -x "$build_tools/aapt"
command -v keytool >/dev/null
command -v openssl >/dev/null
command -v python >/dev/null

umask 077
private_dir="$(mktemp -d -t foodplan-sign-smoke.XXXXXXXX)"
trap 'rm -rf -- "$private_dir"' EXIT

# Never log secrets, private signing material or the transient signed APK.
export FOODPLAN_EPHEMERAL_PASS
FOODPLAN_EPHEMERAL_PASS="$(openssl rand -hex 24)"
keytool -genkeypair \
  -alias foodplan-disposable-test \
  -keyalg RSA -keysize 3072 -sigalg SHA256withRSA \
  -validity 2 \
  -dname "CN=Disposable FoodPlan CI Key, OU=Test Only, O=FoodPlan, C=EE" \
  -keystore "$private_dir/test-only.p12" -storetype PKCS12 \
  -storepass:env FOODPLAN_EPHEMERAL_PASS \
  -keypass:env FOODPLAN_EPHEMERAL_PASS -noprompt >/dev/null 2>&1

# Derive the expected fingerprint from the independently exported certificate.
pinned_sha256="$(
  keytool -exportcert -rfc -alias foodplan-disposable-test \
    -keystore "$private_dir/test-only.p12" \
    -storepass:env FOODPLAN_EPHEMERAL_PASS |
    openssl x509 -noout -fingerprint -sha256 |
    sed 's/.*=//' | tr -d ':\r\n'
)"
if [[ ! "$pinned_sha256" =~ ^[A-Fa-f0-9]{64}$ ]]; then
  echo "FAILED: invalid independently exported test certificate" >&2
  exit 1
fi

"$build_tools/zipalign" -f -p 4 "$unsigned_apk" "$private_dir/aligned.apk"
"$build_tools/apksigner" sign \
  --ks "$private_dir/test-only.p12" \
  --ks-key-alias foodplan-disposable-test \
  --ks-pass env:FOODPLAN_EPHEMERAL_PASS \
  --key-pass env:FOODPLAN_EPHEMERAL_PASS \
  --out "$private_dir/signed-test-only.apk" "$private_dir/aligned.apk"

python tools/m6_signed_internal_gate.py \
  --apk "$private_dir/signed-test-only.apk" \
  --aapt "$build_tools/aapt" \
  --apksigner "$build_tools/apksigner" \
  --expected-signer-sha256 "$pinned_sha256" \
  --output "$output_json"

python - "$output_json" <<'PY'
import json
from pathlib import Path
import sys

path = Path(sys.argv[1])
report = json.loads(path.read_text(encoding="utf-8"))
assert report["technical_gate"] == "PASS_SIGNED_INTERNAL_QA_CANDIDATE"
assert report["signature_verified"] is True
assert report["public_release_gate"] == "BLOCKED"
report.update({
    "test_only_signing_smoke": True,
    "signer_scope": "EPHEMERAL_PER_CI_RUN",
    "persistent_release_signer_verified": False,
    "installable_apk_distributed": False,
    "public_release_gate": "BLOCKED",
})
path.write_text(json.dumps(report, indent=2, ensure_ascii=False) + "\n",
                encoding="utf-8")
print("PASS: disposable CI signer, independent APK and certificate audit")
print("Permanent release signing: NOT VERIFIED; public release: BLOCKED")
PY
