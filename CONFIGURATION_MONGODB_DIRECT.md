# Configuration MongoDB - Stockage Local + Import

## ⚠️ Important : Connexion Directe Non Disponible

Le driver MongoDB Java n'est **pas compatible avec Android** (problèmes avec Records Java et desugaring). 

**Solution actuelle** : Les données sont sauvegardées **localement en JSON** sur l'appareil Android, puis peuvent être **importées dans MongoDB Compass** via les scripts fournis.

## 📦 Comment ça fonctionne

1. **Stockage Local** : Les données sont sauvegardées en JSON dans l'appareil Android
2. **Export** : Utilisez les scripts PowerShell pour récupérer les fichiers JSON
3. **Import** : Importez les données dans MongoDB Compass

## 🔧 Import dans MongoDB Compass

### Méthode 1 : Script automatique (Recommandé)

```powershell
.\import_session_rapide.ps1
```

Ce script :
- Récupère automatiquement les fichiers depuis l'appareil
- Les importe dans MongoDB
- Vous indique comment voir les données dans Compass

### Méthode 2 : Script combiné

```powershell
.\combine_and_import.ps1
```

Ce script combine toutes les sessions et les importe en une seule fois.

## 📊 Vérification

### Dans les logs Android (Logcat)

Cherchez ces messages :

- ✅ **Sauvegarde réussie** :
  ```
  ✅ Session sauvegardée: [sessionId]
  📁 Fichier: /data/user/0/esprit.tn.handy/files/mongodb/sessions/[sessionId].json
  📊 Phase: [learn|test|train], Métriques: [nombre]
  ```

### Dans MongoDB Compass

1. Ouvrez MongoDB Compass
2. Connectez-vous à : `mongodb://127.0.0.1:27017`
3. Actualisez (bouton refresh)
4. Vous devriez voir la base **zoo_dyslexie** avec :
   - Collection **sessions** : toutes les sessions (learn, test, train)
   - Collection **animals** : statistiques par animal

### Vérification dans mongosh

```javascript
mongosh
use zoo_dyslexie

// Voir toutes les sessions
db.sessions.find().pretty()

// Compter les sessions par phase
db.sessions.aggregate([
  { $group: { _id: "$phase", count: { $sum: 1 } } }
])

// Voir les statistiques d'un animal
db.animals.findOne({ animal: "Éléphant" })
```

## 🚨 Dépannage

### Problème : "Impossible de se connecter à MongoDB"

**Solutions :**

1. **Vérifiez que MongoDB est démarré** :
   ```powershell
   # Vérifier si MongoDB écoute sur le port 27017
   netstat -an | findstr 27017
   ```

2. **Vérifiez l'URL dans MongoDBConfig.kt** :
   - Émulateur : `mongodb://10.0.2.2:27017`
   - Appareil physique : `mongodb://[VOTRE_IP]:27017`

3. **Vérifiez le firewall Windows** :
   - Autorisez les connexions entrantes sur le port 27017
   - Ou désactivez temporairement le firewall pour tester

4. **Testez la connexion depuis l'émulateur/appareil** :
   ```powershell
   # Depuis l'émulateur (via adb)
   adb shell ping 10.0.2.2
   
   # Depuis un appareil physique, utilisez une app de ping
   ```

### Problème : Les données n'apparaissent pas dans Compass

1. **Actualisez MongoDB Compass** (bouton refresh)
2. **Vérifiez les logs** pour confirmer que la connexion a réussi
3. **Vérifiez dans mongosh** :
   ```javascript
   use zoo_dyslexie
   db.sessions.countDocuments()
   ```

## 💡 Avantages de la connexion directe

- ✅ **Données visibles immédiatement** dans MongoDB Compass
- ✅ **Pas besoin d'exporter/importer** manuellement
- ✅ **Synchronisation automatique** des collections `sessions` et `animals`
- ✅ **Fallback automatique** sur stockage local si MongoDB n'est pas accessible

## 📝 Notes

- Les données sont **toujours sauvegardées localement** en backup, même si MongoDB fonctionne
- La connexion MongoDB est **asynchrone** et n'affecte pas les performances de l'application
- Si MongoDB n'est pas accessible, l'application continue de fonctionner normalement avec le stockage local

