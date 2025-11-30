package esprit.tn.handy.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Wrapper pour ajouter des zones de sécurité (safe zones) en haut et en bas
 * Évite que les boutons soient cachés et que les titres soient trop surélevés
 */
@Composable
fun SafeArea(
    modifier: Modifier = Modifier,
    topPadding: PaddingValues = PaddingValues(top = 16.dp),
    bottomPadding: PaddingValues = PaddingValues(bottom = 24.dp),
    content: @Composable () -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(top = topPadding.calculateTopPadding())
            .padding(bottom = bottomPadding.calculateBottomPadding())
    ) {
        content()
    }
}

/**
 * Padding horizontal standard pour les écrans
 */
val HorizontalScreenPadding = PaddingValues(horizontal = 24.dp)

/**
 * Padding vertical standard pour les écrans
 */
val VerticalScreenPadding = PaddingValues(vertical = 16.dp)

