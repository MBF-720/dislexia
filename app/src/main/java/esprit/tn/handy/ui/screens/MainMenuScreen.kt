package esprit.tn.handy.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import esprit.tn.handy.ui.components.DyslexiaButton
import esprit.tn.handy.ui.components.SafeArea

@Composable
fun MainMenuScreen(
    onLearnClick: () -> Unit,
    onTestClick: () -> Unit,
    onTrainClick: () -> Unit,
    onRanQuizClick: () -> Unit
) {
    SafeArea {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
        // Titre avec emoji
        Text(
            text = "🦁 Zoo - Module Dyslexie",
            fontSize = 36.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.primary,
            letterSpacing = 1.sp,
            lineHeight = 48.sp,
            modifier = Modifier.padding(bottom = 56.dp)
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Bouton Apprendre
        DyslexiaButton(
            onClick = onLearnClick,
            text = "📚 Apprendre",
            modifier = Modifier.fillMaxWidth(),
            containerColor = MaterialTheme.colorScheme.primary
        )
        
        Spacer(modifier = Modifier.height(20.dp))
        
        // Bouton Se Tester
        DyslexiaButton(
            onClick = onTestClick,
            text = "✏️ Se Tester",
            modifier = Modifier.fillMaxWidth(),
            containerColor = MaterialTheme.colorScheme.secondary
        )
        
        Spacer(modifier = Modifier.height(20.dp))
        
        // Bouton S'Entraîner
        DyslexiaButton(
            onClick = onTrainClick,
            text = "🎯 S'Entraîner",
            modifier = Modifier.fillMaxWidth(),
            containerColor = MaterialTheme.colorScheme.tertiary
        )
        
        Spacer(modifier = Modifier.height(20.dp))
        
        // Bouton Quiz RAN
        DyslexiaButton(
            onClick = onRanQuizClick,
            text = "🔍 Quiz RAN Dyslexie",
            modifier = Modifier.fillMaxWidth(),
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
        )
        }
    }
}

