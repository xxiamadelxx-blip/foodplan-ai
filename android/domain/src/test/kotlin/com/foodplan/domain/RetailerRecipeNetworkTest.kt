package com.foodplan.domain

import org.junit.Assert.*
import org.junit.Test

class RetailerRecipeNetworkTest {
    private val prep = RecipePreparation(emptySet(), 5, 10, 1, 1)
    private val recipes = listOf(
        recipe("only_rice", "rice" to 100),
        recipe("rice_chicken", "rice" to 100, "chicken" to 200),
        recipe("only_chicken", "chicken" to 200),
        recipe("only_potato", "potato" to 350)
    )
    private fun recipe(id: String, vararg parts: Pair<String, Int>): OfflineRecipe =
        OfflineRecipe(id, id, MealType.MAIN,
            parts.map { RecipeIngredient(it.first, it.second) },
            listOf("Подготовить продукты для блюда.", "Приготовить до готовности.",
                "Выложить готовое блюдо на тарелку."),
            prep, setOf("домашняя")
        )

    private fun profile() = FoodProfile(age="30",heightCm="170",weightKg="70",
        budgetRub="5000",appliances=emptySet(),cookingMinutes="30")
    private fun offer(id: String, title: String, urlId: String,
        grams: Int, price: Long, at: Long = 100_000L,
        store: String = "Магнит", city: String = "Санкт-Петербург",
        availability: ListingAvailability = ListingAvailability.UNKNOWN,
        address: Boolean = true): RetailerOffer =
        RetailerOffer(id,store,city,title,
            "https://magnit.ru/product/" + urlId,grams,price,at,
            address,availability)

    private val rice = offer("rice","Рис длиннозерный 900 г","long-rice",900,12_000L)
    private val chicken = offer("chicken","Филе куриной грудки охлажденное 800 г","fillet",800,24_000L)
    private val potato = offer("potato","Картофель свежий 1 кг","potato",1000,9_000L)

    private fun recommend(items: List<RetailerOffer>,
        p: FoodProfile = profile(), selected: Set<String> = emptySet(),
        catalog: List<OfflineRecipe> = recipes
    ) = RetailerRecipeNetwork.recommend(p,items,"Магнит",
        "Санкт-Петербург",110_000L,selectedIngredientIds=selected,recipes=catalog)

    @Test fun twoDifferentSkuLinksOutrankOneAndAreNotCountedAsPurchased() {
        val result = recommend(listOf(chicken, rice))
        assertEquals("rice_chicken",result.first().recipe.id)
        val joined = result.first()
        assertEquals(2,joined.linkedCount)
        assertEquals(setOf("rice","chicken"),
            joined.linkedLines.map { it.ingredientId }.toSet())
        assertTrue(joined.missingIngredientIds.isEmpty())
        assertEquals(36_000L,joined.estimatedRecipeCostKopeks)
        assertFalse(joined.addressStockConfirmed) // UNKNOWN is not in stock
    }

    @Test fun cheapestWholePacksWinAndDuplicateUrlsDontMultiplyLinks() {
        val dearRice = rice.copy(productUrl="https://magnit.ru/product/dear-rice",
            priceKopeks=40_000L)
        val duplicates = listOf(rice,rice,dearRice,chicken)
        val joined = recommend(duplicates).first { it.recipe.id=="rice_chicken" }
        assertEquals(2,joined.linkedCount)
        assertEquals("https://magnit.ru/product/long-rice",
            joined.linkedLines.first { it.ingredientId=="rice" }.skuUrl)
        assertEquals(36_000L,joined.estimatedRecipeCostKopeks)
    }

    @Test fun contradictoryObservationsCannotBecomeCheapRecipeEdges() {
        val cheapSpoof = rice.copy(priceKopeks = 100L)
        val joined = recommend(listOf(rice, cheapSpoof, chicken))
            .first { it.recipe.id == "rice_chicken" }
        assertEquals(setOf("rice"), joined.missingIngredientIds)
        assertNull(joined.estimatedRecipeCostKopeks)
        assertEquals(24_000L, joined.observedSubtotalKopeks)
        assertFalse(joined.addressStockConfirmed)

        val identical = recommend(listOf(rice, rice.copy(observedAtEpochMs = 105_000L), chicken))
            .first { it.recipe.id == "rice_chicken" }
        assertTrue(identical.missingIngredientIds.isEmpty())
        assertEquals(36_000L, identical.estimatedRecipeCostKopeks)
    }

    @Test fun partialRecipeNeverInventsMissingCost() {
        val joined = recommend(listOf(rice)).first { it.recipe.id=="rice_chicken" }
        assertEquals(setOf("chicken"), joined.missingIngredientIds)
        assertNull(joined.estimatedRecipeCostKopeks)
        assertEquals(12_000L,joined.observedSubtotalKopeks)
        assertEquals(1,joined.linkedCount)
    }

    @Test fun wrongCategoryStateAndUnspecifiedRiceTypeRejected() {
        val bad = listOf(
            offer("chicken","Курица тушка 800 г","whole-chicken",800,10_000),
            offer("chicken","Куриное бедро 800 г","thigh",800,10_000),
            offer("rice","Рис 900 г","generic",900,7_000)
        )
        assertTrue(recommend(bad).isEmpty())
        assertFalse(RetailerRecipeNamePolicy.compatible(bad[0]))
        assertFalse(RetailerRecipeNamePolicy.compatible(bad[2]))
    }

    @Test fun staleWrongStoreAndSuspiciousUrlsNeverMakeGraphLinks() {
        assertTrue(RetailerOfferChecks.isUsable(rice.copy(
            city="Москва", addressSelectedByUser=false
        ),"rice","Санкт-Петербург","Магнит",110_000L))
        val bad = listOf(
            rice.copy(storeName="Лента"),
            rice.copy(productUrl="https://magnit.ru@evil.test/product/rice"),
            rice.copy(availability=ListingAvailability.OUT_OF_STOCK)
        )
        bad.forEach { untrusted ->
            assertTrue(untrusted.toString(),recommend(listOf(untrusted)).isEmpty())
        }
        // At 110,000 ms the fixture from 100,000 ms is only 10 seconds old.
        // Test expiry using the actual six-hour contract instead of a fake timestamp.
        val expiredNow = rice.observedAtEpochMs +
            RetailerBasketPricing.MAX_OFFER_AGE_MS + 1L
        assertTrue(RetailerRecipeNetwork.recommend(profile(),listOf(rice),
            "Магнит","Санкт-Петербург",expiredNow,recipes=recipes).isEmpty())
    }

    @Test fun unknownReviewLabelMeansNoAllergenSpecificSuggestions() {
        assertTrue(recommend(listOf(rice,chicken),
            profile().copy(allergens="пшеница")).isEmpty())
        assertTrue(recommend(listOf(rice,chicken),
            profile().copy(allergens="неизвестный аллерген")).isEmpty())
    }

    @Test fun excludedIngredientsNeverBypassHardRejection() {
        val result = recommend(listOf(rice,chicken,potato),
            profile().copy(disliked="курица"))
        assertFalse(result.any { it.recipe.id=="rice_chicken" || it.recipe.id=="only_chicken" })
        assertTrue(result.any { it.recipe.id=="only_rice" })
        assertTrue(result.any { it.recipe.id=="only_potato" })
    }

    @Test fun ingredientChipsFilterRetailerGraphNotOtherCityProducts() {
        val onlyRice = recommend(listOf(rice,chicken),selected=setOf("rice"))
        assertEquals(1,onlyRice.first { it.recipe.id=="rice_chicken" }.linkedCount)
        assertEquals(setOf("chicken"),onlyRice.first {
            it.recipe.id=="rice_chicken" }.missingIngredientIds)
    }

    @Test fun wholePackCostUsesRequiredRecipeQuantity() {
        val hugeRice = recipe("rice_950","rice" to 950)
        val pack500 = offer("rice","Рис длиннозерный 500 г","small-rice",500,10_000)
        val pack1000 = offer("rice","Рис длиннозерный 1 кг","large-rice",1000,17_000)
        val item = recommend(listOf(pack500,pack1000),catalog=listOf(hugeRice)).single()
        assertEquals(17_000L,item.estimatedRecipeCostKopeks)
        assertEquals(1L,item.linkedLines.single().packageCount)
        assertEquals("https://magnit.ru/product/large-rice",item.linkedLines.single().skuUrl)
    }

    @Test fun unavailableRecipeOffersDoNotCreateFakeFullStock() {
        val joined = recommend(listOf(rice.copy(availability=ListingAvailability.IN_STOCK),
            chicken)).first { it.recipe.id=="rice_chicken" }
        assertFalse(joined.addressStockConfirmed)
        val known = recommend(listOf(rice.copy(availability=ListingAvailability.IN_STOCK),
            chicken.copy(availability=ListingAvailability.IN_STOCK)))
            .first { it.recipe.id=="rice_chicken" }
        assertTrue(known.addressStockConfirmed)
    }

    @Test fun zeroPriceCannotBecomeFreeGraphEdge() {
        assertTrue(recommend(listOf(rice.copy(priceKopeks=0))).isEmpty())
    }
}
