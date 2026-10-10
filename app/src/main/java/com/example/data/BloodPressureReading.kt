package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class ActivityContext(val displayName: String, val isIdealForBaseline: Boolean) {
    RESTING("Resting (Seated > 5 min)", true),
    POST_EXERCISE("Post-Exercise (< 30 min)", false),
    STRESSED("High Stress / Anxious", false),
    POST_MEDICATION("Post-Medication", true),
    CAFFEINE_NICOTINE("Recent Caffeine / Nicotine", false)
}

enum class TimeOfDay(val displayName: String) {
    MORNING("Morning (Waking / AM)",),
    MIDDAY("Midday (Noon / PM)"),
    EVENING("Evening (Pre-Dinner)",),
    NIGHT("Night (Before Bed)")
}

enum class MeasurementArm(val displayName: String) {
    LEFT("Left Arm"),
    RIGHT("Right Arm")
}

enum class BpCategory(val label: String, val description: String) {
    NORMAL("Normal", "Systolic < 120 and Diastolic < 80 mmHg"),
    ELEVATED("Elevated", "Systolic 120-129 and Diastolic < 80 mmHg"),
    STAGE_1("Stage 1 Hypertension", "Systolic 130-139 or Diastolic 80-89 mmHg"),
    STAGE_2("Stage 2 Hypertension", "Systolic 140+ or Diastolic 90+ mmHg"),
    CRISIS("Hypertensive Crisis", "Systolic > 180 and/or Diastolic > 120 mmHg")
}

@Entity(tableName = "blood_pressure_readings")
data class BloodPressureReading(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val systolic: Int,
    val diastolic: Int,
    val heartRate: Int,
    val timestamp: Long = System.currentTimeMillis(),
    val activityContext: ActivityContext = ActivityContext.RESTING,
    val timeOfDay: TimeOfDay = TimeOfDay.MORNING,
    val arm: MeasurementArm = MeasurementArm.LEFT,
    val notes: String = ""
) {
    val pulsePressure: Int
        get() = systolic - diastolic

    val meanArterialPressure: Double
        get() = diastolic + (pulsePressure / 3.0)

    val category: BpCategory
        get() = when {
            systolic > 180 || diastolic > 120 -> BpCategory.CRISIS
            systolic >= 140 || diastolic >= 90 -> BpCategory.STAGE_2
            systolic in 130..139 || diastolic in 80..89 -> BpCategory.STAGE_1
            systolic in 120..129 && diastolic < 80 -> BpCategory.ELEVATED
            else -> BpCategory.NORMAL
        }
}
