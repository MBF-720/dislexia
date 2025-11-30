# Guide d'import dans MongoDB Compass

## Méthode 1 : Import manuel via MongoDB Compass

### Étape 1 : Récupérer les fichiers depuis l'appareil Android

```bash
# Activer USB debugging sur l'appareil
# Connecter l'appareil au PC

# Voir les fichiers disponibles
adb shell run-as esprit.tn.handy ls -la files/mongodb/sessions/

# Copier un fichier de session
adb shell run-as esprit.tn.handy cat files/mongodb/sessions/[UUID].json > session.json

# Ou copier tous les fichiers
adb shell run-as esprit.tn.handy tar -czf /sdcard/mongodb_backup.tar.gz files/mongodb/
adb pull /sdcard/mongodb_backup.tar.gz .
tar -xzf mongodb_backup.tar.gz
```

### Étape 2 : Importer dans MongoDB Compass

1. **Ouvrir MongoDB Compass**
2. **Se connecter** à `mongodb://127.0.0.1:27017`
3. **Créer une base de données** : `zoo_dyslexie`
4. **Créer les collections** :
   - `sessions`
   - `animals`
5. **Importer les fichiers** :
   - Cliquez sur la collection `sessions`
   - Cliquez sur "Add Data" > "Import File"
   - Sélectionnez un fichier JSON de session
   - Répétez pour chaque fichier

## Méthode 2 : Import via MongoDB Shell (mongosh)

### Étape 1 : Préparer les fichiers

Copiez les fichiers JSON depuis l'appareil (voir Méthode 1, Étape 1)

### Étape 2 : Importer avec mongosh

```bash
# Se connecter à MongoDB
mongosh

# Dans mongosh:
use zoo_dyslexie

# Importer un fichier
db.sessions.insertOne(JSON.parse(cat("session.json")))

# Ou importer plusieurs fichiers (script)
load("import_to_mongodb.js")
```

### Étape 3 : Vérifier l'import

```javascript
// Voir toutes les sessions
db.sessions.find().pretty()

// Compter
db.sessions.countDocuments()

// Voir une session spécifique
db.sessions.findOne({ phase: "test" })
```

## Méthode 3 : Import automatique avec script

Créez un script `import_all.sh` :

```bash
#!/bin/bash

# Récupérer les fichiers depuis l'appareil
adb shell run-as esprit.tn.handy tar -czf /sdcard/mongodb_backup.tar.gz files/mongodb/
adb pull /sdcard/mongodb_backup.tar.gz .
tar -xzf mongodb_backup.tar.gz

# Importer dans MongoDB
for file in mongodb/sessions/*.json; do
    mongosh zoo_dyslexie --eval "db.sessions.insertOne(JSON.parse(cat('$file')))"
done

echo "✅ Import terminé!"
```

## Structure attendue dans MongoDB Compass

### Collection `sessions`

```json
{
  "_id": ObjectId("..."),
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

### Collection `animals`

```json
{
  "_id": ObjectId("..."),
  "animal": "Éléphant",
  "totalSessions": 10,
  "totalCorrect": 8,
  "totalIncorrect": 2,
  "averageResponseTime": 2300,
  "totalHesitations": 3,
  "totalRepetitions": 5,
  "lastUpdated": "2025-01-23T15:30:00Z",
  "history": [...]
}
```

## Vérification rapide

Dans MongoDB Compass ou mongosh :

```javascript
// Vérifier que les données sont bien importées
use zoo_dyslexie

// Nombre de sessions
db.sessions.countDocuments()

// Dernière session
db.sessions.find().sort({ date: -1 }).limit(1).pretty()

// Sessions par phase
db.sessions.aggregate([
  { $group: { _id: "$phase", count: { $sum: 1 } } }
])

// Animal le plus difficile
db.animals.find().sort({ totalIncorrect: -1 }).limit(1).pretty()
```

## Emplacement des fichiers sur l'appareil

Les fichiers sont stockés dans :
- **Sessions** : `/data/data/esprit.tn.handy/files/mongodb/sessions/`
- **Animals** : `/data/data/esprit.tn.handy/files/mongodb/animals/`

Pour y accéder, vous devez utiliser `adb` avec les droits root ou via `run-as`.

