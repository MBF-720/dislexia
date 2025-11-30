package esprit.tn.handy.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import esprit.tn.handy.ui.components.SafeArea
import esprit.tn.handy.viewmodel.RanQuizViewModel
import esprit.tn.handy.viewmodel.ViewModelFactory
import kotlinx.coroutines.delay

@Composable
fun RanQuizScreen(
    onBack: () -> Unit,
    viewModel: RanQuizViewModel = viewModel(
        factory = ViewModelFactory(androidx.compose.ui.platform.LocalContext.current.applicationContext as android.app.Application)
    )
) {
    val uiState by viewModel.uiState.collectAsState()
    
    // Timer automatique - calcul du temps en temps réel
    var currentTime by remember { mutableStateOf(0L) }
    
    LaunchedEffect(uiState.isStarted, uiState.isCompleted) {
        if (uiState.isStarted && !uiState.isCompleted && uiState.startTime > 0) {
            val startTime = uiState.startTime
            while (true) {
                delay(10) // Mise à jour toutes les 10ms pour un timer fluide
                val state = viewModel.uiState.value
                if (state.isStarted && !state.isCompleted && state.startTime == startTime) {
                    currentTime = System.currentTimeMillis() - startTime
                } else {
                    break
                }
            }
        } else {
            currentTime = 0L
        }
    }
    
    if (uiState.isCompleted && uiState.finalResults != null) {
        RanQuizResultsScreen(
            results = uiState.finalResults!!,
            onBack = onBack,
            onRestart = { viewModel.resetQuiz() }
        )
    } else {
        RanQuizQuestionScreen(
            uiState = uiState,
            currentTimeMs = currentTime,
            onStartTimer = { viewModel.startTimer() },
            onAnswerChange = { viewModel.updateAnswer(it) },
            onSubmit = { viewModel.submitAnswer() },
            onBack = onBack,
            onSpeakQuestion = { viewModel.speakQuestion() }
        )
    }
}

@Composable
fun RanQuizQuestionScreen(
    uiState: esprit.tn.handy.viewmodel.RanQuizUiState,
    currentTimeMs: Long,
    onStartTimer: () -> Unit,
    onAnswerChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onBack: () -> Unit,
    onSpeakQuestion: () -> Unit
) {
    val question = uiState.currentQuestion ?: return
    val timeSeconds = currentTimeMs / 1000.0
    
    SafeArea {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
        // En-tête
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = onBack) {
                Text("Retour")
            }
            Text(
                text = "Question ${uiState.currentQuestionIndex + 1}/${uiState.questions.size}",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.width(80.dp)) // Équilibre visuel
        }
        
        // Catégorie avec bouton Écouter
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = getCategoryLabel(question.category),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(
                    onClick = onSpeakQuestion,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.PlayArrow,
                        contentDescription = "Écouter la question",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
        
        // Item à nommer
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = question.item,
                    fontSize = 64.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
        
        // Timer
        if (uiState.isStarted) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = if (currentTimeMs > 3000) {
                        Color(0xFFFF9800).copy(alpha = 0.2f) // Orange si hésitation
                    } else {
                        MaterialTheme.colorScheme.secondaryContainer
                    }
                )
            ) {
                Text(
                    text = String.format("%.2f", timeSeconds) + " s",
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    color = if (currentTimeMs > 3000) {
                        Color(0xFFFF9800)
                    } else {
                        MaterialTheme.colorScheme.onSecondaryContainer
                    }
                )
            }
        }
        
        Spacer(modifier = Modifier.weight(1f))
        
        // Champ de saisie
        OutlinedTextField(
            value = uiState.studentAnswer,
            onValueChange = onAnswerChange,
            label = { Text("Votre réponse") },
            modifier = Modifier.fillMaxWidth(),
            enabled = uiState.isStarted,
            singleLine = true
        )
        
        Spacer(modifier = Modifier.weight(1f))
        
        // Boutons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (!uiState.isStarted) {
                Button(
                    onClick = onStartTimer,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Commencer", fontSize = 18.sp)
                }
            } else {
                Button(
                    onClick = onSubmit,
                    modifier = Modifier.weight(1f),
                    enabled = uiState.studentAnswer.isNotBlank()
                ) {
                    Text("Valider", fontSize = 18.sp)
                }
            }
            }
        }
    }
}

@Composable
fun RanQuizResultsScreen(
    results: esprit.tn.handy.data.RanQuizResults,
    onBack: () -> Unit,
    onRestart: () -> Unit
) {
    val scrollState = rememberScrollState()
    
    SafeArea {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
        // Contenu scrollable
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(scrollState)
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Résultats du Quiz RAN",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 8.dp, top = 8.dp)
            )
            
            // Statistiques
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "📊 Statistiques",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Temps moyen: ${String.format("%.2f", results.averageResponseTimeMs / 1000.0)}s",
                        fontSize = 16.sp
                    )
                    Text(
                        text = "Erreurs: ${results.totalErrors}/${results.results.size}",
                        fontSize = 16.sp
                    )
                    Text(
                        text = "Hésitations: ${results.totalHesitations}/${results.results.size}",
                        fontSize = 16.sp
                    )
                }
            }
            
            // Interprétation
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = if (results.totalErrors > 3 || results.totalHesitations > 3) {
                        Color(0xFFFF9800).copy(alpha = 0.1f)
                    } else {
                        Color(0xFF4CAF50).copy(alpha = 0.1f)
                    }
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "💡 Interprétation",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = results.interpretation,
                        fontSize = 16.sp,
                        lineHeight = 24.sp,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
            
            // Détails par question
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "📋 Détails par question",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    results.results.forEach { result ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Q${result.questionId}: ${result.item}",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Réponse: ${result.studentAnswer}",
                                    fontSize = 12.sp
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = if (result.isCorrect) "✅" else "❌",
                                    fontSize = 16.sp
                                )
                                Text(
                                    text = "${result.responseTimeMs}ms",
                                    fontSize = 12.sp,
                                    color = if (result.hasHesitation) Color(0xFFFF9800) else Color.Unspecified
                                )
                            }
                        }
                    }
                }
            }
        }
        
        // Boutons
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedButton(
                onClick = onBack,
                modifier = Modifier.weight(1f)
            ) {
                Text("Retour au menu")
            }
            Button(
                onClick = onRestart,
                modifier = Modifier.weight(1f)
            ) {
                Text("Recommencer")
            }
            }
        }
    }
}

fun getCategoryLabel(category: esprit.tn.handy.data.RanCategory): String {
    return when (category) {
        esprit.tn.handy.data.RanCategory.LETTER -> "📝 Nommez la lettre"
        esprit.tn.handy.data.RanCategory.DIGIT -> "🔢 Nommez le chiffre"
        esprit.tn.handy.data.RanCategory.COLOR -> "🎨 Nommez la couleur"
        esprit.tn.handy.data.RanCategory.OBJECT -> "🐾 Nommez l'objet"
        esprit.tn.handy.data.RanCategory.ALTERNATION -> "🔄 Alternance lettre-chiffre"
    }
}

