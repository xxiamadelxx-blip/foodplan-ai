# Gauntlet QA · 20.10.2026
| Тест | Статус | Доказательство |
|---|---|---|
| GitHub репозиторий и запись | PASS | https://github.com/xxiamadelxx-blip/foodplan-ai |
| M0 контракт/fixtures на GitHub CI | PASS | initial run #39422189757 |
| Первичная настройка SDK | FAIL → CORRECTED, pending rerun | initial run #39422189757: obsolete setup-android tried missing package `tools`; switch to runner's preinstalled SDK |
| Kotlin unit и Android APK | PENDING | новая сборка обязательна |
| Android emulator UI tests | PENDING | должна завершиться успешно после сборки |
| М1 хранение профиля после перезапуска | NOT VERIFIED | нужно проверять на эмуляторе |
| Полные российские цены | NOT IMPLEMENTED | только модель и стратегия |
Никакой код или статический аудит не являются доказательством работающего APK.
