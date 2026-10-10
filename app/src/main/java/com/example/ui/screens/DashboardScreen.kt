package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Watch
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.WearableHealthSnapshot
import com.example.domain.DriftAnalysis
import com.example.ui.VitalsUiState
import com.example.ui.components.BpTrendChart
import com.example.ui.components.ChartRangeOption
import com.example.ui.components.EarlyDetectionAlertCard
import com.example.ui.components.MedicalDisclaimerBanner
import com.example.ui.components.MetricType
import com.example.ui.components.VitalsOverviewCard
import com.example.ui.components.WellnessScoreAndActionRow
import com.example.ui.theme.VitalsTheme
import java.util.Locale

@Composable
fun DashboardScreen(
    uiState: VitalsUiState,
    onOpenLogSheet: () -> Unit,
    onSelectRange: (ChartRangeOption) -> Unit,
    onNavigateToReport: () -> Unit,
    onExplainMetric: (MetricType) -> Unit,
    onSyncWearables: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .testTag("dashboard_scroll_list"),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 1. Hero VitalsDrift Overview Card (3 dials: 30D MA, Tachometer Arch Gauge for ΔBP, 180D Baseline + Wave Curve)
            item {
                VitalsOverviewCard(
                    analysis = uiState.driftAnalysis
                )
            }

            // 2. Cardiovascular Wellness Score & + Log BP Capsule Action Button Row
            item {
                WellnessScoreAndActionRow(
                    score = uiState.cardiovascularWellnessScore,
                    onOpenLogSheet = onOpenLogSheet
                )
            }

            // 3. Early Detection Alert Card with arterial diagram, drift velocity indicator & expandable Zone 2 protocol
            item {
                EarlyDetectionAlertCard(
                    analysis = uiState.driftAnalysis,
                    onActionableNudgesClick = { onExplainMetric(MetricType.BASELINE_DRIFT) }
                )
            }

            // 4. Prominent Action Button: "Generate the Report" (Opens standardized PDF clinical report)
            item {
                GenerateReportBannerButton(
                    onClick = onNavigateToReport,
                    totalReadings = uiState.logs.size
                )
            }

            // 5. Android Health Connect & Wearables Integration Card (Resting HR, HRV, Sleep, Steps + Sync)
            item {
                HealthConnectWearablesCard(
                    snapshot = uiState.wearableSnapshot,
                    onSync = onSyncWearables
                )
            }

            // 6. Context Validator Alert (Shown if readings were quarantined due to exercise or stress)
            if (uiState.driftAnalysis.quarantinedCount > 0 || uiState.driftAnalysis.nonRestingAlertCountRecent > 0) {
                item {
                    ContextValidatorAlertCard(
                        count = uiState.driftAnalysis.quarantinedCount,
                        onExplainQuarantine = { onExplainMetric(MetricType.CONTEXT_QUARANTINE) }
                    )
                }
            }

            // 7. Vascular Drift Curves Chart (30D, 90D, 180D moving average curves with raw spot logs)
            item {
                BpTrendChart(
                    dataPoints = uiState.chartPoints,
                    selectedRange = uiState.chartRange,
                    onRangeSelected = onSelectRange
                )
            }

            // 8. Physiological Metrics Row with Tap-to-Explain Modals (Pulse Pressure & MAP)
            item {
                PhysiologicalMetricsRow(
                    analysis = uiState.driftAnalysis,
                    onExplainPp = { onExplainMetric(MetricType.PULSE_PRESSURE) },
                    onExplainMap = { onExplainMetric(MetricType.MEAN_ARTERIAL_PRESSURE) }
                )
            }

            // 9. Medical Disclaimer Banner
            item {
                MedicalDisclaimerBanner()
            }
        }

        // Quick Log Floating Action Button
        ExtendedFloatingActionButton(
            onClick = onOpenLogSheet,
            icon = { Icon(imageVector = Icons.Default.Add, contentDescription = null) },
            text = { Text("Log BP", fontWeight = FontWeight.SemiBold) },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp)
                .testTag("quick_log_fab"),
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
        )
    }
}

@Composable
private fun GenerateReportBannerButton(
    onClick: () -> Unit,
    totalReadings: Int
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("generate_report_banner_btn"),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.primaryContainer,
        tonalElevation = 2.dp
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Description,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Column {
                    Text(
                        text = "Generate the Report",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = "Export standardized 1-page clinical PDF for your doctor ($totalReadings logs)",
                        style = MaterialTheme.typography.bodySmall,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                    )
                }
            }

            Icon(
                imageVector = Icons.Default.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}

@Composable
private fun HealthConnectWearablesCard(
    snapshot: WearableHealthSnapshot,
    onSync: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("health_connect_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Watch,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Column {
                        Text(
                            text = "Health Connect & Wearables",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = snapshot.connectedSource,
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                TextButton(onClick = onSync, modifier = Modifier.testTag("sync_wearables_btn")) {
                    Icon(imageVector = Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Sync", fontSize = 12.sp)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 4-box metrics row: RHR, HRV, Sleep, Steps
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                WearableMetricPill("Resting HR", "${snapshot.restingHeartRate} bpm", Modifier.weight(1f))
                WearableMetricPill(
                    label = "HRV (Recovery)",
                    value = "${snapshot.heartRateVariabilityMs.toInt()} ms",
                    modifier = Modifier.weight(1f),
                    isAlert = snapshot.isLowHrvRisk
                )
                WearableMetricPill(
                    label = "Sleep",
                    value = "${String.format(Locale.US, "%.1f", snapshot.sleepDurationHours)} hrs",
                    modifier = Modifier.weight(1f),
                    isAlert = snapshot.isShortSleepRisk
                )
                WearableMetricPill("Steps", "${snapshot.dailySteps}", Modifier.weight(1f))
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Correlation note
            Text(
                text = snapshot.plainEnglishRiskSummary,
                style = MaterialTheme.typography.bodySmall,
                fontSize = 11.sp,
                color = if (snapshot.isLowHrvRisk || snapshot.isShortSleepRisk) VitalsTheme.colors.statusWarning else MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 15.sp
            )
        }
    }
}

@Composable
private fun WearableMetricPill(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    isAlert: Boolean = false
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        color = if (isAlert) VitalsTheme.colors.statusWarning.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = if (isAlert) VitalsTheme.colors.statusWarning else MaterialTheme.colorScheme.onSurface,
                fontSize = 12.sp
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontSize = 9.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ContextValidatorAlertCard(
    count: Int,
    onExplainQuarantine: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("context_validator_alert"),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.35f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(24.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Context Validator ($count Logs Quarantined)",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                    IconButton(onClick = onExplainQuarantine, modifier = Modifier.size(24.dp)) {
                        Icon(imageVector = Icons.Default.HelpOutline, contentDescription = null, modifier = Modifier.size(16.dp))
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Entries recorded within 30 min of high exertion/stress or flagged workouts are quarantined from long-term baseline velocity calculations to prevent artificial skew.",
                    style = MaterialTheme.typography.bodySmall,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    lineHeight = 16.sp
                )
            }
        }
    }
}

@Composable
private fun PhysiologicalMetricsRow(
    analysis: DriftAnalysis,
    onExplainPp: () -> Unit,
    onExplainMap: () -> Unit
) {
    val pp = analysis.recent30DayAvg?.pulsePressureInt ?: 40
    val map = analysis.recent30DayAvg?.mapFormatted ?: "93.0"

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Pulse Pressure Card
        Surface(
            modifier = Modifier
                .weight(1f)
                .clickable(onClick = onExplainPp)
                .testTag("pulse_pressure_metric_card"),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Pulse Pressure (PP)",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Icon(
                        imageVector = Icons.Default.HelpOutline,
                        contentDescription = "Explain Pulse Pressure",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(14.dp)
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = "$pp",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(text = "mmHg", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Text(
                    text = if (pp in 30..50) "Normal arterial elasticity" else "Increased arterial stiffness",
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // MAP Card
        Surface(
            modifier = Modifier
                .weight(1f)
                .clickable(onClick = onExplainMap)
                .testTag("map_metric_card"),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Mean Arterial (MAP)",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Icon(
                        imageVector = Icons.Default.HelpOutline,
                        contentDescription = "Explain MAP",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(14.dp)
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = map,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(text = "mmHg", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Text(
                    text = "Continuous perfusion (70–100)",
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
