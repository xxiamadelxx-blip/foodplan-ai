"""Negative/positive contract tests; never use or fabricate release credentials."""
import tempfile
import unittest
from pathlib import Path
from unittest.mock import patch
from zipfile import ZipFile

from m6_signed_internal_gate import inspect_signed_candidate


FINGERPRINT = "a" * 64


class SignedInternalGateTests(unittest.TestCase):
    def setUp(self):
        self.tmp = tempfile.TemporaryDirectory()
        self.addCleanup(self.tmp.cleanup)
        self.apk = Path(self.tmp.name) / "internal-release.apk"
        with ZipFile(self.apk, "w") as z:
            z.writestr("AndroidManifest.xml", b"x" * 300)
            z.writestr("classes.dex", b"x" * 1_100_000)

    @staticmethod
    def fake_process(argv, **kwargs):
        from subprocess import CompletedProcess
        if argv[1] == "dump":
            return CompletedProcess(argv, 0,
                "package: name='com.foodplan.ai' versionCode='8' versionName='internal'\n", "")
        return CompletedProcess(argv, 0,
            "Verifies\nSigner #1 certificate SHA-256 digest: " + FINGERPRINT + "\n", "")

    @patch("m6_signed_internal_gate.subprocess.run")
    def test_only_pinned_signature_passes_technical_gate(self, run):
        run.side_effect = self.fake_process
        result = inspect_signed_candidate(self.apk, Path("/aapt"),
                                          Path("/apksigner"), FINGERPRINT)
        self.assertEqual("PASS_SIGNED_INTERNAL_QA_CANDIDATE", result["technical_gate"])
        self.assertEqual("BLOCKED", result["public_release_gate"])
        self.assertEqual("com.foodplan.ai", result["package_name"])

    @patch("m6_signed_internal_gate.subprocess.run")
    def test_different_key_is_rejected(self, run):
        run.side_effect = self.fake_process
        with self.assertRaisesRegex(ValueError, "SIGNER_FINGERPRINT_CHANGED"):
            inspect_signed_candidate(self.apk, Path("/aapt"),
                                     Path("/apksigner"), "b" * 64)

    @patch("m6_signed_internal_gate.subprocess.run")
    def test_missing_signature_and_debuggable_are_rejected(self, run):
        from subprocess import CompletedProcess
        run.side_effect = lambda argv, **kw: (
            CompletedProcess(argv, 0,
                "package: name='com.foodplan.ai' versionCode='8'\napplication-debuggable\n", "")
            if argv[1] == "dump" else self.fake_process(argv, **kw))
        with self.assertRaisesRegex(ValueError, "UNSAFE_DEBUGGABLE_RELEASE"):
            inspect_signed_candidate(self.apk, Path("/aapt"),
                                     Path("/apksigner"), FINGERPRINT)
        run.side_effect = lambda argv, **kw: (
            self.fake_process(argv, **kw) if argv[1] == "dump"
            else CompletedProcess(argv, 1, "", "signature error"))
        with self.assertRaisesRegex(ValueError, "APK_SIGNATURE_INVALID"):
            inspect_signed_candidate(self.apk, Path("/aapt"),
                                     Path("/apksigner"), FINGERPRINT)

    def test_missing_pinned_signer_is_rejected(self):
        with self.assertRaisesRegex(ValueError, "MISSING_PINNED_SIGNER_FINGERPRINT"):
            inspect_signed_candidate(self.apk, Path("/aapt"),
                                     Path("/apksigner"), "")

    def test_protected_workflow_signs_before_verification(self):
        workflow = (Path(__file__).resolve().parents[1] /
            ".github/workflows/m6-signed-internal-qa.yml").read_text(
                encoding="utf-8")
        self.assertIn("if: github.ref == 'refs/heads/main'", workflow)
        self.assertIn("environment: foodplan-signing", workflow)
        self.assertIn("workflow_dispatch:", workflow)
        self.assertNotIn("pull_request:", workflow)
        self.assertIn("app-release-unsigned.apk", workflow)
        self.assertIn('zipalign" -f -p 4', workflow)
        self.assertIn('apksigner" sign', workflow)
        self.assertIn("--ks-pass env:KS_PASS --key-pass env:KEY_PASS", workflow)
        self.assertIn("--expected-signer-sha256", workflow)
        self.assertIn("Do NOT", workflow.replace("never auto publish", "Do NOT"))
        self.assertLess(workflow.index('apksigner" sign'),
                        workflow.index("Independently verify application"))

    def test_disposable_signing_smoke_is_real_and_never_uploaded(self):
        root = Path(__file__).resolve().parents[1]
        script = (root / "tools/m6_ephemeral_sign_smoke.sh").read_text(
            encoding="utf-8")
        workflow = (root / ".github/workflows/m6-unsigned-release-candidate.yml").read_text(
            encoding="utf-8")
        self.assertIn("keytool -genkeypair", script)
        self.assertIn("keytool -exportcert -rfc", script)
        self.assertIn("openssl x509 -noout -fingerprint -sha256", script)
        self.assertIn('zipalign" -f -p 4', script)
        self.assertIn('apksigner" sign', script)
        self.assertIn("--ks-pass env:FOODPLAN_EPHEMERAL_PASS", script)
        self.assertIn("python tools/m6_signed_internal_gate.py", script)
        self.assertIn("trap 'rm -rf --", script)
        self.assertIn('"signer_scope": "EPHEMERAL_PER_CI_RUN"', script)
        self.assertIn('"persistent_release_signer_verified": False', script)
        self.assertIn('"installable_apk_distributed": False', script)
        self.assertLess(script.index("keytool -genkeypair"),
                        script.index('apksigner" sign'))
        self.assertLess(script.index('apksigner" sign'),
                        script.index("python tools/m6_signed_internal_gate.py"))
        self.assertIn("bash -n tools/m6_ephemeral_sign_smoke.sh", workflow)
        self.assertIn("m6-build-evidence/ephemeral-signer-smoke.json", workflow)
        # No CI test key, signed QA APK or permanent-secret input is archived.
        artifact = workflow.split(
            "name: Archive technical evidence and unsigned non-installable artifact", 1
        )[1].split("name: Do NOT publish/tag/install release", 1)[0]
        self.assertNotIn("signed-test-only.apk", artifact)
        self.assertNotIn("test-only.p12", artifact)
        self.assertNotIn("FOODPLAN_RELEASE_KEYSTORE_BASE64", workflow)


if __name__ == "__main__":
    unittest.main()
