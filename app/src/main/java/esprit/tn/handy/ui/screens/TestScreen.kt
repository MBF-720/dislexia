package esprit.tn.handy.ui.screens

import android.content.Context
import android.content.Intent
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import esprit.tn.handy.data.TestRecommendations
import esprit.tn.handy.service.STTService
import esprit.tn.handy.ui.components.SafeArea
import esprit.tn.handy.viewmodel.TestViewModel
import esprit.tn.handy.viewmodel.ViewModelFactory

@Composable
fun TestScreen(
    viewModel: TestViewModel = viewModel(
        factory = ViewModelFactory(LocalContext.current.applicationContext as android.app.Application)
    ),
    onBack: () -> Unit,
    onComplete: (List<esprit.tn.handy.data.Animal>) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val sttService = remember { STTService(context) }
    
    val speechLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val spokenText = sttService.processSpeechResult(
            result.resultCode,
            result.data
        )
        spokenText?.let { viewModel.verifyAnswer(it) }
    }
    
    LaunchedEffect(uiState.isListening) {
        if (uiState.isListening && !uiState.isCompleted) {
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, "fr-FR")
                putExtra(RecognizerIntent.EXTRA_PROMPT, "Parlez maintenant...")
            }
            speechLauncher.launch(intent)
        }
    }
    
    // Ne pas naviguer automatiquement - l'utilisateur doit cliquer sur "Retour au menu"
    // LaunchedEffect supprimé pour éviter la navigation automatique
    
    if (uiState.isCompleted) {
        // Écran de résultats
        TestResultsScreen(
            score = uiState.score,
            totalQuestions = uiState.totalQuestions,
            bestAnimal = viewModel.getBestAnimal(),
            animalsToReview = viewModel.getAnimalsToReview(),
            aiRecommendations = uiState.aiRecommendations,
            isAnalyzing = uiState.isAnalyzing,
            error = uiState.error,
            onBack = onBack
        )
    } else {
        // Écran de test
        SafeArea {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
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
                    text = "Test ${uiState.currentAnimalIndex + 1}/${uiState.totalQuestions}",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Image de l'animal
            uiState.currentAnimal?.let { animal ->
                val drawableId = context.resources.getIdentifier(
                    animal.drawableResName,
                    "drawable",
                    context.packageName
                )
                
                if (drawableId != 0) {
                    val drawable = context.getDrawable(drawableId)
                    drawable?.let {
                        // Utiliser les dimensions du drawable ou des dimensions par défaut
                        val width = if (it.intrinsicWidth > 0) it.intrinsicWidth else 800
                        val height = if (it.intrinsicHeight > 0) it.intrinsicHeight else 800
                        
                        val bitmap = android.graphics.Bitmap.createBitmap(
                            width,
                            height,
                            android.graphics.Bitmap.Config.ARGB_8888
                        )
                        val canvas = android.graphics.Canvas(bitmap)
                        it.setBounds(0, 0, width, height)
                        it.draw(canvas)
                        
                        Image(
                            bitmap = bitmap.asImageBitmap(),
                            contentDescription = animal.name,
                            modifier = Modifier.size(300.dp)
                        )
                    } ?: run {
                        // Si le drawable est null, afficher un placeholder
                        Text(
                            text = "Image non trouvée: ${animal.drawableResName}",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                } else {
                    // Si l'ID est 0, le drawable n'existe pas
                    Text(
                        text = "Image non trouvée: ${animal.drawableResName}",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.error
                    )
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Quel est le nom de cet animal ?",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(
                        onClick = { viewModel.speakQuestion() }
                    ) {
                        Icon(
                            imageVector = Icons.Filled.PlayArrow,
                            contentDescription = "Écouter la question",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(32.dp))
                
                // Feedback
                when {
                    uiState.isListening -> {
                        Text(
                            text = "Parle maintenant...",
                            fontSize = 20.sp,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                    uiState.isVerifying -> {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(
                                text = "Vérification en cours...",
                                fontSize = 20.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    uiState.isCorrect == true -> {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = Color(0xFF4CAF50).copy(alpha = 0.2f)
                            )
                        ) {
                            Text(
                                text = "Correct ! ✓",
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF4CAF50),
                                modifier = Modifier.padding(16.dp),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                    uiState.isCorrect == false -> {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = Color(0xFFF44336).copy(alpha = 0.2f)
                            )
                        ) {
                            Text(
                                text = "Incorrect",
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFF44336),
                                modifier = Modifier.padding(16.dp),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Si une réponse a été donnée, afficher le bouton "Suivant"
                if (uiState.isCorrect != null) {
                    Button(
                        onClick = { viewModel.nextAnimal() },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Text("Animal suivant", fontSize = 20.sp)
                    }
                } else {
                    // Bouton pour parler
                    Button(
                        onClick = {
                            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                                putExtra(RecognizerIntent.EXTRA_LANGUAGE, "fr-FR")
                                putExtra(RecognizerIntent.EXTRA_PROMPT, "Parlez maintenant...")
                            }
                            speechLauncher.launch(intent)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !uiState.isListening && !uiState.isVerifying && uiState.isCorrect == null
                    ) {
                        Text("Parler", fontSize = 20.sp)
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Ou saisie texte
                    var textInput by remember { mutableStateOf("") }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = textInput,
                            onValueChange = { textInput = it },
                            label = { Text("Ou tapez le nom") },
                            modifier = Modifier.weight(1f),
                            enabled = !uiState.isListening && !uiState.isVerifying && uiState.isCorrect == null
                        )
                        Button(
                            onClick = {
                                if (textInput.isNotBlank()) {
                                    viewModel.submitTextAnswer(textInput)
                                    textInput = ""
                                }
                            },
                            enabled = !uiState.isListening && !uiState.isVerifying && uiState.isCorrect == null && textInput.isNotBlank()
                        ) {
                            Text("OK")
                        }
                    }
                }
            }
            
            uiState.error?.let { error ->
                Text(
                    text = "Erreur: $error",
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 14.sp
                )
            }
            }
        }
    }
}

@Composable
fun TestResultsScreen(
    score: Int,
    totalQuestions: Int,
    bestAnimal: esprit.tn.handy.data.Animal?,
    animalsToReview: List<esprit.tn.handy.data.Animal>,
    aiRecommendations: esprit.tn.handy.data.TestRecommendations? = null,
    isAnalyzing: Boolean = false,
    error: String? = null,
    onBack: () -> Unit
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
            // Titre
            Text(
                text = "Résultats du Test",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 8.dp, top = 8.dp)
            )
        
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Score",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "$score / $totalQuestions",
                    fontSize = 48.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
        
        if (bestAnimal != null) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF4CAF50).copy(alpha = 0.2f)
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Meilleur animal",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = bestAnimal.name,
                        fontSize = 24.sp
                    )
                }
            }
        }
        
        if (animalsToReview.isNotEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFFFF9800).copy(alpha = 0.2f)
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Animaux à retravailler",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    animalsToReview.forEach { animal ->
                        Text(
                            text = "• ${animal.name}",
                            fontSize = 16.sp,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }
                }
            }
        }
        
        // Section Recommandations AI
        if (isAnalyzing) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    CircularProgressIndicator()
                    Text(
                        text = "Analyse en cours par l'IA...",
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        } else if (error != null) {
            // Afficher l'erreur
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "⚠️ Erreur",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.error
                    )
                    Text(
                        text = error,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
        } else if (aiRecommendations != null) {
            // Résumé pour l'élève
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF4CAF50).copy(alpha = 0.1f)
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "🌟 Résumé pour toi",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF4CAF50)
                    )
                    
                    Text(
                        text = aiRecommendations.eleve,
                        fontSize = 16.sp,
                        lineHeight = 24.sp,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
            
            // Recommandations pour le professeur
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
                        text = "💡 Recommandations pour le professeur",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    
                    Text(
                        text = aiRecommendations.professeur,
                        fontSize = 16.sp,
                        lineHeight = 24.sp,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }
        } // Ferme la Column scrollable
        
        // Bouton fixe en bas
        Button(
            onClick = onBack,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp)
        ) {
            Text("Retour au menu", fontSize = 18.sp)
        }
        }
    }
}

