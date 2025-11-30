package esprit.tn.handy.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import esprit.tn.handy.data.Animal
import esprit.tn.handy.data.AppDatabase
import esprit.tn.handy.data.LearnSession
import esprit.tn.handy.mongodb.MongoDBService
import esprit.tn.handy.mongodb.model.AnimalMetrics
import esprit.tn.handy.mongodb.model.SessionDocument
import esprit.tn.handy.service.GeminiService
import esprit.tn.handy.service.TTSService

data class LearnUiState(
    val currentAnimalIndex: Int = 0,
    val currentAnimal: Animal? = null,
    val isSpeaking: Boolean = false,
    val isListening: Boolean = false,
    val isVerifying: Boolean = false, // Indicateur de chargement pendant la vérification
    val lastAnswer: String? = null,
    val isCorrect: Boolean? = null,
    val repetitions: Int = 0,
    val correctAnswers: Int = 0,
    val totalAttempts: Int = 0,
    val startTime: Long = 0,
    val error: String? = null
)

class LearnViewModel(application: Application) : AndroidViewModel(application) {
    
    private val database = AppDatabase.getDatabase(application)
    private val ttsService = TTSService(application)
    private val geminiService = GeminiService()
    private val mongoDBService = MongoDBService(application)
    
    private val _uiState = MutableStateFlow(LearnUiState())
    val uiState: StateFlow<LearnUiState> = _uiState.asStateFlow()
    
    private val animals = Animal.ANIMALS
    private var sessionStartTime = System.currentTimeMillis()
    private val sessionMetrics = mutableListOf<AnimalMetrics>()
    
    init {
        loadNextAnimal()
    }
    
    fun loadNextAnimal() {
        val currentIndex = _uiState.value.currentAnimalIndex
        if (currentIndex < animals.size) {
            _uiState.value = _uiState.value.copy(
                currentAnimal = animals[currentIndex],
                currentAnimalIndex = currentIndex,
                isSpeaking = false,
                isListening = false,
                lastAnswer = null,
                isCorrect = null,
                repetitions = 0,
                correctAnswers = 0,
                totalAttempts = 0,
                startTime = System.currentTimeMillis(),
                error = null
            )
            speakAnimalName()
        }
    }
    
    fun speakAnimalName() {
        val animal = _uiState.value.currentAnimal ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSpeaking = true)
            
            val text = "Ceci est un ${animal.nameLowercase}. Peux-tu dire le nom de cet animal ?"
            ttsService.speak(text).collect { success ->
                _uiState.value = _uiState.value.copy(isSpeaking = false)
                if (success) {
                    _uiState.value = _uiState.value.copy(isListening = true)
                }
            }
        }
    }
    
    fun verifyAnswer(spokenText: String) {
        val animal = _uiState.value.currentAnimal ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isListening = false,
                isVerifying = true
            )
            
            val result = geminiService.verifyAnimalName(spokenText, animal.name)
            result.onSuccess { isCorrect ->
                _uiState.value = _uiState.value.copy(isVerifying = false)
                val currentState = _uiState.value
                val newAttempts = currentState.totalAttempts + 1
                val newCorrect = if (isCorrect) currentState.correctAnswers + 1 else currentState.correctAnswers
                
                val responseTime = System.currentTimeMillis() - currentState.startTime
                val hasHesitation = responseTime > 2000
                
                // Ajouter les métriques pour MongoDB
                val metrics = AnimalMetrics(
                    animal = animal.name,
                    responseTimeMs = responseTime,
                    correct = isCorrect,
                    hesitations = if (hasHesitation) 1 else 0,
                    repetitions = currentState.repetitions,
                    inputMethod = "oral"
                )
                sessionMetrics.add(metrics)
                
                _uiState.value = currentState.copy(
                    lastAnswer = spokenText,
                    isCorrect = isCorrect,
                    totalAttempts = newAttempts,
                    correctAnswers = newCorrect
                )
                
                if (isCorrect) {
                    // Animation positive - sera gérée dans l'UI
                    saveSession()
                } else {
                    // Répéter TTS
                    speakAnimalName()
                }
            }.onFailure { error ->
                _uiState.value = _uiState.value.copy(
                    isVerifying = false,
                    error = error.message ?: "Erreur de vérification"
                )
            }
        }
    }
    
    fun listenAgain() {
        speakAnimalName()
    }
    
    fun nextAnimal() {
        val currentIndex = _uiState.value.currentAnimalIndex
        if (currentIndex < animals.size - 1) {
            saveSession()
            _uiState.value = _uiState.value.copy(
                currentAnimalIndex = currentIndex + 1
            )
            loadNextAnimal()
        } else {
            // Fin de la session
            saveSession()
        }
    }
    
    private fun saveSession() {
        val state = _uiState.value
        val animal = state.currentAnimal ?: return
        
        viewModelScope.launch {
            val timeSpent = System.currentTimeMillis() - state.startTime
            // Sauvegarder dans Room
            val session = LearnSession(
                animalId = animal.id,
                animalName = animal.name,
                timeSpent = timeSpent,
                repetitions = state.repetitions,
                correctAnswers = state.correctAnswers,
                totalAttempts = state.totalAttempts
            )
            database.learnSessionDao().insert(session)
            
            // Sauvegarder dans MongoDB si on a des métriques
            if (sessionMetrics.isNotEmpty()) {
                val mongoSession = SessionDocument(
                    date = mongoDBService.formatDate(),
                    phase = "learn",
                    animalMetrics = sessionMetrics.toList(),
                    totalTimeMs = System.currentTimeMillis() - sessionStartTime,
                    score = state.correctAnswers,
                    totalQuestions = state.totalAttempts
                )
                val result = mongoDBService.saveSession(mongoSession)
                result.onSuccess {
                    android.util.Log.d("LearnViewModel", "✅ Session MongoDB sauvegardée: $it")
                }.onFailure { error ->
                    android.util.Log.e("LearnViewModel", "❌ Erreur MongoDB: ${error.message}")
                }
                sessionMetrics.clear() // Réinitialiser pour la prochaine session
            }
        }
    }
    
    override fun onCleared() {
        super.onCleared()
        ttsService.shutdown()
    }
}

