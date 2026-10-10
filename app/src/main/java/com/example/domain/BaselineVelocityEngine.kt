package com.example.domain

import com.example.data.ActivityContext
import com.example.data.BloodPressureReading
import com.example.data.TimeOfDay
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt

data class MovingAverageSummary(
    val systolic: Double,
    val diastolic: Double,
    val heartRate: Double,
    val pulsePressure: Double,
    val meanArterialPressure: Double,
    val count: Int
) {
    val systolicInt: Int get() = systolic.roundToInt()
    val diastolicInt: Int get() = diastolic.roundToInt()
    val heartRateInt: Int get() = heartRate.roundToInt()
    val pulsePressureInt: Int get() = pulsePressure.roundToInt()
    val mapFormatted: String get() = String.format("%.1f", meanArterialPressure)
}

enum class DriftTriageStatus(
    val label: String,
    val summary: String,
    val badgeColorHex: Long
) {
    STABLE(
        "Stable Baseline",
        "Cardiovascular equilibrium maintained (ΔBP < +2 mmHg). No significant vascular drift detected.",
        0xFF10B981 // Green
    ),
    IMPROVING(
        "Downward Trend",
        "Baseline blood pressure is declining (ΔBP < -2 mmHg). Favorable response to lifestyle or medication.",
        0xFF06B6D4 // Teal/Cyan
    ),
    EARLY_DRIFT(
        "Early Baseline Drift",
        "Subtle upward drift detected (+2 to +5 mmHg). Early vascular stiffness or sympathetic activity; lifestyle intervention recommended.",
        0xFFF59E0B // Amber
    ),
    SIGNIFICANT_SHIFT(
        "Requires Attention",
        "Significant upward shift (> +5 mmHg drift). Prompting 7-day clinical home monitoring protocol.",
        0xFFEF4444 // Red
    ),
    INSUFFICIENT_DATA(
        "Calibrating Baseline",
        "Additional logs needed (minimum 5 entries across multiple days) to calculate accurate moving averages.",
        0xFF64748B // Slate
    )
}

data class LifestyleRecommendation(
    val id: String,
    val title: String,
    val category: String,
    val impact: String,
    val description: String,
    val evidenceNote: String
)

data class DriftAnalysis(
    val recent30DayAvg: MovingAverageSummary?,
    val intermediate90DayAvg: MovingAverageSummary?,
    val baseline180DayAvg: MovingAverageSummary?,
    val deltaSystolic: Double,
    val deltaDiastolic: Double,
    val monthlyVelocitySystolic: Double,
    val triageStatus: DriftTriageStatus,
    val morningAvg: MovingAverageSummary?,
    val eveningAvg: MovingAverageSummary?,
    val nonRestingAlertCountRecent: Int,
    val totalReadingsCount: Int,
    val lifestyleTips: List<LifestyleRecommendation>
)

data class ChartDataPoint(
    val timestamp: Long,
    val rawSystolic: Int,
    val rawDiastolic: Int,
    val heartRate: Int,
    val ma30Systolic: Double?,
    val ma90Systolic: Double?,
    val ma180Systolic: Double?,
    val isResting: Boolean,
    val contextTag: String
)

object BaselineVelocityEngine {

    fun analyze(readings: List<BloodPressureReading>): DriftAnalysis {
        if (readings.isEmpty()) {
            return DriftAnalysis(
                recent30DayAvg = null,
                intermediate90DayAvg = null,
                baseline180DayAvg = null,
                deltaSystolic = 0.0,
                deltaDiastolic = 0.0,
                monthlyVelocitySystolic = 0.0,
                triageStatus = DriftTriageStatus.INSUFFICIENT_DATA,
                morningAvg = null,
                eveningAvg = null,
                nonRestingAlertCountRecent = 0,
                totalReadingsCount = 0,
                lifestyleTips = emptyList()
            )
        }

        val now = System.currentTimeMillis()
        val thirtyDaysAgo = now - TimeUnit.DAYS.toMillis(30)
        val ninetyDaysAgo = now - TimeUnit.DAYS.toMillis(90)
        val oneEightyDaysAgo = now - TimeUnit.DAYS.toMillis(180)

        // For true baseline calculations, filter out high stress/post exercise artifact readings if possible,
        // but if user has few resting readings, include all.
        val validRestingReadings = readings.filter { it.activityContext.isIdealForBaseline }
        val pool = if (validRestingReadings.size >= 10) validRestingReadings else readings

        val readings30d = pool.filter { it.timestamp >= thirtyDaysAgo }
        val readings90d = pool.filter { it.timestamp >= ninetyDaysAgo }
        val readings180d = pool.filter { it.timestamp >= oneEightyDaysAgo }

        val avg30d = calculateAverage(readings30d)
        val avg90d = calculateAverage(readings90d)
        val avg180d = calculateAverage(if (readings180d.isNotEmpty()) readings180d else pool)

        // Count non-resting or high stress readings in last 7 days for context validator
        val sevenDaysAgo = now - TimeUnit.DAYS.toMillis(7)
        val nonRestingRecent = readings.count {
            it.timestamp >= sevenDaysAgo && !it.activityContext.isIdealForBaseline
        }

        // Morning vs Evening calculations (from recent 30-day window)
        val recentWindowReadings = readings.filter { it.timestamp >= thirtyDaysAgo }
        val morningReadings = recentWindowReadings.filter { it.timeOfDay == TimeOfDay.MORNING }
        val eveningReadings = recentWindowReadings.filter { it.timeOfDay == TimeOfDay.EVENING || it.timeOfDay == TimeOfDay.NIGHT }

        val morningAvg = calculateAverage(morningReadings)
        val eveningAvg = calculateAverage(eveningReadings)

        if (avg30d == null || avg180d == null || readings.size < 5) {
            return DriftAnalysis(
                recent30DayAvg = avg30d,
                intermediate90DayAvg = avg90d,
                baseline180DayAvg = avg180d,
                deltaSystolic = 0.0,
                deltaDiastolic = 0.0,
                monthlyVelocitySystolic = 0.0,
                triageStatus = DriftTriageStatus.INSUFFICIENT_DATA,
                morningAvg = morningAvg,
                eveningAvg = eveningAvg,
                nonRestingAlertCountRecent = nonRestingRecent,
                totalReadingsCount = readings.size,
                lifestyleTips = getGeneralHeartTips()
            )
        }

        val deltaSystolic = avg30d.systolic - avg180d.systolic
        val deltaDiastolic = avg30d.diastolic - avg180d.diastolic

        // Approximate monthly drift velocity over the 5-month gap between 30d and 180d
        val monthlyVelocity = deltaSystolic / 5.0

        val triageStatus = when {
            deltaSystolic > 5.0 || deltaDiastolic > 4.5 -> DriftTriageStatus.SIGNIFICANT_SHIFT
            deltaSystolic in 2.0..5.0 || deltaDiastolic in 1.5..4.5 -> DriftTriageStatus.EARLY_DRIFT
            deltaSystolic < -2.0 -> DriftTriageStatus.IMPROVING
            else -> DriftTriageStatus.STABLE
        }

        val tips = generateLifestyleTips(triageStatus, deltaSystolic, morningAvg, eveningAvg)

        return DriftAnalysis(
            recent30DayAvg = avg30d,
            intermediate90DayAvg = avg90d,
            baseline180DayAvg = avg180d,
            deltaSystolic = deltaSystolic,
            deltaDiastolic = deltaDiastolic,
            monthlyVelocitySystolic = monthlyVelocity,
            triageStatus = triageStatus,
            morningAvg = morningAvg,
            eveningAvg = eveningAvg,
            nonRestingAlertCountRecent = nonRestingRecent,
            totalReadingsCount = readings.size,
            lifestyleTips = tips
        )
    }

    private fun calculateAverage(list: List<BloodPressureReading>): MovingAverageSummary? {
        if (list.isEmpty()) return null
        val totalS = list.sumOf { it.systolic }
        val totalD = list.sumOf { it.diastolic }
        val totalHr = list.sumOf { it.heartRate }
        val totalPp = list.sumOf { it.pulsePressure }
        val totalMap = list.sumOf { it.meanArterialPressure }
        val n = list.size.toDouble()

        return MovingAverageSummary(
            systolic = totalS / n,
            diastolic = totalD / n,
            heartRate = totalHr / n,
            pulsePressure = totalPp / n,
            meanArterialPressure = totalMap / n,
            count = list.size
        )
    }

    private fun generateLifestyleTips(
        status: DriftTriageStatus,
        deltaSystolic: Double,
        morningAvg: MovingAverageSummary?,
        eveningAvg: MovingAverageSummary?
    ): List<LifestyleRecommendation> {
        val list = mutableListOf<LifestyleRecommendation>()

        // Check morning surge
        if (morningAvg != null && eveningAvg != null && (morningAvg.systolic - eveningAvg.systolic) > 12) {
            list.add(
                LifestyleRecommendation(
                    id = "morning_surge",
                    title = "Morning Blood Pressure Surge Detected",
                    category = "Circadian Rhythm & Sleep",
                    impact = "-3 to -6 mmHg morning reduction",
                    description = "Your morning readings average >12 mmHg higher than evening logs. Evaluate obstructive sleep apnea screening, avoid caffeine within 60 mins of waking, and take morning medications exactly as prescribed.",
                    evidenceNote = "AHA guidelines highlight morning surges as a distinct cardiovascular risk marker."
                )
            )
        }

        when (status) {
            DriftTriageStatus.SIGNIFICANT_SHIFT -> {
                list.add(
                    LifestyleRecommendation(
                        id = "clinical_protocol",
                        title = "Initiate 7-Day Clinical Home Monitoring",
                        category = "Clinical Action",
                        impact = "Diagnostic Confirmation",
                        description = "Log two readings each morning and two each evening for 7 consecutive days while fully seated. Export the compiled clinical PDF/text summary to your primary care physician.",
                        evidenceNote = "Gold standard home BP protocol recommended by ACC/AHA 2017 guidelines."
                    )
                )
                list.add(
                    LifestyleRecommendation(
                        id = "sodium_potassium",
                        title = "Target Dietary Sodium to < 1,500 mg/day",
                        category = "Nutrition (DASH)",
                        impact = "-5 to -8 mmHg systolic reduction",
                        description = "Reduce processed foods and prioritize dietary potassium (spinach, avocados, white beans, sweet potatoes) to promote endothelial vasodilation.",
                        evidenceNote = "DASH diet clinical trials demonstrated systolic drops within 14 days."
                    )
                )
                list.add(
                    LifestyleRecommendation(
                        id = "zone2_cardio",
                        title = "Zone 2 Low-Intensity Aerobic Conditioning",
                        category = "Vascular Exercise",
                        impact = "-4 to -7 mmHg baseline reduction",
                        description = "150 minutes weekly of conversational brisk walking, cycling, or swimming. Lowers peripheral vascular resistance without hypertensive cortisol spikes.",
                        evidenceNote = "Enhances nitric oxide production and arterial compliance."
                    )
                )
            }
            DriftTriageStatus.EARLY_DRIFT -> {
                list.add(
                    LifestyleRecommendation(
                        id = "sodium_nudge",
                        title = "Subtle Sodium Modulation & Hydration",
                        category = "Dietary Micro-habit",
                        impact = "-2 to -4 mmHg systolic drift reversal",
                        description = "Audit hidden sodium in restaurant dining, bread, and condiments. Drink 2–2.5L water daily to maintain consistent renal filtration.",
                        evidenceNote = "Early vascular drift is frequently sensitive to subclinical fluid retention."
                    )
                )
                list.add(
                    LifestyleRecommendation(
                        id = "sleep_extension",
                        title = "Sleep Consistency & Parasympathetic Tone",
                        category = "Rest & Recovery",
                        impact = "-3 mmHg nocturnal dip restoration",
                        description = "Aim for 7.5 to 8.5 hours in a cool, dark room. Elevated baseline drift often reflects chronic autonomic sympathetic overdrive or short sleep duration.",
                        evidenceNote = "Sleep deficiency blunts nocturnal dipping by 40%."
                    )
                )
                list.add(
                    LifestyleRecommendation(
                        id = "stress_respiration",
                        title = "Resonant 4-7-8 Breathing (5 min daily)",
                        category = "Autonomic Regulation",
                        impact = "-2 to -5 mmHg acute and chronic stabilization",
                        description = "Slow diaphragmatic breathing at 6 breaths per minute resets baroreceptor sensitivity and downregulates renin-angiotensin tone.",
                        evidenceNote = "Clinically validated for baroreflex modulation."
                    )
                )
            }
            else -> {
                list.addAll(getGeneralHeartTips())
            }
        }

        return list
    }

    private fun getGeneralHeartTips(): List<LifestyleRecommendation> = listOf(
        LifestyleRecommendation(
            id = "consistent_cuff",
            title = "Strict Measurement Protocol",
            category = "Tracking Best Practice",
            impact = "Ensures clean moving averages",
            description = "Empty your bladder, sit quietly with back supported and feet uncrossed for 5 minutes before pressing start. Keep cuff at exact heart level.",
            evidenceNote = "Measurement errors account for up to 10 mmHg artificial variance."
        ),
        LifestyleRecommendation(
            id = "dash_balance",
            title = "Maintain DASH Nutrient Density",
            category = "Preventative Nutrition",
            impact = "Preserves healthy baseline elasticity",
            description = "Continue eating high-fiber whole grains, leafy greens, nuts, and berries with natural magnesium and polyphenols.",
            evidenceNote = "Long-term vascular protection and arterial compliance."
        )
    )

    fun computeChartPoints(readings: List<BloodPressureReading>): List<ChartDataPoint> {
        if (readings.isEmpty()) return emptyList()

        // Sort chronologically (oldest to newest)
        val sorted = readings.sortedBy { it.timestamp }
        val points = mutableListOf<ChartDataPoint>()

        for (i in sorted.indices) {
            val current = sorted[i]
            val currentTime = current.timestamp

            val t30 = currentTime - TimeUnit.DAYS.toMillis(30)
            val t90 = currentTime - TimeUnit.DAYS.toMillis(90)
            val t180 = currentTime - TimeUnit.DAYS.toMillis(180)

            // Calculate moving averages up to this point in time
            val window30 = sorted.filter { it.timestamp in t30..currentTime && it.activityContext.isIdealForBaseline }
            val window90 = sorted.filter { it.timestamp in t90..currentTime && it.activityContext.isIdealForBaseline }
            val window180 = sorted.filter { it.timestamp in t180..currentTime && it.activityContext.isIdealForBaseline }

            val ma30 = if (window30.isNotEmpty()) window30.map { it.systolic }.average() else null
            val ma90 = if (window90.isNotEmpty()) window90.map { it.systolic }.average() else null
            val ma180 = if (window180.isNotEmpty()) window180.map { it.systolic }.average() else null

            points.add(
                ChartDataPoint(
                    timestamp = currentTime,
                    rawSystolic = current.systolic,
                    rawDiastolic = current.diastolic,
                    heartRate = current.heartRate,
                    ma30Systolic = ma30,
                    ma90Systolic = ma90,
                    ma180Systolic = ma180,
                    isResting = current.activityContext.isIdealForBaseline,
                    contextTag = current.activityContext.displayName
                )
            )
        }

        return points
    }
}
