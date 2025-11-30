# Configuration MongoDB pour l'application Zoo

## Structure des données

L'application stocke toutes les métriques dans MongoDB (format JSON local) avec la structure suivante :

### Collection `sessions`

Chaque session crée un document unique avec cette structure :

```json
{
  "sessionId": "uuid-unique",
  "date": "2025-01-23T15:30:00Z",
  "phase": "learn|test|train",
  "animalMetrics": [
    {
      "animal": "Éléphant",
      "responseTimeMs": 2500,
      "correct": true,
      "hesitations": 1,
      "repetitions": 1,
      "confusedWith": null,
      "inputMethod": "oral"
    }
  ],
  "totalTimeMs": 45000,
  "score": 5,
  "totalQuestions": 6
}
```

### Collection `animals`

Chaque animal a un document avec ses statistiques cumulées :

```json
{
  "animal": "Éléphant",
  "totalSessions": 10,
  "totalCorrect": 8,
  "totalIncorrect": 2,
  "averageResponseTime": 2300,
  "totalHesitations": 3,
  "totalRepetitions": 5,
  "lastUpdated": "2025-01-23T15:30:00Z",
  "history": [
    {
      "animal": "Éléphant",
      "responseTimeMs": 2500,
      "correct": true,
      "hesitations": 1,
      "repetitions": 1,
      "inputMethod": "oral"
    }
  ]
}
```

## Emplacement des données

Les données sont stockées localement dans :
- **Sessions** : `app/filesDir/mongodb/sessions/`
- **Animals** : `app/filesDir/mongodb/animals/`

Chaque fichier est au format JSON et peut être importé directement dans MongoDB Compass.

## Import dans MongoDB Compass

### Méthode 1 : Export automatique

L'application peut exporter toutes les données au format JSON :

```kotlin
val mongoDBService = MongoDBService(context)
val exportPath = mongoDBService.exportToJson()
// Le fichier sera dans: /storage/emulated/0/Android/data/esprit.tn.handy/files/mongodb_export.json
```

### Méthode 2 : Import manuel

1. Connectez votre appareil Android à votre ordinateur
2. Naviguez vers : `Android/data/esprit.tn.handy/files/mongodb/`
3. Copiez les fichiers JSON
4. Dans MongoDB Compass :
   - Créez une base de données `zoo_dyslexie`
   - Créez les collections `sessions` et `animals`
   - Importez les fichiers JSON

### Méthode 3 : Utiliser mongoimport

```bash
# Pour les sessions
mongoimport --db zoo_dyslexie --collection sessions --file sessions/*.json --jsonArray

# Pour les animaux
mongoimport --db zoo_dyslexie --collection animals --file animals/*.json
```

## Métriques collectées

### Par session
- **sessionId** : Identifiant unique de la session
- **date** : Date et heure de la session (ISO 8601)
- **phase** : Type de session (learn, test, train)
- **totalTimeMs** : Temps total de la session en millisecondes
- **score** : Score obtenu
- **totalQuestions** : Nombre total de questions

### Par animal dans une session
- **animal** : Nom de l'animal
- **responseTimeMs** : Temps de réponse en millisecondes
- **correct** : Réponse correcte (true/false)
- **hesitations** : Nombre d'hésitations (>2 secondes)
- **repetitions** : Nombre de répétitions nécessaires
- **confusedWith** : Nom de l'animal confondu (si incorrect)
- **inputMethod** : Méthode d'entrée ("oral" ou "texte")

## Requêtes MongoDB utiles

### Toutes les sessions d'apprentissage
```javascript
db.sessions.find({ phase: "learn" })
```

### Sessions avec erreurs
```javascript
db.sessions.find({ 
  "animalMetrics.correct": false 
})
```

### Animal le plus difficile
```javascript
db.animals.find().sort({ totalIncorrect: -1 }).limit(1)
```

### Progression d'un enfant
```javascript
db.sessions.find().sort({ date: 1 })
```

## Notes

- Les données sont stockées localement sur l'appareil
- Chaque session crée un nouveau document
- Les statistiques par animal sont mises à jour automatiquement
- Le format JSON est compatible avec MongoDB Compass
- Les données peuvent être exportées pour analyse

