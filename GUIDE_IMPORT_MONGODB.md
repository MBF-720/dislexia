# Guide d'import dans MongoDB Compass - Solution Rapide

## ⚠️ Important

Les données sont **stockées localement** sur l'appareil Android en format JSON. Pour les voir dans MongoDB Compass, vous devez les **importer manuellement**.

## 🚀 Méthode la plus rapide

### Étape 1 : Récupérer les fichiers depuis l'appareil

```bash
# 1. Connectez l'appareil Android au PC
# 2. Activez USB debugging
# 3. Ouvrez un terminal PowerShell ou CMD

# Voir les fichiers disponibles
adb shell run-as esprit.tn.handy ls files/mongodb/sessions/

# Copier un fichier de session
adb shell run-as esprit.tn.handy cat files/mongodb/sessions/[UUID].json > session.json
```

### Étape 2 : Importer dans MongoDB

**Option A : Script PowerShell (Windows)**
```powershell
# Utilisez le script fourni
.\import_to_mongodb.ps1
```

**Option B : Commandes manuelles**
```bash
# Créer la base de données
mongosh --eval "use zoo_dyslexie; db.createCollection('sessions');"

# Importer le fichier
mongoimport --db zoo_dyslexie --collection sessions --file session.json --jsonArray
```

### Étape 3 : Vérifier dans MongoDB Compass

1. Ouvrez **MongoDB Compass**
2. Connectez-vous à : `mongodb://127.0.0.1:27017`
3. **Actualisez** (bouton refresh)
4. Vous devriez voir la base de données **zoo_dyslexie**
5. Cliquez dessus pour voir la collection **sessions**

## 📋 Si la base de données n'apparaît pas

### Solution 1 : Créer un document de test

```javascript
// Dans mongosh
use zoo_dyslexie
db.sessions.insertOne({ test: "test", date: new Date() })
```

Puis **actualisez MongoDB Compass**.

### Solution 2 : Vérifier que MongoDB est démarré

```bash
# Windows
net start MongoDB

# Vérifier
mongosh --eval "db.version()"
```

### Solution 3 : Vérifier la connexion

Dans MongoDB Compass, la connexion doit être :
- **Host**: `127.0.0.1` ou `localhost`
- **Port**: `27017`
- **Authentication**: Aucune (par défaut)

## 🔍 Vérification dans mongosh

```javascript
// Se connecter
mongosh

// Utiliser la base
use zoo_dyslexie

// Voir toutes les sessions
db.sessions.find().pretty()

// Compter
db.sessions.countDocuments()

// Voir les sessions par phase
db.sessions.aggregate([
  { $group: { _id: "$phase", count: { $sum: 1 } } }
])

// Dernière session
db.sessions.find().sort({ date: -1 }).limit(1).pretty()
```

## 📁 Emplacement des fichiers sur l'appareil

Les fichiers JSON sont stockés dans :
```
/data/data/esprit.tn.handy/files/mongodb/sessions/
```

Pour y accéder, utilisez `adb` avec `run-as` (nécessite USB debugging activé).

## 💡 Astuce : Import automatique de tous les fichiers

Créez un script pour importer tous les fichiers d'un coup :

```bash
# Récupérer tous les fichiers
adb shell run-as esprit.tn.handy tar -czf /sdcard/mongodb_backup.tar.gz files/mongodb/
adb pull /sdcard/mongodb_backup.tar.gz .
tar -xzf mongodb_backup.tar.gz

# Créer un fichier JSON array avec tous les fichiers
# (nécessite un script pour combiner les JSON)
```

## ✅ Checklist de vérification

- [ ] MongoDB est démarré (`mongosh` fonctionne)
- [ ] Les fichiers JSON sont récupérés depuis l'appareil
- [ ] La base de données `zoo_dyslexie` est créée
- [ ] Les collections `sessions` et `animals` existent
- [ ] Au moins un document est importé
- [ ] MongoDB Compass est actualisé
- [ ] La connexion est correcte (`mongodb://127.0.0.1:27017`)

Si tout est fait et que vous ne voyez toujours pas la base de données, vérifiez les logs MongoDB pour des erreurs.

