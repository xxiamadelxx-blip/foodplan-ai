# CONTEXT.md · FoodPlan AI: постоянные требования и продуктовые решения

**Дата последнего уточнения: 2026-10-22. Статус: AUTHORITATIVE PRODUCT CONTEXT.**

Этот файл хранит прямые решения пользователя между рабочими сессиями. Он **не** является журналом успешных тестов или заменой исходному коду. Перед следующей задачей читать **сначала `CONTEXT.md` и `MILESTONES.md`**, затем `docs/USER_JOURNEY_CONTRACT.md` и применимые спецификации.

- **Текущее выполнение и фактические PASS/FAIL:** [MILESTONES.md](MILESTONES.md).
- **Авторитетный пользовательский сценарий:** [docs/USER_JOURNEY_CONTRACT.md](docs/USER_JOURNEY_CONTRACT.md).
- **Отложенные задачи с тестируемой приёмкой:** [docs/POST_RELEASE_BACKLOG.md](docs/POST_RELEASE_BACKLOG.md).
- **Последовательность до пользовательского релиза:** [docs/ROADMAP.md](docs/ROADMAP.md), M0 → M1 → M2 → M3 → M4 → M5 → M6. **Не придумывать M7/M8/M9 как обязательные этапы текущей дорожной карты.**

## Уточнение 23.10.2026: встроенный автоматический парсер, не ручная корзина

**Прямое требование пользователя:** Android-приложение самостоятельно собирает цены и недельную корзину из Интернета с использованием уже известных и ранее проверенных путей каталогов/публичных товарных страниц выбранной сети. Это работающий **внутри APK** парсер, а не ручной поиск цен помощником и не GitHub Actions как обязательная часть работы телефона. Цикл: сохранили магазин и бюджет → план составлен → приложение без отдельной инженерной страницы само открывает известные каталоги, читает наблюдаемые SKU/цены/фасовки, сопоставляет продукты и пересчитывает список целых упаковок и ориентировочную сумму. При смене меню/продуктов перепроверяются необходимые позиции с разумным кэшем/ограничением запросов. Адрес магазина не нужен для примерной цены сети.

**Непременные ограничения:** никаких генеративных ИИ, платных API, обязательного ПК, ручной разметки *каждой* корзины, обхода антибот-защит или выдуманных цен. Источники могут не отвечать, и тогда явно UNKNOWN; отключение сети не ломает офлайн-рецепты. Нельзя считать 10 яиц известными граммами съедобной части либо 900 мл масла равными 900 г. Публичная цена не подтверждает аллергены и наличие. Не смешивать отдельную CI-проверку парсера с его выполнением на телефоне. Пользователь видит только «Продукты», «Рецепты», «Настройки» и краткий статус обновления.

**Статус этого уточнения:** AUTHORIZED REQUIREMENT. Реализация и Gauntlet отмечаются отдельно в `MILESTONES.md`; запись здесь не является PASS.

## 1. Постоянный пользовательский контракт

FoodPlan AI предназначен для повседневной домашней еды в России. Пользователь заполняет профиль, цель, аллергии/нежелательные продукты и религиозно-диетические ограничения, кухонные приборы, размер семьи и вкусовые предпочтения. Затем пользуется **только тремя разделами**:

1. **Продукты:** выбирает сеть магазина и любой точный бюджет 100–100 000 ₽ на неделю, получает **недельный список покупок** с реальным количеством целых упаковок, ориентировочными ценами сети и явно неизвестными ценами.
2. **Рецепты:** выбирает день, завтрак/обед/ужин, желаемое усилие, другое совместимое блюдо и число последовательных дней готовки. Порции зависят от числа людей; замена меняет список.
3. **Настройки:** личные/пищевые параметры, ограничения, кухонная техника, предпочтения, повторный запуск вкусового опроса.

Внутренние продукт→рецепт связи, SKU-граф, рассуждения и аудит не показывать как четвёртый пользовательский раздел. Расчёт, рецептурное ранжирование, пересчёт корзины и локальное сохранение работают **детерминированно, без ИИ и платных API**. Публичные наблюдения цен сетей допустимы без выбора адреса. Частичная корзина **не** является подтверждённым полноценным рационом.

## 2. Уточнение пользователя: вкусовой опрос, НЕ выбор типа диеты

**Зафиксировано 2026-10-22.** Проблема пользователя относится к **25 карточкам примеров блюд для определения вкусов**, а не к обязанному выбору «веган/всеядный/халяль» через картинки.

### Что не устраивает
Текущий `android/domain/src/main/kotlin/com/foodplan/domain/TasteSurvey.kt` содержит, среди прочего, `tom_yum`, `ramen`, `shrimp_pasta`, `pad_thai`, `mushroom_risotto`. Пользователь справедливо не обязан знать эти блюда или готовить их дома каждый день. Анкета, построенная преимущественно на них, хуже выявляет привычные предпочтения.

`android/app/src/main/java/com/foodplan/ai/TasteSurveyScreen.kt` пока отображает **только название блюда, без изображения**. Ответ `NEVER_TRIED` уже есть и должен оставаться нейтральным: непопробованное **не значит нелюбимое**.

### Продуктовое решение
- Оставить **25 наглядных карточек** с фотографией/качественной иллюстрацией самого блюда, понятным названием и четырьмя действиями: «Нравится», «Нейтрально», «Не нравится», «Не пробовал».
- Основу сделать из **знакомой домашней еды**. Предварительная рабочая структура: **15 распространённых домашних блюд + 5 выразительных вкусов/текстур + до 5 более разнообразных блюд**. Это гипотеза состава теста для реализации и проверки, а не требование ежедневно готовить эти блюда.
- Не обязывать знать том-ям, рамен, пасту с креветками и прочую ресторанную еду. Заменить их в **обычном опросе** более знакомыми примерами или оставить в расширенном необязательном тесте, если он когда-либо понадобится.
- Оценивать отдельно **продукт/основу, способ приготовления, вкусовую характеристику, текстуру**. Не выводить неприязнь ко всем макаронам из неприязни к креветкам; не выводить нелюбовь к азиатской еде из «Не пробовал рамен».
- «Не пробовал» имеет вес **0**, а не «не нравится». Необходимо сохранять понятную кнопочную альтернативу и текстовое название для доступности изображения.
- Изображения доступны **офлайн**, входят в APK (законно используемые/собственные ресурсы), имеют текстовое описание и не скачиваются во время анкеты. Никаких обязательных ИИ-генераций.
- При замене примеров **не терять оценки тех карточек, чьи ID/смысл сохраняются**. Изменённые по смыслу ID версионировать, не переиспользовать старые оценки как оценки другого блюда; без насильственного сброса всего профиля.

**Статус:** РЕШЕНИЕ ЗАПИСАНО, **НЕ РЕАЛИЗОВАНО**. Работы и критерии приёмки: `docs/POST_RELEASE_BACKLOG.md`, P0-TASTE.

## 3. Домашняя применимость недельного меню

**Не путать вкусовой опрос и фактическую генерацию рациона.** Пользователь может любить рамен и не хотеть его готовить среди рабочей недели.

- Для обычного автоматического меню приоритет: простые домашние блюда, продукты российских магазинов, нормальное время готовки, повторное использование ингредиентов.
- Не превращать обычную неделю в последовательность том-яма, рамена, сложной пасты с морепродуктами и блюд с множеством редких ингредиентов.
- Отдельный контролируемый пользовательский режим: «простая домашняя еда», «иногда что-то интересное», «готов готовить сложное». **Бытовая привычность** отличается от времени/сложности приготовления.
- В обычном режиме редкие и многоэтапные блюда ограничить, а не запрещать вкусовые предпочтения пользователя. «Не пробовал» не порождает сложные блюда автоматически.

**Статус:** РЕШЕНИЕ ЗАПИСАНО, **НЕ РЕАЛИЗОВАНО В ДОСТАТОЧНО ПРОВЕРЕННОМ ВИДЕ**; см. P0-HOME в backlog.

## 4. Покупки: упаковки на первом плане, а не граммы

**Пользователь зафиксировал:** приоритет понятности **сколько купить**.

- В строке списка главным шрифтом: **«2 пачки», «1 лоток», «10 шт.», «1 бутылка», «2 банки»** в зависимости от фактически подтверждённой фасовки и единицы продажи SKU.
- Вторая строка или раскрытие: масса/объём пачки, сколько требуется на неделю, цена и дата наблюдения. Не показывать «860 г» как основное действие покупателя, если можно честно показать «1 лоток по 1 кг».
- Когда реальная фасовка/количество неизвестны, показывать **«Фасовка уточняется» / «Цена неизвестна»**, не подменять её удобным «1 уп.».
- Нельзя переводить 10 яиц в граммы упаковки без подтверждённого количества штук; нельзя преобразовывать объём в массу без валидной плотности. Внутренние нутриентные вычисления должны оставаться точными, а UI должен использовать **покупательские** единицы.
- Для яйца **не выводить в пользовательском интерфейсе «съедобная часть куриного яйца»**: это название внутреннего пищевого эталона. Для покупок «Яйца, 10 шт.» при подтверждённой десятке; для рецепта «2 яйца» там, где штуки корректны. Неподтверждённые числа не выдумывать.
- Первичный сценарий «магазин→корзина»: продукт → упаковки/штуки → ориентир суммы → при необходимости раскрытие граммов. Внутреннюю массу для КБЖУ не удалять.

**Статус:** РЕШЕНИЕ ЗАПИСАНО, **UX-ПРОВЕРКА ПОКА НЕ PASS**; см. P0-PACKS и P0-LABELS.

## 5. Сохранение после закрытия/повторного открытия

**Наблюдение пользователя 2026-10-22:** после закрытия и открытия приложения список покупок/неделя **сохраняется**. Это подтверждение обычного пользовательского сценария; **не считать этот сценарий сломанным** и не начинать его «исправлять» без воспроизводимой ошибки.

Технически `SavedWeekSelection` + Android DataStore и пересоздание Compose уже имеют отдельные тесты PASS. **Не путать** обычное закрытие/повторное открытие с ещё не доказанным стресс-сценарием: `force-stop → холодный запуск` на физическом телефоне. Последний критерий остаётся UNVERIFIED в `MILESTONES.md`, пока нет прямых доказательств.

При новом UX/миграции 25 карточек нельзя стирать недельный список, профиль, вкусовые ответы и настройки магазина без осознанного действия пользователя.

## 6. Цены и безопасность

- Цены **примерные по российской сети**, **адрес доставки не требуется**, не подтверждать кассовую цену/остатки филиала.
- Неполный список цен → полная сумма недели **UNKNOWN**. Известная часть может уже превышать бюджет. Не приписывать нулевую цену и не подменять один сырой ингредиент другим переработанным продуктом.
- Аллергии, диета, религиозные запреты и подтверждение пищевой маркировки независимы от ценового ориентирования.
- Режим 100 ₽ обязан честно отразить недостаточность рациона, а не симулировать сбалансированную неделю.

## 7. Правила выполнения изменений

1. **Каждое новое согласованное требование или уточнение сначала сохранять в `CONTEXT.md` и соответствующей спецификации/backlog**, а не только пересказывать в чате.
2. Обновлять `MILESTONES.md` после рабочего цикла: что изменено, PASS/FAIL доказательства, блокеры и следующий шаг. Не выдавать внесённый в backlog пункт за реализованный.
3. **Не расширять текущую M0–M6 дорожную карту** пострелизными изменениями. Приоритеты P0/P1/P2 означают приоритет **после релиза**, а не новые обязательные M-этапы. Отдельные дефекты существующего релизного критерия чинить по действующему протоколу.
4. Gauntlet Loop: реализация → тест → независимая проверка → точечное исправление (не больше 3 раз) → доказательная запись PASS/FAIL → переход к следующей независимой задаче.
5. Не вносить в сохранённое решение автоматические предположения, которые пользователь не подтверждал, как будто это его личное требование. Предложения/допущения помечать отдельно.


## 8. Изменение приоритета 23.10.2026: сначала основная работа приложения

**Прямое решение пользователя:** прекращаем многодневную полировку отдельных SKU (яиц, молока, масла, бананов и прочих) как подмену разработки приложения. Конечный смысл FoodPlan: автоматическая недельная корзина и рецепты по профилю/типу питания. Проверка точности отдельных упаковок не может удерживать весь проект на одной детали.

**Главный критерий продукта:** после первоначальных настроек пользователь выбирает бюджет и магазин, получает 7 дней × завтрак/обед/ужин и агрегированный список ингредиентов на неделю, может заменить рецепт, изменить усилие и количество дней/человек, после чего корзина пересчитывается и сохраняется. Всё это работает без ИИ и без актуальных цен магазина. Если цена/фасовка конкретного SKU неизвестна, недельный список и рецепты продолжают показываться; цена и возможность уложиться в бюджет честно UNKNOWN, не вымышленные.

**Рабочий приоритет:** сквозные тесты фактического приложения, корректность рациона и продуктового списка, экранные сценарии и сохранение → затем пакетная проверка каталога/цен. Никаких повторных исследований одного яйца/масла/молока после зафиксированного BLOCKED без новой независимой исходной информации. Два отдельных статуса: «меню и список работают» и «все цены/пищевые эталоны независимо подтверждены». Второе не заменяет первое и не выдаётся за PASS, если не подтверждено.


## 9. Уточнение 23.10.2026: парсер внутри Android, автоматическая реальная корзина

**Прямое обязательное требование пользователя:** источником автоматического расчёта цен является **встроенный в установленное Android-приложение парсер**, а не ручные прогоны в чате, разовые выгрузки из GitHub Actions или заранее вписанные ценники. В APK должны быть проверенные официальные пути карточек/каталогов как точки входа, **но не цены**.

**Штатный сценарий:** профиль → магазин и точный бюджет → автоматические 7 дней × приёмы пищи → агрегированные продукты → Android-парсер сам запрашивает разрешённые публичные страницы именно выбранного магазина, читает наблюдаемое название товара, фасовку и цену → реальная покупательская корзина с целыми упаковками и частичной суммой → любые изменения блюда, дней/порций/семьи или бюджета **немедленно пересчитывают количества и бюджет на телефоне** из свежих уже полученных офферов. Если набор физических товаров не изменился, для одного пересчёта корзины **не надо заново обходить сеть**. Если появились новые ингредиенты, запрашиваются новые допустимые пути; старые верифицированные предложения не теряются. Автоматическое обновление цен не должно блокировать доступ к рецептам и списку при отказе магазина.

**Безопасность:** не обращаться к запрещённым robots.txt путям, не обходить CAPTCHA/403/429 и не выдумывать фасовки, цены, остатки, массу очищенных/слитых ингредиентов, аллергенные сведения; частичная корзина и неизвестная полная сумма допустимы. Исторический поиск отдельной конкретной SKU только QA инфраструктура, **не рабочий движок**. Другие выбранные магазины не должны выдавать ценники Магнита; до реализации адаптера для них статус цены явно UNKNOWN. Никаких платных ключей, ИИ или обязательной авторизации.

**Критерий разработческой приёмки:** инструментированный Android тест с подставными официальными страницами доказывает весь поток без нажатия «сканировать»: автоматический GET/разбор товара → цена/упаковка внутри списка → пересчёт целых пачек при изменении потребности без повторного GET → сохранение/возобновление и корректный UNKNOWN при недоступном источнике. Реальные сетевые прогоны отдельно проверяют доступность источника, но не заменяют пользовательский сценарий.


## 10. Уточнение 24.10.2026: не требовать физически готовить 150 рецептов

**Прямая реакция пользователя:** требование независимого приготовления, взвешивания и фотографирования **каждого** из 150 блюд не является приемлемой целью приложения. Это избыточный критерий, ошибочно добавленный ассистентом, и он не должен искусственно удерживать M2/M6 в BLOCKED. Пользователь **не давал поручения** организовать 150 реальных готовок или двух персональных дегустаторов на каждый рецепт.

**Исправленное продуктово-инженерное правило:** 150 разных воспроизводимых домашних рецептов проверяются по независимым опубликованным инструкциям или обоснованной общепринятой технологии, с реальными весами ингредиентов, понятными шагами, временем и кухонными приборами. Расчёт КБЖУ делается из пищевых баз по определённому состоянию съедобного сырья, с учётом применимых справочных коэффициентов выхода/сохранности или имеющейся базы готовых ингредиентов. Эти показатели **оценочные**, не лабораторно измеренные; не выдумывать точные граммы готового блюда там, где данные отсутствуют. Допускается использование литературных коэффициентов, а не собственная кулинарная лаборатория на каждый рецепт.

**Проверка готовкой** проводится **выборочно и по риску** для сомнительных технологий/порций, если доступны реальные добровольные тесты. Это предложение процесса QA, **не согласованный фиксированный размер выборки** и не требование 150 приготовлений. Строгие диетические исключения сохраняются, аллергенный состав ингредиентов проверяется отдельно от сведений о следах аллергенов конкретного SKU, которые не должны считаться известными без маркировки.

**Важное следствие для кода:** нынешние `RecipeEvidenceAudit` и `m2_review_intake.py` моделируют чрезмерную схему «измеренный выход + два внешних эксперта на каждое блюдо». Пока код не мигрирован под документарную и расчётную проверку, **нельзя выдавать M2 PASS**. Однако запрещается и в дальнейших планах требовать готовить все 150 рецептов ради закрытия этапа. Не смешивать такую миграцию с ослаблением запретов по аллергиям или выдумыванием приготовленной массы.

**Методические источники:** FAO/INFOODS https://www.fao.org/4/ap810e/ap810e.pdf (расчёт готовых блюд с YF/RF; коэффициенты могут быть из литературы), EuroFIR https://www.eurofir.org/report-on-collection-of-rules-on-use-of-recipe-calculation-procedures-including-the-use-of-yield-and-retention-factors-for-imputing-nutrient-values-for-composite-foods/.
**Статус:** решение зафиксировано, критерии M2 нужно исправить в roadmap и QA, затем мигрировать технический аудит/тесты без фиктивной сертификации.

## 11. Решение пользователя 24.10.2026: два режима конструктора рецептов

**Пользователь:** «Задача создать конструктор рецептов то есть у тебя есть информация о диете ты пропускаешь через фильтр вкусовых предпочтений и фильтров диеты бюджета строишь рецепты составляешь список. И второй модуль когда человек просит заменить рецепт ты выводишь список продуктов и под них подбираешь рецепт на основе доп фильтров типа веган аллергия и любимых блюд».

Обязательный сквозной смысл:
- **A · Неделя:** настройки (диета, цель, аллергены, исключения, домашние запасы, приборы, время, количество людей) → вкусы → бюджет/магазин → подбор воспроизводимых блюд → 7 дней → один список покупок. Кошелёк не даёт отменять пищевые запреты; без полной цены не заявлять бюджетный PASS.
- **B · Замена:** в уже выбранном приёме пищи сначала показать товары текущей недели, затем выбирать рецепты по максимальному использованию продуктов из этого списка, с сохранением жёстких фильтров аллергий, веганства, запретных/нелюбимых и вкусов. При новом блюде честно показывать новые позиции и рост количества уже существующих продуктов, наглядно меняя целую недельную корзину (семья, порции, готовка несколько дней).
- Не создавать отдельную пользовательскую вкладку, показывать технологии графа пользователю или выдумывать способы приготовления с помощью ИИ. Автоматическая детерминированная композиция готовых технологий и количества ингредиентов допустима; статус опубликованного доказательства отдельный M2 gate.

[Подробный контракт, принят как задача для реализации](docs/RECIPE_CONSTRUCTOR_CONTRACT.md). **Статус на момент записи: спецификация зафиксирована, код и Android-приёмка ещё не подтверждены.**


## 2026-10-24 · Two-stage offline recipe constructor, integrated in Android

**User requirement:** dietary restrictions → taste ratings/favorites → available equipment/time/effort → weekly budget → recipes → unified grocery list. Second module: after user requests a recipe change, display weekly ingredient/product choices and build alternative recipes around the chosen products, while retaining vegan/allergen/dislike constraints and favorite dishes.

**Implemented seams, no runtime AI:** `RecipeConstructor.constructWeek` reuses `SevenDayPlanner` + `BudgetWeekRefiner` (only when actual retailer offers exist), `RecipeRanker` applies hard eligibility before taste and ingredient-network scoring, and `ShoppingAggregation` computes one shopping list for all 7 days. `RecipeConstructor.suggestReplacements` now accepts `selectedIngredientIds` and `favoriteRecipeIds`: only recipes linked to at least one chosen weekly product are candidates; hard eligibility precedes favorites, and the whole 7-day grocery list is recomputed for every preview via `RecipeBatchPlanner.apply`. Ranking considers source-product links, new product types, increased purchased quantities, preferences, and verified budget only when retailer data is complete. `fitsBudget=null` means **unknown**, not free or affordable. Product selection does not treat planned shopping as actual pantry stock.

**Android UI:** weekly recipe details → `Заменить блюдо` → `Замена из продуктов`. Show selectable weekly products, cooking effort, 1–remaining-days batch, suggested dishes, new ingredient types, exact updated basket preview and ♥ favorite controls. Apply replacement only via existing `RecipeBatchPlanner.apply` (preserves safety and weekly totals). The visible top-level tab scheme is unchanged; the extra controls are inside replacement dialog.

**Correction after the user's "Завис" report:** the production UI is `FoodPlanHomeScreen.kt`, NOT just the legacy `WeeklyPlanScreen.kt`. On the main 3-tab app's Recipes screen the "Другое блюдо" dialog now renders select/deselect product chips from the real weekly grocery list and filtered preview alternatives. Favorite ♥ buttons pass through `FoodPlanApp` to `ProfileStore.setRecipeFavorite`, persisted as `favorite_recipe_ids_v1` and restored via `ProfileSnapshot.favoriteRecipeIds`; starting a new week does not delete them. Recipe hard filters run before favorite ranking. Further remaining blockers: M2 external documentary review of 150 recipes, complete physical SKU label evidence for users with allergies, full M4 store price coverage. No API-backed LLM or fabricated prices. The preceding CI #40078046584 FAILED 1/284 JVM tests because a test falsely required an arbitrary weekly grocery ingredient to support a compatible meal substitution. Test corrected without making the recipe filter unsafely permissive; [latest exact-code Android CI #40081250873](https://github.com/xxiamadelxx-blip/foodplan-ai/actions/runs/40081250873) static-and-unit PASS, emulator pending at this recording.


## Контрольная точка YAR-6 (24.10.2026, после второй пачки)

**M2 documentary-method gate 5/150 PASS, оставшиеся 145 OPEN.** Отбор именно по самостоятельным опубликованным технологиям с документированной применимостью к нашим авторским рецептам; лабораторная готовка всех 150 не нужна. Реестр `M2PublishedMethodReviews.kt` + `M2PublishedMethodReviewBatch20261024.kt`, записи `M2_FIRST_DOCUMENTARY_REVIEW_20261024.md` и `M2_METHOD_BATCH2_20261024.md`. Два уровня защиты: точный SHA-256 авторского рецепта и отдельный SHA-256 текстовой фактологической выжимки метода; сами по себе эти SHA **не являются подписью публикации**. Независимая арифметика предварительных КБЖУ по конкретным сырьевым значениям USDA и статус UNKNOWN для cooked-yield и аллергенов покупной упаковки.

**Доказательство инженерной проверки:** [GitHub Android #40111932330](https://github.com/xxiamadelxx-blip/foodplan-ai/actions/runs/40111932330), точный исходный SHA `e4818102f0b27a60d20a79a1e2141e09b6c2bd5e`: JVM/два Kotlin модуля и debug APK PASS, **77/77 Android UI PASS**, **8/8 emulator force-stop/cold-relaunch PASS**. Первый прогон #40111639788 выявил и позволил исправить **только** 2 ошибочных тестовых счётчика (`1` в одиночном синтетическом отчёте vs `5` в реальном реестре). Изменения сохранены в git, а не только в чате.

**Дальше** продолжается YAR-6 с новыми непроверенными блюдами, без ковыряния уже зелёного конструктора или ранее подтверждённых методик. Отдельно M3/M4/M5-physical/M6 release BLOCKED.


## FoodPlan M2 · batch 3 source-method handoff (2026-10-24)

Three further candidate authored methods have actual published-source comparison and frozen Kotlin evidence records (`cur_oat_pancakes`, `cur_mushroom_pasta`, `cur_rice_egg_veg`). Independently calculated USDA-source **raw** ingredient kcal/protein/fat/carbs, SHA-pinned method summaries and recipe content fingerprints, and negative tamper tests are committed; [source details](docs/qa/M2_METHOD_BATCH3_20261024.md). Current exact executable commit `3ea53ae8ba36611bc7968a3bac4fd62f02e9f12a`, [Android CI #40118929391](https://github.com/xxiamadelxx-blip/foodplan-ai/actions/runs/40118929391). **5/150 accepted from prior Gauntlet; 8/150 pending new CI PASS.** Not equivalent to 8 clinically/cooked-nutrition-certified meals.

Unaccepted source mismatch documented: lemon peeled whole flesh FDC for an explicitly squeezed lemon juice ingredient in chickpea salad; yogurt-oat 15-minute method versus 6–12h chilled published overnight oats. Neither is currently counted. Public signed APK and physical phone still separate.


### M2 YAR-6 · third batch verified PASS (24.10.2026)

Three additional authored recipes `cur_oat_pancakes`, `cur_mushroom_pasta`, `cur_rice_egg_veg` now have source-specific documented methods and independent **raw edible** K/B/F/C, pinned recipe and excerpt SHA, and regression/negative tampering checks. [Exact source/test SHA `3ea53ae8` Android CI #40118929391](https://github.com/xxiamadelxx-blip/foodplan-ai/actions/runs/40118929391): JVM+APK PASS, Android emulator **77/77 PASS**, OS cold launch **8/8 PASS**. **Current M2 documentary 8/150 PASS, 142 methods OPEN**. No medical, cooked yield or retailer SKU allergens certified. Lemon-juice-versus-whole-lemon ingredient and 15-minute oats-versus-overnight method flagged and not counted.


### Смена темпа M2: пакетная документарная приёмка, 24.10.2026

По запросу пользователя **каждая итерация рассматривает порядка 20 самостоятельных домашних рецептов**. Автоматизировать машинно-проверяемые fingerprints, исходные питательные арифметические суммы, поиск недостающих ингредиентных базисов, CSV и негативные регрессионные тесты. Человеческая сверка **каждого реального внешнего опубликованного метода** обязательна: несовпадающие источники, небезопасные адаптации, фальшивые SHA не принимаются. Не тормозить всю партию из-за трёх отдельных отклонений, но и не переносить их в PASS. Текущий пакет [20 кандидатов, 17 сравненных источников](docs/qa/M2_BULK20_INTAKE_20261024.md) коммит `536bbecd1944658313479b80e9fc017271e91f7e` ожидает [Android CI #40120951034](https://github.com/xxiamadelxx-blip/foodplan-ai/actions/runs/40120951034). Предыдущие подтверждённые **8/150**, предложение на новый gate **25/150** после точного CI PASS. Никакой runtime AI.


### M2 bulk-by-20 PASS, 24.10.2026

Переход от 3 рецептов к **20 кандидатам на итерацию** закреплён. Первая увеличенная партия: **17/20 пригодных опубликованных технологий**, три отклонены без принудительного PASS. Фактический прогресс документарного M2 **25/150, осталось 125**. [Отчёт](docs/qa/M2_BULK20_INTAKE_20261024.md), [точный Android CI #40120951034](https://github.com/xxiamadelxx-blip/foodplan-ai/actions/runs/40120951034), код `536bbecd1944658313479b80e9fc017271e91f7e`, JVM+APK PASS, **77/77 Android**, **8/8 cold relaunch**. Дальше аналогичные пачки по 20 с отдельным учётом непрошедших, одним общим Gauntlet и без ИИ в runtime. Это документарная проверка технологии и ориентировочного КБЖУ сырья, не cooked-yield/SKU/micronutrient/full-release certification.


## M2 · 2026-10-24 · следующая крупная партия 20 (source check)

Воплощение пользовательского требования работать **пачками по 20–25, не по три**: 20 новых авторских блюд проверены на опубликованную технологию, 14 имеют source-reviewed Kotlin-записи и независимую нутриентную арифметику исходного сырья, 6 не прошли документарный gate. Подробности, URL, различия и причины отказов в [M2 bulk second QA](docs/qa/M2_BULK20_SECOND_20261024.md). **Подтверждено ранее 25/150, предложено 39/150 до Gauntlet**. Все алгоритмы приложения автономные без ИИ, выход готового блюда/этикетки SKU/цены вне этого пакета UNKNOWN.


### 2026-10-24 · M2 bulk20 second tranche ACCEPTED

**Engineering evidence:** [FoodPlan Android Actions #40128622385](https://github.com/xxiamadelxx-blip/foodplan-ai/actions/runs/40128622385), SHA `538fecd39c53dfea7e2ca66e191af9331b247f68`, JVM/debug APK PASS, 77/77 Android emulator PASS, 8/8 force-stop/cold relaunch PASS. Second tranche **20 distinct source comparisons, 14 admitted, 6 rejected**, current total **39/150 documentary methods PASS; 111 pending**. [Evidence report and separate reasons for 6 rejection](docs/qa/M2_BULK20_SECOND_20261024.md). Frozen source excerpt SHA protects our own factual synopsis, not website HTML; cooked nutrition, actual packaged allergen trace, M3 nutritional adequacy, M4 full selected-chain products/real cost, M5 physical phone and M6 public signed APK remain unverified. Workflow remains deterministic and offline without any generative AI.


## YAR-6 2026-10-25 · Large-batch preparation

Bulk third review of **20 previously unaccepted authored IDs**, not 3: nine external published methods and independent raw-edible K/P/F/C documented in [M2_BULK20_THIRD_20261025.md](docs/qa/M2_BULK20_THIRD_20261025.md), 11 rejected with reasons. Reviewed records are immutable SHA-pinned Kotlin and include source URL, own-language excerpt, differences and arithmetic. [Exact commit `e2e4130c` Android CI #40143510795](https://github.com/xxiamadelxx-blip/foodplan-ai/actions/runs/40143510795) testing. Confirmed previous 39/150; **48/150 proposed and NOT PASS until actual CI**. User-visible interface unchanged, no AI or false allergen/SKU/cooked-nutrition claims. Next large batch independent, not re-review of these 9.


## 2026-10-25 · M2 YAR-6 accepted 20-candidate bulk handoff

**48 of 150 authored methods documentary-reviewed PASS, 102 OPEN** after **20 new candidates inspected / 9 newly admitted / 11 rejected**. Source evidence and exact raw K/P/F/C: `docs/qa/M2_BULK20_THIRD_20261025.md`; registration `M2PublishedMethodBulkThird20261025.kt` and its 9-source immutable fingerprint/tamper JVM tests; previous reviewed methods cannot be overwritten. Test run [GitHub Actions #40143752638](https://github.com/xxiamadelxx-blip/foodplan-ai/actions/runs/40143752638) at executable SHA `9c364d35596fb87b8a6e111d91d8199454edf10d`: JVM + debug APK PASS, emulator Android UI 77/77 PASS, 8/8 OS cold-relaunch PASS. Prior initial FAIL 2/308 due historical test asserting rejected recipes can never be separately source-reviewed in a later batch; corrected by scoping rejections to their historical batch. No new runtime AI, remote dependency or UX sections. Method-reviewed does NOT mean published cooked food macros, safe SKU labels or release-certified nutrient adequacy. M2 still OPEN, user target bulk 20–25 independently source-reviewed candidates per iteration; next work selects fresh 20–25 from remaining 102, not repetitious acceptance of existing 48.


## FoodPlan M2 / YAR-6: Fourth bulk PASS, 25 candidate dishes (2026-10-25)

Confirmed **66/150 authored cooking methods source-documentary PASS**, up from 48/150. This iteration evaluated 25 distinct previously pending user-friendly home recipe candidates, admitted **18** new published-method matches with explicit adaptations and independently summed raw edible ingredient kcal/protein/fat/carbs, and rejected **7** with material documented defects. 84 distinct authored methods still pending, 200 generated combinatorial prototypes excluded. No requirement for physically preparing all 150.

**Executable code and tests SHA `aa0d29bb3119e086725c771a87541c339ced5266`:** [FoodPlan Android CI #40151170972](https://github.com/xxiamadelxx-blip/foodplan-ai/actions/runs/40151170972) **SUCCESS** both jobs (JVM/domain, debug APK and M2/whole-week exports; 77/77 real Android emulator instrumentation 0 failed/skipped and OS cold relaunch 8/8). Machine-auditable accepted records: `M2PublishedMethodBulkFourth20261025.kt`. Frozen source, recipe fingerprint and nutrition manifests: `docs/qa/M2_BULK25_FOURTH_20261025_EVIDENCE.json`; detailed source-linked source-versus-our-recipe comparison: `docs/qa/M2_BULK25_FOURTH_20261025.md`; negative Gauntlet: `M2PublishedMethodBulkFourth20261025Test.kt`.

**Known unadmitted source errors:** `cur_beef_cabbage_shchi` raw input includes 6g sunflower oil without any use in the cooking steps; several other candidates have unsafe unsupported raw meat methods, lentil/millet differences, binding agents missing or whole-peeled-lemon versus lemon-juice basis. Never cover these with a synthetic SHA. Publisher HTML SHA not independently authenticated; source excerpt digest only covers captured own-language method facts. Raw input macro estimates do not constitute tested cooked nutrition, complete weekly adequacy, SKU-label allergen clearance, retailer pack pricing, physical phone QA or signed public APK release.

**Continue:** process next distinct 20–25 pending recipes without re-reviewing accepted 66, then advance to independent M3/M4/M5/M6 gates per existing criteria when appropriate. YAR-6 still In Progress until 150 source-checked methods.


## 2026-10-25 · YAR-6 fifth 25-candidate methodology batch PASS

Actual current accepted published cooking-method records: **75 of 150**, **75 remain open**, following previous 66/150. This increment assessed **25 new pending recipes**, **9 independently sourced and raw-edible nutrient recomputed + pinned original SHA/digest**, **16 source/ingredients/thermal process/binder/edible-basis mismatches explicitly rejected** in [docs/qa/M2_BULK25_FIFTH_20261025.md](docs/qa/M2_BULK25_FIFTH_20261025.md). Kotlin `M2PublishedMethodBulkFifth20261025.kt` merges without override into the previously accepted 66; `M2PublishedMethodBulkFifth20261025Test.kt` covers frozen fingerprints, exact captured facts SHA, input K/P/F/C and negative tampering. Fixed legacy triage fixture now tests real review record instead of passing null. [Exact executable SHA `daeb46cf` CI #40229830630](https://github.com/xxiamadelxx-blip/foodplan-ai/actions/runs/40229830630): JVM+debug APK PASS, 77/77 Android PASS, 8/8 emulator OS cold start PASS. Prior 1/319 old-fixture FAIL corrected in single iteration. **Method documentary only**; measured cooked yield, formula retention, SKU allergen labels and public release still unknown/not passed. Next independently review a new set of 20–25 from remaining 75, do not retest unchanged approved records.


## Active M2 checkpoint · 2026-10-26, sixth 25-candidate batch PASS

**Authoritative current YAR-6 M2 state: 87 of 150 source-method documentary-reviewed PASS; 63 PENDING**. Last independent 25-candidate batch added **12** real published-method comparisons with frozen recipe/excerpt SHA, raw edible input kcal/P/F/C and adaptation notes; **13 rejected** with reasons in `docs/qa/M2_BULK25_SIXTH_20261026.md`. Source `M2PublishedMethodBulkSixth20261026.kt`; regression `M2PublishedMethodBulkSixth20261026Test.kt` including direct forged-digest negative case. [GitHub Android CI #40281353345](https://github.com/xxiamadelxx-blip/foodplan-ai/actions/runs/40281353345) SUCCESS at exact executable SHA `6af8e4cc50954423a23caac0f8788442d48a6b9e`: Kotlin JVM/domain+debug APK+documentary/week exports PASS, **77/77 real Android emulator instrumentation** 0 failures/skips, **8/8 emulator OS force-stop/cold-relaunch PASS**. No physical mobile-phone QA, measured cooked nutrient/yield, actual SKU labels, complete live retailer prices or signed release APK. Do NOT mistake the local factual excerpt SHA as authenticated original HTML. Continue next **20–25 other pending** authored dishes, while not reopening the passed 87.


## 2026-10-27 · M2 regression synchronization

The actual accepted source registry includes 87 sixth-batch reviews + 2 composed published-method reviews + 2 additional independently reviewed published methods = **91/150 documentary accepted, 59 remaining** (not the older 66/150 or 89/150 status). Existing `M2FourthBatchSourceTriageTest` still expected 89, which is stale and would fail the next fresh full JVM test run. Updated the global assertions to 91 without changing source registry or weakening source/step SHA, K/P/F/C, or food-safety checks. Exact code/test SHA `ffa6d9746b4739a84bb4a07335752685299797b0`, [CI #40417380636](https://github.com/xxiamadelxx-blip/foodplan-ai/actions/runs/40417380636) started; record PASS only once all jobs finish. No new recipe approved in this test-only commit. Continue next independent 20–25 recipe methods without blocking on one candidate; preserve composite published-operations approach with per-step sources and conservative egg casserole temperature. M2 and M3/M4/M5/M6 remain separately gated.


## 2026-10-27 · FoodPlan meal replacement must accept selected pantry products

**User workflow stays THREE tabs** (Продукты / Рецепты / Настройки). In `Рецепты → Другое блюдо`, allow users to select an ingredient present at home even when the week's buy list needs **zero** of it. Root cause: `RecipeConstructor.suggestReplacements` intersected every explicitly selected ingredient with `oldTypes` from `current.shopping`; selected pantry-only ingredients became an empty set and suppressed every alternative. Further, the explicit ingredient-link preference mistakenly counted only shopping reuse. The fixed code uses the selected catalog ingredient IDs directly and ranks candidates by their actual matching ingredient links; empty selection still defaults to weekly shopping, all diet/allergy/equipment restrictions still precede ranking, no runtime AI, whole-week basket and budgets are recalculated, UNKNOWN pricing remains UNKNOWN.

**Implemented:** `RecipeConstructor.kt`, both `FoodPlanHomeScreen.kt` (production three-tab UI) and `WeeklyPlanScreen.kt` (legacy UI) now expose products already at home, with `· дома` on stock-only chips. `RecipeConstructorProductSelectionTest.kt` reproduces out-of-basket explicit choice and blocked allergy; `RecipeReplacementPantryAndroidTest.kt` inspects Compose chip visibility/selection. [RED CI #40462315394](https://github.com/xxiamadelxx-blip/foodplan-ai/actions/runs/40462315394) had **337 JVM tests, 1 genuine regression FAIL**, other JVM tests passed. [Candidate #40462792576](https://github.com/xxiamadelxx-blip/foodplan-ai/actions/runs/40462792576) compiled product APK+domain JVM green but failed test APK compilation because of a redundant `assertExists` import; removed without changing production behavior. Exact source/test commit **`426203dd1330ddb4fb5be39b34ca5009e5020401`** [GitHub Android CI #40463097230](https://github.com/xxiamadelxx-blip/foodplan-ai/actions/runs/40463097230): `static-and-unit` PASS; emulator result must be checked before overall PASS claims.

**Milestones not auto-closed:** 91/150 M2 documentary-reviewed (59 remaining), M3 nutritional sufficiency, M4 complete real chosen-store pack price, M5 physical phone, M6 permanently signed public APK remain independent release gates. Green debug APK is not M6 signed distribution.
