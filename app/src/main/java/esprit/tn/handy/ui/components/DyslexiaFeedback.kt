package esprit.tn.handy.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Affiche un feedback visuel adapté pour dyslexie
 * - Utilise orange/jaune au lieu de rouge pour les erreurs
 * - Animation douce
 * - Message encourageant
 */
@Composable
fun DyslexiaFeedback(
    isCorrect: Boolean?,
    message: String? = null,
    modifier: Modifier = Modifier
) {
    if (isCorrect == null && message == null) return
    
    val scale by animateFloatAsState(
        targetValue = if (isCorrect != null) 1f else 0.9f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "feedbackScale"
    )
    
    val (color, backgroundColor, emoji) = when {
        isCorrect == true -> Triple(
            Color(0xFF4CAF50), // Vert doux
            Color(0xFFE8F5E9).copy(alpha = 0.8f), // Vert très clair
            "✅"
        )
        isCorrect == false -> Triple(
            Color(0xFFFF9800), // Orange doux (pas de rouge)
            Color(0xFFFFF3E0).copy(alpha = 0.8f), // Orange très clair
            "😊"
        )
        else -> Triple(
            MaterialTheme.colorScheme.primary,
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
            "💡"
        )
    }
    
    Card(
        modifier = modifier
            .fillMaxWidth()
            .scale(scale),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = backgroundColor
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = emoji,
                fontSize = 32.sp,
                modifier = Modifier.padding(end = 12.dp)
            )
            Text(
                text = message ?: when {
                    isCorrect == true -> "Bravo ! C'est correct ! 🎉"
                    isCorrect == false -> "Tu peux réessayer 😊"
                    else -> ""
                },
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = color,
                letterSpacing = 0.5.sp
            )
        }
    }
}

/**
 * Instruction simple avec bouton audio
 */
@Composable
fun DyslexiaInstruction(
    text: String,
    onPlayAudio: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = text,
            fontSize = 20.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            letterSpacing = 0.5.sp,
            modifier = Modifier.padding(end = 8.dp)
        )
        IconButton(
            onClick = onPlayAudio,
            modifier = Modifier.size(48.dp) // Grande zone de clic
        ) {
            Icon(
                imageVector = Icons.Filled.PlayArrow,
                contentDescription = "Écouter l'instruction",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(28.dp)
            )
        }
    }
}

