import tempfile
import unittest
from pathlib import Path

from audit_no_runtime_ai import audit


class NoAIRuntimeAuditTests(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        self.android = Path(self.temp.name) / "android"
        self.android.mkdir()

    def put(self, relative, content):
        path = self.android / relative
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_text(content, encoding="utf-8")
        return path

    def test_offline_meal_planner_and_permitted_retailer_http_remain_valid(self):
        self.put("domain/src/main/kotlin/com/foodplan/domain/Planner.kt",
                 "package com.foodplan.domain\nobject Planner { fun plan() = 7 }")
        self.put("app/src/main/java/com/foodplan/ai/Store.kt",
                 "package com.foodplan.ai\nimport java.net.URL\n" +
                 "class Store { fun open() = URL(\"https://vkusvill.ru/search/?q=rice\") }")
        self.put("app/build.gradle.kts", 'android { namespace = "com.foodplan.ai" }')
        count, violations = audit(self.android)
        self.assertEqual(3, count)
        self.assertEqual([], violations)

    def test_any_known_llm_api_or_key_reference_is_rejected(self):
        provider_examples = [
            "https://api.openai.com/v1/chat/completions",
            "https://api.groq.com/openai/v1/chat/completions",
            "https://api.deepseek.com/chat/completions",
            "https://openrouter.ai/api/v1/chat/completions",
            "https://generativelanguage.googleapis.com/v1beta/models",
            "https://api.anthropic.com/v1/messages",
            "https://api-inference.huggingface.co/models/demo",
            "com.google.ai.client.generativeai",
            "implementation(\"com.openai:openai-java:1.0\")",
            "OPENAI_API_KEY",
            "GROQ_API_KEY",
            "implementation(\"langchain\")",
        ]
        for text in provider_examples:
            with self.subTest(text=text):
                self.put("app/src/main/java/com/foodplan/ai/AI.kt",
                         "// " + text)
                _, violations = audit(self.android)
                self.assertTrue(violations, text)

    def test_offline_domain_cannot_acquire_direct_network_transport(self):
        names = ["java.net.HttpURLConnection", "java.net.Socket",
                 "java.net.URL", "okhttp3.OkHttpClient",
                 "retrofit2.Retrofit", "io.ktor.client.HttpClient",
                 "android.webkit.WebView"]
        for name in names:
            with self.subTest(name=name):
                self.put("domain/src/main/kotlin/com/foodplan/domain/Network.kt",
                         "import " + name)
                _, violations = audit(self.android)
                self.assertIn("NETWORK_TRANSPORT_IN_OFFLINE_DOMAIN",
                              [reason for _, reason in violations])

    def test_empty_tree_is_not_approved(self):
        with self.assertRaisesRegex(ValueError, "Zero Android files"):
            audit(self.android)

    def test_generated_build_output_cannot_hide_a_runtime_dependency(self):
        self.put("app/build/generated/Bad.kt", "https://api.openai.com")
        self.put("app/src/main/java/com/foodplan/ai/Actual.kt",
                 "// https://api.groq.com")
        count, problems = audit(self.android)
        self.assertEqual(1, count)
        self.assertEqual([("app/src/main/java/com/foodplan/ai/Actual.kt", "GROQ_ENDPOINT")],
                         problems)


if __name__ == "__main__":
    unittest.main()
