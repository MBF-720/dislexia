package esprit.tn.handy.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "train_sessions")
data class TrainSession(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val animalId: Int,
    val animalName: String,
    val isCorrect: Boolean,
    val attempts: Int,
    val timestamp: Long = System.currentTimeMillis()
)

