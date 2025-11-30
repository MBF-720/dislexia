package esprit.tn.handy.mongodb

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit
import esprit.tn.handy.config.MongoDBConfig
import esprit.tn.handy.mongodb.model.SessionDocument

/**
 * Service MongoDB pour envoyer les métriques directement à MongoDB via service HTTP
 * Fallback sur stockage local si le service HTTP n'est pas accessible
 */
class MongoDBService(private val context: Context) {
    
    private val client = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS) // Timeout réduit pour détecter rapidement si le service n'est pas disponible
        .readTimeout(10, TimeUnit.SECONDS)
        .writeTimeout(5, TimeUnit.SECONDS)
        .build()
    
    private val sessionsDir: File = File(context.filesDir, "mongodb/sessions")
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }
    
    init {
        sessionsDir.mkdirs()
    }
    
    /**
     * Sauvegarde une session directement dans MongoDB via le service HTTP
     * Fallback sur stockage local si le service n'est pas accessible
     */
    suspend fun saveSession(session: SessionDocument): Result<String> {
        return withContext(Dispatchers.IO) {
            // Essayer d'abord le service HTTP
            try {
                val json = session.toJson()
                val url = "${MongoDBConfig.SERVICE_URL}${MongoDBConfig.SESSIONS_ENDPOINT}"
                
                // Log du JSON envoyé pour débogage
                Log.d("MongoDBService", "📤 Envoi de la session à: $url")
                Log.d("MongoDBService", "📄 JSON envoyé:\n${json.toString(2)}")
                
                val requestBody = json.toString()
                    .toRequestBody("application/json".toMediaType())
                
                val request = Request.Builder()
                    .url(url)
                    .post(requestBody)
                    .addHeader("Content-Type", "application/json")
                    .build()
                
                Log.d("MongoDBService", "🔄 Exécution de la requête...")
                val response = client.newCall(request).execute()
                Log.d("MongoDBService", "📥 Réponse reçue: ${response.code}")
                
                if (response.isSuccessful) {
                    val responseBody = response.body?.string() ?: ""
                    val responseJson = JSONObject(responseBody)
                    
                    Log.d("MongoDBService", "✅ Session sauvegardée dans MongoDB: ${session.sessionId}")
                    Log.d("MongoDBService", "📊 Phase: ${session.phase}, Métriques: ${session.animalMetrics.size}")
                    Log.d("MongoDBService", "📡 Service: $url")
                    
                    return@withContext Result.success(session.sessionId)
                } else {
                    val errorBody = response.body?.string() ?: "Erreur inconnue"
                    Log.w("MongoDBService", "⚠️ Erreur HTTP ${response.code}: $errorBody")
                    Log.w("MongoDBService", "💡 Fallback sur stockage local")
                    // Continuer avec le fallback local
                }
            } catch (e: Exception) {
                Log.w("MongoDBService", "⚠️ Service HTTP non accessible: ${e.message}")
                Log.w("MongoDBService", "💡 Fallback sur stockage local")
                Log.w("MongoDBService", "📋 Pour activer l'envoi direct, démarrez le service: cd mongodb-service && npm start")
                // Continuer avec le fallback local
            }
            
            // Fallback: Sauvegarder localement
            try {
                val json = session.toJson()
                val fileName = "${session.sessionId}.json"
                val file = File(sessionsDir, fileName)
                
                FileWriter(file).use { writer ->
                    writer.write(json.toString(2))
                }
                
                Log.d("MongoDBService", "✅ Session sauvegardée localement: ${session.sessionId}")
                Log.d("MongoDBService", "📁 Fichier: ${file.absolutePath}")
                Log.d("MongoDBService", "📊 Phase: ${session.phase}, Métriques: ${session.animalMetrics.size}")
                Log.d("MongoDBService", "💡 Pour importer dans MongoDB: utilisez .\\import_session_rapide.ps1")
                
                Result.success(session.sessionId)
            } catch (e: Exception) {
                Log.e("MongoDBService", "❌ Erreur lors de la sauvegarde locale", e)
                Result.failure(e)
            }
        }
    }
    
    /**
     * Vérifie si le service HTTP MongoDB est accessible
     */
    suspend fun checkServiceHealth(): Result<Boolean> {
        return withContext(Dispatchers.IO) {
            try {
                val url = "${MongoDBConfig.SERVICE_URL}${MongoDBConfig.HEALTH_ENDPOINT}"
                val request = Request.Builder()
                    .url(url)
                    .get()
                    .build()
                
                val response = client.newCall(request).execute()
                
                if (response.isSuccessful) {
                    val responseBody = response.body?.string() ?: ""
                    val responseJson = JSONObject(responseBody)
                    val status = responseJson.optString("status") == "ok"
                    
                    if (status) {
                        Log.d("MongoDBService", "✅ Service MongoDB accessible")
                    } else {
                        Log.w("MongoDBService", "⚠️ Service MongoDB non disponible")
                    }
                    
                    Result.success(status)
                } else {
                    Log.w("MongoDBService", "⚠️ Service MongoDB non accessible (${response.code})")
                    Result.success(false)
                }
            } catch (e: Exception) {
                Log.w("MongoDBService", "⚠️ Impossible de contacter le service MongoDB", e)
                Result.success(false)
            }
        }
    }
    
    /**
     * Formate une date au format ISO 8601
     */
    fun formatDate(date: Date = Date()): String {
        return dateFormat.format(date)
    }
}
