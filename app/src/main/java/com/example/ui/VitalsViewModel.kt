package com.example.ui

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.HealthConnectManager
import com.example.data.MeasurementEntity
import com.example.data.MeasurementRepository
import com.example.data.WearableHealthSnapshot
import com.example.domain.BaselineVelocityEngine
import com.example.domain.ChartDataPoint
import com.example.domain.ClinicalPeriodSummary
import com.example.domain.DoctorReportGenerator
import com.example.domain.DriftAnalysis
import com.example.domain.PdfReportGenerator
import com.example.ui.components.ChartRangeOption
import com.example.ui.components.MetricType
import com.example.ui.theme.ThemePreferencesManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import kotlin.math.roundToInt

private data class ControlsState(
    val chartRange: ChartRangeOption,
    val reportDays: Int,
    val isDarkMode: Boolean,
    val wearableSnapshot: WearableHealthSnapshot,
    val activeMetricExplanation: MetricType?
)

data class VitalsUiState(
    val logs: List<MeasurementEntity> = emptyList(),
    val driftAnalysis: DriftAnalysis = BaselineVelocityEngine.analyze(emptyList()),
    val chartPoints: List<ChartDataPoint> = emptyList(),
    val chartRange: ChartRangeOption = ChartRangeOption.DAYS_90,
    val selectedReportPeriodDays: Int = 30,
    val clinicalReportSummary: ClinicalPeriodSummary? = null,
    val formattedReportText: String = "",
    val isDarkMode: Boolean = false,
    val wearableSnapshot: WearableHealthSnapshot = WearableHealthSnapshot(),
    val activeMetricExplanation: MetricType? = null,
    val cardiovascularWellnessScore: Int = 85,
    val isLoading: Boolean = false,
    val feedbackMessage: String? = null
)

class VitalsViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: MeasurementRepository
    private val themePrefs: ThemePreferencesManager = ThemePreferencesManager.getInstance(application)

    init {
        val db = AppDatabase.getDatabase(application)
        repository = MeasurementRepository(db.measurementDao())

        // Initial check: if Room is empty, seed multi-month drift dataset
        viewModelScope.launch {
            repository.checkAndSeedInitialIfEmpty()
        }

        // Initial Health Connect sync
        viewModelScope.launch {
            val snapshot = HealthConnectManager.syncHealthSnapshot(getApplication())
            _wearableSnapshot.value = snapshot
        }
    }

    private val _chartRange = MutableStateFlow(ChartRangeOption.DAYS_90)
    private val _reportPeriodDays = MutableStateFlow(30)
    private val _wearableSnapshot = MutableStateFlow(WearableHealthSnapshot())
    private val _activeMetricExplanation = MutableStateFlow<MetricType?>(null)
    private val _feedbackMessage = MutableStateFlow<String?>(null)

    private val _controlsFlow = combine(
        _chartRange,
        _reportPeriodDays,
        themePrefs.isDarkMode,
        _wearableSnapshot,
        _activeMetricExplanation
    ) { range, days, isDark, wearable, metric ->
        ControlsState(range, days, isDark, wearable, metric)
    }

    val uiState: StateFlow<VitalsUiState> = combine(
        repository.allLogs,
        _controlsFlow,
        _feedbackMessage
    ) { logs, controls, feedbackMsg ->
        val driftAnalysis = BaselineVelocityEngine.analyze(logs)
        val chartPoints = BaselineVelocityEngine.computeChartPoints(logs)
        val reportSummary = DoctorReportGenerator.generateSummary(logs, controls.reportDays)
        val reportText = if (reportSummary != null) {
            DoctorReportGenerator.buildClinicalReportText(reportSummary, driftAnalysis)
        } else {
            ""
        }

        val wellnessScore = calculateWellnessScore(driftAnalysis, controls.wearableSnapshot)

        VitalsUiState(
            logs = logs,
            driftAnalysis = driftAnalysis,
            chartPoints = chartPoints,
            chartRange = controls.chartRange,
            selectedReportPeriodDays = controls.reportDays,
            clinicalReportSummary = reportSummary,
            formattedReportText = reportText,
            isDarkMode = controls.isDarkMode,
            wearableSnapshot = controls.wearableSnapshot,
            activeMetricExplanation = controls.activeMetricExplanation,
            cardiovascularWellnessScore = wellnessScore,
            isLoading = false,
            feedbackMessage = feedbackMsg
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = VitalsUiState(isLoading = true)
    )

    /**
     * EXACTLY ONE CLICK toggle between Light Mode and Dark Mode with persistent storage
     */
    fun toggleTheme() {
        val newDark = themePrefs.toggleTheme()
        _feedbackMessage.value = if (newDark) "Dark mode enabled" else "Light mode enabled"
    }

    fun syncWearables() {
        viewModelScope.launch {
            val snapshot = HealthConnectManager.syncHealthSnapshot(getApplication())
            _wearableSnapshot.value = snapshot
            _feedbackMessage.value = "Synced with ${snapshot.connectedSource}"
        }
    }

    fun explainMetric(type: MetricType) {
        _activeMetricExplanation.value = type
    }

    fun dismissMetricExplanation() {
        _activeMetricExplanation.value = null
    }

    fun setChartRange(range: ChartRangeOption) {
        _chartRange.value = range
    }

    fun setReportPeriodDays(days: Int) {
        _reportPeriodDays.value = days
    }

    fun clearFeedbackMessage() {
        _feedbackMessage.value = null
    }

    /**
     * Context Validator:
     * When logging, check Health Connect for any ExerciseSessionRecord within preceding 30 minutes.
     * If exercise session exists or tagged as post-exercise, automatically quarantine entry to prevent baseline skew.
     */
    fun saveReading(
        systolic: Int,
        diastolic: Int,
        heartRate: Int,
        contextTag: String = "RESTING",
        notes: String = ""
    ) {
        viewModelScope.launch {
            val context = getApplication<Application>()
            val now = System.currentTimeMillis()

            // 1. Check Health Connect for exercise session within 30 min
            val healthConnectWorkout = HealthConnectManager.checkExerciseWithin30Minutes(context, now)

            // 2. Quarantine if Health Connect detected workout, or user marked post-exercise/stress
            val shouldQuarantine = healthConnectWorkout ||
                    contextTag == "POST_EXERCISE" ||
                    contextTag == "CAFFEINE_STRESS"

            val effectiveNotes = when {
                healthConnectWorkout -> if (notes.isNotBlank()) "$notes (Auto-Quarantined via Health Connect Workout)" else "Auto-Quarantined: Exercise session detected in past 30 min"
                shouldQuarantine -> if (notes.isNotBlank()) "$notes (Quarantined: $contextTag)" else "Quarantined from baseline due to acute state"
                else -> notes
            }

            val entity = MeasurementEntity(
                systolic = systolic,
                diastolic = diastolic,
                heartRate = heartRate,
                timestamp = now,
                contextTag = contextTag,
                isQuarantined = shouldQuarantine,
                notes = effectiveNotes.ifBlank { null }
            )

            repository.insertLog(entity)

            _feedbackMessage.value = if (shouldQuarantine) {
                "Saved! Flagged as acute & quarantined from long-term baseline."
            } else {
                "Blood pressure reading recorded successfully."
            }
        }
    }

    fun deleteLog(id: Long) {
        viewModelScope.launch {
            repository.deleteLogById(id)
            _feedbackMessage.value = "Measurement deleted"
        }
    }

    fun seedSampleData() {
        viewModelScope.launch {
            repository.seedSampleData()
            _feedbackMessage.value = "180-day baseline dataset reloaded"
        }
    }

    fun clearAllData() {
        viewModelScope.launch {
            repository.clearAllLogs()
            _feedbackMessage.value = "All logs cleared"
        }
    }

    /**
     * Dynamic PDF report generation using native PdfDocument and FileProvider
     */
    fun openPdfReport(context: Context): File? {
        val summary = uiState.value.clinicalReportSummary ?: return null
        return try {
            val pdfFile = PdfReportGenerator.generateSinglePagePdf(
                context = context,
                summary = summary,
                driftAnalysis = uiState.value.driftAnalysis
            )
            PdfReportGenerator.openPdf(context, pdfFile)
            pdfFile
        } catch (e: Exception) {
            _feedbackMessage.value = "Error generating PDF: ${e.message}"
            null
        }
    }

    fun sharePdfReport(context: Context) {
        val summary = uiState.value.clinicalReportSummary ?: return
        try {
            val pdfFile = PdfReportGenerator.generateSinglePagePdf(
                context = context,
                summary = summary,
                driftAnalysis = uiState.value.driftAnalysis
            )
            PdfReportGenerator.sharePdf(context, pdfFile, summary.periodDays)
        } catch (e: Exception) {
            _feedbackMessage.value = "Error sharing PDF: ${e.message}"
        }
    }

    /**
     * Real-time calculation of Cardiovascular Wellness Score (0-100 scale)
     * incorporating velocity drift delta, clean log consistency, HRV, and sleep duration.
     */
    private fun calculateWellnessScore(
        driftAnalysis: DriftAnalysis,
        wearables: WearableHealthSnapshot
    ): Int {
        var score = 88.0

        // Velocity Drift Delta Penalty
        val deltaS = driftAnalysis.deltaSystolic
        if (deltaS > 0) {
            score -= (deltaS * 3.2).coerceAtMost(25.0)
        } else if (deltaS < -1.0) {
            score += 3.0 // Bonus for improving downward velocity
        }

        // Quarantined Log ratio penalty
        if (driftAnalysis.quarantinedCount > 0) {
            score -= (driftAnalysis.quarantinedCount * 1.2).coerceAtMost(8.0)
        }

        // HRV Recovery Impact (Health Connect)
        val hrv = wearables.heartRateVariabilityMs
        if (hrv < 30.0) {
            score -= ((30.0 - hrv) * 0.45).coerceAtMost(7.0)
        } else if (hrv >= 40.0) {
            score += 4.0
        }

        // Sleep Duration Impact (Health Connect)
        val sleep = wearables.sleepDurationHours
        if (sleep < 6.0) {
            score -= ((6.0 - sleep) * 3.5).coerceAtMost(8.0)
        } else if (sleep >= 7.5) {
            score += 3.5
        }

        return score.roundToInt().coerceIn(40, 99)
    }
}
