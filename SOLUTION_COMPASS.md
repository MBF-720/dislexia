# Solution : Voir les données dans MongoDB Compass

## ⚠️ Problème

Les données sont **stockées localement** sur l'appareil Android en format JSON. Elles ne sont **PAS automatiquement** dans MongoDB Compass. Il faut les **importer manuellement**.

## ✅ Solution Rapide

### Méthode 1 : Script automatique (Recommandé)

```powershell
# Utilisez le script fourni
.\import_session_rapide.ps1
```

Ce script :
1. Récupère automatiquement le fichier depuis l'appareil
2. L'importe dans MongoDB
3. Vous indique comment voir les données dans Compass

### Méthode 2 : Import manuel

#### Étape 1 : Récupérer le fichier

D'après vos logs, le fichier est :
```
/data/user/0/esprit.tn.handy/files/mongodb/sessions/51a4ef8f-f254-45e4-8996-69189c69fbc4.json
```

Récupérez-le :
```bash
adb shell run-as esprit.tn.handy cat files/mongodb/sessions/51a4ef8f-f254-45e4-8996-69189c69fbc4.json > session.json
```

#### Étape 2 : Importer dans MongoDB

```bash
# Créer la base de données
mongosh --eval "use zoo_dyslexie; db.createCollection('sessions');"

# Importer (le fichier doit être un array JSON)
# Si c'est un objet unique, convertissez-le en array
mongoimport --db zoo_dyslexie --collection sessions --file session.json --jsonArray
```

#### Étape 3 : Voir dans MongoDB Compass

1. Ouvrez **MongoDB Compass**
2. Connectez-vous à : `mongodb://127.0.0.1:27017`
3. **Actualisez** (bouton refresh en haut)
4. Vous devriez voir **zoo_dyslexie** > **sessions**
5. Cliquez sur **sessions** pour voir vos données

## 🔍 Vérification dans mongosh

```javascript
// Se connecter
mongosh

// Utiliser la base
use zoo_dyslexie

// Voir toutes les sessions
db.sessions.find().pretty()

// Voir votre session de test
db.sessions.findOne({ sessionId: "51a4ef8f-f254-45e4-8996-69189c69fbc4" })

// Voir les sessions par phase
db.sessions.find({ phase: "test" }).pretty()
```

## 📊 D'après vos logs

Votre session de test a été sauvegardée avec :
- **Session ID**: `51a4ef8f-f254-45e4-8996-69189c69fbc4`
- **Phase**: `test`
- **Score**: 3/6
- **6 animaux testés** (Zèbre, Éléphant, Oiseau, Singe, Lion, Girafe)

Pour voir cette session dans Compass :
1. Récupérez le fichier JSON (voir ci-dessus)
2. Importez-le dans MongoDB
3. Ouvrez Compass et actualisez

## ⚠️ Si la base de données n'apparaît toujours pas

1. **Créez un document de test** :
   ```javascript
   mongosh
   use zoo_dyslexie
   db.sessions.insertOne({ test: "test", phase: "test", date: new Date() })
   ```

2. **Actualisez MongoDB Compass** (bouton refresh)

3. **Vérifiez la connexion** : `mongodb://127.0.0.1:27017`

## 💡 Astuce

Pour importer automatiquement toutes les sessions :
```powershell
.\combine_and_import.ps1
```

