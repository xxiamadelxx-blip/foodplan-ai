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
    var saved by remember { mutableStateOf(false) }
    var showTasteSurvey by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    if (showTasteSurvey) {
        TasteSurveyScreen(
            votes = loaded.tasteVotes,
            onVote = { id, rating -> scope.launch { store.saveTasteVote(id, rating) } },
            onClose = { showTasteSurvey = false }
        )
        return
    }

    Surface(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 12.dp)) {
            Text("FoodPlan AI", style = MaterialTheme.typography.headlineMedium)
            Text("Россия · меню и корзина без подписки",
                style = MaterialTheme.typography.bodySmall)
            if (loaded.previouslySaved || saved) {
                Text("✓ Анкета сохранена на этом устройстве",
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.bodyMedium)
            }
            if (loaded.previouslySaved || saved) {
                TextButton(onClick = { showTasteSurvey = true }) { Text("Оценить 25 блюд") }
            }
            Spacer(Modifier.height(16.dp))
            LinearProgressIndicator(
                progress = { (step + 1) / 5f },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            Text("Шаг ${step + 1} из 5", style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.height(8.dp))
            Box(Modifier.weight(1f)) {
                ProfileFormStep(step, draft) { draft = it; problems = emptyList(); saved = false }
            }
            if (problems.isNotEmpty()) {
                Text(problems.joinToString("\n"), color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall)
            }
            Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                OutlinedButton(onClick = { step = (step - 1).coerceAtLeast(0); problems = emptyList() },
                    enabled = step > 0) { Text("Назад") }
                if (step < 4) {
                    Button(onClick = { step += 1; problems = emptyList() }) {
                        Text("Далее")
                    }
                } else {
                    Button(onClick = {
                        val errors = ProfileValidator.errors(draft)
                        problems = errors
                        if (errors.isEmpty()) {
                            scope.launch { store.save(draft); saved = true }
                        }
                    }) { Text("Сохранить профиль") }
                }
            }
            Spacer(Modifier.height(4.dp))
        }
    }
}
