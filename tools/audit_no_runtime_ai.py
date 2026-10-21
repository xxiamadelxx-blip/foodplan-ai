#!/usr/bin/env python3
"""Static CI guard: the FoodPlan Android runtime must not depend on generative AI.

This check is intentionally independent of the food/recommendation algorithms.
Retailer HTTP adapters in android/app are allowed; network transports are not
allowed inside the offline android/domain production engine. A static check
cannot prove that arbitrary future code is safe, but rejects known LLM SDKs,
provider endpoints and runtime key references without any paid service.
"""
from pathlib import Path
import re
import sys

ANDROID_ROOT = Path(__file__).resolve().parents[1] / "android"
SOURCE_SUFFIXES = {".kt", ".kts", ".java", ".xml", ".toml", ".properties"}
PROVIDER_PATTERNS = (
    ("OPENAI_ENDPOINT", r"api\.openai\.com|\bcom\.openai\b"),
    ("GROQ_ENDPOINT", r"api\.groq\.com|\bgroq[-.]sdk\b"),
    ("DEEPSEEK_ENDPOINT", r"api\.deepseek\.com"),
    ("OPENROUTER_ENDPOINT", r"openrouter\.ai"),
    ("GOOGLE_GENERATIVE_ENDPOINT", r"generativelanguage\.googleapis\.com|google\.ai\.client\.generativeai"),
    ("ANTHROPIC_ENDPOINT", r"api\.anthropic\.com|\bcom\.anthropic\b"),
    ("MODEL_INFERENCE_ENDPOINT", r"api-inference\.huggingface\.co"),
    ("GENERATIVE_ORCHESTRATION_SDK", r"\blangchain\b|\blanggraph\b"),
    ("LLM_API_KEY_REFERENCE", r"\b(?:OPENAI|GROQ|DEEPSEEK|OPENROUTER|ANTHROPIC)_API_KEY\b"),
)
DOMAIN_NETWORK_PATTERNS = (
    ("NETWORK_TRANSPORT_IN_OFFLINE_DOMAIN",
     r"java\.net\.(?:URL|HttpURLConnection|Socket)\b|\bokhttp3\.|\bretrofit2\.|\bio\.ktor\.client\.|\bandroid\.webkit\."),
)


def audit(android_root=ANDROID_ROOT):
    root = Path(android_root)
    if not root.is_dir():
        raise ValueError("Android source tree is missing; cannot pass the no-AI audit")
    checked = 0
    problems = []
    for path in sorted(root.rglob("*")):
        if not path.is_file() or path.suffix not in SOURCE_SUFFIXES:
            continue
        # Android Gradle builds can create generated sources. Only versioned
        # production files, test source and build declarations are inspected.
        if any(part in {"build", ".gradle", ".idea"} for part in path.relative_to(root).parts):
            continue
        checked += 1
        source = path.read_text(encoding="utf-8", errors="replace")
        patterns = list(PROVIDER_PATTERNS)
        parts = path.relative_to(root).parts
        if len(parts) >= 4 and parts[0] == "domain" and parts[1:3] == ("src", "main"):
            patterns.extend(DOMAIN_NETWORK_PATTERNS)
        for code, pattern in patterns:
            if re.search(pattern, source, re.IGNORECASE):
                problems.append((path.relative_to(root).as_posix(), code))
    if checked == 0:
        raise ValueError("Zero Android files inspected; cannot approve the runtime")
    return checked, problems


def main():
    count, problems = audit()
    if problems:
        for path, reason in problems:
            print(f"FAIL NO-AI RUNTIME: {path}: {reason}")
        raise SystemExit(1)
    print(f"PASS NO-AI RUNTIME: {count} Android source/config files checked; "
          "no known generative API client or remote LLM key found.")
    print("Domain production is guarded against network transports; "
          "retailer read-only HTTP in Android app remains permitted.")


if __name__ == "__main__":
    main()
