# Import Rapide dans MongoDB Compass

## Méthode la plus simple : mongoimport

### Étape 1 : Récupérer le fichier d'export depuis l'appareil

```bash
# Activer USB debugging sur l'appareil Android
# Connecter l'appareil au PC

# Copier le fichier d'export (après avoir fait une session dans l'app)
adb shell run-as esprit.tn.handy cat files/mongodb/sessions/[UUID].json > session.json

# Ou copier tous les fichiers
adb shell run-as esprit.tn.handy tar -czf /sdcard/mongodb_backup.tar.gz files/mongodb/
adb pull /sdcard/mongodb_backup.tar.gz .
tar -xzf mongodb_backup.tar.gz
```

### Étape 2 : Créer un fichier JSON array avec toutes les sessions

Créez un fichier `all_sessions.json` avec ce format :

```json
[
  {
    "sessionId": "...",
    "date": "...",
    "phase": "test",
    "animalMetrics": [...]
  },
  {
    "sessionId": "...",
    "date": "...",
    "phase": "learn",
    "animalMetrics": [...]
  }
]
```

### Étape 3 : Importer dans MongoDB

```bash
# Créer la base de données et la collection
mongosh --eval "use zoo_dyslexie; db.createCollection('sessions');"

# Importer les sessions
mongoimport --db zoo_dyslexie --collection sessions --file all_sessions.json --jsonArray
```

### Étape 4 : Vérifier dans MongoDB Compass

1. Ouvrez MongoDB Compass
2. Connectez-vous à `mongodb://127.0.0.1:27017`
3. Vous devriez voir la base de données `zoo_dyslexie`
4. Cliquez dessus pour voir la collection `sessions`

## Méthode automatique : Script batch (Windows)

1. Récupérez le fichier `import_to_mongodb.bat` créé par l'app
2. Modifiez le chemin du fichier JSON si nécessaire
3. Double-cliquez sur `import_to_mongodb.bat`
4. Les données seront importées automatiquement

## Vérification dans mongosh

```javascript
// Se connecter
mongosh

// Utiliser la base de données
use zoo_dyslexie

// Voir toutes les sessions
db.sessions.find().pretty()

// Compter
db.sessions.countDocuments()

// Voir les sessions par phase
db.sessions.aggregate([
  { $group: { _id: "$phase", count: { $sum: 1 } } }
])
```

## Si la base de données n'apparaît pas dans Compass

1. **Vérifiez que MongoDB est en cours d'exécution** :
   ```bash
   # Windows
   net start MongoDB
   
   # Linux/Mac
   sudo systemctl status mongod
   ```

2. **Créez au moins un document** :
   ```javascript
   use zoo_dyslexie
   db.sessions.insertOne({ test: "test" })
   ```

3. **Actualisez MongoDB Compass** (bouton refresh)

4. **Vérifiez la connexion** : `mongodb://127.0.0.1:27017`

## Commandes utiles

```bash
# Voir toutes les bases de données
mongosh --eval "show dbs"

# Voir les collections
mongosh zoo_dyslexie --eval "show collections"

# Supprimer toutes les sessions (attention!)
mongosh zoo_dyslexie --eval "db.sessions.deleteMany({})"
```

