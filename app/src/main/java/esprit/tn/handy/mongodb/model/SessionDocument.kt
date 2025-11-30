package esprit.tn.handy.mongodb.model

import org.json.JSONArray
import org.json.JSONObject
import java.util.*

/**
 * Document de session MongoDB
 * Structure compatible avec MongoDB Compass
 */
data class SessionDocument(
    val sessionId: String = UUID.randomUUID().toString(),
    val date: String, // ISO 8601 format
    val phase: String, // "learn", "test", ou "train"
    val animalMetrics: List<AnimalMetrics>,
    val totalTimeMs: Long = 0,
    val score: Int = 0,
    val totalQuestions: Int = 0
) {
    fun toJson(): JSONObject {
        return JSONObject().apply {
            put("sessionId", sessionId)
            put("date", date)
            put("phase", phase)
            put("animalMetrics", JSONArray().apply {
                animalMetrics.forEach { metrics ->
                    put(metrics.toJson())
                }
            })
            put("totalTimeMs", totalTimeMs)
            put("score", score)
            put("totalQuestions", totalQuestions)
        }
    }
    
    companion object {
        fun fromJson(json: JSONObject): SessionDocument {
            val metricsArray = json.getJSONArray("animalMetrics")
            val metrics = mutableListOf<AnimalMetrics>()
            for (i in 0 until metricsArray.length()) {
                metrics.add(AnimalMetrics.fromJson(metricsArray.getJSONObject(i)))
            }
            
            return SessionDocument(
                sessionId = json.getString("sessionId"),
                date = json.getString("date"),
                phase = json.getString("phase"),
                animalMetrics = metrics,
                totalTimeMs = json.optLong("totalTimeMs", 0),
                score = json.optInt("score", 0),
                totalQuestions = json.optInt("totalQuestions", 0)
            )
        }
    }
}

