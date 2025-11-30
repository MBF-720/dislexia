# Vérification du stockage MongoDB

## Comment vérifier que les données sont stockées

### Méthode 1 : Via les logs Android

1. Ouvrez **Logcat** dans Android Studio
2. Filtrez par tag : `MongoDBService`
3. Vous verrez les logs suivants lors de chaque sauvegarde :
   ```
   ✅ Session sauvegardée: [sessionId]
   📁 Fichier: /data/data/esprit.tn.handy/files/mongodb/sessions/[sessionId].json
   📊 Phase: learn/test/train, Métriques: X
   📄 Contenu JSON: {...}
   ```

### Méthode 2 : Via ADB (Android Debug Bridge)

1. Connectez votre appareil Android à votre PC
2. Activez le **mode développeur** et **USB debugging**
3. Ouvrez un terminal et exécutez :

```bash
# Voir les fichiers de sessions
adb shell run-as esprit.tn.handy ls -la files/mongodb/sessions/

# Copier un fichier de session
adb shell run-as esprit.tn.handy cat files/mongodb/sessions/[sessionId].json > session.json

# Voir le contenu d'un fichier
adb shell run-as esprit.tn.handy cat files/mongodb/sessions/[sessionId].json
```

### Méthode 3 : Export et import dans MongoDB Compass

1. **Exporter les données depuis l'app** :
   - Les données sont automatiquement sauvegardées dans :
     - `filesDir/mongodb/sessions/` (fichiers JSON individuels)
     - `filesDir/mongodb/animals/` (statistiques par animal)

2. **Récupérer les fichiers** :
   ```bash
   # Copier tous les fichiers de sessions
   adb shell run-as esprit.tn.handy tar -czf /sdcard/mongodb_backup.tar.gz files/mongodb/
   adb pull /sdcard/mongodb_backup.tar.gz .
   tar -xzf mongodb_backup.tar.gz
   ```

3. **Importer dans MongoDB Compass** :
   - Ouvrez MongoDB Compass
   - Connectez-vous à `mongodb://127.0.0.1:27017`
   - Créez une base de données `zoo_dyslexie`
   - Créez les collections `sessions` et `animals`
   - Importez les fichiers JSON

### Méthode 4 : Vérification directe dans MongoDB

Une fois les données importées dans MongoDB Compass, vous pouvez vérifier :

```javascript
// Se connecter à la base de données
use zoo_dyslexie

// Voir toutes les sessions
db.sessions.find().pretty()

// Compter les sessions
db.sessions.countDocuments()

// Voir les sessions d'apprentissage
db.sessions.find({ phase: "learn" }).pretty()

// Voir les statistiques d'un animal
db.animals.findOne({ animal: "Éléphant" })

// Voir toutes les sessions avec erreurs
db.sessions.find({ "animalMetrics.correct": false }).pretty()
```

## Structure des fichiers locaux

Les données sont stockées dans :
```
/data/data/esprit.tn.handy/files/mongodb/
├── sessions/
│   ├── [uuid1].json
│   ├── [uuid2].json
│   └── ...
└── animals/
    ├── elephant.json
    ├── lion.json
    ├── girafe.json
    └── ...
```

## Format des fichiers JSON

Chaque fichier de session est un document JSON valide qui peut être importé directement dans MongoDB :

```json
{
  "sessionId": "550e8400-e29b-41d4-a716-446655440000",
  "date": "2025-01-23T15:30:00Z",
  "phase": "test",
  "animalMetrics": [
    {
      "animal": "Éléphant",
      "responseTimeMs": 2500,
      "correct": true,
      "hesitations": 1,
      "repetitions": 0,
      "inputMethod": "oral"
    }
  ],
  "totalTimeMs": 45000,
  "score": 5,
  "totalQuestions": 6
}
```

## Commandes MongoDB Shell pour import

```javascript
// Dans mongosh
use zoo_dyslexie

// Importer un fichier de session
// (depuis le répertoire où se trouve le fichier JSON)
load("session.json")
db.sessions.insertOne(JSON.parse(cat("session.json")))

// Ou utiliser mongoimport depuis le terminal
mongoimport --db zoo_dyslexie --collection sessions --file session.json --jsonArray
```

## Vérification rapide

Pour vérifier rapidement que les données sont sauvegardées :

1. **Lancez l'application**
2. **Faites une session** (Learn, Test ou Train)
3. **Vérifiez les logs** dans Logcat avec le filtre `MongoDBService`
4. **Vous devriez voir** : `✅ Session sauvegardée: [id]`

Si vous ne voyez pas ces logs, vérifiez que :
- Les permissions sont accordées
- L'application a les droits d'écriture
- Le stockage n'est pas plein

