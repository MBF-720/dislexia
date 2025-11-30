# Script pour combiner tous les fichiers JSON et importer dans MongoDB
# Utilisation: .\combine_and_import.ps1

Write-Host "=== Import Zoo Dyslexie dans MongoDB ===" -ForegroundColor Cyan
Write-Host ""

# Étape 1: Récupérer les fichiers depuis l'appareil
Write-Host "Étape 1: Récupération des fichiers depuis l'appareil..." -ForegroundColor Yellow
Write-Host "Assurez-vous que l'appareil est connecté et que USB debugging est activé" -ForegroundColor Cyan
Write-Host ""

$tempDir = "mongodb_temp"
if (Test-Path $tempDir) {
    Remove-Item -Recurse -Force $tempDir
}
New-Item -ItemType Directory -Path $tempDir | Out-Null

# Copier les fichiers depuis l'appareil
Write-Host "Copie des fichiers de sessions..." -ForegroundColor Yellow
adb shell run-as esprit.tn.handy tar -czf /sdcard/mongodb_backup.tar.gz files/mongodb/ 2>&1 | Out-Null

if ($LASTEXITCODE -eq 0) {
    adb pull /sdcard/mongodb_backup.tar.gz $tempDir\ 2>&1 | Out-Null
    if ($LASTEXITCODE -eq 0) {
        Push-Location $tempDir
        tar -xzf mongodb_backup.tar.gz 2>&1 | Out-Null
        Pop-Location
        Write-Host "✅ Fichiers récupérés" -ForegroundColor Green
    } else {
        Write-Host "❌ Erreur lors de la récupération" -ForegroundColor Red
        Write-Host "   Essayez de copier manuellement les fichiers" -ForegroundColor Yellow
        exit 1
    }
} else {
    Write-Host "⚠️  Impossible de récupérer depuis l'appareil" -ForegroundColor Yellow
    Write-Host "   Utilisez les fichiers JSON locaux si disponibles" -ForegroundColor Yellow
}

Write-Host ""

# Étape 2: Combiner tous les fichiers JSON
Write-Host "Étape 2: Combinaison des fichiers JSON..." -ForegroundColor Yellow

$sessionsDir = "$tempDir\files\mongodb\sessions"
$combinedFile = "all_sessions.json"

if (Test-Path $sessionsDir) {
    $jsonFiles = Get-ChildItem -Path $sessionsDir -Filter "*.json"
    
    if ($jsonFiles.Count -eq 0) {
        Write-Host "❌ Aucun fichier JSON trouvé dans $sessionsDir" -ForegroundColor Red
        exit 1
    }
    
    Write-Host "   Trouvé $($jsonFiles.Count) fichiers" -ForegroundColor Cyan
    
    $combinedArray = @()
    foreach ($file in $jsonFiles) {
        try {
            $content = Get-Content $file.FullName -Raw
            $json = $content | ConvertFrom-Json
            $combinedArray += $json
            Write-Host "   ✅ $($file.Name)" -ForegroundColor Green
        } catch {
            Write-Host "   ⚠️  Erreur avec $($file.Name): $_" -ForegroundColor Yellow
        }
    }
    
    $combinedArray | ConvertTo-Json -Depth 10 | Out-File $combinedFile -Encoding UTF8
    Write-Host "✅ Fichier combiné créé: $combinedFile ($($combinedArray.Count) sessions)" -ForegroundColor Green
} else {
    Write-Host "❌ Dossier sessions non trouvé" -ForegroundColor Red
    exit 1
}

Write-Host ""

# Étape 3: Vérifier MongoDB
Write-Host "Étape 3: Vérification de MongoDB..." -ForegroundColor Yellow
try {
    $mongoCheck = mongosh --eval "db.version()" 2>&1
    if ($LASTEXITCODE -ne 0) {
        Write-Host "❌ MongoDB n'est pas accessible" -ForegroundColor Red
        Write-Host "   Démarrez MongoDB et réessayez" -ForegroundColor Yellow
        exit 1
    }
    Write-Host "✅ MongoDB est accessible" -ForegroundColor Green
} catch {
    Write-Host "❌ MongoDB n'est pas dans le PATH" -ForegroundColor Red
    exit 1
}

Write-Host ""

# Étape 4: Créer la base de données si elle n'existe pas
Write-Host "Étape 4: Création de la base de données (si nécessaire)..." -ForegroundColor Yellow
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

# Étape 5: Importer
Write-Host "Étape 5: Import des sessions..." -ForegroundColor Yellow
$importResult = mongoimport --db zoo_dyslexie --collection sessions --file $combinedFile --jsonArray 2>&1

if ($LASTEXITCODE -eq 0) {
    Write-Host "✅ Import réussi!" -ForegroundColor Green
    Write-Host ""
    
    # Vérification
    Write-Host "Vérification..." -ForegroundColor Yellow
    $count = mongosh zoo_dyslexie --quiet --eval "db.sessions.countDocuments()"
    Write-Host "   Total sessions: $count" -ForegroundColor Cyan
    
    Write-Host ""
    Write-Host "🎉 SUCCÈS! Les données sont maintenant dans MongoDB!" -ForegroundColor Green
    Write-Host ""
    Write-Host "Ouvrez MongoDB Compass et connectez-vous à:" -ForegroundColor Cyan
    Write-Host "   mongodb://127.0.0.1:27017" -ForegroundColor White
    Write-Host ""
    Write-Host "Vous devriez voir la base de données 'zoo_dyslexie' avec la collection 'sessions'" -ForegroundColor Cyan
} else {
    Write-Host "❌ Erreur lors de l'import" -ForegroundColor Red
    Write-Host $importResult -ForegroundColor Red
}

Write-Host ""
Write-Host "Nettoyage..." -ForegroundColor Yellow
if (Test-Path $tempDir) {
    Remove-Item -Recurse -Force $tempDir
}

Write-Host ""
Read-Host "Appuyez sur Entrée pour continuer"

