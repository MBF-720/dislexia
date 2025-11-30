package esprit.tn.handy.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface TestResultDao {
    @Insert
    suspend fun insert(result: TestResult)
    
    @Query("SELECT * FROM test_results ORDER BY timestamp DESC")
    suspend fun getAllResults(): List<TestResult>
    
    @Query("SELECT * FROM test_results WHERE animalId = :animalId ORDER BY timestamp DESC")
    suspend fun getResultsByAnimal(animalId: Int): List<TestResult>
    
    @Query("SELECT COUNT(*) FROM test_results WHERE isCorrect = 1")
    suspend fun getCorrectCount(): Int
    
    @Query("SELECT COUNT(*) FROM test_results")
    suspend fun getTotalCount(): Int
}

