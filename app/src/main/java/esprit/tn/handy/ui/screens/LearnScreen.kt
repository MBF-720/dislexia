package esprit.tn.handy.ui.screens

import android.content.Context
import android.content.Intent
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
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
import esprit.tn.handy.service.STTService
import esprit.tn.handy.ui.components.DyslexiaButton
import esprit.tn.handy.ui.components.DyslexiaCard
import esprit.tn.handy.ui.components.DyslexiaFeedback
import esprit.tn.handy.ui.components.DyslexiaInstruction
import esprit.tn.handy.ui.components.DyslexiaTextButton
import esprit.tn.handy.ui.components.SafeArea
import esprit.tn.handy.viewmodel.LearnViewModel
import esprit.tn.handy.viewmodel.ViewModelFactory

@Composable
fun LearnScreen(
    viewModel: LearnViewModel = viewModel(
        factory = ViewModelFactory(LocalContext.current.applicationContext as android.app.Application)
    ),
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val sttService = remember { STTService(context) }
    
    var speechResult by remember { mutableStateOf<String?>(null) }
    
    val speechLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val spokenText = sttService.processSpeechResult(
            result.resultCode,
            result.data
        )
        speechResult = spokenText
        spokenText?.let { viewModel.verifyAnswer(it) }
    }
    
    LaunchedEffect(uiState.isListening) {
        if (uiState.isListening) {
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
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
            DyslexiaTextButton(onClick = onBack, text = "← Retour")
            Text(
                text = "Apprendre ${uiState.currentAnimalIndex + 1}/6",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(80.dp)) // Équilibre visuel
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
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
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Instruction simple avec bouton audio
            DyslexiaInstruction(
                text = "Dis \"${animal.name}\" quand tu vois l'animal",
                onPlayAudio = { viewModel.speakAnimalName() },
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Nom de l'animal (grande police pour dyslexie)
            Text(
                text = animal.name,
                fontSize = 56.sp,
                fontWeight = FontWeight.Bold,
                color = if (uiState.isCorrect == true) Color(0xFF4CAF50) else MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                letterSpacing = 2.sp,
                lineHeight = 72.sp
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Feedback adapté
            when {
                uiState.isSpeaking -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(32.dp),
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            text = "Écoute... 👂",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                uiState.isListening -> {
                    Text(
                        text = "🎤 Parle maintenant...",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.SemiBold,
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
                            modifier = Modifier.size(32.dp),
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
            }
            
            // Feedback de résultat
            DyslexiaFeedback(
                isCorrect = uiState.isCorrect,
                modifier = Modifier.padding(vertical = 16.dp)
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Spacer(modifier = Modifier.weight(1f))
            
            // Boutons adaptés
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                DyslexiaButton(
                    onClick = { viewModel.listenAgain() },
                    text = "🔊 Écouter encore",
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !uiState.isSpeaking && !uiState.isVerifying,
                    containerColor = MaterialTheme.colorScheme.secondary
                )
                
                if (uiState.isCorrect == true) {
                    DyslexiaButton(
                        onClick = { viewModel.nextAnimal() },
                        text = "➡️ Suivant",
                        modifier = Modifier.fillMaxWidth(),
                        containerColor = Color(0xFF4CAF50)
                    )
                } else if (uiState.isCorrect == false) {
                    DyslexiaButton(
                        onClick = {
                            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                                putExtra(RecognizerIntent.EXTRA_LANGUAGE, "fr-FR")
                                putExtra(RecognizerIntent.EXTRA_PROMPT, "Parlez maintenant...")
                            }
                            speechLauncher.launch(intent)
                        },
                        text = "🔄 Réessayer",
                        modifier = Modifier.fillMaxWidth(),
                        containerColor = MaterialTheme.colorScheme.tertiary
                    )
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
                        text = "Statistiques",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Text("Tentatives: ${uiState.totalAttempts}")
                    Text("Correctes: ${uiState.correctAnswers}")
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

