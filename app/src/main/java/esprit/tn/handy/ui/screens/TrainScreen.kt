package esprit.tn.handy.ui.screens

import android.content.Context
import android.content.Intent
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import esprit.tn.handy.data.Animal
import esprit.tn.handy.service.STTService
import esprit.tn.handy.ui.components.SafeArea
import esprit.tn.handy.viewmodel.TrainViewModel
import esprit.tn.handy.viewmodel.ViewModelFactory

@Composable
fun TrainScreen(
    animalsToTrain: List<Animal> = emptyList(),
    viewModel: TrainViewModel = viewModel(
        factory = ViewModelFactory(LocalContext.current.applicationContext as android.app.Application)
    ),
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val sttService = remember { STTService(context) }

    LaunchedEffect(Unit) {
        viewModel.startTraining(animalsToTrain)
    }

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
                putExtra(
                    RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                    RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
                )
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, "fr-FR")
                putExtra(RecognizerIntent.EXTRA_PROMPT, "Parlez maintenant...")
            }
            speechLauncher.launch(intent)
        }
    }

    // Animation pour feedback positif
    val scale by animateFloatAsState(
        targetValue = if (uiState.isCorrect == true) 1.2f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ), label = ""
    )

    if (uiState.isCompleted) {
        TrainResultsScreen(
            correctCount = uiState.correctCount,
            totalCount = uiState.totalCount,
            bestAnimal = viewModel.getBestAnimal(),
            animalToReview = viewModel.getAnimalToReview(),
            onBack = onBack
        )
    } else {
        SafeArea {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
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
                        text = "Entraînement ${uiState.currentAnimalIndex + 1}/${uiState.totalCount}",
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
                            val bitmap = android.graphics.Bitmap.createBitmap(
                                it.intrinsicWidth,
                                it.intrinsicHeight,
                                android.graphics.Bitmap.Config.ARGB_8888
                            )
                            val canvas = android.graphics.Canvas(bitmap)
                            it.setBounds(0, 0, canvas.width, canvas.height)
                            it.draw(canvas)

                            Image(
                                bitmap = bitmap.asImageBitmap(),
                                contentDescription = animal.name,
                                modifier = Modifier
                                    .size(300.dp)
                                    .scale(scale)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Nom de l'animal
                    Text(
                        text = animal.name,
                        fontSize = 48.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (uiState.isCorrect == true) Color(0xFF4CAF50) else MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(32.dp))

                    // Feedback
                    when {
                        uiState.isSpeaking -> {
                            Text(
                                text = "Écoute...",
                                fontSize = 20.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

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
                                    text = "Bravo ! ✓",
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
                                    text = "Essaie encore avec l'indice",
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFF44336),
                                    modifier = Modifier.padding(16.dp),
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Bouton pour écouter
                    Button(
                        onClick = { viewModel.speakCurrentAnimal() },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !uiState.isSpeaking
                    ) {
                        Text("Écouter encore", fontSize = 20.sp)
                    }
                }

                // Statistiques
                Spacer(modifier = Modifier.height(24.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "Progression",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        Text("Correctes: ${uiState.correctCount} / ${uiState.totalCount}")
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
fun TrainResultsScreen(
    correctCount: Int,
    totalCount: Int,
    bestAnimal: Animal?,
    animalToReview: Animal?,
    onBack: () -> Unit
) {
    SafeArea {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Text(
                text = "Entraînement Terminé !",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
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
                        text = "$correctCount / $totalCount",
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

            if (animalToReview != null) {
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
                            text = "Animal à retravailler",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = animalToReview.name,
                            fontSize = 24.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = onBack,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Retour au menu", fontSize = 18.sp)
            }
        }
    }
}

