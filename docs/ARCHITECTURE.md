# Архитектура

Android Kotlin + Jetpack Compose, offline-first и проверяемые расчёты. Нет обязательного сервера или AI API.

## Слои
1. UI (Compose): анкета, 25 карточек вкуса, меню, рецепты, корзина.
2. Профиль (DataStore Preferences для небольших настроек в M1; далее Room для структурированных рецептов/планов/остатков/истории).
3. Domain: валидатор профиля, фильтр ингредиентов/аллергенов, калькулятор питательности, подборщик меню, расчёт корзины.
4. Data: проверенный локальный каталог, кэш источников пищевой ценности, отдельные адаптеры цен российских сетей.
5. Integrations: только opt-in; ненадёжный или отозванный источник цен не блокирует локальный каталог.

## Модель
Profile(id, age, sex, bodyMeasures, activity, goal, budget, city, stores[], diet, allergens[], exclusions[], household, kitchen, cookingEffort, cookingMinutes).
Recipe(id, ingredientRefs[quantity,basis], steps, yield, tags, allergens, completeness, nutritionSource, provenance).
PriceOffer(ingredientId, storeId, city, packGrams, priceKopeks, observedAt, provenance, status).
PantryStock(ingredientId, quantityGrams).
WeekPlan(days[7], meals[], recipe+portions, nutritionSummary, costingStatus).
Basket(lines[need, pantryUsed, packCount, packCost], verified, unavailable, total).
PreferenceVote(dishId, one of LIKE/OK/DISLIKE/UNKNOWN).

## Потоки
Профиль → безопасные ограничения → каталог → кандидаты → целевая функция для недели → граммовки → объединённый список ингредиентов → вычет остатков → упаковки → предложения магазинов → бюджет и его достоверность → UI.
При изменении блюда инвалидация сметы производится атомарно по всей неделе.

## Валидация
Domain не обращается к сети и не зависит от Android. Цена хранится в целых копейках, граммы в целых единицах. Исключить переполнения при суммировании и умножении. Весь вход из внешних API недоверенный. Даты цен должны быть в локальном времени региона.

## Cooking effort (M1 extension)
DataStore key `cookingEffort` stores stable FAST/INVOLVED/ALL_OUT identifiers, not locale-dependent text. Backward-compatible missing value is FAST. Domain `RecipePreparationRules` currently provides deterministic metadata classification by active minutes, techniques, dishes to wash, and kitchen availability; real recipe records will be populated in M2. Maximum total preparation time is independent. `ALL_OUT` allows simpler recipes. Unsupported equipment is always an exclusion, irrespective of effort level.
