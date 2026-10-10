package com.example.domain

import com.example.data.MeasurementEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.TimeUnit

class BaselineVelocityEngineTest {

    @Test
    fun `empty logs return insufficient data`() {
        val analysis = BaselineVelocityEngine.analyze(emptyList())
        assertEquals(DriftTriageStatus.INSUFFICIENT_DATA, analysis.triageStatus)
        assertEquals(0, analysis.totalReadingsCount)
    }

    @Test
    fun `early drift is flagged when delta is between 2 and 5 mmHg`() {
        val now = System.currentTimeMillis()
        val logs = mutableListOf<MeasurementEntity>()

        // 180 days ago: Baseline around 120/80
        for (i in 40..170 step 10) {
            logs.add(
                MeasurementEntity(
                    id = i.toLong(),
                    systolic = 120,
                    diastolic = 80,
                    heartRate = 65,
                    timestamp = now - TimeUnit.DAYS.toMillis(i.toLong()),
                    contextTag = "RESTING",
                    isQuarantined = false
                )
            )
        }

        // Recent 30 days: Shifted up to 123.5 / 82 (drift of +3.5 mmHg)
        for (i in 1..25 step 3) {
            logs.add(
                MeasurementEntity(
                    id = i.toLong(),
                    systolic = 124,
                    diastolic = 82,
                    heartRate = 68,
                    timestamp = now - TimeUnit.DAYS.toMillis(i.toLong()),
                    contextTag = "RESTING",
                    isQuarantined = false
                )
            )
        }

        val analysis = BaselineVelocityEngine.analyze(logs)
        assertNotNull(analysis.recent30DayAvg)
        assertNotNull(analysis.baseline180DayAvg)
        assertTrue("Delta systolic should be between 2 and 5", analysis.deltaSystolic in 2.0..5.0)
        assertEquals(DriftTriageStatus.EARLY_DRIFT, analysis.triageStatus)
    }

    @Test
    fun `quarantined entries are filtered from clean baseline calculations`() {
        val now = System.currentTimeMillis()
        val logs = mutableListOf<MeasurementEntity>()

        // Clean logs at 120/80
        for (i in 1..10) {
            logs.add(
                MeasurementEntity(
                    id = i.toLong(),
                    systolic = 120,
                    diastolic = 80,
                    heartRate = 65,
                    timestamp = now - TimeUnit.DAYS.toMillis(i.toLong()),
                    contextTag = "RESTING",
                    isQuarantined = false
                )
            )
        }

        // Post-exercise spiked log that was quarantined
        logs.add(
            MeasurementEntity(
                id = 99L,
                systolic = 165,
                diastolic = 95,
                heartRate = 140,
                timestamp = now - TimeUnit.HOURS.toMillis(2),
                contextTag = "POST_EXERCISE",
                isQuarantined = true
            )
        )

        val analysis = BaselineVelocityEngine.analyze(logs)
        assertEquals(1, analysis.quarantinedCount)
        assertEquals(10, analysis.cleanReadingsCount)
        // Since clean logs were used, the average systolic should be 120, not distorted by 165
        assertEquals(120.0, analysis.recent30DayAvg?.systolic ?: 0.0, 0.5)
    }
}
