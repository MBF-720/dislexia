package esprit.tn.handy.service

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import java.io.IOException
import java.util.concurrent.TimeUnit
import esprit.tn.handy.config.GeminiConfig

/**
 * Service pour vérifier quels modèles sont disponibles avec votre clé API
 */
class GeminiModelChecker {
    
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()
    
    /**
     * Liste les modèles disponibles pour votre clé API
     */
    suspend fun listAvailableModels(): Result<List<String>> {
        return withContext(Dispatchers.IO) {
            try {
                val url = "${GeminiConfig.BASE_URL}v1/models?key=${GeminiConfig.API_KEY}"
                
                val request = Request.Builder()
                    .url(url)
                    .get()
                    .build()
                
                val response = client.newCall(request).execute()
                
                if (!response.isSuccessful) {
                    val errorBody = response.body?.string() ?: "Erreur inconnue"
                    return@withContext Result.failure(
                        IOException("Erreur ${response.code}: $errorBody")
                    )
                }
                
                val responseBody = response.body?.string() ?: ""
                val json = org.json.JSONObject(responseBody)
                val models = json.getJSONArray("models")
                
                val modelNames = mutableListOf<String>()
                for (i in 0 until models.length()) {
                    val model = models.getJSONObject(i)
                    val name = model.getString("name")
                    // Extraire juste le nom du modèle (ex: "models/gemini-1.5-pro" -> "gemini-1.5-pro")
                    val modelName = name.substringAfterLast("/")
                    modelNames.add(modelName)
                }
                
                Result.success(modelNames)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
    
    /**
     * Teste si un modèle spécifique fonctionne avec generateContent
     */
    suspend fun testModel(modelName: String): Result<Boolean> {
        return withContext(Dispatchers.IO) {
            try {
                val url = "${GeminiConfig.BASE_URL}v1/models/$modelName:generateContent?key=${GeminiConfig.API_KEY}"
                
                val requestBody = org.json.JSONObject().apply {
                    put("contents", org.json.JSONArray().apply {
                        put(org.json.JSONObject().apply {
                            put("parts", org.json.JSONArray().apply {
                                put(org.json.JSONObject().apply {
                                    put("text", "test")
                                })
                            })
                        })
                    })
                }
                
                val request = Request.Builder()
                    .url(url)
                    .post(
                        okhttp3.RequestBody.create(
                            "application/json".toMediaType(),
                            requestBody.toString()
                        )
                    )
                    .addHeader("Content-Type", "application/json")
                    .build()
                
                val response = client.newCall(request).execute()
                
                Result.success(response.isSuccessful)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
}

