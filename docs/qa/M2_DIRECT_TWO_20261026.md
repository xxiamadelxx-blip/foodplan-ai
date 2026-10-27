# FoodPlan YAR-6 · Two additional source-matched authored recipes · 26 October 2026

## Admission scope and limits

These two methods come from the **currently pending 63 authored recipes** identified after the sixth documentary batch. They were independently compared to genuine published original cooking instructions. Exact frozen authored fingerprints and independently tabulated raw edible-input K/P/F/C are recorded, and mismatches are disclosed. They do not represent laboratory cooked nutrient assays, cooking yield measurements, real-retailer product labels, allergen trace clearance or a physical cooking trial.

### Grечневая запеканка с грибами и сыром · `cur_mushroom_cheese_buckwheat_bake`

- Published method: [Lifehacker, Irina Sakharova, buckwheat mushroom cheese egg casserole](https://lifehacker.ru/recipe/zapekanka-s-grechkoj-gribami-i-syrom/). The original boils dry buckwheat, separately fries chopped onion and mushrooms in vegetable oil, assembles an egg binder in an oven dish, tops with grated cheese and bakes.
- FoodPlan adaptation: 90g dry buckwheat, 200g white mushrooms, 70g onion, 75g Gouda, 65g raw edible egg and 9g sunflower oil. FoodPlan omits the publisher's milk, and uses less egg binder with a combined mushroom/onion frying step. The source's set baking duration, texture and energy absorption cannot be transferred to this smaller recipe. Explicitly **fixed a real defect**: the published draft previously included 9g oil in raw nutrient math but never used it in any instruction. The current step fries mushrooms and onion with sunflower oil.
- Authored recipe SHA-256: `d8318b258464e01ad2788ea7650fe5beb0e8c85465a9da9c855efc5ec7ec6b51`
- Local captured factual-method summary SHA-256: `8957acba4e9f0297ff433967afea4cd490b71fa9c3e5112637c93e0188e7563a`.
- Independent edible raw contribution sum from fixed pinned FDC constants: **822.91 kcal; 44.272g protein; 39.243g fat; 82.802g carbohydrate**.
- The authored instructions now specify checking **74 °C at the casserole centre**, conservatively following USDA FSIS casserole safety guidance, rather than declaring cooked-egg safety from browned surface alone.

### Овсяные вафли с грушей · `cur_oat_waffles_pear`

- Published methods: [Arla UK oat waffles](https://www.arlafoods.co.uk/recipes/no-cheat-treat-waffles/) and [Vitamix oat waffles](https://www.vitamix-europe.com/en-ch/blogs/recipes/oat-waffles).
- FoodPlan adaptation: 70g dry oat flakes, 60g edible egg, 90g milk, 120g fresh Anjou pear and 6g sunflower oil. Oats are blended to coarse flour, mixed with egg and milk, the batter rests before being cooked in a greased waffle iron and is served with a freshly sliced pear topping. Original publications include longer batter rest times, baking powder and/or wheat flour; their light texture is not certified for this minimalist 5-minute-rest formula. The pear is a separate FoodPlan serving preference, NOT a purported ingredient in the publisher's recipe.
- Authored recipe SHA-256: `3c6825cd7139eee4e080142d16afd49a9e36ef4663ca2f29f11b80b94a0b02d6`.
- Local captured factual-method summary SHA-256: `0ed1dcc76ffa611e2be59a6809f6b11a61dee953f4d7c1941032396563235ef4`.
- Independent raw edible-input arithmetic: **535.43356 kcal; 19.855g protein; 19.92856g fat; 69.98888g carbohydrates**.
- The authored cooking step now requires a complete **71 °C internal temperature** for its egg batter, not a mere hot waffle iron.

## QA and release gate

Both frozen review records must pass `RecipeEvidenceAudit.inspect()` and raw input double-entry arithmetic and tamper tests in a real JVM CI run before being counted. The local excerpt digest is **not the HTML digest of a live publisher page**. The 200 generated templates stay outside the 150 original authored dishes. Prior accepted methods are immutable and cannot be overwritten by this intake.

This QA artifact is a documentary review, not a public-health or medical certification. M2 remains OPEN until all 150 authored recipes pass independently sourced method applicability and raw input arithmetic; M3–M6 are separate release gates.
