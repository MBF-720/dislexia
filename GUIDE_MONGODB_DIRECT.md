# Guide : Enregistrement Direct dans MongoDB Compass

## ✅ Solution : Service HTTP + Application Android

Les données sont maintenant envoyées **directement** de l'application Android vers MongoDB Compass via un service HTTP.

## 🚀 Démarrage Rapide

### Étape 1 : Démarrer le Service HTTP

1. **Ouvrir un terminal** dans le dossier `mongodb-service`

2. **Installer les dépendances** (première fois seulement) :
   ```bash
   cd mongodb-service
   npm install
   ```

3. **Démarrer le service** :
   ```bash
   npm start
   ```

   Vous devriez voir :
   ```
   ✅ Connecté à MongoDB: zoo_dyslexie
   🚀 Serveur MongoDB HTTP démarré !
   📡 Écoute sur: http://localhost:3000
   ```

### Étape 2 : Configurer l'Application Android

#### Pour Émulateur Android (déjà configuré)

L'application est déjà configurée pour l'émulateur :
- URL : `http://10.0.2.2:3000`

#### Pour Appareil Physique

1. **Trouver l'IP de votre ordinateur** :
   ```powershell
   ipconfig
   # Cherchez "IPv4 Address" (ex: 192.168.1.100)
   ```

2. **Modifier** `app/src/main/java/esprit/tn/handy/config/MongoDBConfig.kt` :
   ```kotlin
   const val SERVICE_URL = "http://192.168.1.100:3000" // Votre IP
   ```

3. **Recompiler l'application**

### Étape 3 : Vérifier la Connexion

Dans l'application Android, les données seront automatiquement envoyées au service HTTP qui les insère dans MongoDB.

Vérifiez dans les logs Android (Logcat) :
- ✅ `Session sauvegardée dans MongoDB: [sessionId]`
- ✅ `Service: http://10.0.2.2:3000/api/sessions`

### Étape 4 : Voir dans MongoDB Compass

1. Ouvrez **MongoDB Compass**
2. Connectez-vous à : `mongodb://127.0.0.1:27017`
3. **Actualisez** (bouton refresh 🔄)
4. Vous devriez voir **zoo_dyslexie** > **sessions**

Les données apparaissent **immédiatement** après chaque session !

## 📊 Vérification

### Tester le Service HTTP

Dans un navigateur ou avec curl :
```bash
curl http://localhost:3000/api/health
```

Réponse attendue :
```json
{
  "status": "ok",
  "mongodb": "connected",
  "database": "zoo_dyslexie"
}
```

### Vérifier dans MongoDB

```javascript
mongosh
use zoo_dyslexie
db.sessions.find().pretty()
db.sessions.countDocuments()
```

## 🔧 Dépannage

### Le service ne démarre pas

1. **Vérifiez que MongoDB est démarré** :
   ```bash
   mongosh --eval "db.version()"
   ```

2. **Vérifiez que le port 3000 est libre** :
   ```powershell
   netstat -an | findstr 3000
   ```

3. **Changez le port** dans `mongodb-service/server.js` :
   ```javascript
   const PORT = 3001; // Changez le port
   ```
   Et mettez à jour `MongoDBConfig.kt` dans l'app Android.

### L'application ne peut pas se connecter

1. **Pour émulateur** : Vérifiez que `SERVICE_URL = "http://10.0.2.2:3000"`

2. **Pour appareil physique** :
   - Vérifiez que l'IP est correcte
   - Vérifiez que l'ordinateur et l'appareil sont sur le même réseau WiFi
   - Vérifiez le firewall Windows (autoriser le port 3000)

3. **Vérifiez les logs Android** :
   - Cherchez les erreurs de connexion
   - Vérifiez l'URL du service

### Les données n'apparaissent pas dans Compass

1. **Vérifiez que le service HTTP fonctionne** :
   ```bash
   curl http://localhost:3000/api/health
   ```

2. **Vérifiez les logs du service** :
   - Vous devriez voir `✅ Session sauvegardée: [sessionId]`

3. **Actualisez MongoDB Compass** (bouton refresh)

## 💡 Avantages

- ✅ **Pas de stockage local** : Les données vont directement dans MongoDB
- ✅ **Temps réel** : Les données apparaissent immédiatement dans Compass
- ✅ **Pas de scripts d'import** : Tout est automatique
- ✅ **Simple** : Juste démarrer le service HTTP

## 📝 Notes

- Le service HTTP doit être **démarré** avant d'utiliser l'application
- Si le service n'est pas accessible, l'application affichera une erreur dans les logs
- Les données sont **perdues** si le service n'est pas accessible (pas de fallback local)

