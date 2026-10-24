package com.foodplan.ai

import androidx.annotation.DrawableRes
import com.foodplan.domain.TasteSurvey

/**
 * All survey images are original local VectorDrawables (no network or AI calls).
 * Stable dish ID is the lookup key, not index, title or presentation order.
 * Unknown IDs return null; the text title remains the accessible fallback.
 */
object TasteDishArt {
    private val images: Map<String, Int> = mapOf(
        "oatmeal" to R.drawable.taste_oatmeal,
        "omelette" to R.drawable.taste_omelette,
        "cottage_cheese" to R.drawable.taste_cottage_cheese,
        "cheese_sandwich_v2" to R.drawable.taste_cheese_sandwich_v2,
        "borsch" to R.drawable.taste_borsch,
        "chicken_soup_v2" to R.drawable.taste_chicken_soup_v2,
        "cutlets_mash_v2" to R.drawable.taste_cutlets_mash_v2,
        "buckwheat_chicken" to R.drawable.taste_buckwheat_chicken,
        "rice_vegetables_v2" to R.drawable.taste_rice_vegetables_v2,
        "tomato_pasta" to R.drawable.taste_tomato_pasta,
        "chicken_pilaf_v2" to R.drawable.taste_chicken_pilaf_v2,
        "beef_stew" to R.drawable.taste_beef_stew,
        "oven_fish_v2" to R.drawable.taste_oven_fish_v2,
        "tomato_cucumber_salad_v2" to R.drawable.taste_tomato_cucumber_salad_v2,
        "potato_mushrooms_v2" to R.drawable.taste_potato_mushrooms_v2,
        "spicy_chicken_v2" to R.drawable.taste_spicy_chicken_v2,
        "sweet_sour_chicken_v2" to R.drawable.taste_sweet_sour_chicken_v2,
        "crispy_chicken_v2" to R.drawable.taste_crispy_chicken_v2,
        "creamy_mushrooms_v2" to R.drawable.taste_creamy_mushrooms_v2,
        "smoky_potato_v2" to R.drawable.taste_smoky_potato_v2,
        "pizza" to R.drawable.taste_pizza,
        "burger" to R.drawable.taste_burger,
        "fish_rolls_v2" to R.drawable.taste_fish_rolls_v2,
        "chicken_curry_v2" to R.drawable.taste_chicken_curry_v2,
        "vegetable_noodles_v2" to R.drawable.taste_vegetable_noodles_v2
    )

    @DrawableRes
    fun resourceFor(dishId: String): Int? = images[dishId]

    fun coveredDishIds(): Set<String> = images.keys

    init {
        check(images.keys == TasteSurvey.dishes.map { it.id }.toSet()) {
            "Every active taste dish must have exactly one offline image"
        }
    }
}
