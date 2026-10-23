"""Host-side unit checks for ADB UI parser and destructive-test safety."""
import tempfile
import unittest
from pathlib import Path
from unittest.mock import patch

import m5_emulator_cold_restart as cold

UI = '''<?xml version="1.0" encoding="UTF-8"?>
<hierarchy rotation="0">
<node text="Продукты на 7 дней" content-desc="" bounds="[0,50][400,150]"/>
<node text="Рецепты" content-desc="" bounds="[210,720][310,770]"/>
<node text="" content-desc="День 2" bounds="[120,150][200,190]"/>
<node text="Приготовить сразу на 3 дня" content-desc="" bounds="[40,300][350,340]"/>
</hierarchy>'''


class ColdRestartHostTests(unittest.TestCase):
    def test_parse_real_accessibility_text_and_bounds(self):
        nodes = cold.xml_nodes(UI)
        self.assertEqual(4, len(nodes))
        self.assertTrue(any(cold.matches_text(n, "Продукты на 7 дней") for n in nodes))
        self.assertTrue(any(cold.matches_text(n, "День 2") for n in nodes))
        self.assertEqual((260, 745), cold.center(nodes[1]["bounds"]))

    @patch("m5_emulator_cold_restart.adb")
    def test_taps_actual_accessibility_label_not_fixed_coordinates(self, fake_adb):
        nodes = cold.xml_nodes(UI)
        cold.tap_label(nodes, "Рецепты")
        fake_adb.assert_called_once_with(
            "shell", "input", "tap", "260", "745")
        fake_adb.reset_mock()
        cold.tap_label(nodes, "День 2")
        fake_adb.assert_called_once_with(
            "shell", "input", "tap", "160", "170")

    def test_bad_ui_geometry_and_missing_label_fail(self):
        with self.assertRaisesRegex(ValueError, "INVALID_UI_BOUNDS"):
            cold.center("missing")
        with self.assertRaisesRegex(ValueError, "EMPTY_UI_BOUNDS"):
            cold.center("[2,2][1,1]")
        with self.assertRaisesRegex(RuntimeError, "CONTROL_NOT_FOUND"):
            cold.tap_label(cold.xml_nodes(UI), "День 8")

    @patch("m5_emulator_cold_restart.adb")
    def test_never_clears_a_physical_phone(self, fake_adb):
        def pretend(*args, **kwargs):
            if args[:2] == ("shell", "getprop"):
                return "0"
            return ""
        fake_adb.side_effect = pretend
        with tempfile.TemporaryDirectory() as folder:
            report = cold.run_probe(Path(folder))
            self.assertEqual("FAIL_EMULATOR_COLD_RELAUNCH", report["status"])
            self.assertIn("REFUSE_DESTRUCTIVE_TEST_ON_NON_EMULATOR",
                          report["reason"])
            self.assertTrue((Path(folder) / "evidence.json").is_file())
        self.assertFalse(any(
            args[0][:3] == ("shell", "pm", "clear")
            for args in (call.args for call in fake_adb.call_args_list)))


if __name__ == "__main__":
    unittest.main()
