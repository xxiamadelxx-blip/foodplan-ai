#!/usr/bin/env python3
"""Real ADB OS process-death + cold UI relaunch on an ANDROID EMULATOR only.

Normal connectedDebugAndroidTest verifies DataStore and Compose recreation,
but does not terminate the OS process. This independent host-side probe does:
seed a synthetic week through Android instrumentation -> launch -> force-stop
-> assert PID is gone -> relaunch with a new PID -> check the user's 3-day
batch and grocery list in the real UI via uiautomator. No AI and no secrets.
A physical phone remains a separately unverified release criterion.
"""
import argparse
from datetime import datetime, timezone
import json
from pathlib import Path
import re
import subprocess
import time
import xml.etree.ElementTree as ET

PACKAGE = "com.foodplan.ai"
COMPONENT = "com.foodplan.ai/.MainActivity"
RUNNER = "com.foodplan.ai.test/androidx.test.runner.AndroidJUnitRunner"
SEED_TEST = "com.foodplan.ai.M5ColdRestartSeedTest#prepareSavedWeekForExternalProcessRestart"


def adb(*args, timeout=60, check=True):
    cmd = ["adb", *args]
    completed = subprocess.run(cmd, capture_output=True, timeout=timeout, check=False)
    if check and completed.returncode != 0:
        message = (completed.stdout + completed.stderr).decode("utf-8", errors="replace")
        raise RuntimeError("ADB_FAILED " + " ".join(args) + ": " + message[-1400:])
    return completed.stdout.decode("utf-8", errors="replace").strip()


def xml_nodes(xml_content):
    root = ET.fromstring(xml_content)
    return [node.attrib for node in root.iter("node")]


def matches_text(node, expected):
    return any(expected in node.get(attr, "") for attr in ("text", "content-desc"))


def has_visible_shopping_rows(nodes):
    # UIAutomator exposes only what is visible after scrolling, not the
    # long list's heading above the viewport. Require actual purchase rows.
    need = sum(matches_text(n, "Нужно купить:") for n in nodes)
    # Each row must show verified purchasable packs OR explicitly state
    # that packaging is unknown. Reference gram pack sizes are not real SKUs.
    purchase = sum(
        matches_text(n, "Фасовка уточняется") or matches_text(n, "уп. по")
        for n in nodes
    )
    return need >= 2 and purchase >= 2


def center(bounds):
    match = re.fullmatch(r"\[(\d+),(\d+)\]\[(\d+),(\d+)\]", bounds)
    if not match:
        raise ValueError("INVALID_UI_BOUNDS")
    x1, y1, x2, y2 = map(int, match.groups())
    if x2 <= x1 or y2 <= y1:
        raise ValueError("EMPTY_UI_BOUNDS")
    return (x1 + x2) // 2, (y1 + y2) // 2


def read_ui():
    adb("shell", "uiautomator", "dump", "/sdcard/foodplan-m5-ui.xml", timeout=45)
    xml = adb("exec-out", "cat", "/sdcard/foodplan-m5-ui.xml", timeout=20)
    return xml, xml_nodes(xml)


def await_ui(needle, seconds=35):
    last_xml = ""
    end = time.monotonic() + seconds
    while time.monotonic() < end:
        try:
            last_xml, nodes = read_ui()
            if any(matches_text(n, needle) for n in nodes):
                return last_xml, nodes
        except (RuntimeError, ET.ParseError):
            pass
        time.sleep(1)
    raise RuntimeError("UI_NOT_FOUND: " + needle + "; XML: " + last_xml[-1000:])


def tap_label(nodes, label):
    exact = [n for n in nodes if label in (n.get("text"), n.get("content-desc"))]
    candidates = exact or [n for n in nodes if matches_text(n, label)]
    if not candidates:
        raise RuntimeError("CONTROL_NOT_FOUND: " + label)
    # The most specific matching label node usually has its own visible bounds;
    # Android's gesture dispatch targets the clickable parent at that point.
    x, y = center(candidates[-1].get("bounds", ""))
    adb("shell", "input", "tap", str(x), str(y))


def pid():
    return adb("shell", "pidof", PACKAGE, check=False).strip()


def screenshot(path):
    completed = subprocess.run(["adb", "exec-out", "screencap", "-p"],
                               capture_output=True, timeout=25, check=False)
    if completed.returncode != 0 or not completed.stdout.startswith(b"\x89PNG"):
        raise RuntimeError("SCREENSHOT_FAILED")
    path.write_bytes(completed.stdout)


def run_probe(output_dir: Path):
    output_dir.mkdir(parents=True, exist_ok=True)
    evidence = {
        "checked_at_utc": datetime.now(timezone.utc).isoformat(),
        "device_type": "EMULATOR",
        "physical_android_verified": False,
        "user_data_used": False,
        "source": "REAL_ANDROID_ADB_UITEST_WITH_PROCESS_FORCE_STOP",
        "steps": []
    }
    last_xml = ""
    try:
        adb("wait-for-device", timeout=60)
        # Destructive pm clear is permitted ONLY on an Android emulator.
        emulated = [adb("shell", "getprop", prop)
                    for prop in ("ro.kernel.qemu", "ro.boot.qemu")]
        if "1" not in emulated:
            raise RuntimeError("REFUSE_DESTRUCTIVE_TEST_ON_NON_EMULATOR")
        evidence["steps"].append("emulator_identity_verified")
        apk = Path("android/app/build/outputs/apk/debug/app-debug.apk")
        android_test_apk = Path(
            "android/app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk"
        )
        for package_file in (apk, android_test_apk):
            if not package_file.is_file():
                raise RuntimeError("APK_MISSING: " + str(package_file))
            adb("install", "-r", str(package_file), timeout=120)
        adb("shell", "pm", "clear", PACKAGE)
        evidence["steps"].append("synthetic_profile_isolated_from_previous_tests")

        result = adb("shell", "am", "instrument", "-w",
                     "-e", "class", SEED_TEST, "-e", "foodplan_m5_seed", "1",
                     RUNNER, timeout=150)
        if not re.search(r"\bOK\s*\(1\s+test\)", result) or "FAILURES" in result:
            raise RuntimeError("FIXTURE_INSTRUMENTATION_NOT_PASS: " + result[-1600:])
        evidence["steps"].append("saved_real_three_day_batch_in_android_datastore")

        adb("shell", "am", "start", "-W", "-n", COMPONENT, timeout=60)
        last_xml, _ = await_ui("Покупки на неделю")
        before = pid()
        if not re.fullmatch(r"\d+", before):
            raise RuntimeError("APP_NOT_RUNNING_BEFORE_FORCE_STOP: " + before)
        evidence["pid_before_stop"] = before
        adb("shell", "am", "force-stop", PACKAGE)
        if pid():
            raise RuntimeError("FORCE_STOP_DID_NOT_KILL_PROCESS")
        evidence["steps"].append("adb_force_stop_confirmed_process_gone")

        adb("shell", "am", "start", "-W", "-n", COMPONENT, timeout=60)
        last_xml, nodes = await_ui("Покупки на неделю")
        after = pid()
        if not re.fullmatch(r"\d+", after) or before == after:
            raise RuntimeError("NO_DISTINCT_APP_PROCESS_AFTER_RELAUNCH")
        evidence["pid_after_relaunch"] = after
        if any(matches_text(n, "Настроим питание один раз") for n in nodes):
            raise RuntimeError("ONBOARDING_REAPPEARED_AFTER_COLD_START")
        evidence["steps"].append("cold_new_pid_opens_saved_week_not_onboarding")
        screenshot(output_dir / "01-cold-start-shopping.png")

        tap_label(nodes, "Рецепты")
        last_xml, nodes = await_ui("Приготовить сразу на 3 дня")
        evidence["steps"].append("day_1_batch_selection_restored_in_real_ui")
        screenshot(output_dir / "02-restored-meal-day1.png")

        tap_label(nodes, "День 2")
        last_xml, nodes = await_ui("Блюдо готовится заранее на 3 дня")
        evidence["steps"].append("day_2_linked_batch_restored_in_real_ui")
        screenshot(output_dir / "03-restored-meal-day2.png")

        tap_label(nodes, "Продукты")
        last_xml, nodes = await_ui("Покупки на неделю")
        display = re.search(r"(\d+)x(\d+)", adb("shell", "wm", "size"))
        if display is None:
            raise RuntimeError("CANNOT_DETERMINE_EMULATOR_SCREEN_SIZE")
        width, height = map(int, display.groups())
        # Grocery list is below budget/store controls; Android UIAutomator
        # reports only visible nodes. Scroll the user-facing list to inspect it.
        for _ in range(6):
            if has_visible_shopping_rows(nodes):
                break
            adb("shell", "input", "swipe", str(width // 2),
                str(height * 78 // 100), str(width // 2),
                str(height * 25 // 100), "320")
            time.sleep(0.5)
            last_xml, nodes = read_ui()
        else:
            raise RuntimeError("SAVED_SHOPPING_ROWS_NOT_VISIBLE_AFTER_SCROLL")
        evidence["steps"].append("weekly_shopping_list_remains_visible_after_restart")
        screenshot(output_dir / "04-restored-weekly-shopping.png")

        evidence["status"] = "PASS_EMULATOR_OS_FORCE_STOP_COLD_RELAUNCH"
        evidence["release_gate"] = "BLOCKED_EXTERNAL_PHYSICAL_ANDROID_AND_M2_M4"
    except Exception as error:
        evidence["status"] = "FAIL_EMULATOR_COLD_RELAUNCH"
        evidence["reason"] = str(error)[:1800]
        try:
            last_xml, _ = read_ui()
            screenshot(output_dir / "failure-screen.png")
        except Exception:
            pass
    finally:
        if last_xml:
            (output_dir / "last-ui.xml").write_text(last_xml, encoding="utf-8")
        (output_dir / "evidence.json").write_text(
            json.dumps(evidence, indent=2, ensure_ascii=False) + "\n",
            encoding="utf-8"
        )
    return evidence


def main():
    p = argparse.ArgumentParser()
    p.add_argument("--output", default="m5-cold-restart-evidence")
    args = p.parse_args()
    evidence = run_probe(Path(args.output))
    print("M5 cold restart:", evidence["status"],
          "; steps:", len(evidence["steps"]), "; external physical QA still required")
    if evidence["status"] != "PASS_EMULATOR_OS_FORCE_STOP_COLD_RELAUNCH":
        print("M5 failure:", evidence.get("reason"))
        raise SystemExit(1)


if __name__ == "__main__":
    main()
