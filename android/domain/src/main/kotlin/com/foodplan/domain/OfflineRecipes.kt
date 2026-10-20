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
    val sourceReference: String? = null
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
        FoodIngredient("buckwheat","Гречка сухая",343.0,13.0,3.4,72.0,900),
        FoodIngredient("pasta","Макароны сухие",350.0,12.0,1.5,71.0,450,setOf("wheat")),
        FoodIngredient("potato","Картофель",77.0,2.0,0.1,17.0,1000),
        FoodIngredient("oats","Овсяные хлопья сухие",370.0,12.0,6.0,62.0,400,setOf("wheat")),
        FoodIngredient("millet","Пшено сухое",348.0,11.0,3.0,70.0,800),
        FoodIngredient("milk","Молоко",60.0,3.0,3.2,4.7,1000,setOf("milk"),"dairy"),
        FoodIngredient("apple","Яблоко",52.0,0.3,0.2,14.0,1000),
        FoodIngredient("banana","Банан",89.0,1.1,0.3,23.0,1000),
        FoodIngredient("pear","Груша",57.0,0.4,0.1,15.0,1000),
        FoodIngredient("berries","Ягоды замороженные",50.0,1.0,0.4,11.0,300),
        FoodIngredient("chicken","Куриное филе сырое",106.034,22.525,1.934,0.0,800,
            group="meat",nutrientSourceReference="https://fdc.nal.usda.gov/food-details/2646170/nutrients"),
        FoodIngredient("turkey","Индейка сырая",113.0,23.0,2.0,0.0,800,group="meat"),
        FoodIngredient("beef","Говядина сырая",187.0,20.0,12.0,0.0,700,group="meat"),
        FoodIngredient("pork","Свинина сырая",200.0,19.0,14.0,0.0,700,group="meat"),
        FoodIngredient("cod","Треска сырая",82.0,18.0,0.7,0.0,600,setOf("fish"),"fish"),
        FoodIngredient("salmon","Лосось сырой",208.0,20.0,13.0,0.0,500,setOf("fish"),"fish"),
        FoodIngredient("tofu","Тофу",95.0,10.0,5.0,2.0,250,setOf("soy")),
        FoodIngredient("chickpeas","Нут консервированный (без рассола)",120.0,7.0,2.0,18.0,400),
        FoodIngredient("broccoli","Брокколи",34.0,2.8,0.4,7.0,400),
        FoodIngredient("carrot","Морковь",41.0,0.9,0.2,10.0,1000),
        FoodIngredient("zucchini","Кабачки",17.0,1.2,0.3,3.1,600),
        FoodIngredient("cabbage","Капуста",25.0,1.3,0.1,6.0,1000),
        FoodIngredient("tomato","Помидоры",18.0,0.9,0.2,3.9,500),
        FoodIngredient("mushroom","Шампиньоны",22.0,3.1,0.3,3.3,400),
        FoodIngredient("oil","Масло подсолнечное",884.0,0.0,100.0,0.0,1000),
        FoodIngredient("egg","Куриное яйцо (съедобная часть)",143.0,12.6,9.5,0.7,600,setOf("egg"),"egg"),
        FoodIngredient("cottage_cheese","Творог",121.0,17.0,5.0,1.8,400,setOf("milk"),"dairy"),
        FoodIngredient("flour","Пшеничная мука",364.0,10.0,1.0,76.0,1000,setOf("wheat")),
        FoodIngredient("onion","Лук репчатый",40.0,1.1,0.1,9.3,1000),
        FoodIngredient("garlic","Чеснок",149.0,6.4,0.5,33.0,100),
        FoodIngredient("cucumber","Огурцы",15.0,0.7,0.1,3.6,500),
        FoodIngredient("bell_pepper","Перец сладкий",31.0,1.0,0.3,6.0,500),
        FoodIngredient("lemon","Лимон",29.0,1.1,0.3,9.3,250),
        FoodIngredient("lentils","Чечевица сухая",352.0,25.0,1.0,60.0,450),
        FoodIngredient("yogurt","Йогурт натуральный",61.0,3.5,3.0,5.0,400,setOf("milk"),"dairy"),
        FoodIngredient("cheese","Сыр полутвёрдый",356.0,25.0,27.0,2.0,200,setOf("milk"),"dairy")
    ).associateBy { it.id }

    private val grains = listOf("oats", "buckwheat", "rice", "millet")
    private val fruits = listOf("apple","banana","pear","berries")
    private val proteins = listOf("chicken","turkey","beef","pork","cod","salmon","tofu")
    private val sides = listOf("rice","buckwheat","pasta","potato")
    private val vegetables = listOf("broccoli","carrot","zucchini")

    val generatedPrototypes: List<OfflineRecipe> = buildList {
        // 32 breakfast combinations with real quantities, preparation and recipe steps.
        for (grain in grains) for (fruit in fruits) for (withMilk in listOf(false,true)) {
            val grainName = requireNotNull(ingredients[grain]).name.substringBefore(" сух")
            val fruitName = requireNotNull(ingredients[fruit]).name
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
                val pName = requireNotNull(ingredients[protein]).name.substringBefore(" сыр")
                val sName = requireNotNull(ingredients[side]).name.substringBefore(" сух")
                val vName = requireNotNull(ingredients[veg]).name
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
        CuratedRecipeDrafts.allRecipes + CuratedRecipeDraftsPart2.recipes
    val recipes: List<OfflineRecipe> = generatedPrototypes + curatedDrafts

    init {
        check(generatedPrototypes.size == 200)
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
