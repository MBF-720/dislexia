package esprit.tn.handy.config

/**
 * Configuration pour l'envoi direct des données à MongoDB via service HTTP
 */
object MongoDBConfig {
    // URL du service HTTP MongoDB
    // Pour émulateur Android: http://10.0.2.2:3000
    // Pour appareil physique: http://[IP_DE_VOTRE_ORDINATEUR]:3000
    // Exemple: http://192.168.1.100:3000
    const val SERVICE_URL = "http://10.0.2.2:3000"
    
    // Endpoint pour sauvegarder une session
    const val SESSIONS_ENDPOINT = "/api/sessions"
    
    // Endpoint pour vérifier la santé du service
    const val HEALTH_ENDPOINT = "/api/health"
    
    // Timeout de connexion (en millisecondes)
    const val CONNECTION_TIMEOUT_MS = 10000
    const val READ_TIMEOUT_MS = 30000
}

