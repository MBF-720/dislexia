# Diagnostic : Collections MongoDB vides

## 🔍 Problème

Les collections `sessions` et `animals` sont créées dans MongoDB Compass mais sont vides.

## ✅ Vérifications à faire

### 1. Vérifier que le service HTTP est démarré

Dans un terminal, allez dans `mongodb-service` et vérifiez :

```powershell
cd mongodb-service
npm start
```

Vous devriez voir :
```
✅ Connecté à MongoDB: zoo_dyslexie
🚀 Serveur MongoDB HTTP démarré !
📡 Écoute sur: http://localhost:3000
```

### 2. Tester le service HTTP

Dans un autre terminal, testez :

```powershell
curl http://localhost:3000/api/health
```

Réponse attendue :
```json
{
  "status": "ok",
  "mongodb": "connected",
  "database": "zoo_dyslexie",
  "sessionsCount": 0,
  "animalsCount": 0
}
```

### 3. Vérifier les logs du service

Quand vous faites un test dans l'application, regardez les logs du service HTTP. Vous devriez voir :

```
📥 Requête reçue:
   Session ID: [uuid]
   Phase: test
   Métriques: 6
   Score: 2
   Total Questions: 6
💾 Insertion dans MongoDB...
   ✅ Inserté avec ID: [ObjectId]
📊 Mise à jour des statistiques des animaux...
📊 Animaux à traiter: Oiseau, Lion, Singe, ...
   ✅ Animal mis à jour: Oiseau
```

### 4. Vérifier les logs Android (Logcat)

Cherchez dans Logcat :
- `📤 Envoi de la session à: http://10.0.2.2:3000/api/sessions`
- `📄 JSON envoyé:`
- `📥 Réponse reçue: 200` (ou autre code)

### 5. Tester avec un document de test

Créez un fichier `test_session.json` :

```json
{
  "sessionId": "test-123",
  "date": "2025-11-23T06:00:00Z",
  "phase": "test",
  "animalMetrics": [
    {
      "animal": "Lion",
      "responseTimeMs": 5000,
      "correct": true,
      "hesitations": 1,
      "repetitions": 0,
      "inputMethod": "oral"
    }
  ],
  "totalTimeMs": 5000,
  "score": 1,
  "totalQuestions": 1
}
```

Puis testez avec curl :

```powershell
curl -X POST http://localhost:3000/api/sessions -H "Content-Type: application/json" -d @test_session.json
```

Vous devriez voir dans les logs du service :
```
📥 Requête reçue:
   Session ID: test-123
   ...
✅ Session sauvegardée: test-123 (test)
```

### 6. Vérifier dans MongoDB Compass

1. Ouvrez MongoDB Compass
2. Connectez-vous à : `mongodb://127.0.0.1:27017`
3. Ouvrez la base `zoo_dyslexie`
4. Cliquez sur `sessions`
5. **Actualisez** (bouton refresh 🔄)
6. Vous devriez voir les documents

### 7. Vérifier les statistiques

Dans le terminal du service, testez :

```powershell
curl http://localhost:3000/api/stats
```

Cela affichera le nombre de sessions et d'animaux.

## 🐛 Problèmes courants

### Le service ne reçoit pas les requêtes

**Symptôme** : Aucun log "📥 Requête reçue" dans le service

**Solutions** :
1. Vérifiez que le service est bien démarré
2. Vérifiez l'URL dans `MongoDBConfig.kt` :
   - Émulateur : `http://10.0.2.2:3000`
   - Appareil physique : `http://[VOTRE_IP]:3000`
3. Vérifiez le firewall Windows

### Les requêtes arrivent mais les données ne sont pas insérées

**Symptôme** : Logs "📥 Requête reçue" mais pas de "✅ Inserté"

**Solutions** :
1. Vérifiez les logs d'erreur dans le service
2. Vérifiez que MongoDB est bien démarré
3. Vérifiez la connexion MongoDB dans le service

### Les données sont insérées mais n'apparaissent pas dans Compass

**Symptôme** : Logs "✅ Inserté" mais Compass vide

**Solutions** :
1. **Actualisez MongoDB Compass** (bouton refresh)
2. Vérifiez que vous êtes connecté à la bonne base : `zoo_dyslexie`
3. Vérifiez dans mongosh :
   ```javascript
   use zoo_dyslexie
   db.sessions.find().pretty()
   ```

## 💡 Test rapide

Pour tester rapidement, utilisez l'endpoint de stats :

```powershell
curl http://localhost:3000/api/stats
```

Cela vous dira combien de sessions et d'animaux sont dans MongoDB.

