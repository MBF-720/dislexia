# Guide d'Import Rapide dans MongoDB Compass

## ⚠️ Les données sont sur l'appareil Android, pas encore dans MongoDB !

Les données sont sauvegardées **localement** sur l'appareil Android. Pour les voir dans MongoDB Compass, vous devez les **importer**.

## 🚀 Solution Rapide (3 étapes)

### Étape 1 : Connecter l'appareil

Assurez-vous que :
- ✅ L'appareil Android est connecté via USB
- ✅ USB debugging est activé
- ✅ ADB fonctionne : `adb devices`

### Étape 2 : Exécuter le script d'import

Ouvrez PowerShell dans le dossier du projet et exécutez :

```powershell
.\import_session_rapide.ps1
```

Le script va :
1. Lister tous les fichiers de sessions sur l'appareil
2. Vous permettre de choisir un fichier (ou prendre le seul disponible)
3. Le copier sur votre ordinateur
4. Créer la base de données `zoo_dyslexie` si nécessaire
5. Importer les données dans MongoDB

### Étape 3 : Voir dans MongoDB Compass

1. Ouvrez **MongoDB Compass**
2. Connectez-vous à : `mongodb://127.0.0.1:27017`
3. **Actualisez** (bouton refresh 🔄)
4. Vous devriez voir **zoo_dyslexie** > **sessions**

## 📊 D'après vos logs

Votre dernière session :
- **Session ID**: `42fa4434-ba6e-4e4e-a7f8-cafa8491a711`
- **Phase**: `test`
- **Score**: 2/6
- **6 animaux testés** (Oiseau, Lion, Singe, Éléphant, Girafe, Zèbre)

## 🔍 Vérification rapide

Après l'import, vérifiez dans mongosh :

```javascript
mongosh
use zoo_dyslexie
db.sessions.find().pretty()
db.sessions.findOne({ sessionId: "42fa4434-ba6e-4e4e-a7f8-cafa8491a711" })
```

## ⚡ Alternative : Importer toutes les sessions

Si vous voulez importer **toutes** les sessions en une fois :

```powershell
.\combine_and_import.ps1
```

## ❌ Si ça ne fonctionne pas

1. **Vérifiez que MongoDB est démarré** :
   ```powershell
   mongosh --eval "db.version()"
   ```

2. **Vérifiez que l'appareil est connecté** :
   ```powershell
   adb devices
   ```

3. **Vérifiez les fichiers sur l'appareil** :
   ```powershell
   adb shell run-as esprit.tn.handy ls files/mongodb/sessions/
   ```

4. **Vérifiez les logs** pour voir le chemin exact du fichier :
   ```
   📁 Fichier: /data/user/0/esprit.tn.handy/files/mongodb/sessions/[UUID].json
   ```

## 💡 Astuce

Pour importer automatiquement après chaque session, vous pouvez créer un script qui :
1. Surveille les nouveaux fichiers
2. Les importe automatiquement dans MongoDB

Mais pour l'instant, utilisez `import_session_rapide.ps1` après chaque session de test.

