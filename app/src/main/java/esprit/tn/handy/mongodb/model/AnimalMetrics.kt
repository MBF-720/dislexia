package esprit.tn.handy.mongodb.model

import org.json.JSONObject

/**
 * Métriques pour un animal dans une session
 */
data class AnimalMetrics(
    val animal: String,
    val responseTimeMs: Long,
    val correct: Boolean,
    val hesitations: Int = 0,
    val repetitions: Int = 0,
    val confusedWith: String? = null,
    val inputMethod: String = "oral" // "oral" ou "texte"
) {
    fun toJson(): JSONObject {
        return JSONObject().apply {
            put("animal", animal)
            put("responseTimeMs", responseTimeMs)
            put("correct", correct)
            put("hesitations", hesitations)
            put("repetitions", repetitions)
            if (confusedWith != null) {
                put("confusedWith", confusedWith)
            }
            put("inputMethod", inputMethod)
        }
    }
    
    companion object {
        fun fromJson(json: JSONObject): AnimalMetrics {
            return AnimalMetrics(
                animal = json.getString("animal"),
                responseTimeMs = json.getLong("responseTimeMs"),
                correct = json.getBoolean("correct"),
                hesitations = json.optInt("hesitations", 0),
                repetitions = json.optInt("repetitions", 0),
                confusedWith = json.optString("confusedWith", null),
                inputMethod = json.optString("inputMethod", "oral")
            )
        }
    }
}

