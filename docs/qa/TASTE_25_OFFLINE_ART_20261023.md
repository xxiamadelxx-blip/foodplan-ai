# FoodPlan AI · 25 иллюстраций вкусовых карточек · 23.10.2026

## Реализовано в рабочем PR #3

- 25 отдельных оригинальных статических векторных иллюстраций блюд внутри `android/app/src/main/res/drawable-nodpi/taste_<id>.xml`. Это стилизованная графика из Android VectorDrawable, **не фотографии** и не AI-генерация во время работы приложения.
- `TasteDishArt.kt` содержит 25 явных соответствий стабильных ID блюд ресурсу `R.drawable`. При смене порядка карточек картинка не меняет блюдо. Неподдерживаемому ID возвращается `null`, но текст остаётся доступным.
- `TasteSurveyScreen.kt` показывает `Image(painterResource(...))` с `contentDescription = "Иллюстрация блюда: <название>"`. Центральная часть прокручивается, а ответы вынесены в фиксированные две строки по две кнопки для небольшого экрана.
- Android instrumentation: все 25 ID обязаны иметь валидный drawable, отображать доступное описание, четыре ответа, корректный прогресс и сохранять `NEVER_TRIED` как отдельную нейтральную оценку. Нет сетевой загрузки или внешних прав на изображения.

## Доказательства и ограничения

- Кодовой SHA [`3c9f8bb`](https://github.com/xxiamadelxx-blip/foodplan-ai/commit/3c9f8bb19660c6aefd3bcdec1258012394523e84).
- [Android CI #39961689329](https://github.com/xxiamadelxx-blip/foodplan-ai/actions/runs/39961689329): **JVM/debug build PASS, emulator PENDING** на момент фиксации этой записи. PASS всей P0-TASTE-02 не присваивать до лога и артефакта.
- Визуальное качество всех 25 стилизованных изображений не эквивалентно проверенным профессиональным фотографиям блюд. Нужна отдельная UX-приёмка, а также проверка крупного системного шрифта и TalkBack на физическом телефоне.
- Это **пострелизная UX-задача**, она не создаёт M7 и не закрывает M2 (0/150 внешних проверок приготовления), M4 (неизвестная полная стоимость) или M6 (публичный релиз blocked).

Следующее действие: принять точный CI, локально проверить, что 25 drawable вошли в debug APK, устранить ошибки эмулятора максимум в три цикла, затем отметить статус в `MILESTONES.md` и `docs/POST_RELEASE_BACKLOG.md`.


## Независимая приёмка точной версии SHA 3c9f8bb

- [Android CI #39961689329](https://github.com/xxiamadelxx-blip/foodplan-ai/actions/runs/39961689329): **PASS**, `static-and-unit` PASS, `emulator-smoke` PASS, Android **50/50 tests**, 0 failed, 0 skipped, `BUILD SUCCESSFUL`. Все 25 ресурсных ID проверены реально установленным Android-test runtime, 25 карточек пройдены с 4 типами ответов и TalkBack `contentDescription`.
- M5 внешний ADB `force-stop → холодный запуск`: **8/8 PASS** на том же кодовом SHA, **physical_android_verified=false**.
- [Artifact debug APK #12505730074](https://github.com/xxiamadelxx-blip/foodplan-ai/actions/runs/39961689329/artifacts/12505730074) скачан ZIP, APK проверен независимо: **ровно 25** файлов `res/drawable-nodpi-v4/taste_*.xml` внутри настоящего `app-debug.apk`, AndroidManifest.xml и classes.dex присутствуют. APK SHA-256 `b8c5dee767325645ab388c8996099c528320571a93a358e66ad0d57e4d918275`, 12 046 719 байт.
- [M4 live #39961689299](https://github.com/xxiamadelxx-blip/foodplan-ai/actions/runs/39961689299) PASS и [M6 unsigned #39961689263](https://github.com/xxiamadelxx-blip/foodplan-ai/actions/runs/39961689263) PASS независимо; это не завершает полноценную недельную корзину или release.
- **Технический P0-TASTE-02 = PASS для 25/25 локальных изображений, явных ресурсных ID, доступного текста, офлайн-пакета и эмулятора.** Визуальная эстетика не прошла отдельную пользовательскую приёмку и фотографии не создавались; крупный системный шрифт и физический TalkBack не проверены. Поэтому **полный UX P0-TASTE-02 PARTIAL, не release PASS**.
