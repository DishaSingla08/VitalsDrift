package com.example.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface MeasurementDao {

    @Query("SELECT * FROM measurements ORDER BY timestamp DESC")
    fun getAllLogs(): Flow<List<MeasurementEntity>>

    @Query("SELECT * FROM measurements WHERE isQuarantined = 0 ORDER BY timestamp DESC")
    fun getCleanRestingLogs(): Flow<List<MeasurementEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: MeasurementEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllLogs(logs: List<MeasurementEntity>)

    @Delete
    suspend fun deleteLog(log: MeasurementEntity)

    @Query("DELETE FROM measurements WHERE id = :id")
    suspend fun deleteLogById(id: Long)

    @Query("DELETE FROM measurements")
    suspend fun clearAllLogs()

    @Query("SELECT COUNT(*) FROM measurements")
    suspend fun getCount(): Int

    @Query("SELECT COUNT(*) FROM measurements WHERE isQuarantined = 1")
    fun getQuarantinedCount(): Flow<Int>
}
