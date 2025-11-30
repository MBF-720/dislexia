package esprit.tn.handy.data

import android.util.Log
import org.json.JSONObject

/**
 * Recommandations structurées pour l'élève et le professeur après un test
 */
data class TestRecommendations(
    val eleve: String,      // Résumé motivant pour l'élève
    val professeur: String  // Recommandations pour le professeur
) {
    companion object {
        /**
         * Parse les recommandations depuis un JSON retourné par Gemini
         * Gère les JSON incomplets/tronqués en extrayant ce qui est disponible
         */
        fun fromJson(jsonString: String): TestRecommendations? {
            return try {
                // Nettoyer la chaîne (enlever markdown code blocks si présents)
                var cleaned = jsonString.trim()
                
                // Enlever les markdown code blocks
                if (cleaned.startsWith("```json")) {
                    cleaned = cleaned.removePrefix("```json").trim()
                }
                if (cleaned.startsWith("```")) {
                    cleaned = cleaned.removePrefix("```").trim()
                }
                if (cleaned.endsWith("```")) {
                    cleaned = cleaned.removeSuffix("```").trim()
                }
                
                // Enlever les espaces et retours à la ligne en début/fin
                cleaned = cleaned.trim()
                
                Log.d("TestRecommendations", "📄 Tentative de parsing JSON (${cleaned.length} caractères)")
                
                // Vérifier si le JSON est complet (se termine par })
                val isComplete = cleaned.endsWith("}")
                if (!isComplete) {
                    Log.w("TestRecommendations", "⚠️ JSON incomplet détecté, tentative de réparation...")
                    // Essayer de fermer le JSON manuellement
                    if (cleaned.contains("\"eleve\"") && cleaned.contains("\"professeur\"")) {
                        // Chercher la dernière valeur de professeur et fermer
                        val lastQuote = cleaned.lastIndexOf("\"")
                        if (lastQuote > 0) {
                            // Ajouter la fermeture manquante
                            cleaned = cleaned.substring(0, lastQuote + 1) + "\n}"
                            Log.d("TestRecommendations", "🔧 JSON réparé")
                        }
                    }
                }
                
                // Essayer de parser directement le JSON
                val json = JSONObject(cleaned)
                
                // Essayer différentes variantes de noms de champs
                var eleve = json.optString("eleve", "")
                    .takeIf { it.isNotEmpty() } 
                    ?: json.optString("élève", "")
                    ?: json.optString("student", "")
                    ?: ""
                
                var professeur = json.optString("professeur", "")
                    .takeIf { it.isNotEmpty() }
                    ?: json.optString("teacher", "")
                    ?: json.optString("prof", "")
                    ?: ""
                
                // Si le JSON était tronqué, essayer d'extraire manuellement depuis la chaîne brute
                if (eleve.isEmpty() || professeur.isEmpty()) {
                    Log.w("TestRecommendations", "⚠️ Extraction manuelle depuis JSON tronqué...")
                    
                    // Extraire eleve
                    if (eleve.isEmpty()) {
                        val eleveStart = cleaned.indexOf("\"eleve\"")
                        if (eleveStart >= 0) {
                            val valueStart = cleaned.indexOf("\"", eleveStart + 7) + 1
                            val valueEnd = cleaned.indexOf("\",", valueStart)
                            if (valueEnd > valueStart) {
                                eleve = cleaned.substring(valueStart, valueEnd)
                                    .replace("\\n", "\n")
                                    .replace("\\\"", "\"")
                            }
                        }
                    }
                    
                    // Extraire professeur (même si tronqué)
                    if (professeur.isEmpty()) {
                        val profStart = cleaned.indexOf("\"professeur\"")
                        if (profStart >= 0) {
                            val valueStart = cleaned.indexOf("\"", profStart + 12) + 1
                            // Prendre jusqu'à la fin de la chaîne si tronqué
                            val valueEnd = cleaned.indexOf("\",", valueStart).takeIf { it > valueStart }
                                ?: cleaned.length
                            if (valueEnd > valueStart) {
                                professeur = cleaned.substring(valueStart, valueEnd)
                                    .replace("\\n", "\n")
                                    .replace("\\\"", "\"")
                                // Ajouter "..." si tronqué
                                if (!cleaned.endsWith("}")) {
                                    professeur += "... (réponse tronquée)"
                                }
                            }
                        }
                    }
                }
                
                Log.d("TestRecommendations", "📊 Élève trouvé: ${eleve.isNotEmpty()} (${eleve.take(50)}...)")
                Log.d("TestRecommendations", "📊 Professeur trouvé: ${professeur.isNotEmpty()} (${professeur.take(50)}...)")
                
                if (eleve.isNotEmpty() && professeur.isNotEmpty()) {
                    Log.d("TestRecommendations", "✅ Parsing réussi")
                    TestRecommendations(eleve.trim(), professeur.trim())
                } else if (eleve.isNotEmpty()) {
                    // Au moins l'élève est disponible
                    Log.w("TestRecommendations", "⚠️ Seul le champ 'eleve' est disponible")
                    TestRecommendations(eleve.trim(), "Les recommandations détaillées n'ont pas pu être récupérées (réponse tronquée).")
                } else {
                    Log.e("TestRecommendations", "❌ Champs vides ou incomplets")
                    Log.e("TestRecommendations", "   Élève vide: ${eleve.isEmpty()}, Professeur vide: ${professeur.isEmpty()}")
                    Log.e("TestRecommendations", "   Clés disponibles: ${try { json.keys().asSequence().toList() } catch (e: Exception) { "N/A" }}")
                    null
                }
            } catch (e: Exception) {
                Log.e("TestRecommendations", "❌ Erreur lors du parsing JSON", e)
                Log.e("TestRecommendations", "   Message: ${e.message}")
                
                // Dernière tentative : extraction manuelle depuis la chaîne brute
                try {
                    val eleveMatch = Regex("\"eleve\"\\s*:\\s*\"([^\"]*(?:\\\\.[^\"]*)*)\"").find(jsonString)
                    val profMatch = Regex("\"professeur\"\\s*:\\s*\"([^\"]*(?:\\\\.[^\"]*)*)\"").find(jsonString)
                    
                    val eleve = eleveMatch?.groupValues?.get(1)?.replace("\\n", "\n")?.replace("\\\"", "\"") ?: ""
                    var professeur = profMatch?.groupValues?.get(1)?.replace("\\n", "\n")?.replace("\\\"", "\"") ?: ""
                    
                    if (eleve.isNotEmpty()) {
                        if (professeur.isEmpty()) {
                            // Essayer d'extraire jusqu'à la fin si tronqué
                            val profStart = jsonString.indexOf("\"professeur\"")
                            if (profStart >= 0) {
                                val valueStart = jsonString.indexOf("\"", profStart + 12) + 1
                                if (valueStart > 0 && valueStart < jsonString.length) {
                                    professeur = jsonString.substring(valueStart)
                                        .takeWhile { it != '"' || (it == '"' && jsonString.getOrNull(valueStart + professeur.length) == ',') }
                                        .replace("\\n", "\n")
                                        .replace("\\\"", "\"")
                                    if (professeur.isNotEmpty() && !jsonString.endsWith("}")) {
                                        professeur += "... (réponse tronquée)"
                                    }
                                }
                            }
                        }
                        
                        if (professeur.isEmpty()) {
                            professeur = "Les recommandations détaillées n'ont pas pu être récupérées (réponse tronquée)."
                        }
                        
                        Log.d("TestRecommendations", "✅ Extraction manuelle réussie")
                        return TestRecommendations(eleve.trim(), professeur.trim())
                    }
                } catch (ex: Exception) {
                    Log.e("TestRecommendations", "❌ Extraction manuelle échouée", ex)
                }
                
                null
            }
        }
        
        /**
         * Parse les recommandations depuis un texte qui pourrait contenir du JSON
         * Gère les JSON tronqués en extrayant ce qui est disponible
         */
        fun fromText(text: String): TestRecommendations? {
            return try {
                Log.d("TestRecommendations", "🔍 Recherche de JSON dans le texte...")
                
                // Chercher le début du JSON
                val jsonStart = text.indexOf("{")
                if (jsonStart < 0) {
                    Log.e("TestRecommendations", "❌ Aucun JSON trouvé dans le texte")
                    return null
                }
                
                // Chercher la fin du JSON (peut être tronqué)
                var jsonEnd = text.lastIndexOf("}") + 1
                if (jsonEnd <= jsonStart) {
                    // JSON tronqué, prendre jusqu'à la fin
                    jsonEnd = text.length
                    Log.w("TestRecommendations", "⚠️ JSON tronqué détecté, extraction jusqu'à la fin")
                }
                
                val jsonString = text.substring(jsonStart, jsonEnd)
                Log.d("TestRecommendations", "📄 JSON extrait (${jsonString.length} caractères)")
                
                // Essayer d'abord le parsing normal
                fromJson(jsonString) ?: run {
                    // Si échec, essayer l'extraction manuelle
                    Log.w("TestRecommendations", "⚠️ Parsing normal échoué, tentative d'extraction manuelle...")
                    
                    // Extraire eleve avec regex
                    val elevePattern = Regex("\"eleve\"\\s*:\\s*\"((?:[^\"\\\\]|\\\\.)*)\"")
                    val eleveMatch = elevePattern.find(jsonString)
                    val eleve = eleveMatch?.groupValues?.get(1)
                        ?.replace("\\n", "\n")
                        ?.replace("\\\"", "\"")
                        ?.replace("\\\\", "\\")
                        ?: ""
                    
                    // Extraire professeur (peut être tronqué)
                    val profPattern = Regex("\"professeur\"\\s*:\\s*\"((?:[^\"\\\\]|\\\\.)*)")
                    val profMatch = profPattern.find(jsonString)
                    var professeur = profMatch?.groupValues?.get(1)
                        ?.replace("\\n", "\n")
                        ?.replace("\\\"", "\"")
                        ?.replace("\\\\", "\\")
                        ?: ""
                    
                    // Si professeur est tronqué, essayer d'extraire jusqu'à la fin
                    if (professeur.isEmpty() && jsonString.contains("\"professeur\"")) {
                        val profStart = jsonString.indexOf("\"professeur\"")
                        val valueStart = jsonString.indexOf("\"", profStart + 12) + 1
                        if (valueStart > 0 && valueStart < jsonString.length) {
                            // Extraire jusqu'à la fin ou jusqu'à une guillemet non échappé
                            var i = valueStart
                            val sb = StringBuilder()
                            while (i < jsonString.length) {
                                val char = jsonString[i]
                                if (char == '"' && (i == valueStart || jsonString[i-1] != '\\')) {
                                    break
                                }
                                if (char == '\\' && i + 1 < jsonString.length) {
                                    when (jsonString[i + 1]) {
                                        'n' -> sb.append('\n')
                                        '"' -> sb.append('"')
                                        '\\' -> sb.append('\\')
                                        else -> sb.append(char).append(jsonString[i + 1])
                                    }
                                    i += 2
                                } else {
                                    sb.append(char)
                                    i++
                                }
                            }
                            professeur = sb.toString()
                            if (!jsonString.endsWith("}")) {
                                professeur += "... (réponse tronquée)"
                            }
                        }
                    }
                    
                    if (eleve.isNotEmpty()) {
                        if (professeur.isEmpty()) {
                            professeur = "Les recommandations détaillées n'ont pas pu être récupérées (réponse tronquée)."
                        }
                        Log.d("TestRecommendations", "✅ Extraction manuelle réussie")
                        return TestRecommendations(eleve.trim(), professeur.trim())
                    }
                    
                    null
                }
            } catch (e: Exception) {
                Log.e("TestRecommendations", "❌ Erreur lors de l'extraction du JSON", e)
                null
            }
        }
    }
}

