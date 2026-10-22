# Gauntlet: бюджетное меню + внутренняя корзина

Дата: 2026-10-20. Стадия: M3/M4, прототип.

## Подтверждённые проверки, требующие актуального CI
- BudgetBasketOptimizerTest: 2 500 и 25 000 ₽ на одинаковых **синтетических** предложениях.
- BudgetBasketOptimizerTest: не форсировать дорогой, низкооценённый вариант при большом лимите.
- BudgetBasketOptimizerTest: 2 500 ₽ при минимальной доступной полной корзине дороже потолка → OVER_BUDGET.
- BudgetBasketOptimizerTest: отсутствие/региональное несовпадение цены → UNKNOWN; нет в наличии → исключить.
- BudgetWeekRefinerTest: без полного каталога и актуальных цен недельное бюджетное планирование UNVERIFIED.
- MagnitDomReaderTest/MagnitBrowserDomTest: реальный HTML-паттерн карты рейтинга/числа отзывов, проверка парсинга на имитации WebView.

В пользовательской корзине теперь показываются:
1. Неделя и рецепты.
2. Названия SKU, фасовки, число упаковок, сумма на неделю.
3. Рейтинг и число отзывов (если сайт их предоставляет).
4. Известный/неизвестный остаток.
5. Статус «помещается в бюджет», «перерасход» или «не хватает цен».
6. Действие «Подогнать меню под бюджет и ассортимент», корректировка по доступным вариантам без принудительной траты всего лимита.

## Риски, запрещающие PASS
- Все цены и отзывы из live Magnit WebView ещё не независимо проверены на адресе покупателя.
- В библиотеке 200 комбинаторных черновых рецептов, а не 150 официально верифицированных.
- Изменение меню ограничено эвристикой до 5 экономящих замен, а не доказательным глобальным оптимумом.
- Учёт доставки и персональных скидок/карт лояльности не завершён.
- Сайты других сетей не интегрированы.
- Абсолютная безопасность по аллергенам не доказана.
- Отсутствующие цены/наличие не могут быть заменены выдуманными.

## 20 October 2026: Catalog alternatives + diverse recipes

- GitHub main source: `android/domain/src/main/kotlin/com/foodplan/domain/CatalogCandidateSelector.kt`
  - UI can retain up to 12 matching alternatives per ingredient from the currently browsed official Magnit catalog page.
  - Each stored SKU preserves package grams, displayed price, observed rating, review count, city/shop and observed time. Missing ratings/stock are NOT set to zero or in stock.
  - In-app `BudgetBasketOptimizer` evaluates variants against TOTAL WEEKLY spending limit; low and generous budgets can produce distinct choices. No retailer-side shopping basket is modified.
  - Unit test explicitly compares 180 ₽ and 25,000 ₽ for matching rice SKUs with independently authored synthetic reviews. Those fixture amounts do not claim anything about market prices.
- Week diversity: `MealReplacement.alternatives(..., maxResults)` now searches beyond 12 highly similar recipes when `BudgetWeekRefiner` is given a generous budget. Domain test starts from repetitive chicken and expects a larger protein set while still fitting the synthetic basket budget.
- CI evidence: [run #39454484643](https://github.com/xxiamadelxx-blip/foodplan-ai/actions/runs/39454484643): domain tests + Android APK build **PASS**. Android emulator job **PENDING** at documentation update time.
- **Still NOT ACCEPTED:** real full-catalog scanning, verified user-address stock, 150 independently verified recipes, traceable nutrition, broad retailer support, full price-driven optimization and release APK.

## 21 October 2026 · Retail product-name completeness (not retailer approval)

- Formerly unsupported: egg, cottage_cheese, flour, onion, garlic, cucumber, bell_pepper, lemon, lentils, yogurt and cheese. These 11 could leave shopping lines without candidate price despite a readable name.
- Matcher now explicitly handles **37/37 current ingredient IDs**, with test protection against new unhandled IDs. Synonym matching is only a **necessary condition** for price candidate selection; it is not chemical food equivalence or inspected physical packaging.
- Reject known wrong states: dried garlic/lemon in place of raw; pickled cucumber; canned lentils in place of dry; sweetened yogurt instead of unsweetened; processed Gouda; different-fat/unbranded curd instead of exact ROSMOL 5% 350 g; dry chickpeas instead of drained canned ingredients. For canned chickpeas, net drained grams and exact product label still require confirmation.
- Live retailer snapshots remain sporadic and address-specific prices unverified. **No synthetic test or generic USDA record establishes actual buyer-address price, checkout availability, full nutrient adequacy or allergen safety.**
