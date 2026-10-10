package com.example.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class BloodPressureRepository(private val dao: BloodPressureDao) {

    val allReadings: Flow<List<BloodPressureReading>> = dao.getAllReadings()

    fun getReadingsSince(sinceTimestamp: Long): Flow<List<BloodPressureReading>> =
        dao.getReadingsSince(sinceTimestamp)

    suspend fun insert(reading: BloodPressureReading): Long = withContext(Dispatchers.IO) {
        dao.insert(reading)
    }

    suspend fun deleteById(id: Long) = withContext(Dispatchers.IO) {
        dao.deleteById(id)
    }

    suspend fun clearAll() = withContext(Dispatchers.IO) {
        dao.clearAll()
    }

    suspend fun seedSampleData() = withContext(Dispatchers.IO) {
        val sample = SampleDataSeeder.generate180DayDriftDataset()
        dao.clearAll()
        dao.insertAll(sample)
    }

    suspend fun checkAndSeedInitialIfEmpty() = withContext(Dispatchers.IO) {
        val count = dao.getCount()
        if (count == 0) {
            val sample = SampleDataSeeder.generate180DayDriftDataset()
            dao.insertAll(sample)
        }
    }
}
