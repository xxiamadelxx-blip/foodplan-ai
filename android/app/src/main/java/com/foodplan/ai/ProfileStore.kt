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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.profileDataStore by preferencesDataStore(name = "foodplan_profile")

data class ProfileSnapshot(
    val profile: FoodProfile,
    val previouslySaved: Boolean,
    val tasteVotes: Map<String, TasteRating>,
    val manualPrices: Map<String, Long>,
    val pantryGrams: Map<String, Int>
)

/** Small on-device profile is stored transactionally. Recipe/price history will use Room. */
class ProfileStore(private val context: Context) {
    private object Key {
        val saved = booleanPreferencesKey("saved")
        val tasteVotes = stringSetPreferencesKey("taste_votes")
        val manualPrices = stringSetPreferencesKey("manual_prices")
        val pantryGrams = stringSetPreferencesKey("pantry_grams")
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
                if (TasteSurvey.dishes.any { it.id == id } && rating != null) id to rating else null
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
            }.toMap()
        )
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
