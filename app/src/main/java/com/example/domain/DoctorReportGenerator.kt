package com.example.domain

import android.content.Context
import android.content.Intent
import com.example.data.BpCategory
import com.example.data.MeasurementEntity
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt

data class ClinicalPeriodSummary(
    val periodDays: Int,
    val totalReadings: Int,
    val avgSystolic: Int,
    val avgDiastolic: Int,
    val avgHeartRate: Int,
    val avgPulsePressure: Int,
    val avgMeanArterialPressure: Double,
    val morningAvg: Pair<Int, Int>?, // (Systolic, Diastolic)
    val morningCount: Int,
    val eveningAvg: Pair<Int, Int>?, // (Systolic, Diastolic)
    val eveningCount: Int,
    val minReading: Pair<Int, Int>, // (Systolic, Diastolic)
    val maxReading: Pair<Int, Int>,
    val normalCount: Int,
    val elevatedCount: Int,
    val stage1Count: Int,
    val stage2Count: Int,
    val crisisCount: Int,
    val quarantinedCount: Int,
    val latestReadings: List<MeasurementEntity>
)

object DoctorReportGenerator {

    fun generateSummary(logs: List<MeasurementEntity>, days: Int): ClinicalPeriodSummary? {
        val cutoff = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(days.toLong())
        val periodLogs = logs.filter { it.timestamp >= cutoff }

        if (periodLogs.isEmpty()) return null

        val total = periodLogs.size
        val avgSys = periodLogs.map { it.systolic }.average().roundToInt()
        val avgDia = periodLogs.map { it.diastolic }.average().roundToInt()
        val avgHr = periodLogs.map { it.heartRate }.average().roundToInt()
        val avgPp = periodLogs.map { it.pulsePressure }.average().roundToInt()
        val avgMap = periodLogs.map { it.meanArterialPressure }.average()

        val cal = Calendar.getInstance()
        val mornings = periodLogs.filter {
            cal.timeInMillis = it.timestamp
            val hour = cal.get(Calendar.HOUR_OF_DAY)
            hour in 5..11
        }
        val morningAvg = if (mornings.isNotEmpty()) {
            Pair(
                mornings.map { it.systolic }.average().roundToInt(),
                mornings.map { it.diastolic }.average().roundToInt()
            )
        } else null

        val evenings = periodLogs.filter {
            cal.timeInMillis = it.timestamp
            val hour = cal.get(Calendar.HOUR_OF_DAY)
            hour in 17..23
        }
        val eveningAvg = if (evenings.isNotEmpty()) {
            Pair(
                evenings.map { it.systolic }.average().roundToInt(),
                evenings.map { it.diastolic }.average().roundToInt()
            )
        } else null

        val minSys = periodLogs.minByOrNull { it.systolic }?.let { Pair(it.systolic, it.diastolic) } ?: Pair(avgSys, avgDia)
        val maxSys = periodLogs.maxByOrNull { it.systolic }?.let { Pair(it.systolic, it.diastolic) } ?: Pair(avgSys, avgDia)

        var norm = 0
        var elev = 0
        var s1 = 0
        var s2 = 0
        var cris = 0
        var quarantined = 0

        for (r in periodLogs) {
            when (r.category) {
                BpCategory.NORMAL -> norm++
                BpCategory.ELEVATED -> elev++
                BpCategory.STAGE_1 -> s1++
                BpCategory.STAGE_2 -> s2++
                BpCategory.CRISIS -> cris++
            }
            if (r.isQuarantined) {
                quarantined++
            }
        }

        return ClinicalPeriodSummary(
            periodDays = days,
            totalReadings = total,
            avgSystolic = avgSys,
            avgDiastolic = avgDia,
            avgHeartRate = avgHr,
            avgPulsePressure = avgPp,
            avgMeanArterialPressure = avgMap,
            morningAvg = morningAvg,
            morningCount = mornings.size,
            eveningAvg = eveningAvg,
            eveningCount = evenings.size,
            minReading = minSys,
            maxReading = maxSys,
            normalCount = norm,
            elevatedCount = elev,
            stage1Count = s1,
            stage2Count = s2,
            crisisCount = cris,
            quarantinedCount = quarantined,
            latestReadings = periodLogs.take(15)
        )
    }

    fun buildClinicalReportText(
        summary: ClinicalPeriodSummary,
        driftAnalysis: DriftAnalysis?,
        patientName: String = "Patient"
    ): String {
        val dateFormat = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
        val generatedDate = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())

        val sb = StringBuilder()
        sb.appendLine("====================================================")
        sb.appendLine("           VITALSDRIFT CLINICAL BP SUMMARY")
        sb.appendLine("====================================================")
        sb.appendLine("Generated: $generatedDate")
        sb.appendLine("Observation Window: Past ${summary.periodDays} Days (${summary.totalReadings} logged readings)")
        sb.appendLine()

        sb.appendLine("1. COMPOSITE AVERAGES:")
        sb.appendLine("  • Blood Pressure: ${summary.avgSystolic}/${summary.avgDiastolic} mmHg")
        sb.appendLine("  • Resting Heart Rate: ${summary.avgHeartRate} bpm")
        sb.appendLine("  • Pulse Pressure (PP): ${summary.avgPulsePressure} mmHg (Normal: 30-50 mmHg)")
        sb.appendLine("  • Mean Arterial Pressure (MAP): ${String.format(Locale.US, "%.1f", summary.avgMeanArterialPressure)} mmHg (Target: 70-100 mmHg)")
        sb.appendLine()

        sb.appendLine("2. DIURNAL / CIRCADIAN BREAKDOWN:")
        if (summary.morningAvg != null) {
            sb.appendLine("  • Morning Averages (${summary.morningCount} logs): ${summary.morningAvg.first}/${summary.morningAvg.second} mmHg")
        } else {
            sb.appendLine("  • Morning Averages: Insufficient data")
        }
        if (summary.eveningAvg != null) {
            sb.appendLine("  • Evening Averages (${summary.eveningCount} logs): ${summary.eveningAvg.first}/${summary.eveningAvg.second} mmHg")
        } else {
            sb.appendLine("  • Evening Averages: Insufficient data")
        }
        if (summary.morningAvg != null && summary.eveningAvg != null) {
            val morningSurge = summary.morningAvg.first - summary.eveningAvg.first
            sb.appendLine("  • Morning-to-Evening Delta: ${if (morningSurge >= 0) "+$morningSurge" else "$morningSurge"} mmHg")
        }
        sb.appendLine()

        sb.appendLine("3. LONG-TERM BASELINE DRIFT VELOCITY:")
        if (driftAnalysis != null && driftAnalysis.baseline180DayAvg != null && driftAnalysis.recent30DayAvg != null) {
            sb.appendLine("  • Status: ${driftAnalysis.triageStatus.label}")
            sb.appendLine("  • 180-Day Baseline Average: ${driftAnalysis.baseline180DayAvg.systolicInt}/${driftAnalysis.baseline180DayAvg.diastolicInt} mmHg")
            sb.appendLine("  • Recent 30-Day Moving Average: ${driftAnalysis.recent30DayAvg.systolicInt}/${driftAnalysis.recent30DayAvg.diastolicInt} mmHg")
            sb.appendLine("  • Baseline Delta (ΔBP): ${String.format(Locale.US, "%+.1f", driftAnalysis.deltaSystolic)} / ${String.format(Locale.US, "%+.1f", driftAnalysis.deltaDiastolic)} mmHg")
            sb.appendLine("  • Estimated Monthly Velocity: ${String.format(Locale.US, "%+.1f", driftAnalysis.monthlyVelocitySystolic)} mmHg / month")
        } else {
            sb.appendLine("  • Baseline velocity requires multi-month trend logs.")
        }
        sb.appendLine()

        sb.appendLine("4. ACC/AHA CATEGORICAL DISTRIBUTION:")
        val total = summary.totalReadings.toDouble()
        sb.appendLine("  • Normal (<120/<80): ${summary.normalCount} (${(summary.normalCount * 100 / total).roundToInt()}%)")
        sb.appendLine("  • Elevated (120-129/<80): ${summary.elevatedCount} (${(summary.elevatedCount * 100 / total).roundToInt()}%)")
        sb.appendLine("  • Stage 1 (130-139 or 80-89): ${summary.stage1Count} (${(summary.stage1Count * 100 / total).roundToInt()}%)")
        sb.appendLine("  • Stage 2 (≥140 or ≥90): ${summary.stage2Count} (${(summary.stage2Count * 100 / total).roundToInt()}%)")
        if (summary.crisisCount > 0) {
            sb.appendLine("  • Hypertensive Crisis (>180/>120): ${summary.crisisCount} (${(summary.crisisCount * 100 / total).roundToInt()}%)")
        }
        sb.appendLine()

        sb.appendLine("5. CONTEXT VALIDATOR & QUARANTINED LOGS:")
        sb.appendLine("  • Quarantined Logs: ${summary.quarantinedCount} of ${summary.totalReadings} entries were flagged within 30 min of exercise/acute stress and quarantined from baseline.")
        sb.appendLine()

        sb.appendLine("6. RECENT OBSERVATIONS (LAST ${summary.latestReadings.size} ENTRIES):")
        for (r in summary.latestReadings) {
            val dateStr = dateFormat.format(Date(r.timestamp))
            sb.appendLine("  - $dateStr | ${r.systolic}/${r.diastolic} mmHg | HR ${r.heartRate} bpm | ${r.contextTag}${if (r.isQuarantined) " [QUARANTINED]" else ""}${if (!r.notes.isNullOrBlank()) " | Note: ${r.notes}" else ""}")
        }
        sb.appendLine()
        sb.appendLine("----------------------------------------------------")
        sb.appendLine("DISCLAIMER: VitalsDrift is an observational tracking")
        sb.appendLine("and drift analysis tool, not a diagnostic medical device.")
        sb.appendLine("Please share this report with your qualified physician.")
        sb.appendLine("====================================================")

        return sb.toString()
    }

    fun shareReport(context: Context, reportText: String, periodDays: Int) {
        val sendIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, reportText)
            putExtra(Intent.EXTRA_SUBJECT, "VitalsDrift Clinical BP Summary (${periodDays}-Day Report)")
            type = "text/plain"
        }
        val shareIntent = Intent.createChooser(sendIntent, "Share Clinical BP Report")
        context.startActivity(shareIntent)
    }
}
