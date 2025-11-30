package esprit.tn.handy.service

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit
import esprit.tn.handy.config.GeminiConfig

class GeminiService {
    
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()
    
    /**
     * Génère du contenu texte avec Gemini
     */
    suspend fun generateContent(prompt: String): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                val url = GeminiConfig.getFullUrl()
                
                // Construire le body JSON
                val requestBody = JSONObject().apply {
                    put("contents", org.json.JSONArray().apply {
                        put(JSONObject().apply {
                            put("parts", org.json.JSONArray().apply {
                                put(JSONObject().apply {
                                    put("text", prompt)
                                })
                            })
                        })
                    })
                    put("generationConfig", JSONObject().apply {
                        put("temperature", 0.7)
                        put("maxOutputTokens", 4096) // Augmenté pour éviter les réponses tronquées
                    })
                }
                
                // Créer la requête
                val request = Request.Builder()
                    .url(url)
                    .post(
                        requestBody.toString()
                            .toRequestBody("application/json".toMediaType())
                    )
                    .addHeader("Content-Type", "application/json")
                    .build()
                
                // Exécuter la requête
                val response = client.newCall(request).execute()
                
                if (!response.isSuccessful) {
                    val errorBody = response.body?.string() ?: "Erreur inconnue"
                    val errorMessage = parseErrorMessage(response.code, errorBody)
                    return@withContext Result.failure(
                        IOException(errorMessage)
                    )
                }
                
                // Parser la réponse
                val responseBody = response.body?.string() ?: ""
                val responseJson = JSONObject(responseBody)
                
                val text = responseJson
                    .getJSONArray("candidates")
                    .getJSONObject(0)
                    .getJSONObject("content")
                    .getJSONArray("parts")
                    .getJSONObject(0)
                    .getString("text")
                
                Result.success(text)
                
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
    
    /**
     * Parse et formate les messages d'erreur de l'API Gemini pour un affichage plus clair
     */
    private fun parseErrorMessage(code: Int, errorBody: String): String {
        return try {
            val errorJson = JSONObject(errorBody)
            
            // Erreur 429 : Quota dépassé
            if (code == 429) {
                val message = errorJson.optString("message", "")
                if (message.contains("quota", ignoreCase = true)) {
                    return "⚠️ Quota API dépassé\n\n" +
                            "Vous avez atteint la limite de 50 requêtes gratuites par jour.\n\n" +
                            "Solutions :\n" +
                            "• Réessayez demain (quota quotidien)\n" +
                            "• Utilisez une autre clé API\n" +
                            "• Passez à un plan payant\n\n" +
                            "Les résultats du test sont toujours sauvegardés !"
                }
            }
            
            // Erreur 400 : Requête invalide
            if (code == 400) {
                return "❌ Requête invalide\n\nVérifiez votre configuration API."
            }
            
            // Erreur 401/403 : Clé API invalide
            if (code == 401 || code == 403) {
                return "🔑 Clé API invalide\n\nVérifiez votre clé API dans GeminiConfig.kt"
            }
            
            // Autres erreurs
            val message = errorJson.optString("message", errorBody)
            "Erreur ${code}: $message"
        } catch (e: Exception) {
            "Erreur ${code}: $errorBody"
        }
    }
    
    /**
     * Analyse la réponse vocale de l'enfant pour vérifier si elle correspond à l'animal attendu
     */
    suspend fun verifyAnimalName(spokenText: String, expectedAnimal: String): Result<Boolean> {
        return withContext(Dispatchers.IO) {
            try {
                val prompt = """
                    Tu es un assistant qui aide des enfants dyslexiques à apprendre les noms d'animaux.
                    L'enfant a dit: "$spokenText"
                    L'animal attendu est: "$expectedAnimal"
                    
                    Réponds UNIQUEMENT par "OUI" si l'enfant a correctement nommé l'animal (même avec des fautes de prononciation mineures).
                    Réponds UNIQUEMENT par "NON" sinon.
                    
                    Exemples:
                    - Attendu: "Éléphant", dit: "éléphant" -> OUI
                    - Attendu: "Éléphant", dit: "elefant" -> OUI
                    - Attendu: "Éléphant", dit: "lion" -> NON
                """.trimIndent()
                
                val result = generateContent(prompt)
                result.map { response ->
                    response.trim().uppercase().contains("OUI")
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
    
    /**
     * Analyse les résultats du test et génère des recommandations pédagogiques
     * Retourne un JSON avec deux sections : "eleve" (résumé motivant) et "professeur" (recommandations)
     */
    suspend fun analyzeTestResults(
        results: List<esprit.tn.handy.data.TestResult>,
        score: Int,
        totalQuestions: Int,
        totalTimeMs: Long,
        animalsToReview: List<String>
    ): Result<esprit.tn.handy.data.TestRecommendations> {
        return withContext(Dispatchers.IO) {
            try {
                // Construire un résumé des résultats par animal
                val animalsData = results.map { result ->
                    val animal = result.animalName
                    val responseTime = result.responseTime
                    val correct = result.isCorrect
                    val hesitations = if (result.hasHesitation) 1 else 0
                    val repetitions = 0 // Pas stocké dans TestResult pour l'instant
                    val confusedWith = result.confusedWith
                    
                    """
                    "$animal": {
                        "responseTimeMs": $responseTime,
                        "correct": $correct,
                        "hesitations": $hesitations,
                        "repetitions": $repetitions${if (confusedWith != null) ",\n                        \"confusedWith\": \"$confusedWith\"" else ""}
                    }"""
                }.joinToString(",\n")
                
                val correctCount = results.count { it.isCorrect }
                val incorrectCount = results.count { !it.isCorrect }
                val percentage = if (totalQuestions > 0) (correctCount * 100 / totalQuestions) else 0
                
                val prompt = """
                    Tu es un expert en pédagogie spécialisé dans l'aide aux enfants dyslexiques.
                    
                    Voici les résultats du test RAN de l'élève sur 6 animaux (Éléphant, Lion, Girafe, Zèbre, Singe, Oiseau) :
                    
                    {
                      $animalsData
                    }
                    
                    Score: $correctCount/$totalQuestions ($percentage%)
                    Temps total: ${totalTimeMs / 1000} secondes
                    Animaux à retravailler: ${if (animalsToReview.isNotEmpty()) animalsToReview.joinToString(", ") else "Aucun"}
                    
                    Analyse ces résultats et génère :
                    
                    1. Un résumé motivant pour l'élève en indiquant :
                       - Animaux bien réussis (liste les noms)
                       - Animaux à retravailler (liste les noms)
                       - Encouragements positifs et motivants
                       - Ton simple, adapté à un enfant dyslexique
                    
                    2. Des recommandations pour le professeur :
                       - Quels animaux nécessitent plus de travail et pourquoi
                       - Conseils pour l'entraînement futur
                       - Stratégies d'apprentissage adaptées à la dyslexie
                       - Points d'attention spécifiques
                    
                    Format de sortie : JSON UNIQUEMENT, sans texte avant ou après :
                    {
                      "eleve": "texte motivant pour l'élève (maximum 200 mots, court et simple)",
                      "professeur": "texte avec recommandations pour le professeur (maximum 300 mots, concis)"
                    }
                    
                    IMPORTANT : 
                    - Réponds UNIQUEMENT avec le JSON, sans explication, sans markdown, sans code blocks
                    - Limite tes réponses : 200 mots max pour "eleve", 300 mots max pour "professeur"
                    - Assure-toi que le JSON est complet et valide (se termine par })
                    - Sois concis et direct dans tes réponses
                """.trimIndent()
                
                val result = generateContent(prompt)
                result.map { responseText ->
                    android.util.Log.d("GeminiService", "📥 Réponse brute de Gemini:\n$responseText")
                    
                    // Essayer de parser le JSON depuis la réponse
                    val recommendations = esprit.tn.handy.data.TestRecommendations.fromJson(responseText)
                        ?: esprit.tn.handy.data.TestRecommendations.fromText(responseText)
                    
                    if (recommendations == null) {
                        android.util.Log.e("GeminiService", "❌ Impossible de parser le JSON. Réponse:\n$responseText")
                        throw Exception("Impossible de parser les recommandations depuis la réponse de l'IA. Format attendu: {\"eleve\": \"...\", \"professeur\": \"...\"}")
                    }
                    
                    android.util.Log.d("GeminiService", "✅ Recommandations parsées avec succès")
                    recommendations
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
}

