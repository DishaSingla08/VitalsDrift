package com.example.data

import java.util.concurrent.TimeUnit
import kotlin.random.Random

object SampleDataSeeder {

    fun generate180DayDriftDataset(): List<MeasurementEntity> {
        val readings = mutableListOf<MeasurementEntity>()
        val now = System.currentTimeMillis()
        val random = Random(42) // deterministic seed for repeatable medical modeling

        // Generate ~120 readings over 180 days with realistic multi-month upward drift
        for (day in 180 downTo 0) {
            val shouldLogMorning = random.nextFloat() < 0.75f
            val shouldLogEvening = random.nextFloat() < 0.55f

            // Baseline drift progression:
            // Day 180 (6 months ago): Baseline ~ 118 SBP, 75 DBP
            // Day 90 (3 months ago): Baseline ~ 121 SBP, 78 DBP
            // Day 30 to Day 0 (recent 30 days): Baseline ~ 126.5 SBP, 82.5 DBP (giving +3.5 mmHg drift)
            val driftFactor = (180 - day) / 180.0
            val baselineSystolic = 118.0 + (driftFactor * 8.8)
            val baselineDiastolic = 75.0 + (driftFactor * 7.5)

            val dayMillis = now - TimeUnit.DAYS.toMillis(day.toLong())

            if (shouldLogMorning) {
                val noiseS = random.nextInt(-3, 4)
                val noiseD = random.nextInt(-2, 3)
                val hr = 65 + random.nextInt(-5, 7)
                val timestamp = dayMillis + TimeUnit.HOURS.toMillis(8) + TimeUnit.MINUTES.toMillis(random.nextLong(0, 45))

                // Occasional morning rush coffee stress
                val isCaffeine = day % 15 == 0
                val contextTag = if (isCaffeine) "CAFFEINE_STRESS" else "RESTING"

                readings.add(
                    MeasurementEntity(
                        systolic = (baselineSystolic + noiseS + if (isCaffeine) 6 else 0).toInt().coerceIn(100, 160),
                        diastolic = (baselineDiastolic + noiseD + if (isCaffeine) 3 else 0).toInt().coerceIn(60, 100),
                        heartRate = hr.coerceIn(55, 105),
                        timestamp = timestamp,
                        contextTag = contextTag,
                        isQuarantined = isCaffeine,
                        notes = if (isCaffeine) "Measured right after black espresso" else "Morning resting baseline reading"
                    )
                )
            }

            if (shouldLogEvening) {
                val isPostExercise = day % 7 == 3
                val isStress = day % 11 == 2
                val contextTag = when {
                    isPostExercise -> "POST_EXERCISE"
                    isStress -> "CAFFEINE_STRESS"
                    else -> "RESTING"
                }
                val isQuarantined = isPostExercise || isStress

                val extraS = if (isPostExercise) 9 else if (isStress) 5 else 0
                val extraD = if (isPostExercise) 4 else if (isStress) 3 else 0

                val noiseS = random.nextInt(-3, 4)
                val noiseD = random.nextInt(-2, 3)
                val hr = 72 + random.nextInt(-4, 10) + (if (isPostExercise) 16 else 0)
                val timestamp = dayMillis + TimeUnit.HOURS.toMillis(19) + TimeUnit.MINUTES.toMillis(random.nextLong(0, 50))

                readings.add(
                    MeasurementEntity(
                        systolic = (baselineSystolic + extraS + noiseS).toInt().coerceIn(105, 165),
                        diastolic = (baselineDiastolic + extraD + noiseD).toInt().coerceIn(65, 105),
                        heartRate = hr.coerceIn(58, 125),
                        timestamp = timestamp,
                        contextTag = contextTag,
                        isQuarantined = isQuarantined,
                        notes = when {
                            isPostExercise -> "Within 20 min of 5k jog (Auto-Quarantined)"
                            isStress -> "High work deadline stress"
                            else -> "Evening seated reading"
                        }
                    )
                )
            }
        }

        return readings.sortedByDescending { it.timestamp }
    }
}
