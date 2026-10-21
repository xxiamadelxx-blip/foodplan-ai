package com.foodplan.domain

import org.junit.Assert.*
import org.junit.Test

class RetailerCatalogTest {
    @Test fun parsesRenderedMagnitCatalogueExamples() {
        assertEquals(8999L,CatalogValueParser.parseDisplayedRubles("89,99 ₽"))
        assertEquals(17999L,CatalogValueParser.parseDisplayedRubles("179,99 ₽"))
        assertEquals(500,CatalogValueParser.packageGrams("Сода пищевая 500г в ассортименте"))
        assertEquals(800,CatalogValueParser.packageGrams("Рис Националь Здоровье 800г"))
        assertEquals(2500,CatalogValueParser.packageGrams("Картофель фасованный 2.5кг"))
        assertEquals(400,CatalogValueParser.packageGrams("Рис сухой 5х80 г"))
        assertEquals(450,CatalogValueParser.packageGrams("Крупа 3×150г"))
        assertEquals(1000,CatalogValueParser.packageGrams("Гречка 2 по 500 г"))
        assertEquals(1500,CatalogValueParser.packageGrams("Пшено 2x0,75 кг"))
        assertNull(CatalogValueParser.packageGrams("Молоко 1л"))
        assertNull(CatalogValueParser.packageGrams("Цена за 1 шт"))
        assertNull(CatalogValueParser.parseDisplayedRubles("сегодня"))
    }
    private fun offer(store: String="Магнит",city: String="Санкт-Петербург",
        grams: Int=800,price: Long=17999L,timestamp: Long=100_000L,
        addressConfirmed:Boolean=true) =
        RetailerOffer("rice",store,city,"Рис 800г","https://magnit.ru/product/100-rice",
            grams,price,timestamp,addressConfirmed)
    private val rice = OfflineRecipes.ingredients.getValue("rice")
    private val shopping = listOf(ShoppingNeed(rice,950,2))
    @Test fun observedPackSizeOverridesCatalogueAssumption() {
        val quote=RetailerBasketPricing.estimate(shopping,listOf(offer()),
            "Санкт-Петербург","Магнит","1000",110_000L)
        assertEquals(2L,quote.lines.single().packages)
        assertEquals(800,quote.lines.single().chosenPackGrams)
        assertEquals(35998L,quote.knownTotalKopeks)
        assertEquals(false,quote.verifiedForCheckout)
        assertEquals(true,quote.budgetEstimate)
    }
    @Test fun unknownCityOrStoreOrAddressNeverProvesBudget() {
        for (entry in listOf(offer(city="Москва"),offer(store="Лента"),offer(addressConfirmed=false))) {
            val quote=RetailerBasketPricing.estimate(shopping,listOf(entry),
                "Санкт-Петербург","Магнит","1000",110_000L)
            assertNull(quote.budgetEstimate)
            assertEquals(1,quote.missingLines)
        }
    }
    @Test fun stalePriceMustNotBeUsed() {
        val quote=RetailerBasketPricing.estimate(shopping,listOf(offer()),
            "Санкт-Петербург","Магнит","1000",
            100_000L + RetailerBasketPricing.MAX_OFFER_AGE_MS + 1)
        assertNull(quote.budgetEstimate)
    }
    @Test fun unpricedLinesAreNeverFreeProducts() {
        val quote=RetailerBasketPricing.estimate(shopping,emptyList(),
            "Санкт-Петербург","Магнит","500",110_000)
        assertEquals(0L,quote.knownTotalKopeks)
        assertEquals(1,quote.missingLines)
        assertNull(quote.budgetEstimate)
    }
    @Test fun quotedPriceIsNotCheckoutPrice() {
        val quote=RetailerBasketPricing.estimate(shopping,
            listOf(offer(price=99999L)),"Санкт-Петербург","Магнит","100",110_000L)
        assertEquals(false,quote.budgetEstimate)
        assertFalse(quote.verifiedForCheckout)
    }
    @Test fun unrelatedSodaCannotBeSilentlyPricedAsRice() {
        assertFalse(RetailerProductMatcher.matchesIngredient("rice","Сода пищевая 500г"))
        assertTrue(RetailerProductMatcher.matchesIngredient("rice","Рис Националь Золотистый 900г"))
        assertFalse(RetailerProductMatcher.matchesIngredient("rice","Рисовый уксус 400мл"))
        assertTrue(RetailerProductMatcher.matchesIngredient("buckwheat","Гречка ядрица 900г"))
    }

    @Test fun mismatchedProductNamesNeverConvertPotatoOrSodaPriceToRice() {
        val counterfeit=offer(price=100L).copy(productName="Сода пищевая 800г")
        val quote=RetailerBasketPricing.estimate(shopping,listOf(counterfeit),
            "Санкт-Петербург","Магнит","100",110_000L)
        assertEquals(1,quote.missingLines)
        assertNull(quote.budgetEstimate)
        assertNull(quote.lines.single().costKopeks)
    }

    @Test fun soldOutOrUntrustedRetailerLinksAreNeverAcceptedAsCheapOffers() {
        val invalid=listOf(
            offer(price=100L).copy(availability=ListingAvailability.OUT_OF_STOCK),
            offer(price=100L).copy(productUrl="https://fake-magnit.ru/product/rice"),
            offer(price=100L).copy(productUrl="https://magnit.ru@evil.test/product/rice"),
            offer(price=100L).copy(productUrl="https://magnit.ru/product/rice#old"),
            offer(price=100L).copy(ratingStars=Double.NaN),
            offer(price=100L).copy(reviewCount=-1)
        )
        invalid.forEach { bad ->
            val quote=RetailerBasketPricing.estimate(shopping,listOf(bad),
                "Санкт-Петербург","Магнит","100",110_000L)
            assertEquals(bad.toString(),1,quote.missingLines)
            assertNull(bad.toString(),quote.budgetEstimate)
        }
    }

    @Test fun acceptedKnownShopQuotesStayEstimatesNotConfirmedCheckout() {
        val vkusvill=offer(store="ВкусВилл",price=9000L)
            .copy(productUrl="https://vkusvill.ru/goods/real-rice")
        val quote=RetailerBasketPricing.estimate(shopping,listOf(vkusvill),
            "Санкт-Петербург","ВкусВилл","356",110_000L)
        assertEquals(18_000L,quote.knownTotalKopeks)
        assertEquals(true,quote.budgetEstimate)
        assertFalse(quote.verifiedForCheckout)
        val unsupported=RetailerBasketPricing.estimate(shopping,
            listOf(vkusvill.copy(storeName="Лента")),
            "Санкт-Петербург","Лента","356",110_000L)
        assertNull(unsupported.budgetEstimate)
    }

    @Test fun nonContractBudgetCannotBeMarkedAsAffordable() {
        for (amount in listOf("99","100001","3.56","")) {
            val quote=RetailerBasketPricing.estimate(shopping,listOf(offer()),
                "Санкт-Петербург","Магнит",amount,110_000L)
            assertNull(amount,quote.budgetEstimate)
        }
    }

    @Test fun rawAndDryFoodMustNotInheritPricesFromCookedOrProcessedProducts() {
        assertFalse(RetailerProductMatcher.matchesIngredient("rice","Рис вареный 800г"))
        assertFalse(RetailerProductMatcher.matchesIngredient("rice","Рис с овощами 800г"))
        assertFalse(RetailerProductMatcher.matchesIngredient("potato","Картофель фри 1кг"))
        assertFalse(RetailerProductMatcher.matchesIngredient("potato","Картофель пюре 500г"))
        assertFalse(RetailerProductMatcher.matchesIngredient("pasta","Паста томатная 500г"))
        assertFalse(RetailerProductMatcher.matchesIngredient("berries","Малина замороженная 300г"))
        assertFalse(RetailerProductMatcher.matchesIngredient("berries","Клубника свежая 300г"))
        assertFalse(RetailerProductMatcher.matchesIngredient("berries","Клубника замороженная с сахаром 300г"))
        assertFalse(RetailerProductMatcher.matchesIngredient("rice","Рис Увелка круглозерный шлифованный 800г"))
        assertFalse(RetailerProductMatcher.matchesIngredient("rice","Рис Увелка длиннозерный пропаренный 800г"))
        assertFalse(RetailerProductMatcher.matchesIngredient("rice","Рис Увелка для плова по-узбекски 800г"))
        assertTrue(RetailerProductMatcher.matchesIngredient("rice","Рис длиннозерный сухой 800г"))
        assertTrue(RetailerProductMatcher.matchesIngredient("potato","Картофель свежий 2кг"))
        assertTrue(RetailerProductMatcher.matchesIngredient("berries","Клубника замороженная 300г"))
    }

    @Test fun cheapReadyMealDoesNotPassAsRawRiceAtAnyBudget() {
        val wrong=offer(price=100L).copy(productName="Рис отварной 800г")
        val basket=RetailerBasketPricing.estimate(shopping,listOf(wrong),
            "Санкт-Петербург","Магнит","100",110_000L)
        assertEquals(1,basket.missingLines)
        assertNull(basket.budgetEstimate)
    }

    @Test fun ambiguousRetailerSupplierMustNotBePricedAsKnownNutrition() {
        val oats=OfflineRecipes.ingredients.getValue("oats")
        val need=listOf(ShoppingNeed(oats,350,1))
        val ambiguous=RetailerOffer(
            "oats","ВкусВилл","Санкт-Петербург",
            "Геркулес Овсяные хлопья 400 г",
            "https://vkusvill.ru/goods/gerkules-ovsyanye-khlopya-1248/",
            400,5400L,100_000L,true
        )
        val risk=RetailerVariantRiskRegistry.riskFor(ambiguous.productUrl)
        assertNotNull(risk)
        assertEquals(3,risk!!.observedSupplierCount)
        assertTrue(RetailerVariantRiskRegistry.needsManufacturerReview(ambiguous))
        val quote=RetailerBasketPricing.estimate(need,listOf(ambiguous),
            "Санкт-Петербург","ВкусВилл","100",110_000L)
        assertNull(quote.budgetEstimate)
        assertEquals(1,quote.missingLines)
        val optimized=BudgetBasketOptimizer.choose(need,listOf(ambiguous),
            "Санкт-Петербург","ВкусВилл",100,110_000L)
        assertNull(optimized.fitsBudget)
        assertEquals(listOf("oats"),optimized.missingIngredientIds)
        val p=FoodProfile(age="30",heightCm="165",weightKg="65",
            budgetRub="100",city="Санкт-Петербург")
        val fallback=LimitedBudgetFoodPlanner.choose(
            p,"ВкусВилл",listOf(ambiguous),emptyMap(),110_000L,2000
        )
        assertEquals(LimitedFoodStatus.NO_RELIABLE_PRICES,fallback.status)
    }

    @Test fun buckwheatFourSupplierCardIsBlockedForGenericNutritionAtEveryBudget() {
        val buckwheat=OfflineRecipes.ingredients.getValue("buckwheat")
        val shopping=listOf(ShoppingNeed(buckwheat,850,1))
        val ambiguous=RetailerOffer(
            "buckwheat","ВкусВилл","Санкт-Петербург",
            "Крупа гречневая ядрица, 900 г",
            "https://vkusvill.ru/goods/krupa-grechnevaya-yadritsa-17699/",
            900,9000L,100_000L,true
        )
        val risk=RetailerVariantRiskRegistry.riskFor(ambiguous.productUrl)
        assertNotNull(risk)
        assertEquals(4,risk!!.observedSupplierCount)
        assertTrue(RetailerVariantRiskRegistry.needsManufacturerReview(ambiguous))
        val estimate=RetailerBasketPricing.estimate(shopping,listOf(ambiguous),
            "Санкт-Петербург","ВкусВилл","100",110_000L)
        assertEquals(1,estimate.missingLines)
        assertNull(estimate.budgetEstimate)
        val optimized=BudgetBasketOptimizer.choose(shopping,listOf(ambiguous),
            "Санкт-Петербург","ВкусВилл",100,110_000L)
        assertNull(optimized.fitsBudget)
        assertEquals(listOf("buckwheat"),optimized.missingIngredientIds)
        val fallback=LimitedBudgetFoodPlanner.choose(
            FoodProfile(age="30",heightCm="165",weightKg="65",budgetRub="100",
                city="Санкт-Петербург"),
            "ВкусВилл",listOf(ambiguous),emptyMap(),110_000L,2000
        )
        assertEquals(LimitedFoodStatus.NO_RELIABLE_PRICES,fallback.status)
    }

    @Test fun exactSupplierReviewRiskIsNotGeneralBanOnAllOatListings() {
        val offer=RetailerOffer(
            "oats","ВкусВилл","Санкт-Петербург",
            "Геркулес Русский продукт Традиционный 500 г",
            "https://vkusvill.ru/goods/gerkules-russkiy-produkt-traditsionnyy-500-g-55980/",
            500,12000L,100_000L,true
        )
        assertNull(RetailerVariantRiskRegistry.riskFor(offer.productUrl))
        val estimate=RetailerBasketPricing.estimate(
            listOf(ShoppingNeed(OfflineRecipes.ingredients.getValue("oats"),400,1)),
            listOf(offer),"Санкт-Петербург","ВкусВилл","356",110_000L)
        assertEquals(true,estimate.budgetEstimate)
        assertFalse(estimate.verifiedForCheckout)
        assertEquals("egg",RetailerVariantRiskRegistry.riskFor(
            "https://vkusvill.ru/goods/yaytso-kurinoe-s1-15544/?ref=home")?.ingredientId)
    }

    @Test fun multiPackWholeMassDeterminesNumberOfPackages() {
        val need=listOf(ShoppingNeed(OfflineRecipes.ingredients.getValue("rice"),950,1))
        val multi=RetailerOffer("rice","Магнит","Санкт-Петербург",
            "Рис сухой длиннозерный 5х80 г",
            "https://magnit.ru/product/rice-5x80g",
            400,12000L,100_000L,true)
        val quote=RetailerBasketPricing.estimate(need,listOf(multi),
            "Санкт-Петербург","Магнит","356",110_000L)
        assertEquals(3L,quote.lines.single().packages)
        assertEquals(400,quote.lines.single().chosenPackGrams)
        assertEquals(36_000L,quote.knownTotalKopeks)
        assertEquals(false,quote.budgetEstimate)
        val wrong=RetailerBasketPricing.estimate(need,
            listOf(multi.copy(packGrams=80)),
            "Санкт-Петербург","Магнит","356",110_000L)
        assertNull(wrong.budgetEstimate)
        assertEquals(1,wrong.missingLines)
    }

    @Test fun productTitleMassMismatchCannotFabricateAnAffordableWholePack() {
        val potato=OfflineRecipes.ingredients.getValue("potato")
        val shopping=listOf(ShoppingNeed(potato,1900,1))
        val actualTwoKg=RetailerOffer(
            "potato","Магнит","Санкт-Петербург","Картофель 2 кг",
            "https://magnit.ru/product/potato-2kg",2000,10000L,
            100_000L,true
        )
        val fakeMass=actualTwoKg.copy(packGrams=500)
        val wrong=RetailerBasketPricing.estimate(shopping,listOf(fakeMass),
            "Санкт-Петербург","Магнит","100",110_000L)
        assertNull(wrong.budgetEstimate)
        assertEquals(1,wrong.missingLines)
        val correct=RetailerBasketPricing.estimate(shopping,listOf(actualTwoKg),
            "Санкт-Петербург","Магнит","100",110_000L)
        assertEquals(10000L,correct.knownTotalKopeks)
        assertEquals(true,correct.budgetEstimate)
        assertEquals(1L,correct.lines.single().packages)
    }

    @Test fun everyFoodIngredientNowHasRetailerNameMatcherPolicy() {
        assertEquals(37, OfflineRecipes.ingredients.size)
        assertEquals(OfflineRecipes.ingredients.keys,
            RetailerProductMatcher.supportedIngredientIds())
    }

    @Test fun elevenPreviouslyMissingIngredientNamesCanBeMatchedToExplicitFoodForms() {
        val names=mapOf(
            "egg" to "Яйца куриные С1 600 г",
            "cottage_cheese" to "Творог РОСМОЛ 5% 350г",
            "flour" to "Мука пшеничная 1кг",
            "onion" to "Лук репчатый 1кг",
            "garlic" to "Чеснок свежий 100г",
            "cucumber" to "Огурцы свежие 500г",
            "bell_pepper" to "Перец сладкий красный 500г",
            "lemon" to "Лимоны свежие 250г",
            "lentils" to "Чечевица сухая 450г",
            "yogurt" to "Йогурт натуральный без добавок 400г",
            "cheese" to "Сыр Гауда 200г"
        )
        assertEquals(11,names.size)
        for((id,name) in names) {
            assertTrue("$id: $name",
                RetailerProductMatcher.matchesIngredient(id,name))
            val offer=RetailerOffer(id,"Магнит","Санкт-Петербург",name,
                "https://magnit.ru/product/test-"+id,
                requireNotNull(CatalogValueParser.packageGrams(name)),
                9900L,100_000L,true)
            assertTrue("$id price was ignored",
                RetailerOfferChecks.isUsable(offer,id,
                    "Санкт-Петербург","Магнит",110_000L))
        }
    }

    @Test fun visiblyDifferentFoodOrPreparationCannotInheritRawAndSpecificMacros() {
        val wrong=mapOf(
            "egg" to "Яйца перепелиные 400г",
            "cottage_cheese" to "Творог 9% 350г",
            "flour" to "Мука рисовая 1кг",
            "onion" to "Лук зелёный 100г",
            "garlic" to "Чеснок гранулированный 100г",
            "cucumber" to "Огурцы маринованные 500г",
            "bell_pepper" to "Перец чили острый 100г",
            "lemon" to "Лимоны сушеные 100г",
            "lentils" to "Чечевица консервированная 400г",
            "yogurt" to "Йогурт клубничный сладкий 400г",
            "cheese" to "Сыр плавленый Гауда 200г",
            "chickpeas" to "Нут сухой 400г"
        )
        for((id,name) in wrong)
            assertFalse("$id: $name",
                RetailerProductMatcher.matchesIngredient(id,name))
        assertTrue(RetailerProductMatcher.matchesIngredient(
            "chickpeas","Нут консервированный 400г"))
    }

}
