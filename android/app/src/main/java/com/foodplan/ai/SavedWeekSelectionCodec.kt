package com.foodplan.ai

import com.foodplan.domain.SavedMealSelection
import com.foodplan.domain.SavedWeekPlanner
import com.foodplan.domain.SavedWeekSelection
import org.json.JSONArray
import org.json.JSONObject

/** Local-only wire format. No automatic migration of invalid or old snapshots. */
internal object SavedWeekSelectionCodec {
    private const val MAX_BYTES = 30_000

    fun encode(snapshot: SavedWeekSelection): String {
        val days = JSONArray()
        snapshot.mealsByDay.forEach { selections ->
            val day = JSONArray()
            selections.forEach { meal ->
                day.put(JSONObject().put("id", meal.recipeId)
                    .put("servings", meal.servings)
                    .put("start", meal.batchStartDay ?: JSONObject.NULL)
                    .put("days", meal.batchDays))
            }
            days.put(day)
        }
        val raw = JSONObject()
            .put("version", snapshot.version)
            .put("fingerprint", snapshot.profileFingerprint)
            .put("week", days).toString()
        require(raw.length <= MAX_BYTES) { "Saved recipe selection is too large" }
        return raw
    }

    fun decode(raw: String?): SavedWeekSelection? {
        if (raw == null || raw.isBlank() || raw.length > MAX_BYTES) return null
        return try {
            val root = JSONObject(raw)
            val version = root.getInt("version")
            if (version != SavedWeekPlanner.VERSION) return null
            val fingerprint = root.getString("fingerprint")
            if (!Regex("[0-9a-f]{64}").matches(fingerprint)) return null
            val days = root.getJSONArray("week")
            if (days.length() != 7) return null
            val output = (0 until days.length()).map { dayIndex ->
                val meals = days.getJSONArray(dayIndex)
                if (meals.length() !in 1..6) return null
                (0 until meals.length()).map { index ->
                    val item = meals.getJSONObject(index)
                    val id = item.getString("id")
                    if (id.isBlank() || id.length > 150) return null
                    val servings = item.getDouble("servings")
                    if (!servings.isFinite() || servings !in 0.75..2.5) return null
                    val batchStart = if (item.isNull("start")) null
                        else item.getInt("start")
                    val batchDays = item.getInt("days")
                    if (batchDays !in 1..7) return null
                    SavedMealSelection(id,servings,batchStart,batchDays)
                }
            }
            SavedWeekSelection(version,fingerprint,output)
        } catch (_: Exception) {
            null
        }
    }
}
