#!/usr/bin/env python3
"""Structural and schema-fixture verification; DOES NOT build Android."""
import json
from pathlib import Path
from jsonschema import Draft202012Validator, FormatChecker

root = Path(__file__).resolve().parents[1]
checks = [
    "README.md", "docs/PRD.md", "docs/ARCHITECTURE.md",
    "docs/DATA_AND_PRICING.md", "docs/PLANNER.md",
    "docs/SAFETY_PRIVACY.md", "docs/qa/ACCEPTANCE.md",
    "android/settings.gradle.kts", "android/app/build.gradle.kts",
    "android/domain/build.gradle.kts",
    "android/app/src/main/java/com/foodplan/ai/MainActivity.kt",
    "android/app/src/main/java/com/foodplan/ai/ProfileStore.kt",
    "android/app/src/main/java/com/foodplan/ai/ProfileFormStep.kt",
    "android/domain/src/main/kotlin/com/foodplan/domain/Profile.kt",
    ".github/workflows/android-ci.yml",
]
for name in checks:
    assert (root / name).is_file(), f"Missing {name}"
    print("PASS file", name)
for name in ("profile", "recipe", "offer", "week_plan"):
    schema = json.loads((root / f"contracts/{name}.schema.json").read_text())
    Draft202012Validator.check_schema(schema)
    instance = json.loads((root / f"fixtures/{name}.example.json").read_text())
    Draft202012Validator(schema, format_checker=FormatChecker()).validate(instance)
    print("PASS schema fixture", name)
print("STATIC AUDIT PASS (not Android build)")
