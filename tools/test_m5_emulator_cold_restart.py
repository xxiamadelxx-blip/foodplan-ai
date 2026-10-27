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

    def test_scrolled_shopping_rows_count_even_when_heading_is_offscreen(self):
        rows = """<hierarchy>
<node text="Сыр Гауда" bounds="[0,0][100,30]"/>
<node text="Нужно купить: 230 г" bounds="[0,30][100,60]"/>
<node text="Фасовка уточняется · Цена неизвестна" bounds="[0,60][300,90]"/>
<node text="Чечевица сухая" bounds="[0,90][100,120]"/>
<node text="Нужно купить: 448 г" bounds="[0,120][100,150]"/>
<node text="Фасовка уточняется · Цена неизвестна" bounds="[0,150][300,180]"/>
</hierarchy>"""
        self.assertTrue(cold.has_visible_shopping_rows(cold.xml_nodes(rows)))
        self.assertFalse(cold.has_visible_shopping_rows(cold.xml_nodes(UI)))

    def test_real_new_products_screen_after_force_stop_is_not_a_false_failure(self):
        # Extracted literal strings from emulator CI #40014617540 failure UI.
        # The selected 3-day meal and list were actually RESTORED; the probe
        # still required obsolete "Нужно купить:" copy and incorrectly failed.
        rows = """<hierarchy>
<node text="Сыр Гауда" bounds="[0,10][280,40]"/>
<node text="Купить 174 г" bounds="[0,41][280,80]"/>
<node text="Фасовка уточняется · Цена магазина неизвестна" bounds="[0,81][300,121]"/>
<node text="Творог" bounds="[0,122][280,158]"/>
<node text="Купить 1103 г" bounds="[0,159][280,195]"/>
<node text="Фасовка уточняется · Цена магазина неизвестна" bounds="[0,196][300,236]"/>
<node text="Треска" bounds="[0,237][280,274]"/>
<node text="Купить 1071 г" bounds="[0,275][280,309]"/>
<node text="Фасовка уточняется · Цена магазина неизвестна" bounds="[0,310][300,345]"/>
</hierarchy>"""
        self.assertTrue(cold.has_visible_shopping_rows(cold.xml_nodes(rows)))
        self.assertFalse(cold.has_visible_shopping_rows(cold.xml_nodes(
            """<hierarchy>
            <node text="Продукты на 7 дней"/>
            <node text="Фасовка уточняется"/>
            <node text="Купить что-нибудь"/>
            </hierarchy>""")))

    def test_verified_pack_rows_prove_ingredient_demand_without_old_label(self):
        rows = """<hierarchy>
<node text="По рецептам: 901 г"/>
<node text="2 уп. по 800 г · ~99,98 ₽"/>
<node text="По рецептам: 1103 г"/>
<node text="3 уп. по 450 г · ~129,99 ₽"/>
</hierarchy>"""
        self.assertTrue(cold.has_visible_shopping_rows(cold.xml_nodes(rows)))

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
