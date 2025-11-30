package esprit.tn.handy.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface LearnSessionDao {
    @Insert
    suspend fun insert(session: LearnSession)
    
    @Query("SELECT * FROM learn_sessions ORDER BY timestamp DESC")
    suspend fun getAllSessions(): List<LearnSession>
    
    @Query("SELECT * FROM learn_sessions WHERE animalId = :animalId ORDER BY timestamp DESC")
    suspend fun getSessionsByAnimal(animalId: Int): List<LearnSession>
}

