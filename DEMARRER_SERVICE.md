# 🚀 Comment démarrer le service MongoDB HTTP

## ⚠️ IMPORTANT

**Pour que les données soient enregistrées directement dans MongoDB Compass, vous DEVEZ démarrer le service HTTP.**

## 🎯 Démarrage rapide (Windows)

### Option 1 : Utiliser le script automatique (RECOMMANDÉ)

1. **Double-cliquez sur** `mongodb-service/start.bat`
2. Le script va :
   - Vérifier que Node.js est installé
   - Installer les dépendances si nécessaire
   - Vérifier que MongoDB est accessible
   - Démarrer le service

### Option 2 : Démarrage manuel

1. **Ouvrez un terminal PowerShell** dans le dossier `mongodb-service`
2. **Installez les dépendances** (première fois seulement) :
   ```powershell
   npm install
   ```
3. **Démarrez le service** :
   ```powershell
   npm start
   ```

## ✅ Vérification

Quand le service est démarré, vous devriez voir :

```
✅ Connecté à MongoDB: zoo_dyslexie
🚀 Serveur MongoDB HTTP démarré !
📡 Écoute sur: http://localhost:3000
```

## 🧪 Tester le service

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

## 📱 Configuration Android

L'application Android est déjà configurée pour utiliser :
- **Émulateur** : `http://10.0.2.2:3000` ✅
- **Appareil physique** : Modifiez `MongoDBConfig.kt` avec votre IP

## 🔍 Vérifier que ça fonctionne

1. **Démarrez le service HTTP** (voir ci-dessus)
2. **Lancez l'application Android**
3. **Faites un test complet** (Learn, Test ou Train)
4. **Regardez les logs du service** - vous devriez voir :
   ```
   📥 Requête reçue:
      Session ID: [uuid]
      Phase: test
      Métriques: 6
   💾 Insertion dans MongoDB...
      ✅ Inserté avec ID: [ObjectId]
   ```
5. **Actualisez MongoDB Compass** (bouton refresh 🔄)
6. **Vérifiez les collections** `sessions` et `animals`

## ❌ Problèmes courants

### "MongoDB non accessible"
- **Solution** : Démarrez MongoDB (via MongoDB Compass ou service Windows)

### "Port 3000 déjà utilisé"
- **Solution** : Fermez l'autre application qui utilise le port 3000

### "Node.js n'est pas installé"
- **Solution** : Téléchargez depuis https://nodejs.org/

### Les données n'apparaissent pas dans Compass
- **Solution 1** : Actualisez Compass (bouton refresh 🔄)
- **Solution 2** : Vérifiez les logs du service pour voir les erreurs
- **Solution 3** : Testez avec `curl http://localhost:3000/api/stats`

## 💡 Astuce

Pour démarrer automatiquement le service au démarrage de Windows :
1. Créez un raccourci vers `start.bat`
2. Ajoutez-le au dossier de démarrage Windows

