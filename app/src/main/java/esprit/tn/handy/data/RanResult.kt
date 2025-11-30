package esprit.tn.handy.data

/**
 * Résultat d'une question du quiz RAN
 */
data class RanResult(
    val questionId: Int,
    val category: RanCategory,
    val item: String,
    val correctAnswer: String,
    val studentAnswer: String,
    val responseTimeMs: Long,
    val isCorrect: Boolean,
    val hasHesitation: Boolean // true si responseTime > 3000ms
)

/**
 * Résultats complets du quiz RAN avec interprétation
 */
data class RanQuizResults(
    val results: List<RanResult>,
    val averageResponseTimeMs: Long,
    val totalErrors: Int,
    val totalHesitations: Int,
    val interpretation: String // Texte d'interprétation généré
) {
    companion object {
        fun fromResults(results: List<RanResult>): RanQuizResults {
            val averageTime = if (results.isNotEmpty()) {
                results.map { it.responseTimeMs }.average().toLong()
            } else 0L
            
            val errors = results.count { !it.isCorrect }
            val hesitations = results.count { it.hasHesitation }
            
            val interpretation = generateInterpretation(
                averageTime = averageTime,
                errors = errors,
                hesitations = hesitations,
                totalQuestions = results.size
            )
            
            return RanQuizResults(
                results = results,
                averageResponseTimeMs = averageTime,
                totalErrors = errors,
                totalHesitations = hesitations,
                interpretation = interpretation
            )
        }
        
        /**
         * Génère une interprétation basée sur les métriques
         */
        private fun generateInterpretation(
            averageTime: Long,
            errors: Int,
            hesitations: Int,
            totalQuestions: Int
        ): String {
            val errorRate = (errors * 100.0 / totalQuestions).toInt()
            val hesitationRate = (hesitations * 100.0 / totalQuestions).toInt()
            
            val timeSeconds = averageTime / 1000.0
            val isSlow = timeSeconds > 2.0 // Plus de 2 secondes en moyenne
            val isVerySlow = timeSeconds > 3.0 // Plus de 3 secondes en moyenne
            
            val riskFactors = mutableListOf<String>()
            
            // Analyse du temps de réponse
            when {
                isVerySlow -> riskFactors.add("temps de réponse très lent (${String.format("%.1f", timeSeconds)}s en moyenne)")
                isSlow -> riskFactors.add("temps de réponse lent (${String.format("%.1f", timeSeconds)}s en moyenne)")
            }
            
            // Analyse des erreurs
            when {
                errorRate >= 50 -> riskFactors.add("taux d'erreurs très élevé ($errorRate%)")
                errorRate >= 30 -> riskFactors.add("taux d'erreurs élevé ($errorRate%)")
                errorRate >= 20 -> riskFactors.add("taux d'erreurs modéré ($errorRate%)")
            }
            
            // Analyse des hésitations
            when {
                hesitationRate >= 50 -> riskFactors.add("nombreuses hésitations ($hesitationRate%)")
                hesitationRate >= 30 -> riskFactors.add("hésitations fréquentes ($hesitationRate%)")
            }
            
            // Génération de l'interprétation
            return when {
                riskFactors.isEmpty() -> {
                    "✅ L'élève montre des performances normales dans les tâches de nommage rapide. " +
                    "Les temps de réponse sont dans la moyenne (${String.format("%.1f", timeSeconds)}s), " +
                    "avec peu d'erreurs ($errors/$totalQuestions) et peu d'hésitations ($hesitations/$totalQuestions). " +
                    "Aucun signe de faiblesse RAN détecté."
                }
                riskFactors.size == 1 -> {
                    "⚠️ L'élève présente ${riskFactors[0]}. " +
                    "Cela peut indiquer une possible faiblesse dans le nommage rapide (RAN). " +
                    "Il est recommandé de poursuivre l'observation et de consulter un professionnel si les difficultés persistent."
                }
                else -> {
                    "⚠️ L'élève présente plusieurs indicateurs de difficultés RAN : " +
                    riskFactors.joinToString(", ") + ". " +
                    "Ces signes peuvent indiquer une possible faiblesse dans le nommage rapide automatisé, " +
                    "qui est souvent associée à la dyslexie. " +
                    "Il est fortement recommandé de consulter un professionnel (orthophoniste, psychologue) " +
                    "pour une évaluation approfondie."
                }
            }
        }
    }
}

