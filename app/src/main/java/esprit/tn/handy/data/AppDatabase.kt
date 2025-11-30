package esprit.tn.handy.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [LearnSession::class, TestResult::class, TrainSession::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun learnSessionDao(): LearnSessionDao
    abstract fun testResultDao(): TestResultDao
    abstract fun trainSessionDao(): TrainSessionDao
    
    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null
        
        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "handy_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}

