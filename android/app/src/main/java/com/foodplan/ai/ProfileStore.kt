package com.foodplan.ai

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.foodplan.domain.FoodProfile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.profileDataStore by preferencesDataStore(name = "foodplan_profile")

data class ProfileSnapshot(val profile: FoodProfile, val previouslySaved: Boolean)

/** Small on-device profile is stored transactionally. Recipe/price history will use Room. */
class ProfileStore(private val context: Context) {
    private object Key {
        val saved = booleanPreferencesKey("saved")
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
                appliances = pref[Key.appliances] ?: fallback.appliances
            ), previouslySaved = pref[Key.saved] ?: false
        )
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
            prefs[Key.appliances] = p.appliances
            prefs[Key.saved] = true
        }
    }
}
