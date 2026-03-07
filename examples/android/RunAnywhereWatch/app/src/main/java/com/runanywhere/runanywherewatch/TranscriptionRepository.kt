package com.runanywhere.runanywherewatch

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow
import java.text.SimpleDateFormat
import java.util.*

/**
 * Room database for storing transcriptions with timestamps
 */
@Database(entities = [TranscriptionEntity::class], version = 1)
@TypeConverters(Converters::class)
abstract class TranscriptionDatabase :
    RoomDatabase() {
    abstract fun transcriptionDao(): TranscriptionDao
    
    companion object {
        private var INSTANCE: TranscriptionDatabase? = null
        
        fun getInstance(context: Context): TranscriptionDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    TranscriptionDatabase::class.java,
                    "transcription_db"
                ).build().also { INSTANCE = it }
            }
        }
    }
}

/**
 * DAO for transcription operations
 */
@Dao
interface TranscriptionDao {
    @Query("SELECT * FROM transcriptions ORDER BY timestamp DESC")
    fun getAllTranscriptions(): Flow<List<TranscriptionEntity>>
    
    @Query("SELECT * FROM transcriptions ORDER BY timestamp DESC")
    fun getAllTranscriptionsSync(): List<TranscriptionEntity>
    
    @Query("SELECT * FROM transcriptions WHERE sessionId = :sessionId")
    fun getTranscriptionsBySession(sessionId: String): Flow<List<TranscriptionEntity>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTranscription(transcription: TranscriptionEntity)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTranscriptions(transcriptions: List<TranscriptionEntity>)
    
    @Query("DELETE FROM transcriptions WHERE sessionId = :sessionId")
    suspend fun deleteBySession(sessionId: String)
    
    @Query("DELETE FROM transcriptions")
    suspend fun deleteAll()
    
    @Query("SELECT COUNT(*) FROM transcriptions")
    fun getTranscriptionCount(): Flow<Int>
    
    @Query("SELECT * FROM transcriptions WHERE timestamp >= :startDate AND timestamp < :endDate")
    fun getTranscriptionsByDateRange(startDate: Date, endDate: Date): Flow<List<TranscriptionEntity>>
}

/**
 * Entity representing a transcription record
 */
@Entity(tableName = "transcriptions")
data class TranscriptionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId: String,
    val transcription: String,
    val timestamp: Long,
    val confidence: Float = 0.0f,
    val durationSeconds: Double = 0.0,
    val summary: String? = null,
    val metadata: String? = null
) {
    fun getFormattedTimestamp(): String {
        val format = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        return format.format(Date(timestamp))
    }
}

/**
 * Type converter for Date objects
 */
class Converters {
    @TypeConverter
    fun fromTimestamp(value: Long?): Date? {
        return value?.let { Date(it) }
    }
    
    @TypeConverter
    fun dateToTimestamp(date: Date?): Long? {
        return date?.time
    }
}

/**
 * Repository for transcription operations
 */
class TranscriptionRepository(private val dao: TranscriptionDao) {
    val allTranscriptions: Flow<List<TranscriptionEntity>> = dao.getAllTranscriptions()
    
    suspend fun insert(transcription: TranscriptionEntity) {
        dao.insertTranscription(transcription)
    }
    
    suspend fun insertAll(transcriptions: List<TranscriptionEntity>) {
        dao.insertTranscriptions(transcriptions)
    }
    
    fun getBySession(sessionId: String): Flow<List<TranscriptionEntity>> {
        return dao.getTranscriptionsBySession(sessionId)
    }
    
    suspend fun deleteBySession(sessionId: String) {
        dao.deleteBySession(sessionId)
    }
    
    suspend fun deleteAll() {
        dao.deleteAll()
    }
    
    fun getTranscriptionCount(): Flow<Int> {
        return dao.getTranscriptionCount()
    }
    
    fun getByDateRange(startDate: Date, endDate: Date): Flow<List<TranscriptionEntity>> {
        return dao.getTranscriptionsByDateRange(startDate, endDate)
    }
    
    fun getAllSync(): List<TranscriptionEntity> {
        return dao.getAllTranscriptionsSync()
    }
}

/**
 * Helper to get repository instance
 */
fun getTranscriptionRepository(context: Context): TranscriptionRepository {
    val database = TranscriptionDatabase.getInstance(context)
    return TranscriptionRepository(database.transcriptionDao())
}
