package com.example.domain

import com.example.data.BpCategory
import com.example.data.MeasurementEntity
import java.util.Calendar
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
        "Cardiovascular equilibrium maintained (ΔBP < +2.0 mmHg). No significant vascular drift detected.",
        0xFF10B981 // Green
    ),
    IMPROVING(
        "Downward Trend",
        "Baseline blood pressure is declining (ΔBP < -2.0 mmHg). Favorable vascular elasticity.",
        0xFF06B6D4 // Teal/Cyan
    ),
    EARLY_DRIFT(
        "Early Baseline Drift",
        "Subtle upward drift detected (+2.0 to +5.0 mmHg). Early action window open to prevent permanent arterial stiffness.",
        0xFFF59E0B // Amber
    ),
    SIGNIFICANT_SHIFT(
        "Significant Shift",
        "Significant upward shift (> +5.0 mmHg drift). Recommending 7-day home clinical monitoring protocol.",
        0xFFEF4444 // Red
    ),
    INSUFFICIENT_DATA(
        "Calibrating Baseline",
        "Additional logs needed (minimum 3 entries) to calculate reliable multi-month moving averages.",
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
    val cleanReadingsCount: Int,
    val quarantinedCount: Int,
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

    fun analyze(logs: List<MeasurementEntity>): DriftAnalysis {
        if (logs.isEmpty()) {
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
                cleanReadingsCount = 0,
                quarantinedCount = 0,
                lifestyleTips = emptyList()
            )
        }

        val now = System.currentTimeMillis()
        val thirtyDaysAgo = now - TimeUnit.DAYS.toMillis(30)
        val ninetyDaysAgo = now - TimeUnit.DAYS.toMillis(90)
        val oneEightyDaysAgo = now - TimeUnit.DAYS.toMillis(180)

        // Clean logs: exclude quarantined (isQuarantined == true) to prevent post-workout artifacts
        val cleanLogs = logs.filter { !it.isQuarantined }
        val quarantinedTotal = logs.count { it.isQuarantined }

        val pool = if (cleanLogs.size >= 5) cleanLogs else logs

        val logs30d = pool.filter { it.timestamp >= thirtyDaysAgo }
        val logs90d = pool.filter { it.timestamp >= ninetyDaysAgo }
        val logs180d = pool.filter { it.timestamp >= oneEightyDaysAgo }

        val avg30d = calculateAverage(logs30d)
        val avg90d = calculateAverage(logs90d)
        val avg180d = calculateAverage(if (logs180d.isNotEmpty()) logs180d else pool)

        // Quarantined / non-resting count in last 7 days
        val sevenDaysAgo = now - TimeUnit.DAYS.toMillis(7)
        val nonRestingRecent = logs.count {
            it.timestamp >= sevenDaysAgo && it.isQuarantined
        }

        // Morning vs Evening calculations (Morning: 5AM-12PM, Evening: 5PM-11PM)
        val cal = Calendar.getInstance()
        val logsRecentWindow = logs.filter { it.timestamp >= thirtyDaysAgo }
        val morningLogs = logsRecentWindow.filter {
            cal.timeInMillis = it.timestamp
            val hour = cal.get(Calendar.HOUR_OF_DAY)
            hour in 5..11
        }
        val eveningLogs = logsRecentWindow.filter {
            cal.timeInMillis = it.timestamp
            val hour = cal.get(Calendar.HOUR_OF_DAY)
            hour in 17..23
        }

        val morningAvg = calculateAverage(morningLogs)
        val eveningAvg = calculateAverage(eveningLogs)

        if (avg30d == null || avg180d == null) {
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
                totalReadingsCount = logs.size,
                cleanReadingsCount = cleanLogs.size,
                quarantinedCount = quarantinedTotal,
                lifestyleTips = getGeneralHeartTips()
            )
        }

        val deltaSystolic = avg30d.systolic - avg180d.systolic
        val deltaDiastolic = avg30d.diastolic - avg180d.diastolic
        val monthlyVelocity = deltaSystolic / 5.0

        val triageStatus = when {
            deltaSystolic > 5.0 -> DriftTriageStatus.SIGNIFICANT_SHIFT
            deltaSystolic in 2.0..5.0 -> DriftTriageStatus.EARLY_DRIFT
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
            totalReadingsCount = logs.size,
            cleanReadingsCount = cleanLogs.size,
            quarantinedCount = quarantinedTotal,
            lifestyleTips = tips
        )
    }

    private fun calculateAverage(list: List<MeasurementEntity>): MovingAverageSummary? {
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

        if (morningAvg != null && eveningAvg != null && (morningAvg.systolic - eveningAvg.systolic) > 12) {
            list.add(
                LifestyleRecommendation(
                    id = "morning_surge",
                    title = "Morning Blood Pressure Surge Detected",
                    category = "Circadian Rhythm & Sleep",
                    impact = "-3 to -6 mmHg morning reduction",
                    description = "Your morning readings average >12 mmHg higher than evening logs. Evaluate obstructive sleep apnea screening, avoid caffeine within 60 mins of waking, and take morning medications consistently.",
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
                        description = "Log two readings each morning and two each evening for 7 consecutive days while seated. Export the compiled clinical PDF summary to your physician.",
                        evidenceNote = "Gold standard home BP protocol recommended by ACC/AHA 2017 guidelines."
                    )
                )
                list.add(
                    LifestyleRecommendation(
                        id = "sodium_potassium",
                        title = "Cap Dietary Sodium to < 1,500 mg/day",
                        category = "Nutrition (DASH)",
                        impact = "-5 to -8 mmHg systolic reduction",
                        description = "Reduce processed foods and prioritize dietary potassium (spinach, avocados, white beans) to promote endothelial vasodilation.",
                        evidenceNote = "DASH diet clinical trials demonstrated systolic drops within 14 days."
                    )
                )
            }
            DriftTriageStatus.EARLY_DRIFT -> {
                list.add(
                    LifestyleRecommendation(
                        id = "sodium_reduction",
                        title = "Modest Sodium Reduction (< 2,000 mg/day)",
                        category = "Nutrition",
                        impact = "-3 to -5 mmHg baseline stabilization",
                        description = "Subtle drift is highly sensitive to sodium intake. Audit packaged broths and takeout meals over the next 2 weeks.",
                        evidenceNote = "Cochrane review: moderate sodium reduction halts pre-hypertensive trajectory."
                    )
                )
                list.add(
                    LifestyleRecommendation(
                        id = "zone2_exercise",
                        title = "Zone 2 Cardiovascular Aerobic Conditioning",
                        category = "Exercise Physiology",
                        impact = "-4 to -6 mmHg velocity reduction",
                        description = "Perform 20–30 minutes of conversational walking, swimming, or stationary cycling 4 times per week to stimulate endothelial nitric oxide.",
                        evidenceNote = "Low-intensity aerobic exercise enhances vascular distensibility without cortisol surges."
                    )
                )
                list.add(
                    LifestyleRecommendation(
                        id = "sleep_hygiene",
                        title = "Restore Sleep Duration & Nocturnal Dip",
                        category = "Sleep Architecture",
                        impact = "-2 to -4 mmHg nocturnal normalization",
                        description = "Target >7.5 hours of uninterrupted sleep. Avoid blue screen light 45 min prior to sleep to support natural nocturnal dipping.",
                        evidenceNote = "Blunted nocturnal dipping is closely correlated with progressive 180-day baseline drift."
                    )
                )
            }
            DriftTriageStatus.STABLE, DriftTriageStatus.IMPROVING -> {
                list.add(
                    LifestyleRecommendation(
                        id = "maintenance",
                        title = "Vascular Elasticity Maintenance",
                        category = "Longevity",
                        impact = "Baseline Stability",
                        description = "Your multi-month moving average velocity is stable within +2 mmHg. Continue current hydration, regular activity, and resting measurement habits.",
                        evidenceNote = "Consistency in resting measurements provides the highest diagnostic accuracy."
                    )
                )
            }
            DriftTriageStatus.INSUFFICIENT_DATA -> {
                list.addAll(getGeneralHeartTips())
            }
        }

        return list
    }

    private fun getGeneralHeartTips(): List<LifestyleRecommendation> {
        return listOf(
            LifestyleRecommendation(
                id = "baseline_calibration",
                title = "Calibrate Your Baseline",
                category = "Protocol",
                impact = "High Diagnostic Accuracy",
                description = "Measure seated with feet flat on the floor after 5 minutes of quiet rest. Support your arm at heart level.",
                evidenceNote = "Improper posture can elevate systolic readings by 6–10 mmHg."
            )
        )
    }

    fun computeChartPoints(logs: List<MeasurementEntity>): List<ChartDataPoint> {
        if (logs.isEmpty()) return emptyList()

        val sortedLogs = logs.sortedBy { it.timestamp }
        val points = mutableListOf<ChartDataPoint>()

        for (i in sortedLogs.indices) {
            val current = sortedLogs[i]
            val timestamp = current.timestamp

            val thirtyDaysCutoff = timestamp - TimeUnit.DAYS.toMillis(30)
            val ninetyDaysCutoff = timestamp - TimeUnit.DAYS.toMillis(90)
            val oneEightyDaysCutoff = timestamp - TimeUnit.DAYS.toMillis(180)

            val window30 = sortedLogs.subList(0, i + 1).filter { it.timestamp >= thirtyDaysCutoff && !it.isQuarantined }
            val window90 = sortedLogs.subList(0, i + 1).filter { it.timestamp >= ninetyDaysCutoff && !it.isQuarantined }
            val window180 = sortedLogs.subList(0, i + 1).filter { it.timestamp >= oneEightyDaysCutoff && !it.isQuarantined }

            val ma30 = if (window30.isNotEmpty()) window30.map { it.systolic }.average() else null
            val ma90 = if (window90.isNotEmpty()) window90.map { it.systolic }.average() else null
            val ma180 = if (window180.isNotEmpty()) window180.map { it.systolic }.average() else null

            points.add(
                ChartDataPoint(
                    timestamp = timestamp,
                    rawSystolic = current.systolic,
                    rawDiastolic = current.diastolic,
                    heartRate = current.heartRate,
                    ma30Systolic = ma30,
                    ma90Systolic = ma90,
                    ma180Systolic = ma180,
                    isResting = !current.isQuarantined && current.contextTag == "RESTING",
                    contextTag = current.contextTag
                )
            )
        }

        return points
    }
}
