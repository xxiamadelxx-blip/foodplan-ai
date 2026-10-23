package com.foodplan.ai

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.foodplan.domain.FoodProfile
import com.foodplan.domain.CookingEffort
import com.foodplan.domain.TasteRating
import com.foodplan.domain.TasteSurvey
import com.foodplan.domain.OfflineRecipes
import com.foodplan.domain.RetailerOffer
import com.foodplan.domain.SavedWeekSelection
import com.foodplan.domain.ListingAvailability
import com.foodplan.domain.RetailerProductMatcher
import org.json.JSONObject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.profileDataStore by preferencesDataStore(name = "foodplan_profile")

data class ProfileSnapshot(
    val profile: FoodProfile,
    val previouslySaved: Boolean,
    val tasteVotes: Map<String, TasteRating>,
    val manualPrices: Map<String, Long>,
    val pantryGrams: Map<String, Int>,
    val retailerOffers: List<RetailerOffer>,
    val savedWeekSelection: SavedWeekSelection? = null
)

/** Small on-device profile is stored transactionally. Recipe/price history will use Room. */
class ProfileStore(private val context: Context) {
    private object Key {
        val saved = booleanPreferencesKey("saved")
        val tasteVotes = stringSetPreferencesKey("taste_votes")
        val manualPrices = stringSetPreferencesKey("manual_prices")
        val pantryGrams = stringSetPreferencesKey("pantry_grams")
        val retailerOffers = stringSetPreferencesKey("retailer_offers")
        val savedWeekSelection = stringPreferencesKey("saved_week_selection_v1")
        val sex = stringPreferencesKey("sex")
        val age = stringPreferencesKey("age")
        val height = stringPreferencesKey("height")
        val weight = stringPreferencesKey("weight")
        val activity = stringPreferencesKey("activity")
        val goal = stringPreferencesKey("goal")
        val city = stringPreferencesKey("city")
        val budget = stringPreferencesKey("budget")
        val stores = stringSetPreferencesKey("stores")
        val diet = stringPreferencesKey("diet")
        val allergens = stringPreferencesKey("allergens")
        val disliked = stringPreferencesKey("disliked")
        val household = stringPreferencesKey("household")
        val meals = stringPreferencesKey("meals")
        val cooking = stringPreferencesKey("cooking")
        val cookingEffort = stringPreferencesKey("cookingEffort")
        val appliances = stringSetPreferencesKey("appliances")
    }
    val snapshot: Flow<ProfileSnapshot> = context.profileDataStore.data.map { pref ->
        val fallback = FoodProfile()
        ProfileSnapshot(
            profile = FoodProfile(
                sex = pref[Key.sex] ?: fallback.sex,
                age = pref[Key.age] ?: fallback.age,
                heightCm = pref[Key.height] ?: fallback.heightCm,
                weightKg = pref[Key.weight] ?: fallback.weightKg,
                activity = pref[Key.activity] ?: fallback.activity,
                goal = pref[Key.goal] ?: fallback.goal,
                city = pref[Key.city] ?: fallback.city,
                budgetRub = pref[Key.budget] ?: fallback.budgetRub,
                stores = pref[Key.stores] ?: fallback.stores,
                diet = pref[Key.diet] ?: fallback.diet,
                allergens = pref[Key.allergens] ?: fallback.allergens,
                disliked = pref[Key.disliked] ?: fallback.disliked,
                household = pref[Key.household] ?: fallback.household,
                mealsPerDay = pref[Key.meals] ?: fallback.mealsPerDay,
                cookingMinutes = pref[Key.cooking] ?: fallback.cookingMinutes,
                cookingEffort = CookingEffort.fromStorageId(pref[Key.cookingEffort]),
                appliances = pref[Key.appliances] ?: fallback.appliances
            ),
            previouslySaved = pref[Key.saved] ?: false,
            tasteVotes = (pref[Key.tasteVotes] ?: emptySet()).mapNotNull { item ->
                val id = item.substringBefore(':')
                val rating = TasteRating.entries.firstOrNull { it.name == item.substringAfter(':', "") }
                // Preserve known v1 votes in storage and snapshots so a previously
                // saved week's fingerprint survives the catalog upgrade.
                // TasteSurvey.tasteWeights filters them out of current ranking.
                if (TasteSurvey.recognizesStoredId(id) && rating != null) id to rating else null
            }.toMap(),
            manualPrices = (pref[Key.manualPrices] ?: emptySet()).mapNotNull { item ->
                val pieces = item.split('|')
                if (pieces.size != 4) null else {
                    val cents = pieces[3].toLongOrNull()
                    if (cents == null || cents < 0) null else
                        (pieces[0] + "|" + pieces[1] + "|" + pieces[2]) to cents
                }
            }.toMap(),
            pantryGrams = (pref[Key.pantryGrams] ?: emptySet()).mapNotNull { item ->
                val id = item.substringBefore('|')
                val grams = item.substringAfter('|', "").toIntOrNull()
                if (id in OfflineRecipes.ingredients && grams != null && grams >= 0)
                    id to grams else null
            }.toMap(),
            savedWeekSelection = SavedWeekSelectionCodec.decode(pref[Key.savedWeekSelection]),
            retailerOffers = (pref[Key.retailerOffers] ?: emptySet()).mapNotNull { raw ->
                try {
                    val item=JSONObject(raw)
                    val offer=RetailerOffer(
                        item.getString("ingredient"),item.getString("store"),
                        item.getString("city"),item.getString("name"),
                        item.getString("url"),item.getInt("grams"),
                        item.getLong("price"),item.getLong("at"),
                        item.optBoolean("addressConfirmed",false),
                        ListingAvailability.entries.firstOrNull {
                            it.name == item.optString("availability") } ?: ListingAvailability.UNKNOWN,
                        item.optDouble("stars",Double.NaN).takeIf { it.isFinite() && it in 0.0..5.0 },
                        item.optInt("reviewCount",-1).takeIf { it >= 0 }
                    )
                    if (offer.ingredientId in OfflineRecipes.ingredients &&
                        offer.storeName == "Магнит" &&
                        offer.productUrl.startsWith("https://magnit.ru/product/") &&
                        offer.packGrams in 1..50000 &&
                        offer.priceKopeks>0 &&
                        RetailerProductMatcher.matchesIngredient(offer.ingredientId,offer.productName)) offer else null
                } catch (_: Exception) { null }
            }
        )
    }
    /** Atomic on-device persistence of recipe picks, never live retailer prices. */
    suspend fun saveWeekSelection(selection: SavedWeekSelection) {
        val serialized = SavedWeekSelectionCodec.encode(selection)
        context.profileDataStore.edit { preferences ->
            preferences[Key.savedWeekSelection] = serialized
        }
    }

    suspend fun clearWeekSelection() {
        context.profileDataStore.edit { preferences ->
            preferences.remove(Key.savedWeekSelection)
        }
    }

    suspend fun saveRetailerOffer(offer: RetailerOffer) {
        require(offer.ingredientId in OfflineRecipes.ingredients)
        require(offer.storeName == "Магнит")
        require(offer.city.isNotBlank())
        require(offer.productUrl.startsWith("https://magnit.ru/product/"))
        require(RetailerProductMatcher.matchesIngredient(offer.ingredientId,offer.productName))
        require(offer.packGrams in 1..50000 && offer.priceKopeks > 0)
        val entry = JSONObject()
            .put("ingredient",offer.ingredientId).put("store",offer.storeName)
            .put("city",offer.city).put("name",offer.productName)
            .put("url",offer.productUrl).put("grams",offer.packGrams)
            .put("price",offer.priceKopeks).put("at",offer.observedAtEpochMs)
            .put("addressConfirmed",offer.addressSelectedByUser)
            .put("availability",offer.availability.name)
            .put("stars",offer.ratingStars ?: JSONObject.NULL)
            .put("reviewCount",offer.reviewCount ?: JSONObject.NULL).toString()
        context.profileDataStore.edit { prefs ->
            val kept=(prefs[Key.retailerOffers] ?: emptySet()).filterNot { old ->
                try {
                    val j=JSONObject(old)
                    j.optString("ingredient")==offer.ingredientId &&
                        j.optString("store")==offer.storeName &&
                        j.optString("city")==offer.city &&
                        j.optString("url")==offer.productUrl
                } catch (_: Exception) { false }
            }.toSet()
            prefs[Key.retailerOffers] = kept + entry
        }
    }
    suspend fun savePantryGrams(id: String, grams: Int) {
        require(id in OfflineRecipes.ingredients && grams in 0..500000)
        context.profileDataStore.edit { prefs ->
            prefs[Key.pantryGrams] =
                (prefs[Key.pantryGrams] ?: emptySet()).filterNot { it.startsWith("$id|") }
                    .toSet() + "$id|$grams"
        }
    }
    suspend fun saveManualPrice(city: String, store: String, id: String, kopeks: Long) {
        require(kopeks >= 0)
        require(!city.contains('|') && !store.contains('|') && !id.contains('|'))
        val key = "$city|$store|$id"
        context.profileDataStore.edit { prefs ->
            prefs[Key.manualPrices] =
                (prefs[Key.manualPrices] ?: emptySet()).filterNot { it.startsWith("$key|") }
                    .toSet() + "$key|$kopeks"
        }
    }
    suspend fun saveTasteVote(dishId: String, rating: TasteRating) {
        require(TasteSurvey.dishes.any { it.id == dishId }) { "Unknown dish id" }
        context.profileDataStore.edit { prefs ->
            val old = prefs[Key.tasteVotes] ?: emptySet()
            prefs[Key.tasteVotes] = old.filterNot { it.startsWith("$dishId:") }.toSet() +
                "$dishId:${rating.name}"
        }
    }

    suspend fun save(p: FoodProfile) {
        context.profileDataStore.edit { prefs ->
            prefs[Key.sex] = p.sex
            prefs[Key.age] = p.age
            prefs[Key.height] = p.heightCm
            prefs[Key.weight] = p.weightKg
            prefs[Key.activity] = p.activity
            prefs[Key.goal] = p.goal
            prefs[Key.city] = p.city.trim()
            prefs[Key.budget] = p.budgetRub
            prefs[Key.stores] = p.stores
            prefs[Key.diet] = p.diet
            prefs[Key.allergens] = p.allergens.trim()
            prefs[Key.disliked] = p.disliked.trim()
            prefs[Key.household] = p.household
            prefs[Key.meals] = p.mealsPerDay
            prefs[Key.cooking] = p.cookingMinutes
            prefs[Key.cookingEffort] = p.cookingEffort.storageId
            prefs[Key.appliances] = p.appliances
            prefs[Key.saved] = true
        }
    }
}
