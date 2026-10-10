package com.example.data

import java.util.concurrent.TimeUnit
import kotlin.random.Random

object SampleDataSeeder {
    fun generate180DayDriftDataset(): List<BloodPressureReading> {
        val readings = mutableListOf<BloodPressureReading>()
        val now = System.currentTimeMillis()
        val random = Random(42) // deterministic seed for realistic repeatable medical data

        // Generate ~120-140 readings over 180 days (approx morning and evening logs every 1-3 days)
        for (day in 180 downTo 0) {
            // Generate roughly 1 to 2 readings on selected days
            val shouldLogMorning = random.nextFloat() < 0.85f
            val shouldLogEvening = random.nextFloat() < 0.60f

            // Baseline drift curve progression:
            // Day 180 (earliest): Baseline ~ 118 SBP, 75 DBP
            // Day 90: Baseline ~ 121 SBP, 78 DBP
            // Day 30 to Day 0 (recent month): Baseline ~ 127 SBP, 83 DBP
            val driftFactor = (180 - day) / 180.0 // 0.0 at day 180, 1.0 at day 0
            val baselineSystolic = 118.0 + (driftFactor * 9.0) // 118 -> 127
            val baselineDiastolic = 75.0 + (driftFactor * 8.0) // 75 -> 83

            val dayMillis = now - TimeUnit.DAYS.toMillis(day.toLong())

            if (shouldLogMorning) {
                // Morning reading: typically lower or standard waking
                val noiseS = random.nextInt(-4, 5)
                val noiseD = random.nextInt(-3, 4)
                val hr = 66 + random.nextInt(-6, 8)
                val timestamp = dayMillis + TimeUnit.HOURS.toMillis(8) + TimeUnit.MINUTES.toMillis(random.nextLong(0, 45))

                // Occasional morning rush stress or coffee
                val context = if (day % 14 == 0) ActivityContext.CAFFEINE_NICOTINE else ActivityContext.RESTING

                readings.add(
                    BloodPressureReading(
                        systolic = (baselineSystolic + noiseS).toInt().coerceIn(100, 160),
                        diastolic = (baselineDiastolic + noiseD).toInt().coerceIn(60, 100),
                        heartRate = hr.coerceIn(55, 105),
                        timestamp = timestamp,
                        activityContext = context,
                        timeOfDay = TimeOfDay.MORNING,
                        arm = MeasurementArm.LEFT,
                        notes = if (day % 14 == 0) "Measured right after black coffee" else "Morning resting reading"
                    )
                )
            }

            if (shouldLogEvening) {
                // Evening reading: sometimes elevated after work stress or gym
                val isPostExercise = day % 7 == 3
                val isStressed = day % 11 == 2
                val context = when {
                    isPostExercise -> ActivityContext.POST_EXERCISE
                    isStressed -> ActivityContext.STRESSED
                    else -> ActivityContext.RESTING
                }

                val extraS = if (isPostExercise) 8 else if (isStressed) 6 else 0
                val extraD = if (isPostExercise) 3 else if (isStressed) 4 else 0

                val noiseS = random.nextInt(-3, 5)
                val noiseD = random.nextInt(-2, 4)
                val hr = 74 + random.nextInt(-5, 12) + (if (isPostExercise) 16 else 0)
                val timestamp = dayMillis + TimeUnit.HOURS.toMillis(19) + TimeUnit.MINUTES.toMillis(random.nextLong(0, 50))

                readings.add(
                    BloodPressureReading(
                        systolic = (baselineSystolic + extraS + noiseS).toInt().coerceIn(105, 165),
                        diastolic = (baselineDiastolic + extraD + noiseD).toInt().coerceIn(65, 105),
                        heartRate = hr.coerceIn(58, 125),
                        timestamp = timestamp,
                        activityContext = context,
                        timeOfDay = TimeOfDay.EVENING,
                        arm = if (day % 5 == 0) MeasurementArm.RIGHT else MeasurementArm.LEFT,
                        notes = when {
                            isPostExercise -> "Within 25 min of brisk walk / jog"
                            isStressed -> "Felt work deadline tension"
                            else -> "Evening seated reading"
                        }
                    )
                )
            }
        }

        return readings.sortedByDescending { it.timestamp }
    }
}
