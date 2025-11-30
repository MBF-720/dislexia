# Service HTTP MongoDB pour Zoo Dyslexie

Ce service reçoit les données de l'application Android et les insère directement dans MongoDB Compass.

## 🚀 Installation

1. **Installer Node.js** (si pas déjà installé) : https://nodejs.org/

2. **Installer les dépendances** :
   ```bash
   cd mongodb-service
   npm install
   ```

## ▶️ Démarrage

```bash
npm start
```

Le serveur démarre sur `http://localhost:3000`

## 📡 Configuration dans l'application Android

L'application Android doit envoyer les données à :
- **Émulateur** : `http://10.0.2.2:3000/api/sessions`
- **Appareil physique** : `http://[IP_DE_VOTRE_ORDINATEUR]:3000/api/sessions`

Pour trouver votre IP :
- Windows : `ipconfig` → Cherchez "IPv4 Address"
- Mac/Linux : `ifconfig` → Cherchez "inet"

## ✅ Vérification

Testez la connexion :
```bash
curl http://localhost:3000/api/health
```

Ou ouvrez dans un navigateur : `http://localhost:3000/api/health`

## 📊 MongoDB Compass

Une fois le service démarré et l'application Android configurée :
1. Les données seront insérées **directement** dans MongoDB
2. Ouvrez MongoDB Compass : `mongodb://127.0.0.1:27017`
3. Base de données : `zoo_dyslexie`
4. Collections : `sessions` et `animals`

## 🔧 Dépannage

- **Port déjà utilisé** : Changez `PORT = 3000` dans `server.js`
- **MongoDB non accessible** : Vérifiez que MongoDB est démarré
- **Firewall** : Autorisez le port 3000 dans le firewall Windows

