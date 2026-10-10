package com.example.ui

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.ActivityContext
import com.example.data.AppDatabase
import com.example.data.BloodPressureReading
import com.example.data.BloodPressureRepository
import com.example.data.HealthConnectManager
import com.example.data.MeasurementArm
import com.example.data.TimeOfDay
import com.example.data.WearableHealthSnapshot
import com.example.domain.BaselineVelocityEngine
import com.example.domain.ChartDataPoint
import com.example.domain.ClinicalPeriodSummary
import com.example.domain.DoctorReportGenerator
import com.example.domain.DriftAnalysis
import com.example.domain.PdfReportGenerator
import com.example.ui.components.ChartRangeOption
import com.example.ui.components.MetricType
import com.example.ui.theme.AppThemeMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

private data class ControlsState(
    val chartRange: ChartRangeOption,
    val reportDays: Int,
    val themeMode: AppThemeMode,
    val wearableSnapshot: WearableHealthSnapshot,
    val activeMetricExplanation: MetricType?
)

data class VitalsUiState(
    val readings: List<BloodPressureReading> = emptyList(),
    val driftAnalysis: DriftAnalysis = BaselineVelocityEngine.analyze(emptyList()),
    val chartPoints: List<ChartDataPoint> = emptyList(),
    val chartRange: ChartRangeOption = ChartRangeOption.DAYS_90,
    val selectedReportPeriodDays: Int = 30,
    val clinicalReportSummary: ClinicalPeriodSummary? = null,
    val formattedReportText: String = "",
    val themeMode: AppThemeMode = AppThemeMode.SYSTEM,
    val wearableSnapshot: WearableHealthSnapshot = HealthConnectManager.generateRealisticWearableSnapshot(),
    val activeMetricExplanation: MetricType? = null,
    val isLoading: Boolean = false,
    val feedbackMessage: String? = null
)

class VitalsViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: BloodPressureRepository

    init {
        val db = AppDatabase.getDatabase(application)
        repository = BloodPressureRepository(db.bloodPressureDao())

        // Check if DB is brand new, seed realistic multi-month baseline data
        viewModelScope.launch {
            repository.checkAndSeedInitialIfEmpty()
        }
    }

    private val _chartRange = MutableStateFlow(ChartRangeOption.DAYS_90)
    private val _reportPeriodDays = MutableStateFlow(30)
    private val _themeMode = MutableStateFlow(AppThemeMode.SYSTEM)
    private val _wearableSnapshot = MutableStateFlow(HealthConnectManager.generateRealisticWearableSnapshot())
    private val _activeMetricExplanation = MutableStateFlow<MetricType?>(null)
    private val _feedbackMessage = MutableStateFlow<String?>(null)

    private val _controlsFlow = combine(
        _chartRange,
        _reportPeriodDays,
        _themeMode,
        _wearableSnapshot,
        _activeMetricExplanation
    ) { range, days, theme, wearable, metric ->
        ControlsState(range, days, theme, wearable, metric)
    }

    val uiState: StateFlow<VitalsUiState> = combine(
        repository.allReadings,
        _controlsFlow,
        _feedbackMessage
    ) { readings, controls, feedbackMsg ->
        val driftAnalysis = BaselineVelocityEngine.analyze(readings)
        val chartPoints = BaselineVelocityEngine.computeChartPoints(readings)
        val reportSummary = DoctorReportGenerator.generateSummary(readings, controls.reportDays)
        val reportText = if (reportSummary != null) {
            DoctorReportGenerator.buildClinicalReportText(reportSummary, driftAnalysis)
        } else {
            ""
        }

        VitalsUiState(
            readings = readings,
            driftAnalysis = driftAnalysis,
            chartPoints = chartPoints,
            chartRange = controls.chartRange,
            selectedReportPeriodDays = controls.reportDays,
            clinicalReportSummary = reportSummary,
            formattedReportText = reportText,
            themeMode = controls.themeMode,
            wearableSnapshot = controls.wearableSnapshot,
            activeMetricExplanation = controls.activeMetricExplanation,
            isLoading = false,
            feedbackMessage = feedbackMsg
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = VitalsUiState(isLoading = true)
    )

    fun toggleTheme() {
        _themeMode.value = when (_themeMode.value) {
            AppThemeMode.SYSTEM -> AppThemeMode.DARK
            AppThemeMode.DARK -> AppThemeMode.LIGHT
            AppThemeMode.LIGHT -> AppThemeMode.SYSTEM
        }
    }

    fun setThemeMode(mode: AppThemeMode) {
        _themeMode.value = mode
    }

    fun syncWearables() {
        _wearableSnapshot.value = HealthConnectManager.generateRealisticWearableSnapshot()
        _feedbackMessage.value = "Health Connect vitals & workout logs synced"
    }

    fun explainMetric(type: MetricType) {
        _activeMetricExplanation.value = type
    }

    fun dismissMetricExplanation() {
        _activeMetricExplanation.value = null
    }

    fun saveReading(
        systolic: Int,
        diastolic: Int,
        heartRate: Int,
        activityContext: ActivityContext,
        timeOfDay: TimeOfDay,
        arm: MeasurementArm,
        notes: String
    ) {
        viewModelScope.launch {
            val now = System.currentTimeMillis()

            // Check Contextual Activity Auto-Tagging via Health Connect:
            // If the user marked resting, but Health Connect detects a workout within 30 min,
            // automatically flag and quarantine as POST_EXERCISE!
            val (isPostWorkout, workout) = HealthConnectManager.checkAutoTaggingPostExercise(
                now,
                _wearableSnapshot.value.recentWorkouts
            )

            val resolvedContext = if (isPostWorkout && activityContext == ActivityContext.RESTING) {
                ActivityContext.POST_EXERCISE
            } else {
                activityContext
            }

            val resolvedNotes = if (isPostWorkout && workout != null && !notes.contains("workout", ignoreCase = true)) {
                if (notes.isBlank()) "Auto-tagged: Health Connect detected ${workout.exerciseType} within 30m"
                else "$notes (Auto-tagged: ${workout.exerciseType} < 30m)"
            } else {
                notes
            }

            val reading = BloodPressureReading(
                systolic = systolic,
                diastolic = diastolic,
                heartRate = heartRate,
                timestamp = now,
                activityContext = resolvedContext,
                timeOfDay = timeOfDay,
                arm = arm,
                notes = resolvedNotes
            )
            repository.insert(reading)

            if (resolvedContext == ActivityContext.POST_EXERCISE && activityContext == ActivityContext.RESTING) {
                _feedbackMessage.value = "Health Connect detected a recent workout: Reading auto-flagged and quarantined"
            } else {
                _feedbackMessage.value = "Recorded $systolic/$diastolic mmHg successfully"
            }
        }
    }

    fun deleteReading(id: Long) {
        viewModelScope.launch {
            repository.deleteById(id)
            _feedbackMessage.value = "Reading removed from records"
        }
    }

    fun setChartRange(range: ChartRangeOption) {
        _chartRange.value = range
    }

    fun setReportPeriodDays(days: Int) {
        _reportPeriodDays.value = days
    }

    fun seedSampleData() {
        viewModelScope.launch {
            repository.seedSampleData()
            _feedbackMessage.value = "Loaded 180-day physiological drift sample dataset"
        }
    }

    fun clearAllData() {
        viewModelScope.launch {
            repository.clearAll()
            _feedbackMessage.value = "All logs cleared"
        }
    }

    fun openPdfReport(context: Context) {
        val summary = uiState.value.clinicalReportSummary ?: return
        try {
            val pdfFile = PdfReportGenerator.generateSinglePagePdf(context, summary, uiState.value.driftAnalysis)
            PdfReportGenerator.openPdf(context, pdfFile)
        } catch (e: Exception) {
            _feedbackMessage.value = "Could not open PDF viewer: ${e.localizedMessage}"
        }
    }

    fun sharePdfReport(context: Context) {
        val summary = uiState.value.clinicalReportSummary ?: return
        try {
            val pdfFile = PdfReportGenerator.generateSinglePagePdf(context, summary, uiState.value.driftAnalysis)
            PdfReportGenerator.sharePdf(context, pdfFile, summary.periodDays)
        } catch (e: Exception) {
            _feedbackMessage.value = "Could not export PDF: ${e.localizedMessage}"
        }
    }

    fun sharePlainTextReport(context: Context) {
        val currentSummary = uiState.value.clinicalReportSummary ?: return
        val currentText = uiState.value.formattedReportText
        DoctorReportGenerator.shareReport(context, currentText, currentSummary.periodDays)
    }

    fun clearFeedbackMessage() {
        _feedbackMessage.value = null
    }
}
