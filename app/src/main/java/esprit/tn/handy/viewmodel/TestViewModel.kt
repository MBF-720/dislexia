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
import esprit.tn.handy.data.TestResult
import esprit.tn.handy.data.TestRecommendations
import esprit.tn.handy.mongodb.MongoDBService
import esprit.tn.handy.mongodb.model.AnimalMetrics
import esprit.tn.handy.mongodb.model.SessionDocument
import esprit.tn.handy.service.GeminiService
import esprit.tn.handy.service.TTSService

data class TestUiState(
    val currentAnimalIndex: Int = 0,
    val currentAnimal: Animal? = null,
    val shuffledAnimals: List<Animal> = emptyList(),
    val isListening: Boolean = false,
    val isVerifying: Boolean = false, // Indicateur de chargement pendant la vérification
    val lastAnswer: String? = null,
    val isCorrect: Boolean? = null,
    val responseTime: Long = 0,
    val questionStartTime: Long = 0,
    val results: List<TestResult> = emptyList(),
    val isCompleted: Boolean = false,
    val score: Int = 0,
    val totalQuestions: Int = 0,
    val aiRecommendations: esprit.tn.handy.data.TestRecommendations? = null, // Recommandations structurées (élève + professeur)
    val isAnalyzing: Boolean = false, // Indicateur d'analyse en cours
    val error: String? = null
)

class TestViewModel(application: Application) : AndroidViewModel(application) {
    
    private val database = AppDatabase.getDatabase(application)
    private val geminiService = GeminiService()
    private val mongoDBService = MongoDBService(application)
    private val ttsService = TTSService(application)
    
    private val _uiState = MutableStateFlow(TestUiState())
    val uiState: StateFlow<TestUiState> = _uiState.asStateFlow()
    
    private val testStartTime = System.currentTimeMillis()
    private val sessionMetrics = mutableListOf<AnimalMetrics>()
    
    init {
        startTest()
    }
    
    fun startTest() {
        val shuffled = Animal.ANIMALS.shuffled()
        sessionMetrics.clear() // Réinitialiser les métriques
        _uiState.value = TestUiState(
            shuffledAnimals = shuffled,
            currentAnimalIndex = 0,
            currentAnimal = shuffled.firstOrNull(),
            totalQuestions = shuffled.size,
            questionStartTime = System.currentTimeMillis()
        )
        // Lire automatiquement la première question
        speakQuestion()
    }
    
    private var currentInputMethod = "oral" // "oral" ou "texte"
    
    /**
     * Vérifie si la réponse correspond à l'animal attendu (sans utiliser l'IA)
     * Utilise une comparaison simple et tolérante aux variations
     */
    private fun verifyAnswerLocally(spokenText: String, expectedAnimal: Animal): Boolean {
        val spoken = spokenText.trim().lowercase()
        val expected = expectedAnimal.nameLowercase.lowercase()
        val expectedName = expectedAnimal.name.lowercase()
        
        // Comparaison exacte (ignorant la casse)
        if (spoken == expected || spoken == expectedName) {
            return true
        }
        
        // Vérifier si le texte parlé contient le nom de l'animal ou vice versa
        if (spoken.contains(expected) || expected.contains(spoken)) {
            return true
        }
        
        // Comparaison avec les autres animaux pour détecter les confusions
        // Si le texte correspond à un autre animal, c'est incorrect
        val otherAnimal = Animal.ANIMALS.find { 
            it.id != expectedAnimal.id && 
            (spoken.contains(it.nameLowercase.lowercase()) || 
             spoken.contains(it.name.lowercase()))
        }
        
        // Si c'est un autre animal, c'est incorrect
        if (otherAnimal != null) {
            return false
        }
        
        // Comparaison de similarité simple (premiers caractères)
        val spokenStart = spoken.take(3)
        val expectedStart = expected.take(3)
        if (spokenStart == expectedStart && spoken.length >= expected.length * 0.7) {
            return true
        }
        
        return false
    }
    
    fun verifyAnswer(spokenText: String, inputMethod: String = "oral") {
        val state = _uiState.value
        val animal = state.currentAnimal ?: return
        
        currentInputMethod = inputMethod
        
        viewModelScope.launch {
            _uiState.value = state.copy(
                isListening = false,
                isVerifying = true
            )
            
            // Petit délai pour l'effet visuel (simulation de vérification)
            kotlinx.coroutines.delay(300)
            
            val responseTime = System.currentTimeMillis() - state.questionStartTime
            val hasHesitation = responseTime > 2000
            
            // Vérification locale sans IA
            val isCorrect = verifyAnswerLocally(spokenText, animal)
            
            _uiState.value = _uiState.value.copy(isVerifying = false)
            
            val testResult = TestResult(
                animalId = animal.id,
                animalName = animal.name,
                isCorrect = isCorrect,
                responseTime = responseTime,
                hasHesitation = hasHesitation,
                confusedWith = if (!isCorrect) spokenText else null
            )
            
            // Ajouter les métriques pour MongoDB
            val metrics = AnimalMetrics(
                animal = animal.name,
                responseTimeMs = responseTime,
                correct = isCorrect,
                hesitations = if (hasHesitation) 1 else 0,
                confusedWith = if (!isCorrect) spokenText else null,
                inputMethod = currentInputMethod
            )
            sessionMetrics.add(metrics)
            
            val newResults = state.results + testResult
            val newScore = newResults.count { it.isCorrect }
            val newIndex = state.currentAnimalIndex + 1
            
            if (newIndex < state.shuffledAnimals.size) {
                // Sauvegarder le résultat
                database.testResultDao().insert(testResult)
                
                // Mettre à jour l'état avec le résultat, mais garder isCorrect pour afficher le feedback
                _uiState.value = _uiState.value.copy(
                    lastAnswer = spokenText,
                    isCorrect = isCorrect,
                    isVerifying = false,
                    responseTime = responseTime,
                    results = newResults,
                    score = newScore
                )
            } else {
                // Test terminé - Sauvegarder dans MongoDB et analyser avec Gemini
                viewModelScope.launch {
                    // Sauvegarder dans MongoDB
                    val mongoSession = SessionDocument(
                        date = mongoDBService.formatDate(),
                        phase = "test",
                        animalMetrics = sessionMetrics.toList(),
                        totalTimeMs = System.currentTimeMillis() - testStartTime,
                        score = newScore,
                        totalQuestions = state.totalQuestions
                    )
                    mongoDBService.saveSession(mongoSession)
                    
                    // Analyser avec Gemini (UNIQUEMENT à la fin du test)
                    android.util.Log.d("TestViewModel", "🔍 Démarrage de l'analyse IA...")
                    _uiState.value = _uiState.value.copy(isAnalyzing = true)
                    
                    val animalsToReviewNames = getAnimalsToReview().map { it.name }
                    android.util.Log.d("TestViewModel", "📊 Résultats: ${newResults.size}, Score: $newScore/${state.totalQuestions}")
                    
                    val analysisResult = geminiService.analyzeTestResults(
                        results = newResults,
                        score = newScore,
                        totalQuestions = state.totalQuestions,
                        totalTimeMs = System.currentTimeMillis() - testStartTime,
                        animalsToReview = animalsToReviewNames
                    )
                    
                    analysisResult.onSuccess { recommendations ->
                        android.util.Log.d("TestViewModel", "✅ Recommandations reçues de l'IA")
                        android.util.Log.d("TestViewModel", "📝 Élève: ${recommendations.eleve.take(100)}...")
                        android.util.Log.d("TestViewModel", "📝 Professeur: ${recommendations.professeur.take(100)}...")
                        _uiState.value = _uiState.value.copy(
                            aiRecommendations = recommendations,
                            isAnalyzing = false
                        )
                    }.onFailure { error ->
                        android.util.Log.e("TestViewModel", "❌ Erreur lors de l'analyse IA", error)
                        
                        // Déterminer le type d'erreur pour un message plus clair
                        val errorMessage = when {
                            error.message?.contains("quota", ignoreCase = true) == true -> {
                                "⚠️ Quota API dépassé\n\n" +
                                "Vous avez atteint la limite de 50 requêtes gratuites par jour.\n\n" +
                                "Réessayez demain ou utilisez une autre clé API.\n\n" +
                                "Les résultats du test sont toujours sauvegardés !"
                            }
                            error.message?.contains("parser", ignoreCase = true) == true -> {
                                "⚠️ Format de réponse inattendu\n\n" +
                                "L'IA a retourné un format différent.\n\n" +
                                "Les résultats du test sont toujours sauvegardés !"
                            }
                            else -> {
                                "Impossible d'obtenir les recommandations: ${error.message}\n\n" +
                                "Les résultats du test sont toujours sauvegardés !"
                            }
                        }
                        
                        _uiState.value = _uiState.value.copy(
                            isAnalyzing = false,
                            error = errorMessage
                        )
                    }
                }
                
                _uiState.value = _uiState.value.copy(
                    lastAnswer = spokenText,
                    isCorrect = isCorrect,
                    isVerifying = false,
                    responseTime = responseTime,
                    results = newResults,
                    score = newScore,
                    isCompleted = true
                )
                
                database.testResultDao().insert(testResult)
            }
        }
    }
    
    fun submitTextAnswer(text: String) {
        verifyAnswer(text, inputMethod = "texte")
    }
    
    fun nextAnimal() {
        val state = _uiState.value
        val newIndex = state.currentAnimalIndex + 1
        
        if (newIndex < state.shuffledAnimals.size) {
            _uiState.value = state.copy(
                lastAnswer = null,
                isCorrect = null,
                isVerifying = false,
                responseTime = 0,
                currentAnimalIndex = newIndex,
                currentAnimal = state.shuffledAnimals[newIndex],
                questionStartTime = System.currentTimeMillis()
            )
            // Lire automatiquement la question pour la nouvelle animal
            speakQuestion()
        } else {
            // Test terminé
            _uiState.value = state.copy(
                isCompleted = true
            )
        }
    }
    
    /**
     * Lit la question à voix haute
     */
    fun speakQuestion() {
        viewModelScope.launch {
            val question = "Peux-tu dire le nom de cet animal ?"
            ttsService.speak(question).collect { }
        }
    }
    
    fun getAnimalsToReview(): List<Animal> {
        val state = _uiState.value
        val incorrectResults = state.results.filter { !it.isCorrect }
        return incorrectResults.mapNotNull { result ->
            Animal.getAnimalById(result.animalId)
        }.distinct()
    }
    
    fun getBestAnimal(): Animal? {
        val state = _uiState.value
        val correctResults = state.results.filter { it.isCorrect }
        if (correctResults.isEmpty()) return null
        
        val animalScores = correctResults.groupBy { it.animalId }
            .mapValues { it.value.size }
        
        val bestAnimalId = animalScores.maxByOrNull { it.value }?.key
        return bestAnimalId?.let { Animal.getAnimalById(it) }
    }
}

