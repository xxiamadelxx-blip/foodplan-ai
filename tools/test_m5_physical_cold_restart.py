"""Fail-closed tests of the M5 non-destructive physical Android acceptance probe."""
import json
import tempfile
import unittest
from pathlib import Path
from unittest.mock import patch

import m5_physical_cold_restart as physical


class FakeDevice:
    def __init__(self, emulator=False, installed=True):
        self.calls = []
        self.pids = iter(("101", "", "202"))
        self.emulator = emulator
        self.installed = installed

    def prop(self, name):
        return {
            "ro.kernel.qemu": "1" if self.emulator else "0",
            "ro.boot.qemu": "0",
            "ro.build.characteristics": "phone",
            "ro.build.fingerprint": "vendor/model/build:15/QA.123/release-keys",
            "ro.product.model": "QA physical phone",
        }.get(name, "")

    def call(self, *args, **kwargs):
        self.calls.append(args)
        if args[:3] == ("shell", "pm", "path"):
            return "package:/data/app/foodplan/base.apk" if self.installed else ""
        return ""

    def pid(self):
        return next(self.pids)

    def read_ui(self):
        raise AssertionError("Expected snapshot to be stubbed in this host test")


class PhysicalColdRestartTests(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        self.output = Path(self.temp.name)

    def test_no_device_is_ever_accessed_without_explicit_force_stop_opt_in(self):
        device = FakeDevice()
        result = physical.run_physical_probe(self.output, device, False)
        self.assertFalse(result["physical_android_verified"])
        self.assertIn("EXPLICIT_FORCE_STOP_OPT_IN_REQUIRED", result["reason"])
        self.assertEqual([], device.calls)
        self.assertEqual("FAIL_PHYSICAL_OR_UNVERIFIED",
                         json.loads((self.output / "evidence.json").read_text())["status"])

    def test_emulator_and_missing_application_are_rejected_before_force_stop(self):
        for device in [FakeDevice(emulator=True), FakeDevice(installed=False)]:
            with self.subTest(emulator=device.emulator):
                report = physical.run_physical_probe(self.output, device, True)
                self.assertFalse(report["physical_android_verified"])
                self.assertFalse(any(call[:3] == ("shell", "am", "force-stop")
                                     for call in device.calls))
                self.assertFalse(any("clear" in " ".join(call) for call in device.calls))

    @patch("m5_physical_cold_restart.snapshot_prepared_week")
    def test_real_process_death_with_preserved_synthetic_snapshot_logic(self, snapshot):
        snapshot.side_effect = [(174, 323, 1103), (174, 323, 1103)]
        device = FakeDevice()
        report = physical.run_physical_probe(self.output, device, True)
        self.assertEqual("PASS_PHYSICAL_OS_COLD_RELAUNCH", report["status"])
        self.assertTrue(report["physical_android_verified"])
        self.assertEqual(3, report["shopping_demands_compared"])
        self.assertTrue(report["distinct_pid_after_relaunch"])
        self.assertFalse(report["installed_app_data_cleared"])
        self.assertFalse(report["user_data_uploaded"])
        self.assertEqual(2, snapshot.call_count)
        self.assertEqual(1, sum(call[:3] == ("shell", "am", "force-stop")
                                for call in device.calls))
        self.assertFalse(any(call[:3] == ("shell", "pm", "clear")
                             for call in device.calls))
        self.assertNotIn("174", (self.output / "evidence.json").read_text())
        self.assertNotIn("1103", (self.output / "evidence.json").read_text())

    @patch("m5_physical_cold_restart.snapshot_prepared_week")
    def test_changed_demand_after_relaunch_cannot_pass(self, snapshot):
        snapshot.side_effect = [(174, 323), (174, 324)]
        result = physical.run_physical_probe(self.output, FakeDevice(), True)
        self.assertFalse(result["physical_android_verified"])
        self.assertEqual("FAIL_PHYSICAL_OR_UNVERIFIED", result["status"])
        self.assertIn("WEEKLY_GROCERY_DEMAND_CHANGED_AFTER_RESTART", result["reason"])

    @patch("m5_physical_cold_restart.snapshot_prepared_week")
    def test_missing_prepared_three_day_batch_blocks_test_before_force_stop(self, snapshot):
        snapshot.side_effect = RuntimeError("REQUIRED_UI_NOT_VISIBLE: three day batch")
        device = FakeDevice()
        report = physical.run_physical_probe(self.output, device, True)
        self.assertFalse(report["physical_android_verified"])
        self.assertFalse(any(call[:3] == ("shell", "am", "force-stop")
                             for call in device.calls))

    def test_rejects_serial_argument_shell_injection(self):
        with self.assertRaisesRegex(ValueError, "EXPLICIT_DEVICE_SERIAL_REQUIRED"):
            physical.PhysicalAdb("device; rm -rf /")
        self.assertEqual("R58M1234", physical.PhysicalAdb("R58M1234").serial)

    @patch("m5_physical_cold_restart.time.sleep")
    def test_grocery_snapshot_requires_multiple_visible_real_demand_lines(self, _sleep):
        xml = """<hierarchy>
<node text="Купить 174 г"/>
<node text="Фасовка уточняется · Цена магазина неизвестна"/>
<node text="Купить 323 г"/>
<node text="Фасовка уточняется · Цена магазина неизвестна"/>
</hierarchy>"""
        device = FakeDevice()
        device.read_ui = lambda: physical.xml_nodes(xml)
        device.call = lambda *args, **kw: "Physical size: 1080x2400" if args[:3] == (
            "shell", "wm", "size") else ""
        self.assertEqual((174, 323), physical.shopping_snapshot(device))

    @patch("m5_physical_cold_restart.time.sleep")
    def test_generic_shopping_title_is_not_falsely_accepted(self, _sleep):
        device = FakeDevice()
        device.read_ui = lambda: physical.xml_nodes(
            '<hierarchy><node text="Продукты на 7 дней"/><node text="Купить что-то"/></hierarchy>')
        device.call = lambda *args, **kw: "Physical size: 1080x2400" if args[:3] == (
            "shell", "wm", "size") else ""
        with self.assertRaisesRegex(RuntimeError, "PREPARED_SHOPPING_LIST"):
            physical.shopping_snapshot(device)


if __name__ == "__main__":
    unittest.main()
