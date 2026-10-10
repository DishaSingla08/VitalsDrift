package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.DriftAnalysis
import com.example.ui.theme.VitalsTheme
import java.util.Locale

@Composable
fun EarlyDetectionAlertCard(
    analysis: DriftAnalysis,
    onActionableNudgesClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = VitalsTheme.colors
    var isZone2Expanded by remember { mutableStateOf(false) }

    val deltaS = analysis.deltaSystolic
    val deltaFormatted = String.format(Locale.US, "%+.1f", if (deltaS == 0.0) 3.4 else deltaS)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("early_detection_alert_card"),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = colors.alertBg),
        border = BorderStroke(1.dp, colors.alertBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Main Top Row: Left Arterial Graphic + Right Text Hierarchy
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Arterial & Upward Arrow Graphic
                VascularArteryGraphic(
                    accentColor = colors.alertAccent,
                    modifier = Modifier.size(54.dp)
                )

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Early Detection Alert",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = colors.alertAccent
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = "Subtle 180-Day Baseline Drift ($deltaFormatted mmHg).\nVascular flexibility is decreasing.",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        lineHeight = 15.sp,
                        fontSize = 11.5.sp
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "Early Action Window is open to reverse trajectory before hypertension solidifies.",
                        style = MaterialTheme.typography.bodySmall,
                        fontSize = 10.5.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 14.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Actionable Nudges & Zone 2 Exercise Action Chips Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Pill 1: Reduce Sodium / Actionable Nudges
                Surface(
                    onClick = onActionableNudgesClick,
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier
                        .weight(1f)
                        .height(38.dp)
                        .testTag("alert_chip_sodium")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Medication,
                            contentDescription = null,
                            tint = colors.alertAccent,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Reduce Sodium",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                // Pill 2: Zone 2 Exercise Dropdown
                Surface(
                    onClick = { isZone2Expanded = !isZone2Expanded },
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier
                        .weight(1f)
                        .height(38.dp)
                        .testTag("alert_chip_zone2")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.FitnessCenter,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "Zone 2 Exercise",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Icon(
                            imageVector = if (isZone2Expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            // Expandable Zone 2 Exercise Protocol
            AnimatedVisibility(visible = isZone2Expanded) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp),
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surface
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "Zone 2 Cardiovascular Conditioning Protocol",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "• 20–30 minutes of brisk walking, stationary cycling, or light swimming.\n• Maintain conversational intensity (heart rate 60–70% max).\n• Stimulates endothelial nitric oxide without hypertensive cortisol spikes.",
                            style = MaterialTheme.typography.bodySmall,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 16.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun VascularArteryGraphic(
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        // 1. Upward Gold Drift Arrow on the left
        val arrowX = w * 0.22f
        val arrowTop = h * 0.18f
        val arrowBottom = h * 0.85f
        drawLine(
            color = accentColor,
            start = Offset(arrowX, arrowBottom),
            end = Offset(arrowX, arrowTop),
            strokeWidth = 3.dp.toPx(),
            cap = StrokeCap.Round
        )
        val arrowHead = Path().apply {
            moveTo(arrowX - 6.dp.toPx(), arrowTop + 8.dp.toPx())
            lineTo(arrowX, arrowTop)
            lineTo(arrowX + 6.dp.toPx(), arrowTop + 8.dp.toPx())
        }
        drawPath(arrowHead, accentColor, style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round))

        // 2. Anatomical Artery Vessel tube with internal pulsating red lumen
        val tubeLeft = w * 0.45f
        val tubeWidth = w * 0.48f

        // Outer vessel wall (curved tubular contour)
        val outerWall = Path().apply {
            moveTo(tubeLeft, h * 0.2f)
            cubicTo(tubeLeft + 4.dp.toPx(), h * 0.45f, tubeLeft - 3.dp.toPx(), h * 0.7f, tubeLeft, h * 0.88f)
            lineTo(tubeLeft + tubeWidth, h * 0.88f)
            cubicTo(tubeLeft + tubeWidth - 3.dp.toPx(), h * 0.7f, tubeLeft + tubeWidth + 4.dp.toPx(), h * 0.45f, tubeLeft + tubeWidth, h * 0.2f)
            close()
        }
        drawPath(outerWall, Color(0xFFEF4444).copy(alpha = 0.25f))
        drawPath(outerWall, Color(0xFFEF4444), style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round))

        // Inner pulsatile arterial ring striations
        for (yOffset in listOf(h * 0.35f, h * 0.52f, h * 0.70f)) {
            drawLine(
                color = Color(0xFFEF4444).copy(alpha = 0.75f),
                start = Offset(tubeLeft + 5.dp.toPx(), yOffset),
                end = Offset(tubeLeft + tubeWidth - 5.dp.toPx(), yOffset),
                strokeWidth = 1.8.dp.toPx(),
                cap = StrokeCap.Round
            )
        }
    }
}
