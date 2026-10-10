package com.example.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class MeasurementRepository(private val dao: MeasurementDao) {

    val allLogs: Flow<List<MeasurementEntity>> = dao.getAllLogs()
    val cleanRestingLogs: Flow<List<MeasurementEntity>> = dao.getCleanRestingLogs()
    val quarantinedCount: Flow<Int> = dao.getQuarantinedCount()

    suspend fun insertLog(log: MeasurementEntity): Long = withContext(Dispatchers.IO) {
        dao.insertLog(log)
    }

    suspend fun deleteLogById(id: Long) = withContext(Dispatchers.IO) {
        dao.deleteLogById(id)
    }

    suspend fun clearAllLogs() = withContext(Dispatchers.IO) {
        dao.clearAllLogs()
    }

    suspend fun seedSampleData() = withContext(Dispatchers.IO) {
        val sample = SampleDataSeeder.generate180DayDriftDataset()
        dao.clearAllLogs()
        dao.insertAllLogs(sample)
    }

    suspend fun checkAndSeedInitialIfEmpty() = withContext(Dispatchers.IO) {
        val count = dao.getCount()
        if (count == 0) {
            val sample = SampleDataSeeder.generate180DayDriftDataset()
            dao.insertAllLogs(sample)
        }
    }
}
