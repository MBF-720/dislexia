package esprit.tn.handy.data

data class Animal(
    val id: Int,
    val name: String,
    val nameLowercase: String,
    val drawableResName: String,
    val description: String = ""
) {
    companion object {
        val ANIMALS = listOf(
            Animal(1, "Éléphant", "éléphant", "elephant", "Un grand animal avec une trompe"),
            Animal(2, "Lion", "lion", "lion", "Le roi de la jungle"),
            Animal(3, "Girafe", "girafe", "girafe", "Un animal avec un long cou"),
            Animal(4, "Zèbre", "zèbre", "zebre", "Un cheval avec des rayures"),
            Animal(5, "Singe", "singe", "singe", "Un animal qui grimpe aux arbres"),
            Animal(6, "Oiseau", "oiseau", "oiseau", "Un animal qui vole")
        )
        
        fun getAnimalByName(name: String): Animal? {
            return ANIMALS.find { 
                it.name.equals(name, ignoreCase = true) || 
                it.nameLowercase.equals(name, ignoreCase = true)
            }
        }
        
        fun getAnimalById(id: Int): Animal? {
            return ANIMALS.find { it.id == id }
        }
    }
}

