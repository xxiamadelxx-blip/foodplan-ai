package com.foodplan.domain

/**
 * Generated recipe prototypes. All nutrient values below are approximate example values per
 * 100 g of the named ingredient, NOT verified Russian product labels or a FoodData Central import.
 * Must be replaced / verified before nutrition claims or M2 final acceptance.
 */
data class FoodIngredient(
    val id: String, val name: String, val kcal100: Double, val protein100: Double,
    val fat100: Double, val carbs100: Double, val packGrams: Int,
    val allergens: Set<String> = emptySet(), val group: String = "plant",
    val nutrientSourceReference: String? = null,
    val allergenCompositionReviewed: Boolean = false
)
data class RecipeIngredient(val id: String, val grams: Int)
data class OfflineRecipe(
    val id: String, val name: String, val type: MealType,
    val ingredients: List<RecipeIngredient>, val steps: List<String>,
    val preparation: RecipePreparation, val tags: Set<String>,
    val nutritionVerified: Boolean = false,
    val evidenceStatus: RecipeEvidenceStatus = RecipeEvidenceStatus.GENERATED_TEMPLATE,
    val sourceReference: String? = null,
    /** Measured mass of the edible finished dish, not a sum of raw ingredients. */
    val cookedYieldGrams: Int? = null,
    val cookedYieldSourceReference: String? = null
)
enum class RecipeEvidenceStatus { GENERATED_TEMPLATE, CURATED_DRAFT, INDEPENDENTLY_VERIFIED }
enum class MealType { BREAKFAST, MAIN }
data class Nutrition(val kcal: Double, val protein: Double, val fat: Double, val carbs: Double) {
    operator fun plus(other: Nutrition): Nutrition =
        Nutrition(kcal + other.kcal, protein + other.protein, fat + other.fat, carbs + other.carbs)
    operator fun times(factor: Double): Nutrition =
        Nutrition(kcal * factor, protein * factor, fat * factor, carbs * factor)
    companion object { val ZERO = Nutrition(0.0, 0.0, 0.0, 0.0) }
}

object OfflineRecipes {
    val ingredients: Map<String, FoodIngredient> = listOf(
        FoodIngredient("rice","Рис сухой",358.705,7.03885,1.033,80.31315,900,
            nutrientSourceReference="https://fdc.nal.usda.gov/food-details/2512381/nutrients"),
        FoodIngredient("buckwheat","Гречка ядрица, обжаренная, сухая",346.0,11.73,2.71,74.95,900,
            nutrientSourceReference="https://fdc.nal.usda.gov/food-details/170685/nutrients"),
        FoodIngredient("pasta","Макароны сухие, необогащённые",371.0,13.04,1.51,74.67,450,setOf("wheat"),
            nutrientSourceReference="https://fdc.nal.usda.gov/food-details/168927/nutrients"),
        FoodIngredient("potato","Картофель с кожурой, сырой",77.0,2.05,0.09,17.49,1000,
            nutrientSourceReference="https://fdc.nal.usda.gov/food-details/170026/nutrients"),
        FoodIngredient("oats","Овсяные хлопья сухие, без обогащения",379.0,13.15,6.52,67.7,400,setOf("wheat"),
            nutrientSourceReference="https://fdc.nal.usda.gov/food-details/173904/nutrients"),
        FoodIngredient("millet","Пшено сухое",378.0,11.02,4.22,72.85,800,
            nutrientSourceReference="https://fdc.nal.usda.gov/food-details/169702/nutrients"),
        FoodIngredient("milk","Молоко цельное 3,25%, без добавленных витаминов",61.0,3.15,3.27,4.78,1000,setOf("milk"),"dairy",
            nutrientSourceReference="https://fdc.nal.usda.gov/food-details/172217/nutrients"),
        FoodIngredient("apple","Яблоки с кожурой, сырые",52.0,0.26,0.17,13.81,1000,
            nutrientSourceReference="https://fdc.nal.usda.gov/food-details/171688/nutrients"),
        FoodIngredient("banana","Бананы, сырые",89.0,1.09,0.33,22.84,1000,
            nutrientSourceReference="https://fdc.nal.usda.gov/food-details/173944/nutrients"),
        FoodIngredient("pear","Груша сорта Анжу, с кожурой (сырая)",63.6613,0.3125,0.3713,14.7674,1000,
            nutrientSourceReference="https://fdc.nal.usda.gov/food-details/2710836/nutrients"),
        FoodIngredient("berries","Клубника замороженная, без сахара",35.0,0.43,0.11,9.13,300,
            nutrientSourceReference="https://fdc.nal.usda.gov/food-details/168173/nutrients"),
        FoodIngredient("chicken","Куриное филе сырое",106.034,22.525,1.934,0.0,800,
            group="meat",nutrientSourceReference="https://fdc.nal.usda.gov/food-details/2646170/nutrients"),
        FoodIngredient("turkey","Грудка индейки без кожи, мясо сырое",114.0,23.66,1.48,0.14,800,group="meat",
            nutrientSourceReference="https://fdc.nal.usda.gov/food-details/171098/nutrients"),
        FoodIngredient("beef","Говядина, пашина (постный стейк без внешнего жира), сырая",149.0,21.72,6.29,0.0,700,group="meat",
            nutrientSourceReference="https://fdc.nal.usda.gov/food-details/168609/nutrients"),
        FoodIngredient("pork","Свинина, корейка с постной частью и жиром, сырая",198.0,19.74,12.58,0.0,700,group="meat",
            nutrientSourceReference="https://fdc.nal.usda.gov/food-details/167818/nutrients"),
        FoodIngredient("cod","Треска атлантическая, дикая (сырая)",65.9855,16.06875,0.6675,0.0,600,
            setOf("fish"),"fish",
            nutrientSourceReference="https://fdc.nal.usda.gov/food-details/2684444/nutrients"),
        FoodIngredient("salmon","Лосось атлантический, выращенный (сырой)",197.294,20.31875,13.11,0.0,500,
            setOf("fish"),"fish",
            nutrientSourceReference="https://fdc.nal.usda.gov/food-details/2684441/nutrients"),
        FoodIngredient("tofu","Тофу твёрдый, сульфат кальция и нигари",78.0,9.04,4.17,2.85,250,setOf("soy"),
            nutrientSourceReference="https://fdc.nal.usda.gov/food-details/172448/nutrients"),
        FoodIngredient("chickpeas","Нут консервированный (без рассола)",139.0,7.05,2.77,22.53,400,
            nutrientSourceReference="https://fdc.nal.usda.gov/food-details/173800/nutrients"),
        FoodIngredient("broccoli","Брокколи (сырая)",32.0,2.57,0.34,6.29,400,
            nutrientSourceReference="https://fdc.nal.usda.gov/food-details/321900/nutrients"),
        FoodIngredient("carrot","Морковь сырая",41.0,0.93,0.24,9.58,1000,
            nutrientSourceReference="https://fdc.nal.usda.gov/food-details/170393/nutrients"),
        FoodIngredient("zucchini","Кабачки с кожурой, сырые",17.0,1.21,0.32,3.11,600,
            nutrientSourceReference="https://fdc.nal.usda.gov/food-details/169291/nutrients"),
        FoodIngredient("cabbage","Капуста белокочанная (сырая)",31.4275,0.96125,0.2275,6.38375,1000,
            nutrientSourceReference="https://fdc.nal.usda.gov/food-details/2346407/nutrients"),
        FoodIngredient("tomato","Помидоры красные спелые, сырые",18.0,0.88,0.2,3.89,500,
            nutrientSourceReference="https://fdc.nal.usda.gov/food-details/170457/nutrients"),
        FoodIngredient("mushroom","Шампиньоны белые, сырые",22.0,3.09,0.34,3.26,400,
            nutrientSourceReference="https://fdc.nal.usda.gov/food-details/169251/nutrients"),
        FoodIngredient("oil","Масло подсолнечное, линолевое (около 65%)",884.0,0.0,100.0,0.0,1000,
            nutrientSourceReference="https://fdc.nal.usda.gov/food-details/171025/nutrients"),
        FoodIngredient("egg","Куриное яйцо (съедобная часть)",143.0,12.4,9.96,0.96,600,
            setOf("egg"),"egg",
            nutrientSourceReference="https://fdc.nal.usda.gov/food-details/748967/nutrients"),
        FoodIngredient("cottage_cheese","Творог РОСМОЛ 5%, 350 г",121.0,16.0,5.0,3.0,350,setOf("milk"),"dairy",
            nutrientSourceReference="https://rossmol.ru/tvorog"),
        FoodIngredient("flour","Мука пшеничная общего назначения, необогащённая",362.0,12.0,1.7,74.6,1000,
            setOf("wheat"),
            nutrientSourceReference="https://fdc.nal.usda.gov/food-details/790018/nutrients"),
        FoodIngredient("onion","Лук репчатый сырой",40.0,1.1,0.1,9.34,1000,
            nutrientSourceReference="https://fdc.nal.usda.gov/food-details/170000/nutrients"),
        FoodIngredient("garlic","Чеснок (сырой)",143.0,6.62,0.38,28.2,100,
            nutrientSourceReference="https://fdc.nal.usda.gov/food-details/1104647/nutrients"),
        FoodIngredient("cucumber","Огурцы с кожурой (сырые)",15.9075,0.625,0.1775,2.9525,500,
            nutrientSourceReference="https://fdc.nal.usda.gov/food-details/2346406/nutrients"),
        FoodIngredient("bell_pepper","Перец сладкий красный, сырой",26.0,0.99,0.3,6.03,500,
            nutrientSourceReference="https://fdc.nal.usda.gov/food-details/170108/nutrients"),
        FoodIngredient("lemon","Лимон без кожуры, сырой",29.0,1.1,0.3,9.32,250,
            nutrientSourceReference="https://fdc.nal.usda.gov/food-details/167746/nutrients"),
        FoodIngredient("lentils","Чечевица сухая",352.0,24.63,1.06,63.35,450,
            nutrientSourceReference="https://fdc.nal.usda.gov/food-details/172420/nutrients"),
        FoodIngredient("yogurt","Йогурт натуральный из цельного молока, без добавок",61.0,3.47,3.25,4.66,400,setOf("milk"),"dairy",
            nutrientSourceReference="https://fdc.nal.usda.gov/food-details/171284/nutrients"),
        FoodIngredient("cheese","Сыр Гауда",356.0,24.94,27.44,2.22,200,setOf("milk"),"dairy",
            nutrientSourceReference="https://fdc.nal.usda.gov/food-details/171241/nutrients")
    ).associateBy { it.id }

    private val grains = listOf("oats", "buckwheat", "rice", "millet")
    private val fruits = listOf("apple","banana","pear","berries")
    private val proteins = listOf("chicken","turkey","beef","pork","cod","salmon","tofu")
    private val sides = listOf("rice","buckwheat","pasta","potato")
    private val vegetables = listOf("broccoli","carrot","zucchini")

    val generatedPrototypes: List<OfflineRecipe> = buildList {
        // 32 breakfast combinations with real quantities, preparation and recipe steps.
        for (grain in grains) for (fruit in fruits) for (withMilk in listOf(false,true)) {
            val grainName = ShoppingDisplayNames.forIngredient(grain)
            val fruitName = ShoppingDisplayNames.forIngredient(fruit)
            val liquid = if (withMilk) "на молоке" else "на воде"
            val list = listOf(RecipeIngredient(grain, 65), RecipeIngredient(fruit, 100)) +
                if (withMilk) listOf(RecipeIngredient("milk", 200)) else emptyList()
            add(OfflineRecipe(
                "breakfast_${grain}_${fruit}_${if (withMilk) "milk" else "water"}",
                "$grainName $liquid с добавкой: $fruitName", MealType.BREAKFAST,
                list,
                listOf("Промойте крупу при необходимости и отмерьте сухое количество из списка ингредиентов.",
                    "Отварите крупу ${if (withMilk) "с указанным в списке количеством молока и небольшим количеством воды" else "в воде"} до готовности; при необходимости доливайте воду.",
                    "Добавьте указанное в списке количество: $fruitName. Подавайте."),
                RecipePreparation(setOf("Плита","Кастрюля"), 10, 25, 2, 2),
                setOf("завтрак","каша","домашняя","сладкое",grain,fruit,
                    when (grain) {
                        "rice" -> "рис"; "buckwheat" -> "гречка"
                        "oats" -> "овсянка"; else -> "пшено"
                    },
                    if (withMilk) "молоко" else "на воде")
            ))
        }
        // 7 proteins × 4 sides × 3 vegetables × 2 cooking methods = 168 main dishes.
        for (protein in proteins) for (side in sides) for (veg in vegetables)
            for (method in listOf("pan","oven")) {
                val pName = ShoppingDisplayNames.forIngredient(protein)
                val sName = ShoppingDisplayNames.forIngredient(side)
                val vName = ShoppingDisplayNames.forIngredient(veg)
                val oven = method == "oven"
                val proteinGrams = if (protein in setOf("tofu","cod")) 180 else 160
                val sideGrams = if (side == "potato") 280 else 85
                val list = listOf(RecipeIngredient(protein,proteinGrams),
                    RecipeIngredient(side,sideGrams), RecipeIngredient(veg,170),
                    RecipeIngredient("oil",8))
                val equip = if (oven) setOf("Духовка","Кастрюля")
                    else setOf("Плита","Сковорода","Кастрюля")
                add(OfflineRecipe(
                    "main_${protein}_${side}_${veg}_$method",
                    "$pName, $sName и $vName (${if (oven) "духовка" else "сковорода"})",
                    MealType.MAIN, list,
                    listOf(
                        "Отмерьте сырые ингредиенты в количествах из списка на экране.",
                        "Гарнир: ${if (side == "potato") "очистите картофель и приготовьте до мягкости" else "отварите сухую крупу или макароны до готовности"}.",
                        if (oven) "Запеките ${pName.lowercase()} с овощами и маслом до безопасной готовности; для птицы и мяса проверяйте прожарку."
                        else "Обжарьте или потушите ${pName.lowercase()} с овощами и маслом до безопасной готовности.",
                        "Подавайте приготовленный белок и овощи вместе с гарниром."
                    ),
                    RecipePreparation(equip,
                        if (oven) 22 else 14, if (oven) 50 else 32,
                        if (oven) 3 else 2, if (oven) 4 else 2),
                    setOf("основное", "овощи", protein, side, veg,
                        if (oven) "запечённое" else "жареное",
                        when (protein) {
                            "chicken" -> "курица"; "turkey" -> "индейка"
                            "beef" -> "говядина"; "pork" -> "свинина"
                            "cod", "salmon" -> "рыба"; else -> "бобовые"
                        },
                        when (side) {
                            "rice" -> "рис"; "buckwheat" -> "гречка"
                            "pasta" -> "паста"; else -> "картофель"
                        },
                        if (protein in setOf("cod","salmon")) "рыба"
                        else if (protein=="tofu") "веганская" else "мясо")
                ))
            }
    }

    /** All generated prototypes are retained for regression tests, but never count toward M2 verified recipes. */
    val curatedDrafts: List<OfflineRecipe> =
        CuratedRecipeDrafts.allRecipes + CuratedRecipeDraftsPart2.recipes +
        CuratedRecipeBatch2.recipes + CuratedRecipeBatch3.recipes +
        CuratedRecipeBatch4.recipes + CuratedRecipeBatch5.recipes
    val recipes: List<OfflineRecipe> = generatedPrototypes + curatedDrafts

    init {
        check(generatedPrototypes.size == 200)
        check(curatedDrafts.size == 150)
        check(curatedDrafts.map { it.name }.toSet().size == 150)
        check(recipes.size == 350)
        check(recipes.map { it.id }.toSet().size == recipes.size)
        check(curatedDrafts.all { it.evidenceStatus == RecipeEvidenceStatus.CURATED_DRAFT &&
            !it.nutritionVerified && it.sourceReference == null })
        check(recipes.all { recipe ->
            recipe.ingredients.isNotEmpty() && recipe.steps.size >= 3 &&
                recipe.ingredients.all { it.grams > 0 && ingredients.containsKey(it.id) } &&
                !recipe.nutritionVerified
        })
    }
    fun allergens(recipe: OfflineRecipe): Set<String> = recipe.ingredients.flatMap {
        ingredients.getValue(it.id).allergens
    }.toSet()

    fun diets(recipe: OfflineRecipe): Set<String> {
        val groups = recipe.ingredients.map { ingredients.getValue(it.id).group }.toSet()
        val result = mutableSetOf("Всеядный")
        if ("meat" !in groups) result.add("Пескетарианский")
        if ("meat" !in groups && "fish" !in groups) result.add("Вегетарианский")
        if (groups.intersect(setOf("meat","fish","dairy","egg")).isEmpty()) result.add("Веганский")
        return result
    }
    fun nutrition(recipe: OfflineRecipe): Nutrition = recipe.ingredients.fold(Nutrition.ZERO) { acc, part ->
        val item = ingredients.getValue(part.id)
        val scale = part.grams / 100.0
        acc + Nutrition(item.kcal100,item.protein100,item.fat100,item.carbs100) * scale
    }
    fun toCandidate(recipe: OfflineRecipe): RecipeCandidate =
        RecipeCandidate(recipe.id, recipe.name,
            RecipeSafety(recipe.ingredients.map { it.id }.toSet(), allergens(recipe), true),
            diets(recipe), recipe.preparation, recipe.tags, recipe.evidenceStatus)
}
