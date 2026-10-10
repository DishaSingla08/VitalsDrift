package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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

enum class MetricType(
    val title: String,
    val subtitle: String,
    val analogyHeader: String,
    val analogyText: String,
    val clinicalMeaning: String,
    val targetRange: String
) {
    PULSE_PRESSURE(
        title = "Pulse Pressure (PP)",
        subtitle = "The Arterial Flexibility Meter",
        analogyHeader = "The Garden Hose Analogy",
        analogyText = "Think of your arteries like a brand-new, flexible rubber garden hose. When water pulses through, the soft rubber expands smoothly to absorb the shock wave. If the hose turns old, dry, and rigid, every pulse strikes the pipe walls with harsh force.\n\nPulse Pressure (Systolic minus Diastolic) measures this arterial bounce.",
        clinicalMeaning = "A rising pulse pressure means your aorta and major arteries are becoming less elastic, forcing your heart to pump harder against stiff walls.",
        targetRange = "Ideal range: 30 to 50 mmHg"
    ),
    MEAN_ARTERIAL_PRESSURE(
        title = "Mean Arterial Pressure (MAP)",
        subtitle = "The Organ Perfusion Index",
        analogyHeader = "The Household Water Pressure Analogy",
        analogyText = "Systolic is the peak surge when the dishwasher valves open, while Diastolic is the resting pipe pressure between cycles. MAP is the continuous, steady driving pressure that keeps clean water flowing through your home's pipes into every fixture.\n\nBecause your heart spends twice as long resting (diastole) as pumping (systole), MAP weighs resting pressure more heavily.",
        clinicalMeaning = "MAP indicates whether enough oxygenated blood reaches your brain, kidneys, and liver to keep tissues alive without causing capillary shear damage.",
        targetRange = "Healthy clinical target: 70 to 100 mmHg"
    ),
    BASELINE_DRIFT(
        title = "Baseline Drift (ΔBP)",
        subtitle = "Weather vs. Climate Velocity",
        analogyHeader = "The Climate Shift Analogy",
        analogyText = "A single 90°F afternoon is just daily weather — like a spot blood pressure spike from a tense phone call or strong espresso. But Baseline Drift is climate change across months.\n\nWhen your true 180-day baseline moves upward by even 3 to 5 mmHg, it reflects a structural shift in baseline autonomic tone or arterial stiffness.",
        clinicalMeaning = "Detects silent cardiovascular changes months before conventional spot checks cross the official hypertension threshold (130/80 mmHg).",
        targetRange = "Normal stability: Delta < +2.0 mmHg"
    ),
    CONTEXT_QUARANTINE(
        title = "Context Quarantine",
        subtitle = "Preserving Clean Baseline Data",
        analogyHeader = "The Bathroom Scale Analogy",
        analogyText = "You wouldn't step onto a bathroom scale holding a heavy dumbbell and log that as your body weight. Likewise, measuring blood pressure right after a gym workout, a heated argument, or 3 cups of coffee captures temporary adrenaline, not your true resting physiology.",
        clinicalMeaning = "By quarantining non-resting entries from your 30-day moving average, VitalsDrift ensures your baseline velocity reflects true vascular health, not workout spikes.",
        targetRange = "Take baseline readings seated quietly for >5 minutes."
    )
}

@Composable
fun MetricExplanationDialog(
    metricType: MetricType,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(
                onClick = onDismiss,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.testTag("dialog_dismiss_btn")
            ) {
                Text("Got It")
            }
        },
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lightbulb,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Text(
                        text = metricType.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                }
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = metricType.subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Plain English analogy card
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = metricType.analogyHeader,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = metricType.analogyText,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 17.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "Clinical Importance:",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = metricType.clinicalMeaning,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    lineHeight = 16.sp
                )

                Spacer(modifier = Modifier.height(8.dp))

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                ) {
                    Text(
                        text = metricType.targetRange,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        },
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.testTag("metric_explanation_dialog")
    )
}
