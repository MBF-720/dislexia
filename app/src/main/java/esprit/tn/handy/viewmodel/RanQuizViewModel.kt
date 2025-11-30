package esprit.tn.handy.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import esprit.tn.handy.data.RanQuestion
import esprit.tn.handy.data.RanQuestions
import esprit.tn.handy.data.RanResult
import esprit.tn.handy.data.RanQuizResults
import esprit.tn.handy.service.TTSService
import kotlinx.coroutines.launch

data class RanQuizUiState(
    val currentQuestionIndex: Int = 0,
    val currentQuestion: RanQuestion? = null,
    val questions: List<RanQuestion> = RanQuestions.ALL_QUESTIONS,
    val isStarted: Boolean = false,
    val startTime: Long = 0,
    val currentTimeMs: Long = 0,
    val studentAnswer: String = "",
    val results: List<RanResult> = emptyList(),
    val isCompleted: Boolean = false,
    val finalResults: RanQuizResults? = null
)

class RanQuizViewModel(application: Application) : AndroidViewModel(application) {
    
    private val _uiState = MutableStateFlow(RanQuizUiState())
    val uiState: StateFlow<RanQuizUiState> = _uiState.asStateFlow()
    private val ttsService = TTSService(application)
    
    init {
        startQuiz()
    }
    
    fun startQuiz() {
        _uiState.value = RanQuizUiState(
            currentQuestionIndex = 0,
            currentQuestion = RanQuestions.ALL_QUESTIONS.firstOrNull(),
            questions = RanQuestions.ALL_QUESTIONS
        )
        // Lire automatiquement la première question
        speakQuestion()
    }
    
    fun startTimer() {
        _uiState.value = _uiState.value.copy(
            isStarted = true,
            startTime = System.currentTimeMillis(),
            currentTimeMs = 0
        )
    }
    
    fun updateAnswer(answer: String) {
        _uiState.value = _uiState.value.copy(studentAnswer = answer)
    }
    
    fun submitAnswer() {
        val state = _uiState.value
        val question = state.currentQuestion ?: return
        
        if (!state.isStarted) {
            // Si le timer n'a pas été démarré, le démarrer maintenant
            startTimer()
            return
        }
        
        val endTime = System.currentTimeMillis()
        val responseTime = endTime - state.startTime
        val hasHesitation = responseTime > 3000
        
        // Vérifier si la réponse est correcte (tolérance aux variations)
        val isCorrect = checkAnswer(state.studentAnswer.trim(), question.correctAnswer)
        
        val result = RanResult(
            questionId = question.id,
            category = question.category,
            item = question.item,
            correctAnswer = question.correctAnswer,
            studentAnswer = state.studentAnswer.trim(),
            responseTimeMs = responseTime,
            isCorrect = isCorrect,
            hasHesitation = hasHesitation
        )
        
        val newResults = state.results + result
        val newIndex = state.currentQuestionIndex + 1
        
        if (newIndex < state.questions.size) {
            // Passer à la question suivante
            _uiState.value = RanQuizUiState(
                currentQuestionIndex = newIndex,
                currentQuestion = state.questions[newIndex],
                questions = state.questions,
                results = newResults,
                isStarted = false,
                startTime = 0,
                currentTimeMs = 0,
                studentAnswer = ""
            )
            // Lire automatiquement la nouvelle question
            speakQuestion()
        } else {
            // Quiz terminé
            val finalResults = RanQuizResults.fromResults(newResults)
            _uiState.value = state.copy(
                results = newResults,
                isCompleted = true,
                finalResults = finalResults
            )
        }
    }
    
    /**
     * Lit la question à voix haute
     */
    fun speakQuestion() {
        val state = _uiState.value
        val question = state.currentQuestion ?: return
        
        viewModelScope.launch {
            // Construire le texte à lire selon la catégorie
            val textToSpeak = when (question.category) {
                esprit.tn.handy.data.RanCategory.LETTER -> {
                    "Nommez la lettre : ${question.item}"
                }
                esprit.tn.handy.data.RanCategory.DIGIT -> {
                    "Nommez le chiffre : ${question.item}"
                }
                esprit.tn.handy.data.RanCategory.COLOR -> {
                    "Nommez la couleur : ${question.item}"
                }
                esprit.tn.handy.data.RanCategory.OBJECT -> {
                    "Nommez l'objet : ${question.item}"
                }
                esprit.tn.handy.data.RanCategory.ALTERNATION -> {
                    "Nommez l'alternance : ${question.item}"
                }
            }
            
            ttsService.speak(textToSpeak).collect { }
        }
    }
    
    /**
     * Vérifie si la réponse de l'élève correspond à la réponse correcte
     * Tolérant aux variations (majuscules/minuscules, accents, etc.)
     */
    private fun checkAnswer(studentAnswer: String, correctAnswer: String): Boolean {
        val student = studentAnswer.lowercase().trim()
        val correct = correctAnswer.lowercase().trim()
        
        // Comparaison exacte
        if (student == correct) return true
        
        // Pour les alternations, accepter différentes variantes
        if (correct.contains("quatre")) {
            if (student.contains("4") || student.contains("quatre")) return true
        }
        if (correct.contains("sept")) {
            if (student.contains("7") || student.contains("sept")) return true
        }
        
        // Pour les couleurs, accepter les variantes
        val colorVariants = mapOf(
            "rouge" to listOf("red", "rouge"),
            "bleu" to listOf("blue", "bleu", "bleue")
        )
        colorVariants.forEach { (key, variants) ->
            if (correct == key && variants.any { student.contains(it, ignoreCase = true) }) {
                return true
            }
        }
        
        // Pour les objets, accepter les variantes
        val objectVariants = mapOf(
            "chien" to listOf("dog", "chien", "chienne"),
            "voiture" to listOf("car", "voiture", "auto", "automobile")
        )
        objectVariants.forEach { (key, variants) ->
            if (correct == key && variants.any { student.contains(it, ignoreCase = true) }) {
                return true
            }
        }
        
        return false
    }
    
    fun resetQuiz() {
        startQuiz()
    }
}

