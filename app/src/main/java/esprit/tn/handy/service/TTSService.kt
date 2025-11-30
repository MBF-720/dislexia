package esprit.tn.handy.service

import android.content.Context
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import java.util.*

class TTSService(private val context: Context) {
    
    private var tts: TextToSpeech? = null
    private var isInitialized = false
    
    init {
        initializeTTS()
    }
    
    private fun initializeTTS() {
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val result = tts?.setLanguage(Locale.FRENCH)
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    // Fallback to default
                    tts?.setLanguage(Locale.getDefault())
                }
                isInitialized = true
            }
        }
    }
    
    fun speak(text: String): Flow<Boolean> = callbackFlow {
        if (!isInitialized || tts == null) {
            trySend(false)
            close()
            return@callbackFlow
        }
        
        val utteranceId = UUID.randomUUID().toString()
        var isCompleted = false
        
        tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceIdParam: String?) {
                // Started
            }
            
            override fun onDone(utteranceIdParam: String?) {
                if (utteranceIdParam == utteranceId && !isCompleted) {
                    isCompleted = true
                    trySend(true)
                    close()
                }
            }
            
            override fun onError(utteranceIdParam: String?) {
                if (utteranceIdParam == utteranceId && !isCompleted) {
                    isCompleted = true
                    trySend(false)
                    close()
                }
            }
        })
        
        val params = Bundle().apply {
            putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, utteranceId)
        }
        
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, params, utteranceId)
        
        awaitClose { }
    }
    
    fun stop() {
        tts?.stop()
    }
    
    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
        tts = null
        isInitialized = false
    }
}

