package esprit.tn.handy.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "learn_sessions")
data class LearnSession(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val animalId: Int,
    val animalName: String,
    val timeSpent: Long, // en millisecondes
    val repetitions: Int,
    val correctAnswers: Int,
    val totalAttempts: Int,
    val timestamp: Long = System.currentTimeMillis()
)

