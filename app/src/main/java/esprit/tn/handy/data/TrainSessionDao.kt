package esprit.tn.handy.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface TrainSessionDao {
    @Insert
    suspend fun insert(session: TrainSession)
    
    @Query("SELECT * FROM train_sessions ORDER BY timestamp DESC")
    suspend fun getAllSessions(): List<TrainSession>
    
    @Query("SELECT * FROM train_sessions WHERE animalId = :animalId ORDER BY timestamp DESC")
    suspend fun getSessionsByAnimal(animalId: Int): List<TrainSession>
}

