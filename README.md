# Zoo - Module RAN Dyslexie

Application Android Kotlin pour aider les enfants dyslexiques à apprendre les noms d'animaux.

## Fonctionnalités

### Phase 1 - Apprendre (Learn)
- Affiche 6 animaux : Éléphant, Lion, Girafe, Zèbre, Singe, Oiseau
- Pour chaque animal :
  - Image de l'animal
  - TTS prononce le nom ("Ceci est un éléphant")
  - Affiche le mot en grande police adaptée aux dyslexiques
  - Question vocale : "Peux-tu dire le nom de cet animal ?" → réponse via STT
  - Si réponse correcte → animation positive, sinon → répète TTS
- Bouton "Écouter encore une fois"
- Enregistre : temps passé, répétitions, précision vocale

### Phase 2 - Se Tester (Test)
- Affiche chaque animal à la suite (ordre aléatoire)
- Interaction : dire le nom (STT) ou taper le nom
- Mesure :
  - Temps de réponse
  - Précision (correct/incorrect)
  - Hésitation (>2 sec)
  - Animaux confondus
- Score initial + recommandation pour entraînement

### Phase 3 - S'Entraîner (Train)
- Affiche 6-8 images (focus sur animaux confondus)
- TTS prononce le mot
- Question : "Peux-tu dire le nom ?" → STT
- Feedback immédiat :
  - Correct → animation / son positif
  - Incorrect → répète TTS + mot écrit + indice phonologique
- Progression automatique selon score
- Résultat final :
  - Score
  - Meilleur animal
  - Animal à retravailler

## Technologies

- **Langage** : Kotlin
- **UI** : Jetpack Compose
- **TTS** : Android Text-to-Speech
- **STT** : Android Speech Recognition + Gemini AI pour vérification
- **Stockage** : Room Database (local)
- **Architecture** : MVVM avec ViewModel et StateFlow

## Configuration

### Clé API Gemini

La clé API Gemini est configurée dans `app/src/main/java/esprit/tn/handy/config/GeminiConfig.kt`.

**Note** : Pour la production, utilisez un système de configuration sécurisé (BuildConfig, etc.).

### Permissions

L'application nécessite les permissions suivantes (déjà configurées dans AndroidManifest.xml) :
- `INTERNET` : Pour les appels API Gemini
- `ACCESS_NETWORK_STATE` : Pour vérifier la connectivité
- `RECORD_AUDIO` : Pour la reconnaissance vocale

## Structure du Projet

```
app/src/main/java/esprit/tn/handy/
├── config/
│   └── GeminiConfig.kt          # Configuration Gemini API
├── data/
│   ├── Animal.kt                  # Modèle de données Animal
│   ├── LearnSession.kt            # Session d'apprentissage
│   ├── TestResult.kt              # Résultat de test
│   ├── TrainSession.kt            # Session d'entraînement
│   ├── AppDatabase.kt             # Base de données Room
│   └── *Dao.kt                    # DAOs pour Room
├── service/
│   ├── GeminiService.kt           # Service pour appels Gemini API
│   ├── TTSService.kt              # Service Text-to-Speech
│   └── STTService.kt               # Service Speech-to-Text
├── viewmodel/
│   ├── LearnViewModel.kt          # ViewModel pour Learn
│   ├── TestViewModel.kt            # ViewModel pour Test
│   ├── TrainViewModel.kt          # ViewModel pour Train
│   └── ViewModelFactory.kt        # Factory pour ViewModels
├── ui/
│   └── screens/
│       ├── MainMenuScreen.kt      # Écran menu principal
│       ├── LearnScreen.kt          # Écran Learn
│       ├── TestScreen.kt           # Écran Test
│       └── TrainScreen.kt          # Écran Train
├── navigation/
│   └── NavGraph.kt                # Navigation Compose
└── MainActivity.kt                 # Activity principale
```

## Images

Les images des animaux doivent être placées dans `app/src/main/res/drawable/` avec les noms suivants :
- `Éléphant.jfif` (ou `.png`, `.jpg`)
- `Lion.jfif`
- `Girafe.jfif`
- `Zèbre.jfif`
- `Singe.jfif`
- `Oiseau.jfif`

## Compilation

1. Ouvrir le projet dans Android Studio
2. Synchroniser les dépendances Gradle
3. Compiler et exécuter sur un appareil Android ou émulateur (minSdk 24)

## Utilisation

1. **Apprendre** : L'enfant parcourt les 6 animaux, écoute le nom et essaie de le répéter
2. **Se Tester** : L'enfant est testé sur tous les animaux dans un ordre aléatoire
3. **S'Entraîner** : L'enfant s'entraîne sur les animaux qu'il a mal identifiés

## Base de Données

Les données sont stockées localement dans une base de données Room :
- Sessions d'apprentissage (temps, répétitions, précision)
- Résultats de tests (temps de réponse, hésitations, confusions)
- Sessions d'entraînement (progression)

Pour visualiser les données avec MongoDB Compass, vous devrez exporter les données de Room vers MongoDB (fonctionnalité à implémenter si nécessaire).

## Notes

- L'application utilise la reconnaissance vocale Android native pour capturer la voix
- Gemini AI est utilisé pour vérifier si la réponse de l'enfant correspond à l'animal attendu (tolérance aux fautes de prononciation)
- Le TTS utilise la langue française par défaut
- L'UI est conçue pour être simple et adaptée aux enfants dyslexiques (grandes polices, feedback visuel clair)

## Dépendances Principales

- Jetpack Compose
- Room Database
- OkHttp (pour Gemini API)
- Navigation Compose
- Coroutines
- ViewModel

