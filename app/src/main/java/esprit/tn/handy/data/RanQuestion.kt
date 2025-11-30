package esprit.tn.handy.data

/**
 * Question pour le quiz de dépistage RAN (Rapid Automatized Naming)
 */
data class RanQuestion(
    val id: Int,
    val category: RanCategory,
    val item: String, // L'item à nommer (ex: "b", "7", "red", "dog", "b 4")
    val correctAnswer: String // La réponse correcte attendue
)

enum class RanCategory {
    LETTER,        // Nommage de lettres
    DIGIT,         // Nommage de chiffres
    COLOR,         // Nommage de couleurs
    OBJECT,        // Nommage d'objets
    ALTERNATION    // Alternance lettre-chiffre
}

object RanQuestions {
    val ALL_QUESTIONS = listOf(
        RanQuestion(1, RanCategory.LETTER, "b", "b"),
        RanQuestion(2, RanCategory.LETTER, "m", "m"),
        RanQuestion(3, RanCategory.DIGIT, "7", "7"),
        RanQuestion(4, RanCategory.DIGIT, "3", "3"),
        RanQuestion(5, RanCategory.COLOR, "red", "rouge"),
        RanQuestion(6, RanCategory.COLOR, "blue", "bleu"),
        RanQuestion(7, RanCategory.OBJECT, "dog", "chien"),
        RanQuestion(8, RanCategory.OBJECT, "car", "voiture"),
        RanQuestion(9, RanCategory.ALTERNATION, "b 4", "b quatre"),
        RanQuestion(10, RanCategory.ALTERNATION, "m 7", "m sept")
    )
}

