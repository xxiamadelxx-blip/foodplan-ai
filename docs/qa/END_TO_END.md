# Sketched end-to-end acceptance: budget → weekly menu → in-app cart

State: WORK IN PROGRESS; M2–M6 not accepted.

Test flow:
1. Complete profile, 25 taste ratings and choose target Russian retailer and city.
2. Enter weekly limit, e.g. 2 500 ₽ vs 25 000 ₽ for the same household.
3. Build 7 days of meals respecting taste, exclusions, allergies, availability of equipment and approximate nutrition targets.
4. Read products and alternative SKUs from the chosen retailer, with price, pack size, URL, capture time, known/unknown availability, real star rating and review count when provided.
5. Generate **in-app** shopping list with actual product names and pack counts, aggregate the entire week and subtract pantry once.
6. If all required offers are observed, calculate price of the complete week; never show green budget success while offers are missing. When over budget, attempt safe comparable meal replacements and recalculate the *whole* list.
7. High budget may unlock higher-rated variants and more diverse ingredients, but never forces using all money.
8. All editing, replacement, pantry and saved list stays in FoodPlan AI. There is no order placement nor transfer to retailer cart.
9. Android test verifies UI, local persistence, unavailable retailer fallback and missing-price behavior.
10. Separate independent human review of real shop prices, allergens, recipe adequacy and useful sorting for both budgets before release.

Remaining blockers:
- Verified recipe library and safe nutrition targets, not synthetic combinations.
- Larger catalog coverage; live user-address store availability, rating metadata/price correctness.
- Price comparisons including loyalty and delivery, richer alternative SKU/review UI.
- Full acceptance and signed production APK.
