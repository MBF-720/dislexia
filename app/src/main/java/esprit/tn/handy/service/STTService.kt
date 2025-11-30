package esprit.tn.handy.service

import android.content.Context
import android.content.Intent
import android.speech.RecognizerIntent
import androidx.activity.result.ActivityResultLauncher
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

class STTService(private val context: Context) {
    
    /**
     * Lance la reconnaissance vocale Android native
     * Retourne le texte reconnu via un Flow
     */
    fun startSpeechRecognition(
        launcher: ActivityResultLauncher<Intent>
    ): Flow<String?> = callbackFlow {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "fr-FR")
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Parlez maintenant...")
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        }
        
        launcher.launch(intent)
        
        awaitClose { }
    }
    
    /**
     * Traite le résultat de la reconnaissance vocale
     */
    fun processSpeechResult(resultCode: Int, data: android.content.Intent?): String? {
        if (resultCode == android.app.Activity.RESULT_OK && data != null) {
            val results = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            return results?.firstOrNull()
        }
        return null
    }
}

