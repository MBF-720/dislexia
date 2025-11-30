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
import esprit.tn.handy.data.TrainSession
import esprit.tn.handy.mongodb.MongoDBService
import esprit.tn.handy.mongodb.model.AnimalMetrics
import esprit.tn.handy.mongodb.model.SessionDocument
import esprit.tn.handy.service.GeminiService
import esprit.tn.handy.service.TTSService

data class TrainUiState(
    val currentAnimalIndex: Int = 0,
    val currentAnimal: Animal? = null,
    val animalsToTrain: List<Animal> = emptyList(),
    val isSpeaking: Boolean = false,
    val isListening: Boolean = false,
    val isVerifying: Boolean = false, // Indicateur de chargement pendant la vérification
    val lastAnswer: String? = null,
    val isCorrect: Boolean? = null,
    val attempts: Int = 0,
    val correctCount: Int = 0,
    val totalCount: Int = 0,
    val isCompleted: Boolean = false,
    val error: String? = null
)

class TrainViewModel(application: Application) : AndroidViewModel(application) {
    
    private val database = AppDatabase.getDatabase(application)
    private val ttsService = TTSService(application)
    private val geminiService = GeminiService()
    private val mongoDBService = MongoDBService(application)
    
    private val _uiState = MutableStateFlow(TrainUiState())
    val uiState: StateFlow<TrainUiState> = _uiState.asStateFlow()
    
    private val trainStartTime = System.currentTimeMillis()
    private val sessionMetrics = mutableListOf<AnimalMetrics>()
    
    fun startTraining(animalsToTrain: List<Animal>) {
        val animals = if (animalsToTrain.isEmpty()) {
            // Par défaut, utiliser tous les animaux
            Animal.ANIMALS
        } else {
            // Répéter chaque animal 2-3 fois pour l'entraînement
            animalsToTrain.flatMap { listOf(it, it, it) }.shuffled()
        }
        
        _uiState.value = TrainUiState(
            animalsToTrain = animals,
            currentAnimalIndex = 0,
            currentAnimal = animals.firstOrNull(),
            totalCount = animals.size
        )
        
        speakCurrentAnimal()
    }
    
    fun speakCurrentAnimal() {
        val animal = _uiState.value.currentAnimal ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSpeaking = true)
            
            val text = "Peux-tu dire le nom de cet animal ? C'est un ${animal.nameLowercase}."
            ttsService.speak(text).collect { success ->
                _uiState.value = _uiState.value.copy(isSpeaking = false)
                if (success) {
                    _uiState.value = _uiState.value.copy(isListening = true)
                }
            }
        }
    }
    
    fun verifyAnswer(spokenText: String) {
        val state = _uiState.value
        val animal = state.currentAnimal ?: return
        
        viewModelScope.launch {
            _uiState.value = state.copy(
                isListening = false,
                isVerifying = true
            )
            
            val result = geminiService.verifyAnimalName(spokenText, animal.name)
            result.onSuccess { isCorrect ->
                _uiState.value = _uiState.value.copy(isVerifying = false)
                val newAttempts = state.attempts + 1
                val newCorrect = if (isCorrect) state.correctCount + 1 else state.correctCount
                
                // Calculer le temps de réponse (approximatif)
                val responseTime = System.currentTimeMillis() - (state.currentAnimalIndex * 5000L)
                val hasHesitation = responseTime > 2000
                
                // Ajouter les métriques pour MongoDB
                val metrics = AnimalMetrics(
                    animal = animal.name,
                    responseTimeMs = responseTime,
                    correct = isCorrect,
                    hesitations = if (hasHesitation) 1 else 0,
                    repetitions = newAttempts - 1,
                    inputMethod = "oral"
                )
                sessionMetrics.add(metrics)
                
                _uiState.value = state.copy(
                    lastAnswer = spokenText,
                    isCorrect = isCorrect,
                    attempts = newAttempts,
                    correctCount = newCorrect
                )
                
                // Sauvegarder la session
                val trainSession = TrainSession(
                    animalId = animal.id,
                    animalName = animal.name,
                    isCorrect = isCorrect,
                    attempts = newAttempts
                )
                database.trainSessionDao().insert(trainSession)
                
                if (isCorrect) {
                    // Passer à l'animal suivant après un court délai
                    nextAnimal()
                } else {
                    // Répéter avec indice phonologique
                    repeatWithHint(animal)
                }
            }.onFailure { error ->
                _uiState.value = state.copy(
                    isVerifying = false,
                    error = error.message ?: "Erreur de vérification"
                )
            }
        }
    }
    
    private fun repeatWithHint(animal: Animal) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSpeaking = true)
            
            val hint = when (animal.name.lowercase()) {
                "éléphant" -> "É- lé- phant. Commence par É."
                "lion" -> "Li- on. Commence par L."
                "girafe" -> "Gi- ra- fe. Commence par G."
                "zèbre" -> "Zè- bre. Commence par Z."
                "singe" -> "Sin- ge. Commence par S."
                "oiseau" -> "Oi- seau. Commence par O."
                else -> "Répète : ${animal.name}"
            }
            
            val text = "$hint Peux-tu dire le nom de cet animal ?"
            ttsService.speak(text).collect { success ->
                _uiState.value = _uiState.value.copy(isSpeaking = false)
                if (success) {
                    _uiState.value = _uiState.value.copy(isListening = true)
                }
            }
        }
    }
    
    fun nextAnimal() {
        val state = _uiState.value
        val newIndex = state.currentAnimalIndex + 1
        
        if (newIndex < state.animalsToTrain.size) {
            _uiState.value = state.copy(
                currentAnimalIndex = newIndex,
                currentAnimal = state.animalsToTrain[newIndex],
                lastAnswer = null,
                isCorrect = null,
                attempts = 0
            )
            speakCurrentAnimal()
        } else {
            // Entraînement terminé - Sauvegarder dans MongoDB
            viewModelScope.launch {
                val mongoSession = SessionDocument(
                    date = mongoDBService.formatDate(),
                    phase = "train",
                    animalMetrics = sessionMetrics.toList(),
                    totalTimeMs = System.currentTimeMillis() - trainStartTime,
                    score = state.correctCount,
                    totalQuestions = state.totalCount
                )
                mongoDBService.saveSession(mongoSession)
            }
            
            _uiState.value = state.copy(isCompleted = true)
        }
    }
    
    fun getBestAnimal(): Animal? {
        val state = _uiState.value
        // Logique simplifiée - retourner l'animal avec le plus de bonnes réponses
        // Dans une vraie implémentation, on analyserait les sessions sauvegardées
        return state.animalsToTrain.firstOrNull()
    }
    
    fun getAnimalToReview(): Animal? {
        val state = _uiState.value
        // Retourner un animal qui a été mal répondu
        // Dans une vraie implémentation, on analyserait les sessions sauvegardées
        return state.animalsToTrain.firstOrNull()
    }
    
    override fun onCleared() {
        super.onCleared()
        ttsService.shutdown()
    }
}

