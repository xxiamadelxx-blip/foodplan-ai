#!/usr/bin/env python3
"""Non-destructive M5 cold-relaunch probe for a real, prepared Android phone.

Requires a separately connected *physical* phone with FoodPlan already
installed and a three-day breakfast batch selected in the app. Never installs,
uninstalls, clears DataStore, seeds synthetic profiles, or uploads private UI.
It only opens FoodPlan, reads accessibility text, force-stops FoodPlan itself,
and verifies the previously chosen week after an actual new OS process.
"""
import argparse
from datetime import datetime, timezone
import hashlib
import json
from pathlib import Path
import re
import subprocess
import time

from m5_emulator_cold_restart import (
    COMPONENT, PACKAGE, center, has_visible_shopping_rows, matches_text,
    xml_nodes,
)

THREE_DAY_START = "Приготовить сразу на 3 дня"
THREE_DAY_LINK = "Блюдо готовится заранее на 3 дня"
NEED_RE = re.compile(r"^(?:Купить|Нужно купить:|По рецептам:)\s*(\d+)\s*г(?:\s*по рецептам)?$")
APP_TITLE = "Покупки на неделю"


class PhysicalAdb:
    def __init__(self, serial):
        if not serial or not re.fullmatch(r"[A-Za-z0-9._:-]{3,128}", serial):
            raise ValueError("EXPLICIT_DEVICE_SERIAL_REQUIRED")
        self.serial = serial

    def call(self, *args, timeout=60, check=True):
        result = subprocess.run(["adb", "-s", self.serial, *args],
                                capture_output=True, timeout=timeout, check=False)
        if check and result.returncode != 0:
            raise RuntimeError("ADB_COMMAND_FAILED: " +
                               (result.stderr.decode("utf-8", errors="replace"))[-350:])
        return result.stdout.decode("utf-8", errors="replace").strip()

    def prop(self, key):
        return self.call("shell", "getprop", key).strip()

    def read_ui(self):
        self.call("shell", "uiautomator", "dump", "/sdcard/foodplan-m5-physical.xml",
                  timeout=45)
        xml = self.call("exec-out", "cat", "/sdcard/foodplan-m5-physical.xml", timeout=25)
        return xml_nodes(xml)

    def pid(self):
        return self.call("shell", "pidof", PACKAGE, check=False).strip()


def verify_physical_device(device):
    """Fail before any device mutation if physical identity is unproven."""
    if device.prop("ro.kernel.qemu") == "1" or device.prop("ro.boot.qemu") == "1":
        raise RuntimeError("REFUSE_EMULATOR_AS_PHYSICAL")
    if device.prop("ro.build.characteristics").find("emulator") >= 0:
        raise RuntimeError("REFUSE_EMULATOR_AS_PHYSICAL")
    fingerprint = device.prop("ro.build.fingerprint")
    model = device.prop("ro.product.model")
    if len(fingerprint) < 12 or not model:
        raise RuntimeError("DEVICE_IDENTITY_UNVERIFIABLE")
    if "package:" not in device.call("shell", "pm", "path", PACKAGE, check=False):
        raise RuntimeError("FOODPLAN_NOT_INSTALLED")
    return hashlib.sha256(fingerprint.encode("utf-8")).hexdigest()


def await_visible(device, needle, seconds=35):
    end = time.monotonic() + seconds
    while time.monotonic() < end:
        try:
            nodes = device.read_ui()
            if any(matches_text(node, needle) for node in nodes):
                return nodes
            if any(matches_text(node, "Настроим питание один раз") for node in nodes):
                raise RuntimeError("PROFILE_NOT_PREPARED")
        except (ValueError, OSError):
            pass
        time.sleep(1)
    raise RuntimeError("REQUIRED_UI_NOT_VISIBLE: " + needle)


def tap(device, nodes, label):
    exact = [n for n in nodes if label in (n.get("text"), n.get("content-desc"))]
    candidates = exact or [n for n in nodes if matches_text(n, label)]
    if not candidates:
        raise RuntimeError("REQUIRED_CONTROL_NOT_VISIBLE: " + label)
    x, y = center(candidates[-1].get("bounds", ""))
    device.call("shell", "input", "tap", str(x), str(y))


def shopping_snapshot(device):
    """Read the actual weekly demand while only keeping it in local memory."""
    display = device.call("shell", "wm", "size")
    sizes = re.findall(r"(\d+)x(\d+)", display)
    if not sizes:
        raise RuntimeError("DISPLAY_SIZE_UNKNOWN")
    width, height = map(int, sizes[-1])
    needs = set()
    pack_evidence = False
    for _ in range(10):
        nodes = device.read_ui()
        pack_evidence |= has_visible_shopping_rows(nodes)
        for node in nodes:
            for attr in ("text", "content-desc"):
                match = NEED_RE.fullmatch(node.get(attr, "").strip())
                if match:
                    needs.add(int(match.group(1)))
        device.call("shell", "input", "swipe", str(width // 2),
                    str(height * 76 // 100), str(width // 2),
                    str(height * 29 // 100), "320")
        time.sleep(0.25)
    if len(needs) < 2 or not pack_evidence:
        raise RuntimeError("PREPARED_SHOPPING_LIST_NOT_VERIFIED")
    return tuple(sorted(needs))


def snapshot_prepared_week(device):
    """Requires a deliberate 3-day saved user action, never creates one."""
    nodes = await_visible(device, APP_TITLE)
    tap(device, nodes, "Рецепты")
    nodes = await_visible(device, THREE_DAY_START)
    tap(device, nodes, "День 2")
    nodes = await_visible(device, THREE_DAY_LINK)
    tap(device, nodes, "Продукты")
    await_visible(device, APP_TITLE)
    return shopping_snapshot(device)


def run_physical_probe(output: Path, device, allow_force_stop=False):
    output.mkdir(parents=True, exist_ok=True)
    proof = {
        "checked_at_utc": datetime.now(timezone.utc).isoformat(),
        "device_type": "PHYSICAL_CANDIDATE",
        "physical_android_verified": False,
        "user_data_uploaded": False,
        "installed_app_data_cleared": False,
        "synthetic_profile_seeded": False,
        "status": "FAIL_NOT_RUN",
        "steps": [],
    }
    try:
        if not allow_force_stop:
            raise RuntimeError("EXPLICIT_FORCE_STOP_OPT_IN_REQUIRED")
        proof["build_fingerprint_sha256"] = verify_physical_device(device)
        proof["steps"].append("physical_hardware_preflight")
        device.call("shell", "am", "start", "-W", "-n", COMPONENT)
        before_needs = snapshot_prepared_week(device)
        proof["steps"].append("existing_saved_three_day_batch_and_shopping_prechecked")
        old_pid = device.pid()
        if not re.fullmatch(r"\d+", old_pid):
            raise RuntimeError("PROCESS_MISSING_BEFORE_STOP")
        device.call("shell", "am", "force-stop", PACKAGE)
        if device.pid():
            raise RuntimeError("FORCE_STOP_DID_NOT_END_APP_PROCESS")
        proof["steps"].append("real_os_force_stop_no_profile_reset")
        device.call("shell", "am", "start", "-W", "-n", COMPONENT)
        new_pid = device.pid()
        if not re.fullmatch(r"\d+", new_pid) or new_pid == old_pid:
            raise RuntimeError("NEW_ANDROID_PROCESS_NOT_CONFIRMED")
        after_needs = snapshot_prepared_week(device)
        if before_needs != after_needs:
            raise RuntimeError("WEEKLY_GROCERY_DEMAND_CHANGED_AFTER_RESTART")
        proof["steps"].append("batch_day1_day2_and_weekly_grocery_demand_restored")
        proof["shopping_demands_compared"] = len(before_needs)
        proof["distinct_pid_after_relaunch"] = True
        proof["physical_android_verified"] = True
        proof["status"] = "PASS_PHYSICAL_OS_COLD_RELAUNCH"
    except Exception as exc:
        proof["status"] = "FAIL_PHYSICAL_OR_UNVERIFIED"
        proof["reason"] = str(exc)[:450]
    finally:
        (output / "evidence.json").write_text(
            json.dumps(proof, ensure_ascii=False, indent=2) + "\n",
            encoding="utf-8"
        )
    return proof


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--serial", required=True,
                        help="Explicit ADB serial of a physically connected device")
    parser.add_argument("--allow-force-stop", action="store_true",
                        help="Opt in to closing/reopening FoodPlan ONLY (never clears data)")
    parser.add_argument("--output", type=Path, default=Path("m5-physical-local-evidence"))
    args = parser.parse_args()
    report = run_physical_probe(args.output, PhysicalAdb(args.serial),
                                args.allow_force_stop)
    print("M5 physical:", report["status"], "steps:", len(report["steps"]))
    if report["status"] != "PASS_PHYSICAL_OS_COLD_RELAUNCH":
        raise SystemExit(1)


if __name__ == "__main__":
    main()
