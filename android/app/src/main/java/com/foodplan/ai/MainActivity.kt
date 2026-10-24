package com.foodplan.ai

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.foodplan.domain.FoodProfile
import com.foodplan.domain.ProfileValidator
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val store = ProfileStore(applicationContext)
        setContent { MaterialTheme { FoodPlanApp(store) } }
    }
}

/** One-time setup -> taste survey -> normal app. Settings can reopen setup later. */
@Composable
fun FoodPlanApp(store: ProfileStore) {
    val snapshot by store.snapshot.collectAsState(initial = null)
    if (snapshot == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }
    val loaded = requireNotNull(snapshot)
    var draft by remember(loaded.profile) { mutableStateOf(loaded.profile) }
    var step by remember { mutableIntStateOf(0) }
    var problems by remember { mutableStateOf(emptyList<String>()) }
    var editingSettings by remember { mutableStateOf(false) }
    var showTasteSurvey by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    if (showTasteSurvey) {
        TasteSurveyScreen(
            votes = loaded.tasteVotes,
            onVote = { id, rating -> scope.launch { store.saveTasteVote(id,rating) } },
            onClose = { showTasteSurvey = false },
            onCompleted = { showTasteSurvey = false }
        )
        return
    }

    if (loaded.previouslySaved && !editingSettings) {
        FoodPlanHomeScreen(
            profile = loaded.profile,
            votes = loaded.tasteVotes,
            favoriteRecipeIds = loaded.favoriteRecipeIds,
            onSetRecipeFavorite = { id, value ->
                scope.launch { store.setRecipeFavorite(id, value) }
            },
            pantryGrams = loaded.pantryGrams,
            retailerOffers = loaded.retailerOffers,
            onSaveRetailerOffer = { offer ->
                scope.launch { store.saveRetailerOffer(offer) }
            },
            onSaveProfile = { profile -> scope.launch { store.save(profile) } },
            onOpenSettings = {
                draft = loaded.profile
                editingSettings = true
                step = 0
                problems = emptyList()
            },
            onReviewTastes = { showTasteSurvey = true },
            savedWeekSelection = loaded.savedWeekSelection,
            onSaveWeekSelection = { selected ->
                scope.launch { store.saveWeekSelection(selected) }
            },
            onStartNewWeek = { onCleared ->
                scope.launch {
                    val cleared = try {
                        store.clearWeekSelection()
                        true
                    } catch (_: Exception) {
                        false
                    }
                    onCleared(cleared)
                }
            }
        )
        return
    }

    // Budget and store are NOT setup steps. Those are week-by-week choices under Products.
    val setupSteps = listOf(0,1,3,4)
    Surface(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 12.dp)) {
            Text(if (editingSettings) "Настройки" else "FoodPlan AI",
                style = MaterialTheme.typography.headlineMedium)
            Text(if (editingSettings) "Измените параметры питания"
                else "Настроим питание один раз, затем перейдём к выбору блюд.",
                style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.height(12.dp))
            LinearProgressIndicator(
                progress = { (step + 1) / setupSteps.size.toFloat() },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            Text("Шаг ${step + 1} из ${setupSteps.size}",
                style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.height(8.dp))
            Box(Modifier.weight(1f)) {
                ProfileFormStep(setupSteps[step], draft) {
                    draft = it
                    problems = emptyList()
                }
            }
            if (problems.isNotEmpty()) {
                Text(problems.joinToString("\n"),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall)
            }
            Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                OutlinedButton(onClick = {
                    if (step == 0 && editingSettings) {
                        draft = loaded.profile
                        editingSettings = false
                    }
                    else step = (step - 1).coerceAtLeast(0)
                    problems = emptyList()
                }, enabled = step > 0 || editingSettings) {
                    Text(if (step == 0 && editingSettings) "Отмена" else "Назад")
                }
                if (step < setupSteps.lastIndex) {
                    Button(onClick = { step += 1; problems = emptyList() }) {
                        Text("Далее")
                    }
                } else {
                    Button(onClick = {
                        problems = ProfileValidator.onboardingErrors(draft)
                        if (problems.isEmpty()) {
                            scope.launch {
                                store.save(draft)
                                if (editingSettings) {
                                    editingSettings = false
                                } else {
                                    showTasteSurvey = true
                                }
                                step = 0
                            }
                        }
                    }) {
                        Text(if (editingSettings) "Сохранить изменения"
                            else "Продолжить к выбору блюд")
                    }
                }
            }
            Spacer(Modifier.height(4.dp))
        }
    }
}
