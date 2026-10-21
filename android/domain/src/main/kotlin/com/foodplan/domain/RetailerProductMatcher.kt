package com.foodplan.domain

/** Prevents applying a price of an unrelated SKU to a grocery ingredient. */
object RetailerProductMatcher {
    private val aliases = mapOf(
        "rice" to listOf("рис"), "buckwheat" to listOf("гречка","гречневая крупа"),
        "pasta" to listOf("макароны","спагетти","паста"),
        "potato" to listOf("картофель","картошка"),
        "oats" to listOf("овсяные хлопья","геркулес"),
        "millet" to listOf("пшено"), "milk" to listOf("молоко"),
        "apple" to listOf("яблоки","яблоко"),"banana" to listOf("банан","бананы"),
        "pear" to listOf("груша","груши"),
        "berries" to listOf("клубника","земляника"),
        "chicken" to listOf("курица","куриное","куриный","куриной",
            "цыпленка","цыпленок","цыпленка-бройлера"),
        "turkey" to listOf("индейка","индейки","индюш"),
        "beef" to listOf("говядина","говяж"),
        "pork" to listOf("свинина","свин"),
        "cod" to listOf("треска","трески"),
        "salmon" to listOf("лосось","семга","сёмга"),
        "tofu" to listOf("тофу"),"chickpeas" to listOf("нут"),
        "broccoli" to listOf("брокколи"),"carrot" to listOf("морковь","морков"),
        "zucchini" to listOf("кабачок","кабачки"),
        "cabbage" to listOf("капуста","капуст"),
        "tomato" to listOf("помидоры","помидор","томаты","томат"),
        "mushroom" to listOf("шампиньоны","шампиньон","грибы"),
        "oil" to listOf("масло подсолнечное","подсолнечное масло"),
        "egg" to listOf("яйцо","яйца"),
        "cottage_cheese" to listOf("творог"),
        "flour" to listOf("мука пшеничная","пшеничная мука"),
        "onion" to listOf("лук репчатый","репчатый лук"),
        "garlic" to listOf("чеснок"),
        "cucumber" to listOf("огурец","огурцы"),
        "bell_pepper" to listOf("перец сладкий","сладкий перец",
            "перец болгарский","болгарский перец"),
        "lemon" to listOf("лимон","лимоны"),
        "lentils" to listOf("чечевица","чечевицы"),
        "yogurt" to listOf("йогурт"),
        "cheese" to listOf("гауда")
    )

    /** All catalogue ingredients must have an explicit name policy. */
    fun supportedIngredientIds(): Set<String> = aliases.keys

    fun matchesIngredient(ingredientId: String, productName: String): Boolean {
        val fragments=aliases[ingredientId] ?: return false
        val title=productName.lowercase().replace('ё','е')
        // The food-composition record is for a specific physical state, not for
        // a prepared meal or similarly named sauce. Ambiguous SKUs fail closed.
        val processed = Regex(
            """(?<![\p{L}])(?:отварн[а-я]*|варен[а-я]*|жарен[а-я]*|запечен[а-я]*|готов[а-я]*|копчен[а-я]*|маринован[а-я]*|гриль|шницель|котлет[а-я]*|панировк[а-я]*|пюре|чипсы|фри|в соусе|в сиропе|с овощами|с мясом|с курицей)(?![\p{L}])"""
        )
        // Real VkusVill public search includes cooked chicken meals, thighs,
        // marinated breasts and raw breast under the same search query.
        // Our source nutrition anchor is *raw chicken breast fillet*, not any
        // chicken meat. Search relevance alone never proves SKU equivalence.
        if (ingredientId == "chicken") {
            val unprocessedBreast =
                Regex("""(?<![\p{L}])филе(?![\p{L}])""").containsMatchIn(title) &&
                "грудк" in title &&
                (title.contains("курин") || title.contains("цыпленк"))
            if (!unprocessedBreast) return false
            if (listOf(
                    "запеч", "гриль", "паниров", "маринад", "соус",
                    "корочк", "пряност", "травами", "приправ", "лимонный перец",
                    "с перцем", "в специях", "в масле", "с овощами",
                    "с цукини", "с моцарелл", "шницель", "вялен"
                ).any { it in title }) return false
        }
        val basis=IngredientWeightBasisRegistry.basisFor(ingredientId)
        // SKU display titles cannot establish manufacturer/allergen equivalence.
        // Extra checks only prevent *known* substitutions for differently sourced foods.
        if (ingredientId == "cottage_cheese" &&
            (!title.contains("росмол") || !title.contains("5%") ||
                !Regex("""(?<!\d)350\s*г(?![\p{L}])""").containsMatchIn(title))) return false
        if (ingredientId == "cheese" &&
            listOf("плавлен", "творожн", "сырн").any { it in title }) return false
        if (ingredientId == "yogurt" &&
            (!title.contains("натуральн") ||
                listOf("с сахаром", "сладк", "фрукт", "ягод", "обезжир").any { it in title })) return false
        if (ingredientId == "egg" && "перепел" in title) return false
        if (ingredientId == "chickpeas" &&
            (!title.contains("консерв") || "сух" in title)) return false
        if (basis == IngredientWeightBasis.RAW_EDIBLE &&
            listOf("сушен", "сушён", "солен", "солён", "консерв",
                "порош", "гранул", "приправа", "вялен", "вялен",
                "заморож").any { it in title }) return false
        if (basis == IngredientWeightBasis.DRY_UNCOOKED &&
            listOf("консерв", "готовое блюдо", "быстрого приготовления",
                "в соусе").any { it in title }) return false
        if (basis in setOf(IngredientWeightBasis.RAW_EDIBLE,
                IngredientWeightBasis.DRY_UNCOOKED,
                IngredientWeightBasis.FROZEN_UNCOOKED) &&
            processed.containsMatchIn(title)) return false
        // Nutritional evidence for 'berries' is specifically frozen, unsweetened
        // strawberries. Not fresh raspberries, mixed berries or sweetened packs.
        if (ingredientId=="berries" &&
            (!title.contains("заморож") || title.contains("с сахаром") ||
                title.contains("сироп") || title.contains("смесь"))) return false
        if (ingredientId=="pasta" &&
            (title.contains("паста томат") || title.contains("паста орех") ||
                title.contains("паста шоколад"))) return false
        // USDA FDC 2512381 is dry raw WHITE LONG-GRAIN rice. These explicitly
        // different varieties/preparations have different published macros.
        // A retail product needs its own source record before being considered
        // nutritionally equivalent to that generic ingredient.
        if (ingredientId=="rice" &&
            listOf("круглозер", "пропарен", "бурый", "нешлифован",
                "дикий", "смесь", "для плова").any { it in title }) return false
        return fragments.any { term ->
            val escaped=Regex.escape(term.lowercase().replace('ё','е'))
            Regex("(?<![\\p{L}])$escaped(?![\\p{L}])").containsMatchIn(title)
        }
    }
}
