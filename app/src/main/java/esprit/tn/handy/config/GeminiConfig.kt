package esprit.tn.handy.config

object GeminiConfig {
    // ⚠️ REMPLACEZ PAR VOTRE CLÉ API GEMINI
    // Obtenez votre clé sur: https://aistudio.google.com/app/apikey
    const val API_KEY = "AIzaSyBr0weABCTBdyPXER8pnAPWPANfVTQZn5Q"
    
    const val BASE_URL = "https://generativelanguage.googleapis.com/"
    
    // Modèle utilisé
    const val MODEL = "gemini-2.5-pro"
    
    fun getFullUrl(model: String = MODEL): String {
        return "${BASE_URL}v1/models/$model:generateContent?key=$API_KEY"
    }
}

