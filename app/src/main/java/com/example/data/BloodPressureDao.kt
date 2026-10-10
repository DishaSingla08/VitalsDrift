package com.example.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface BloodPressureDao {
    @Query("SELECT * FROM blood_pressure_readings ORDER BY timestamp DESC")
    fun getAllReadings(): Flow<List<BloodPressureReading>>

    @Query("SELECT * FROM blood_pressure_readings WHERE timestamp >= :sinceTimestamp ORDER BY timestamp ASC")
    fun getReadingsSince(sinceTimestamp: Long): Flow<List<BloodPressureReading>>

    @Query("SELECT COUNT(*) FROM blood_pressure_readings")
    suspend fun getCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(reading: BloodPressureReading): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(readings: List<BloodPressureReading>)

    @Update
    suspend fun update(reading: BloodPressureReading)

    @Delete
    suspend fun delete(reading: BloodPressureReading)

    @Query("DELETE FROM blood_pressure_readings WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM blood_pressure_readings")
    suspend fun clearAll()
}
