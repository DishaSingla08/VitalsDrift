package com.example.data

import java.util.concurrent.TimeUnit

data class WorkoutSessionRecord(
    val id: String,
    val exerciseType: String,
    val startTime: Long,
    val endTime: Long,
    val avgHeartRate: Int
) {
    fun isWithin30Minutes(readingTimestamp: Long): Boolean {
        val thirtyMinMillis = TimeUnit.MINUTES.toMillis(30)
        // Reading taken during workout or within 30 minutes after workout concluded
        return readingTimestamp in (startTime - TimeUnit.MINUTES.toMillis(5))..(endTime + thirtyMinMillis)
    }
}

data class WearableHealthSnapshot(
    val lastSyncTimestamp: Long = System.currentTimeMillis(),
    val restingHeartRate: Int = 63,
    val heartRateVariabilityMs: Double = 27.8, // Correlated with sympathetically driven drift (<30ms)
    val sleepDurationHours: Double = 5.5, // Correlated with blunted nocturnal dipping (<6.0h)
    val dailySteps: Int = 8420,
    val activeMinutes: Int = 45,
    val connectedDeviceName: String = "Android Health Connect (Wear OS)",
    val isConnected: Boolean = true,
    val recentWorkouts: List<WorkoutSessionRecord> = emptyList()
) {
    val isLowHrvRisk: Boolean get() = heartRateVariabilityMs < 30.0
    val isShortSleepRisk: Boolean get() = sleepDurationHours < 6.0

    val plainEnglishRiskSummary: String
        get() = when {
            isLowHrvRisk && isShortSleepRisk -> "Short sleep (5.5 hrs) and reduced autonomic recovery (HRV 28ms) correlate with your upward vascular drift."
            isLowHrvRisk -> "Low autonomic HRV (<30ms) indicates sympathetic nervous system dominance, increasing vascular tone."
            isShortSleepRisk -> "Sub-6-hour sleep duration limits natural nocturnal dipping, elevating next-day baseline averages."
            else -> "Wearable vitals indicate optimal autonomic recovery and sleep balance."
        }
}

object HealthConnectManager {

    fun generateRealisticWearableSnapshot(): WearableHealthSnapshot {
        val now = System.currentTimeMillis()

        // Generate realistic workout sessions over the last 48 hours for activity auto-tagging
        val workouts = listOf(
            WorkoutSessionRecord(
                id = "workout_1",
                exerciseType = "HIIT / Resistance Training",
                startTime = now - TimeUnit.HOURS.toMillis(3) - TimeUnit.MINUTES.toMillis(40),
                endTime = now - TimeUnit.HOURS.toMillis(3),
                avgHeartRate = 138
            ),
            WorkoutSessionRecord(
                id = "workout_2",
                exerciseType = "Brisk Cardio / Treadmill",
                startTime = now - TimeUnit.HOURS.toMillis(26),
                endTime = now - TimeUnit.HOURS.toMillis(25) - TimeUnit.MINUTES.toMillis(15),
                avgHeartRate = 142
            )
        )

        return WearableHealthSnapshot(
            lastSyncTimestamp = now,
            restingHeartRate = 63,
            heartRateVariabilityMs = 27.4,
            sleepDurationHours = 5.7,
            dailySteps = 8910,
            activeMinutes = 45,
            connectedDeviceName = "Health Connect (Wear OS Sync)",
            isConnected = true,
            recentWorkouts = workouts
        )
    }

    /**
     * Contextual Activity Auto-Tagging:
     * Check if a reading timestamp overlaps with a workout or occurs within 30 minutes of finish.
     */
    fun checkAutoTaggingPostExercise(
        readingTimestamp: Long,
        workouts: List<WorkoutSessionRecord>
    ): Pair<Boolean, WorkoutSessionRecord?> {
        for (w in workouts) {
            if (w.isWithin30Minutes(readingTimestamp)) {
                return Pair(true, w)
            }
        }
        return Pair(false, null)
    }
}
