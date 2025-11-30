# Script PowerShell pour importer les données Zoo dans MongoDB
# Utilisation: .\import_to_mongodb.ps1

Write-Host "=== Import Zoo Dyslexie dans MongoDB ===" -ForegroundColor Cyan
Write-Host ""

# Vérifier que MongoDB est en cours d'exécution
Write-Host "Vérification de MongoDB..." -ForegroundColor Yellow
try {
    $mongoCheck = mongosh --eval "db.version()" 2>&1
    if ($LASTEXITCODE -ne 0) {
        Write-Host "❌ MongoDB n'est pas accessible. Démarrez MongoDB d'abord." -ForegroundColor Red
        exit 1
    }
    Write-Host "✅ MongoDB est accessible" -ForegroundColor Green
} catch {
    Write-Host "❌ MongoDB n'est pas dans le PATH ou n'est pas démarré" -ForegroundColor Red
    Write-Host "   Démarrez MongoDB et réessayez" -ForegroundColor Yellow
    exit 1
}

Write-Host ""

# Créer la base de données et les collections si elles n'existent pas
Write-Host "Création de la base de données 'zoo_dyslexie' (si nécessaire)..." -ForegroundColor Yellow
$createDbScript = @"
use zoo_dyslexie;
if (!db.getCollectionNames().includes('sessions')) {
    db.createCollection('sessions');
    print('✅ Collection sessions créée');
} else {
    print('ℹ️  Collection sessions existe déjà');
}
if (!db.getCollectionNames().includes('animals')) {
    db.createCollection('animals');
    print('✅ Collection animals créée');
} else {
    print('ℹ️  Collection animals existe déjà');
}
print('✅ Base de données zoo_dyslexie prête');
"@
mongosh --eval $createDbScript 2>&1 | Out-Null
Write-Host "✅ Base de données vérifiée/créée" -ForegroundColor Green

Write-Host ""

# Demander le chemin du fichier JSON
$jsonFile = Read-Host "Entrez le chemin du fichier JSON à importer (ou appuyez sur Entrée pour chercher)"

if ([string]::IsNullOrWhiteSpace($jsonFile)) {
    # Chercher les fichiers JSON dans le répertoire courant
    $jsonFiles = Get-ChildItem -Path . -Filter "*.json" -Recurse | Where-Object { $_.Name -like "*session*" -or $_.Name -like "*export*" }
    
    if ($jsonFiles.Count -eq 0) {
        Write-Host "❌ Aucun fichier JSON trouvé" -ForegroundColor Red
        Write-Host ""
        Write-Host "Pour récupérer les fichiers depuis l'appareil Android:" -ForegroundColor Yellow
        Write-Host "  adb shell run-as esprit.tn.handy cat files/mongodb/sessions/[UUID].json > session.json" -ForegroundColor Cyan
        exit 1
    }
    
    if ($jsonFiles.Count -eq 1) {
        $jsonFile = $jsonFiles[0].FullName
        Write-Host "Fichier trouvé: $jsonFile" -ForegroundColor Green
    } else {
        Write-Host "Plusieurs fichiers trouvés:" -ForegroundColor Yellow
        for ($i = 0; $i -lt $jsonFiles.Count; $i++) {
            Write-Host "  [$i] $($jsonFiles[$i].Name)" -ForegroundColor Cyan
        }
        $choice = Read-Host "Choisissez un fichier (0-$($jsonFiles.Count-1))"
        $jsonFile = $jsonFiles[$choice].FullName
    }
}

if (-not (Test-Path $jsonFile)) {
    Write-Host "❌ Fichier non trouvé: $jsonFile" -ForegroundColor Red
    exit 1
}

Write-Host ""
Write-Host "Import du fichier: $jsonFile" -ForegroundColor Yellow

# Importer avec mongoimport
$importResult = mongoimport --db zoo_dyslexie --collection sessions --file $jsonFile --jsonArray 2>&1

if ($LASTEXITCODE -eq 0) {
    Write-Host ""
    Write-Host "✅ Import réussi!" -ForegroundColor Green
    Write-Host ""
    Write-Host "Vérification..." -ForegroundColor Yellow
    
    # Compter les documents
    $count = mongosh zoo_dyslexie --quiet --eval "db.sessions.countDocuments()"
    Write-Host "   Total sessions: $count" -ForegroundColor Cyan
    
    $learnCount = mongosh zoo_dyslexie --quiet --eval "db.sessions.countDocuments({ phase: 'learn' })"
    $testCount = mongosh zoo_dyslexie --quiet --eval "db.sessions.countDocuments({ phase: 'test' })"
    $trainCount = mongosh zoo_dyslexie --quiet --eval "db.sessions.countDocuments({ phase: 'train' })"
    
    Write-Host "   Sessions learn: $learnCount" -ForegroundColor Cyan
    Write-Host "   Sessions test: $testCount" -ForegroundColor Cyan
    Write-Host "   Sessions train: $trainCount" -ForegroundColor Cyan
    
    Write-Host ""
    Write-Host "✅ Vous pouvez maintenant ouvrir MongoDB Compass" -ForegroundColor Green
    Write-Host "   Connectez-vous à: mongodb://127.0.0.1:27017" -ForegroundColor Cyan
    Write-Host "   Base de données: zoo_dyslexie" -ForegroundColor Cyan
    Write-Host "   Collection: sessions" -ForegroundColor Cyan
} else {
    Write-Host ""
    Write-Host "❌ Erreur lors de l'import" -ForegroundColor Red
    Write-Host $importResult -ForegroundColor Red
    Write-Host ""
    Write-Host "Vérifiez que:" -ForegroundColor Yellow
    Write-Host "  1. MongoDB est en cours d'exécution" -ForegroundColor Yellow
    Write-Host "  2. Le fichier JSON est valide" -ForegroundColor Yellow
    Write-Host "  3. mongoimport est dans votre PATH" -ForegroundColor Yellow
}

Write-Host ""
Read-Host "Appuyez sur Entrée pour continuer"

