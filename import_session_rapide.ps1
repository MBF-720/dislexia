# Script rapide pour importer une session dans MongoDB
# Utilisation: .\import_session_rapide.ps1

Write-Host "=== Import Session Zoo dans MongoDB ===" -ForegroundColor Cyan
Write-Host ""

# Étape 1: Récupérer le fichier depuis l'appareil
Write-Host "Étape 1: Récupération du fichier depuis l'appareil..." -ForegroundColor Yellow
Write-Host "Assurez-vous que l'appareil est connecté et que USB debugging est activé" -ForegroundColor Cyan
Write-Host ""

# Lister les fichiers disponibles
Write-Host "Fichiers de sessions disponibles:" -ForegroundColor Yellow
$files = adb shell run-as esprit.tn.handy ls files/mongodb/sessions/ 2>&1

if ($LASTEXITCODE -ne 0) {
    Write-Host "❌ Erreur: Impossible d'accéder aux fichiers" -ForegroundColor Red
    Write-Host "   Vérifiez que:" -ForegroundColor Yellow
    Write-Host "   1. L'appareil est connecté" -ForegroundColor Yellow
    Write-Host "   2. USB debugging est activé" -ForegroundColor Yellow
    Write-Host "   3. L'application a été exécutée au moins une fois" -ForegroundColor Yellow
    exit 1
}

$fileList = $files -split "`n" | Where-Object { $_ -match "\.json$" }
if ($fileList.Count -eq 0) {
    Write-Host "❌ Aucun fichier de session trouvé" -ForegroundColor Red
    Write-Host "   Faites d'abord une session (Learn, Test ou Train) dans l'application" -ForegroundColor Yellow
    exit 1
}

Write-Host "Fichiers trouvés:" -ForegroundColor Green
for ($i = 0; $i -lt $fileList.Count; $i++) {
    Write-Host "  [$i] $($fileList[$i])" -ForegroundColor Cyan
}

if ($fileList.Count -eq 1) {
    $selectedFile = $fileList[0].Trim()
    Write-Host "Fichier sélectionné: $selectedFile" -ForegroundColor Green
} else {
    $choice = Read-Host "Choisissez un fichier (0-$($fileList.Count-1))"
    $selectedFile = $fileList[$choice].Trim()
}

Write-Host ""
Write-Host "Copie du fichier..." -ForegroundColor Yellow
$localFile = "session_import.json"
adb shell run-as esprit.tn.handy cat "files/mongodb/sessions/$selectedFile" > $localFile 2>&1

if ($LASTEXITCODE -ne 0) {
    Write-Host "❌ Erreur lors de la copie" -ForegroundColor Red
    exit 1
}

Write-Host "✅ Fichier copié: $localFile" -ForegroundColor Green
Write-Host ""

# Étape 2: Vérifier MongoDB
Write-Host "Étape 2: Vérification de MongoDB..." -ForegroundColor Yellow
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

# Étape 3: Créer la base de données si elle n'existe pas
Write-Host "Étape 3: Création de la base de données (si nécessaire)..." -ForegroundColor Yellow
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

# Étape 4: Importer
Write-Host "Étape 4: Import de la session..." -ForegroundColor Yellow

# Convertir le fichier en array JSON si nécessaire
$content = Get-Content $localFile -Raw
$json = $content | ConvertFrom-Json

# Si c'est un objet unique, le mettre dans un array
if ($json -isnot [Array]) {
    $jsonArray = @($json)
    $jsonArray | ConvertTo-Json -Depth 10 | Out-File "session_array.json" -Encoding UTF8
    $importFile = "session_array.json"
} else {
    $importFile = $localFile
}

$importResult = mongoimport --db zoo_dyslexie --collection sessions --file $importFile --jsonArray 2>&1

if ($LASTEXITCODE -eq 0) {
    Write-Host "✅ Import réussi!" -ForegroundColor Green
    Write-Host ""
    
    # Vérification
    Write-Host "Vérification..." -ForegroundColor Yellow
    $count = mongosh zoo_dyslexie --quiet --eval "db.sessions.countDocuments()"
    Write-Host "   Total sessions: $count" -ForegroundColor Cyan
    
    $phase = $json.phase
    Write-Host "   Phase de cette session: $phase" -ForegroundColor Cyan
    
    Write-Host ""
    Write-Host "🎉 SUCCÈS! La session est maintenant dans MongoDB!" -ForegroundColor Green
    Write-Host ""
    Write-Host "Ouvrez MongoDB Compass et connectez-vous à:" -ForegroundColor Cyan
    Write-Host "   mongodb://127.0.0.1:27017" -ForegroundColor White
    Write-Host ""
    Write-Host "Base de données: zoo_dyslexie" -ForegroundColor Cyan
    Write-Host "Collection: sessions" -ForegroundColor Cyan
    Write-Host "Phase: $phase" -ForegroundColor Cyan
} else {
    Write-Host "❌ Erreur lors de l'import" -ForegroundColor Red
    Write-Host $importResult -ForegroundColor Red
}

Write-Host ""
Read-Host "Appuyez sur Entrée pour continuer"

