# Comment démarrer le service HTTP MongoDB

## ⚠️ Important

Pour que les données soient envoyées **directement** dans MongoDB Compass, vous devez **démarrer le service HTTP**.

## 🚀 Démarrage rapide

### Étape 1 : Installer Node.js (si pas déjà fait)

Téléchargez depuis : https://nodejs.org/

### Étape 2 : Installer les dépendances

Ouvrez un terminal dans le dossier du projet et exécutez :

```powershell
cd mongodb-service
npm install
```

### Étape 3 : Démarrer le service

```powershell
npm start
```

Vous devriez voir :
```
✅ Connecté à MongoDB: zoo_dyslexie
🚀 Serveur MongoDB HTTP démarré !
📡 Écoute sur: http://localhost:3000
```

## ✅ Vérification

Testez dans un navigateur ou avec curl :
```powershell
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

## 📱 Configuration Android

### Pour Émulateur (déjà configuré)
- URL : `http://10.0.2.2:3000` ✅

### Pour Appareil Physique
1. Trouvez votre IP : `ipconfig` (cherchez "IPv4 Address")
2. Modifiez `app/src/main/java/esprit/tn/handy/config/MongoDBConfig.kt` :
   ```kotlin
   const val SERVICE_URL = "http://192.168.1.100:3000" // Votre IP
   ```

## 🔄 Fallback automatique

Si le service HTTP n'est **pas démarré** :
- ✅ Les données sont **automatiquement sauvegardées localement**
- ✅ Vous pouvez les importer plus tard avec `.\import_session_rapide.ps1`
- ⚠️ Un message d'avertissement apparaît dans les logs

## 💡 Astuce

Pour démarrer automatiquement le service au démarrage de Windows :
1. Créez un fichier `start-mongodb-service.bat` :
   ```batch
   @echo off
   cd /d D:\handy\mongodb-service
   npm start
   ```
2. Ajoutez-le au démarrage Windows

