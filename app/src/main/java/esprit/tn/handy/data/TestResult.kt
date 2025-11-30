package esprit.tn.handy.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "test_results")
data class TestResult(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val animalId: Int,
    val animalName: String,
    val isCorrect: Boolean,
    val responseTime: Long, // en millisecondes
    val hasHesitation: Boolean, // > 2 secondes
    val confusedWith: String? = null, // nom de l'animal confondu
    val timestamp: Long = System.currentTimeMillis()
)

