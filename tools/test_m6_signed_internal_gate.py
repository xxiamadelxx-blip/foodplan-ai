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


if __name__ == "__main__":
    unittest.main()
