@echo off
echo ========================================
echo   Service MongoDB HTTP pour Zoo App
echo ========================================
echo.

REM Vérifier si Node.js est installé
where node >nul 2>&1
if %ERRORLEVEL% NEQ 0 (
    echo ❌ Node.js n'est pas installé !
    echo 💡 Téléchargez Node.js depuis: https://nodejs.org/
    pause
    exit /b 1
)

echo ✅ Node.js trouvé
echo.

REM Vérifier si les dépendances sont installées
if not exist "node_modules" (
    echo 📦 Installation des dépendances...
    call npm install
    if %ERRORLEVEL% NEQ 0 (
        echo ❌ Erreur lors de l'installation des dépendances
        pause
        exit /b 1
    )
    echo ✅ Dépendances installées
    echo.
)

REM Vérifier si MongoDB est accessible
echo 🔍 Vérification de MongoDB...
node -e "const { MongoClient } = require('mongodb'); const client = new MongoClient('mongodb://127.0.0.1:27017'); client.connect().then(() => { console.log('✅ MongoDB accessible'); client.close(); process.exit(0); }).catch(e => { console.log('❌ MongoDB non accessible:', e.message); process.exit(1); });"
if %ERRORLEVEL% NEQ 0 (
    echo.
    echo ⚠️  MongoDB n'est pas accessible !
    echo 💡 Assurez-vous que MongoDB est démarré
    echo 💡 Vérifiez dans MongoDB Compass: mongodb://127.0.0.1:27017
    echo.
    pause
    exit /b 1
)

echo.
echo ========================================
echo   Démarrage du service HTTP...
echo ========================================
echo.
echo 📡 Le service écoutera sur: http://localhost:3000
echo 📡 Pour émulateur Android: http://10.0.2.2:3000
echo.
echo 💡 Appuyez sur Ctrl+C pour arrêter le service
echo.

REM Démarrer le service
node server.js

