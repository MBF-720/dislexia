// Script MongoDB pour importer les données de l'application Zoo
// Utilisation: mongosh < import_to_mongodb.js
// OU: mongosh --file import_to_mongodb.js

// Se connecter à la base de données
use zoo_dyslexie

// Créer les collections si elles n'existent pas
const collections = db.getCollectionNames();

if (!collections.includes('sessions')) {
    db.createCollection("sessions");
    print("✅ Collection sessions créée");
} else {
    print("ℹ️  Collection sessions existe déjà");
}

if (!collections.includes('animals')) {
    db.createCollection("animals");
    print("✅ Collection animals créée");
} else {
    print("ℹ️  Collection animals existe déjà");
}

print("✅ Base de données zoo_dyslexie prête");

// Pour importer un fichier JSON spécifique:
// db.sessions.insertOne(JSON.parse(cat("path/to/session.json")))

// Pour importer tous les fichiers d'un dossier (à adapter selon votre chemin):
// const fs = require('fs');
// const path = './mongodb/sessions/';
// const files = fs.readdirSync(path);
// files.forEach(file => {
//   if (file.endsWith('.json')) {
//     const content = fs.readFileSync(path + file, 'utf8');
//     const doc = JSON.parse(content);
//     db.sessions.insertOne(doc);
//     print(`✅ Importé: ${file}`);
//   }
// });

print("\n📋 Commandes utiles:")
print("  - Voir toutes les sessions: db.sessions.find().pretty()")
print("  - Compter les sessions: db.sessions.countDocuments()")
print("  - Voir les sessions de test: db.sessions.find({ phase: 'test' }).pretty()")
print("  - Voir les statistiques d'un animal: db.animals.findOne({ animal: 'Éléphant' })")

