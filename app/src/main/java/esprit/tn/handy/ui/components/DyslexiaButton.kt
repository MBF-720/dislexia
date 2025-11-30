package esprit.tn.handy.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Bouton adapté pour enfants dyslexiques
 * - Grande taille (minimum 48dp)
 * - Formes arrondies
 * - Animation subtile au clic
 * - Texte grand et clair
 */
@Composable
fun DyslexiaButton(
    onClick: () -> Unit,
    text: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    containerColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.primary,
    contentColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onPrimary
) {
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "buttonScale"
    )
    
    Button(
        onClick = {
            isPressed = true
            onClick()
            // Reset après un court délai
            kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main).launch {
                kotlinx.coroutines.delay(100)
                isPressed = false
            }
        },
        modifier = modifier
            .height(64.dp) // Minimum 48dp, ici 64dp pour plus de confort
            .scale(scale),
        enabled = enabled,
        shape = RoundedCornerShape(16.dp), // Formes très arrondies
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = contentColor,
            disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
            disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant
        ),
        contentPadding = PaddingValues(horizontal = 32.dp, vertical = 16.dp) // Grand padding
    ) {
        Text(
            text = text,
            fontSize = 20.sp, // Taille minimale 18-20sp
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.5.sp // Espacement des lettres
        )
    }
}

/**
 * Bouton de texte adapté pour dyslexie
 */
@Composable
fun DyslexiaTextButton(
    onClick: () -> Unit,
    text: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    TextButton(
        onClick = onClick,
        modifier = modifier.height(56.dp), // Grande zone de clic
        enabled = enabled,
        shape = RoundedCornerShape(12.dp),
        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp)
    ) {
        Text(
            text = text,
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.5.sp
        )
    }
}

/**
 * Carte adaptée pour dyslexie avec coins arrondis et espacement généreux
 */
@Composable
fun DyslexiaCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp), // Coins très arrondis
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp) // Ombre douce
    ) {
        Column(
            modifier = Modifier.padding(20.dp), // Padding généreux
            content = content
        )
    }
}

