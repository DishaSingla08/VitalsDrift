package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.NightlightRound
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.DriftAnalysis
import com.example.domain.DriftTriageStatus
import com.example.domain.LifestyleRecommendation
import com.example.ui.VitalsUiState
import com.example.ui.components.MedicalDisclaimerBanner
import com.example.ui.theme.VitalsTheme
import java.util.Locale

@Composable
fun AnalyticsScreen(
    uiState: VitalsUiState,
    modifier: Modifier = Modifier
) {
    val analysis = uiState.driftAnalysis

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("analytics_scroll_list"),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 48.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Header description
        item {
            Column {
                Text(
                    text = "Baseline Velocity Engine",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Tracking silent vascular stiffness and multi-month drift",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // 2. Velocity Engine Delta Card
        item {
            VelocityEngineOverviewCard(analysis = analysis)
        }

        // 3. Triage Risk Assessment & Protocol Card
        item {
            TriageRiskAssessmentCard(analysis = analysis)
        }

        // 4. Circadian Diurnal Balance Card (Morning vs Evening)
        item {
            DiurnalCircadianCard(analysis = analysis)
        }

        // 5. Section Header for Lifestyle Nudges
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Lightbulb,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = "Evidence-Based Intervention Nudges",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // 6. Actionable Lifestyle Cards
        if (analysis.lifestyleTips.isEmpty()) {
            item {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Continue logging readings to unlock personalized vascular velocity tips.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        } else {
            items(analysis.lifestyleTips, key = { it.id }) { tip ->
                LifestyleActionCard(tip = tip)
            }
        }

        // 7. Medical Disclaimer
        item {
            MedicalDisclaimerBanner()
        }
    }
}

@Composable
private fun VelocityEngineOverviewCard(analysis: DriftAnalysis) {
    val status = analysis.triageStatus
    val colors = VitalsTheme.colors
    val badgeColor = when (status) {
        DriftTriageStatus.STABLE -> colors.statusStable
        DriftTriageStatus.EARLY_DRIFT -> colors.statusWarning
        DriftTriageStatus.SIGNIFICANT_SHIFT -> colors.statusCritical
        else -> colors.statusStable
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("velocity_overview_card"),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Vascular Drift Velocity (ΔBP)",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = badgeColor.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = status.label,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = badgeColor,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Mathematical formula explanation
            Text(
                text = "ΔBP = (30-Day Moving Average) − (180-Day True Baseline)",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 11.sp
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Two-column readout for Systolic and Diastolic Drift
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Systolic Drift
                Surface(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "Systolic Drift (ΔSBP)",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        val sStr = String.format(Locale.US, "%+.1f", analysis.deltaSystolic)
                        Text(
                            text = "$sStr mmHg",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = if (analysis.deltaSystolic >= 2.0) colors.statusCritical else MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Velocity: ${String.format(Locale.US, "%+.1f", analysis.monthlyVelocitySystolic)} mmHg/mo",
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Diastolic Drift
                Surface(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "Diastolic Drift (ΔDBP)",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        val dStr = String.format(Locale.US, "%+.1f", analysis.deltaDiastolic)
                        Text(
                            text = "$dStr mmHg",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = if (analysis.deltaDiastolic >= 1.5) colors.statusCritical else MaterialTheme.colorScheme.secondary
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Peripheral resistance shift",
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TriageRiskAssessmentCard(analysis: DriftAnalysis) {
    val status = analysis.triageStatus
    val colors = VitalsTheme.colors
    val isSignificant = status == DriftTriageStatus.SIGNIFICANT_SHIFT
    val isEarly = status == DriftTriageStatus.EARLY_DRIFT

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSignificant) colors.statusCritical.copy(alpha = 0.12f)
            else if (isEarly) colors.statusWarning.copy(alpha = 0.12f)
            else MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = when {
                        isSignificant -> Icons.Default.Warning
                        isEarly -> Icons.Default.Info
                        else -> Icons.Default.CheckCircle
                    },
                    contentDescription = null,
                    tint = when {
                        isSignificant -> colors.statusCritical
                        isEarly -> colors.statusWarning
                        else -> colors.statusStable
                    },
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    text = when {
                        isSignificant -> "Clinical Monitoring Protocol Triggered"
                        isEarly -> "Early Baseline Drift Intervention Window"
                        else -> "Cardiovascular Baseline Equilibrium"
                    },
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = when {
                    isSignificant -> "Your 30-day blood pressure velocity is drifting >5 mmHg above your 180-day baseline. Rather than relying on single spot checks, execute a 7-day home protocol (2 AM readings + 2 PM readings daily) and share the generated clinical summary with your doctor."
                    isEarly -> "A drift of +2 to +5 mmHg was detected. Early interventions such as modest sodium reduction, restoring sleep architecture, and Zone 2 aerobic exercise can reverse this trajectory before clinical hypertension solidifies."
                    else -> "No upward baseline drift observed. Long-term cardiovascular compliance and arterial elasticity remain stable."
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface,
                lineHeight = 18.sp
            )
        }
    }
}

@Composable
private fun DiurnalCircadianCard(analysis: DriftAnalysis) {
    val morning = analysis.morningAvg
    val evening = analysis.eveningAvg
    val colors = VitalsTheme.colors

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Schedule,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = "Diurnal Circadian Rhythm (Morning vs. Evening)",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Morning
                Surface(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(imageVector = Icons.Default.WbSunny, contentDescription = null, tint = colors.statusWarning, modifier = Modifier.size(16.dp))
                            Text("Morning (AM)", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = if (morning != null) "${morning.systolicInt}/${morning.diastolicInt} mmHg" else "--/--",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (morning != null) "Avg HR: ${morning.heartRateInt} bpm" else "No AM logs",
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Evening
                Surface(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(imageVector = Icons.Default.NightlightRound, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                            Text("Evening (PM)", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = if (evening != null) "${evening.systolicInt}/${evening.diastolicInt} mmHg" else "--/--",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (evening != null) "Avg HR: ${evening.heartRateInt} bpm" else "No PM logs",
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            if (morning != null && evening != null) {
                val gap = morning.systolicInt - evening.systolicInt
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = if (gap > 10) "Morning surge detected: AM blood pressure is $gap mmHg higher than PM. Share this with your physician for nocturnal dip assessment."
                    else "Healthy diurnal balance: Morning and evening readings are closely aligned within ${gap.coerceAtLeast(0)} mmHg.",
                    style = MaterialTheme.typography.bodySmall,
                    fontSize = 11.sp,
                    color = if (gap > 10) colors.statusCritical else MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 15.sp
                )
            }
        }
    }
}

@Composable
private fun LifestyleActionCard(tip: LifestyleRecommendation) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("lifestyle_card_${tip.id}"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                ) {
                    Text(
                        text = tip.category,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
                ) {
                    Text(
                        text = tip.impact,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.secondary,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = tip.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = tip.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface,
                lineHeight = 18.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Clinical evidence: ${tip.evidenceNote}",
                style = MaterialTheme.typography.labelSmall,
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
