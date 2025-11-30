// Service HTTP simple pour recevoir les données de l'app Android
// et les insérer directement dans MongoDB
// 
// Installation: npm install express mongodb cors
// Démarrage: node server.js

const express = require('express');
const { MongoClient } = require('mongodb');
const cors = require('cors');

const app = express();
const PORT = 3000;
const MONGODB_URI = 'mongodb://127.0.0.1:27017';
const DATABASE_NAME = 'zoo_dyslexie';

app.use(cors());
app.use(express.json());

let mongoClient;
let db;

// Connexion à MongoDB
async function connectToMongoDB() {
    try {
        mongoClient = new MongoClient(MONGODB_URI);
        await mongoClient.connect();
        db = mongoClient.db(DATABASE_NAME);
        
        // Créer les collections si elles n'existent pas
        const collections = await db.listCollections().toArray();
        const collectionNames = collections.map(c => c.name);
        
        if (!collectionNames.includes('sessions')) {
            await db.createCollection('sessions');
            console.log('✅ Collection sessions créée');
        }
        
        if (!collectionNames.includes('animals')) {
            await db.createCollection('animals');
            console.log('✅ Collection animals créée');
        }
        
        console.log(`✅ Connecté à MongoDB: ${DATABASE_NAME}`);
        return true;
    } catch (error) {
        console.error('❌ Erreur de connexion MongoDB:', error);
        return false;
    }
}

// Endpoint pour sauvegarder une session
app.post('/api/sessions', async (req, res) => {
    try {
        const session = req.body;
        
        console.log('\n📥 Requête reçue:');
        console.log('   Session ID:', session.sessionId);
        console.log('   Phase:', session.phase);
        console.log('   Métriques:', session.animalMetrics?.length || 0);
        console.log('   Score:', session.score);
        console.log('   Total Questions:', session.totalQuestions);
        
        if (!session.sessionId || !session.phase) {
            console.error('❌ Données invalides: sessionId ou phase manquant');
            return res.status(400).json({ error: 'Données de session invalides' });
        }
        
        // Vérifier que db est bien initialisé
        if (!db) {
            console.error('❌ Base de données non initialisée');
            return res.status(500).json({ error: 'Base de données non initialisée' });
        }
        
        // Insérer la session
        console.log('💾 Insertion dans MongoDB...');
        const result = await db.collection('sessions').insertOne(session);
        console.log('   ✅ Inserté avec ID:', result.insertedId);
        
        // Mettre à jour les statistiques des animaux
        if (session.animalMetrics && session.animalMetrics.length > 0) {
            console.log('📊 Mise à jour des statistiques des animaux...');
            await updateAnimalsStats(session.animalMetrics);
        } else {
            console.log('⚠️ Aucune métrique d\'animal dans cette session');
        }
        
        // Vérifier l'insertion
        const count = await db.collection('sessions').countDocuments();
        console.log(`✅ Session sauvegardée: ${session.sessionId} (${session.phase})`);
        console.log(`📊 Total sessions dans MongoDB: ${count}`);
        
        res.json({ 
            success: true, 
            sessionId: session.sessionId,
            insertedId: result.insertedId,
            totalSessions: count
        });
    } catch (error) {
        console.error('❌ Erreur lors de la sauvegarde:', error);
        console.error('   Stack:', error.stack);
        res.status(500).json({ error: error.message });
    }
});

// Fonction pour mettre à jour les statistiques des animaux
async function updateAnimalsStats(metrics) {
    if (!metrics || metrics.length === 0) {
        console.log('⚠️ Aucune métrique à traiter');
        return;
    }
    
    console.log(`📊 Traitement de ${metrics.length} métriques...`);
    const metricsByAnimal = {};
    
    // Grouper par animal
    metrics.forEach(metric => {
        if (!metric.animal) {
            console.warn('⚠️ Métrique sans nom d\'animal:', metric);
            return;
        }
        if (!metricsByAnimal[metric.animal]) {
            metricsByAnimal[metric.animal] = [];
        }
        metricsByAnimal[metric.animal].push(metric);
    });
    
    console.log(`📊 Animaux à traiter: ${Object.keys(metricsByAnimal).join(', ')}`);
    
    // Mettre à jour chaque animal
    for (const [animalName, animalMetrics] of Object.entries(metricsByAnimal)) {
        console.log(`   🐾 Traitement de: ${animalName} (${animalMetrics.length} métriques)`);
        const existing = await db.collection('animals').findOne({ animal: animalName });
        
        const totalSessions = (existing?.totalSessions || 0) + 1;
        const totalCorrect = (existing?.totalCorrect || 0) + animalMetrics.filter(m => m.correct).length;
        const totalIncorrect = (existing?.totalIncorrect || 0) + animalMetrics.filter(m => !m.correct).length;
        const totalHesitations = (existing?.totalHesitations || 0) + animalMetrics.reduce((sum, m) => sum + (m.hesitations || 0), 0);
        const totalRepetitions = (existing?.totalRepetitions || 0) + animalMetrics.reduce((sum, m) => sum + (m.repetitions || 0), 0);
        
        const allResponseTimes = animalMetrics.map(m => m.responseTimeMs || 0);
        const averageResponseTime = allResponseTimes.length > 0 
            ? Math.round(allResponseTimes.reduce((a, b) => a + b, 0) / allResponseTimes.length)
            : (existing?.averageResponseTime || 0);
        
        const history = existing?.history || [];
        animalMetrics.forEach(metric => {
            history.push({
                animal: metric.animal,
                responseTimeMs: metric.responseTimeMs,
                correct: metric.correct,
                hesitations: metric.hesitations,
                repetitions: metric.repetitions,
                inputMethod: metric.inputMethod,
                confusedWith: metric.confusedWith,
                timestamp: new Date()
            });
        });
        
        const updateResult = await db.collection('animals').updateOne(
            { animal: animalName },
            {
                $set: {
                    animal: animalName,
                    totalSessions,
                    totalCorrect,
                    totalIncorrect,
                    averageResponseTime,
                    totalHesitations,
                    totalRepetitions,
                    lastUpdated: new Date(),
                    history
                }
            },
            { upsert: true }
        );
        
        console.log(`   ✅ Animal mis à jour: ${animalName}`);
        console.log(`      - Sessions: ${totalSessions}, Correct: ${totalCorrect}, Incorrect: ${totalIncorrect}`);
        console.log(`      - Upsert: ${updateResult.upsertedId ? 'Créé' : 'Mis à jour'}`);
    }
    
    // Vérifier le nombre total d'animaux
    const animalsCount = await db.collection('animals').countDocuments();
    console.log(`📊 Total animaux dans MongoDB: ${animalsCount}`);
}

// Endpoint de santé
app.get('/api/health', async (req, res) => {
    try {
        await db.admin().ping();
        const sessionsCount = await db.collection('sessions').countDocuments();
        const animalsCount = await db.collection('animals').countDocuments();
        
        res.json({ 
            status: 'ok', 
            mongodb: 'connected',
            database: DATABASE_NAME,
            sessionsCount: sessionsCount,
            animalsCount: animalsCount
        });
    } catch (error) {
        res.status(500).json({ 
            status: 'error', 
            mongodb: 'disconnected',
            error: error.message 
        });
    }
});

// Endpoint de test pour vérifier les données
app.get('/api/stats', async (req, res) => {
    try {
        const sessionsCount = await db.collection('sessions').countDocuments();
        const animalsCount = await db.collection('animals').countDocuments();
        
        const sessions = await db.collection('sessions').find({}).limit(5).toArray();
        const animals = await db.collection('animals').find({}).limit(5).toArray();
        
        res.json({
            sessionsCount,
            animalsCount,
            recentSessions: sessions.map(s => ({
                sessionId: s.sessionId,
                phase: s.phase,
                score: s.score,
                totalQuestions: s.totalQuestions,
                metricsCount: s.animalMetrics?.length || 0
            })),
            animals: animals.map(a => ({
                animal: a.animal,
                totalSessions: a.totalSessions,
                totalCorrect: a.totalCorrect,
                totalIncorrect: a.totalIncorrect
            }))
        });
    } catch (error) {
        res.status(500).json({ error: error.message });
    }
});

// Démarrer le serveur
async function startServer() {
    const connected = await connectToMongoDB();
    
    if (!connected) {
        console.error('❌ Impossible de se connecter à MongoDB. Vérifiez que MongoDB est démarré.');
        process.exit(1);
    }
    
    app.listen(PORT, '0.0.0.0', () => {
        console.log('');
        console.log('🚀 Serveur MongoDB HTTP démarré !');
        console.log(`📡 Écoute sur: http://localhost:${PORT}`);
        console.log(`📡 Pour appareil Android: http://[VOTRE_IP]:${PORT}`);
        console.log('');
        console.log('📋 Endpoints disponibles:');
        console.log(`   POST http://localhost:${PORT}/api/sessions - Sauvegarder une session`);
        console.log(`   GET  http://localhost:${PORT}/api/health - Vérifier la connexion`);
        console.log('');
        console.log('💡 Pour trouver votre IP: ipconfig (Windows) ou ifconfig (Mac/Linux)');
        console.log('💡 Pour émulateur Android: http://10.0.2.2:${PORT}');
        console.log('');
    });
}

// Gestion de l'arrêt propre
process.on('SIGINT', async () => {
    console.log('\n🛑 Arrêt du serveur...');
    if (mongoClient) {
        await mongoClient.close();
    }
    process.exit(0);
});

startServer();

