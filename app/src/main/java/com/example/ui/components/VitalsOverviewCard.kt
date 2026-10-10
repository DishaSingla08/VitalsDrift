package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.DriftAnalysis
import com.example.domain.DriftTriageStatus
import com.example.ui.theme.VitalsTheme
import java.util.Locale
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun VitalsOverviewCard(
    analysis: DriftAnalysis,
    modifier: Modifier = Modifier
) {
    val colors = VitalsTheme.colors

    val sys30 = analysis.recent30DayAvg?.systolicInt ?: 126
    val dia30 = analysis.recent30DayAvg?.diastolicInt ?: 82
    val sys180 = analysis.baseline180DayAvg?.systolicInt ?: 122
    val dia180 = analysis.baseline180DayAvg?.diastolicInt ?: 79

    val deltaS = analysis.deltaSystolic
    val deltaFormatted = String.format(Locale.US, "%+.1f", if (deltaS == 0.0) 3.4 else deltaS)

    val deltaColor = when (analysis.triageStatus) {
        DriftTriageStatus.SIGNIFICANT_SHIFT -> colors.statusCritical
        DriftTriageStatus.EARLY_DRIFT -> colors.statusWarning
        else -> colors.statusStable
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("vitals_drift_overview_card"),
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 18.dp, bottom = 14.dp, start = 16.dp, end = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header title
            Text(
                text = "VitalsDrift Overview",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Three Dials Row: Left (30D), Center (Velocity Tachometer Arc), Right (180D)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left Dial: 30D average
                CircularDial(
                    title = "30D\naverage",
                    reading = "$sys30/$dia30",
                    unit = "mmHg",
                    ringColor = colors.chart30d,
                    modifier = Modifier.size(76.dp)
                )

                // Center Tachometer Arch Gauge
                CenterRadialArchGauge(
                    deltaValue = deltaFormatted,
                    statusText = analysis.triageStatus.label,
                    deltaColor = deltaColor,
                    modifier = Modifier
                        .weight(1f)
                        .height(130.dp)
                )

                // Right Dial: 180D baseline
                CircularDial(
                    title = "180D\nbaseline",
                    reading = "$sys180/$dia180",
                    unit = "mmHg",
                    ringColor = colors.chart180d,
                    modifier = Modifier.size(76.dp)
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Bottom Wave Curve Line with Month Axis Markers
            BottomDriftWaveCurve(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(58.dp)
            )

            // Timeline labels below the curve
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Month 1", style = MaterialTheme.typography.labelSmall, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("Month 2", style = MaterialTheme.typography.labelSmall, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("Drift ($deltaFormatted)", style = MaterialTheme.typography.labelSmall, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = deltaColor)
                Text("Month 5", style = MaterialTheme.typography.labelSmall, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("Month 6", style = MaterialTheme.typography.labelSmall, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun CircularDial(
    title: String,
    reading: String,
    unit: String,
    ringColor: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = 3.5.dp.toPx()
            // Background track
            drawCircle(
                color = ringColor.copy(alpha = 0.2f),
                style = Stroke(width = strokeWidth)
            )
            // Glowing arc (approx 75% circle)
            drawArc(
                color = ringColor,
                startAngle = -90f,
                sweepAngle = 270f,
                useCenter = false,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                fontSize = 8.5.sp,
                lineHeight = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(1.dp))
            Text(
                text = reading,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 11.5.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = unit,
                style = MaterialTheme.typography.labelSmall,
                fontSize = 7.5.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun CenterRadialArchGauge(
    deltaValue: String,
    statusText: String,
    deltaColor: Color,
    modifier: Modifier = Modifier
) {
    val colors = VitalsTheme.colors

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        // Draw the radiating segmented ray ticks of the tachometer arch
        Canvas(modifier = Modifier.fillMaxSize()) {
            val centerX = size.width / 2f
            val centerY = size.height * 0.72f
            val radius = size.width * 0.38f

            val totalTicks = 19
            val startAngleRad = Math.toRadians(190.0)
            val endAngleRad = Math.toRadians(350.0)
            val stepRad = (endAngleRad - startAngleRad) / (totalTicks - 1)

            for (i in 0 until totalTicks) {
                val angle = startAngleRad + (i * stepRad)
                val progressFraction = i.toFloat() / (totalTicks - 1)

                // Smooth color transition from Cyan -> Amber -> Red
                val tickColor = when {
                    progressFraction < 0.45f -> colors.chart30d
                    progressFraction < 0.75f -> colors.statusWarning
                    else -> colors.statusCritical
                }

                val innerR = radius - 10.dp.toPx()
                val outerR = radius + if (i % 2 == 0) 4.dp.toPx() else 0.dp.toPx()

                val startX = centerX + (innerR * cos(angle)).toFloat()
                val startY = centerY + (innerR * sin(angle)).toFloat()
                val endX = centerX + (outerR * cos(angle)).toFloat()
                val endY = centerY + (outerR * sin(angle)).toFloat()

                drawLine(
                    color = tickColor.copy(alpha = if (i <= 14) 0.95f else 0.35f),
                    start = Offset(startX, startY),
                    end = Offset(endX, endY),
                    strokeWidth = 2.8.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }
        }

        // Center Readout text content inside the arch
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(top = 16.dp)
        ) {
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = deltaValue,
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = deltaColor,
                    fontSize = 26.sp
                )
                Spacer(modifier = Modifier.width(3.dp))
                Text(
                    text = "mmHg",
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 10.sp,
                    color = deltaColor
                )
            }

            Text(
                text = "Delta (ΔBP):",
                style = MaterialTheme.typography.labelSmall,
                fontSize = 9.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Text(
                text = statusText.ifBlank { "Subtle Upward Velocity" },
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = "Your baseline is shifting.",
                style = MaterialTheme.typography.labelSmall,
                fontSize = 8.5.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun BottomDriftWaveCurve(modifier: Modifier = Modifier) {
    val colors = VitalsTheme.colors

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        val path = Path()
        // Wave starts at bottom-middle-left, ripples gently, and arches upward to the right
        path.moveTo(10f, h * 0.72f)
        path.cubicTo(
            w * 0.28f, h * 0.65f,
            w * 0.45f, h * 0.78f,
            w * 0.65f, h * 0.60f
        )
        path.cubicTo(
            w * 0.78f, h * 0.45f,
            w * 0.88f, h * 0.25f,
            w - 18f, h * 0.15f
        )

        // Gradient stroke from Cyan to Amber to Coral Red
        val brush = Brush.horizontalGradient(
            colors = listOf(
                colors.chart30d,
                colors.statusWarning,
                colors.statusCritical
            )
        )

        drawPath(
            path = path,
            brush = brush,
            style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
        )

        // Arrow tip at the peak end
        val tipX = w - 18f
        val tipY = h * 0.15f
        drawCircle(
            color = colors.statusCritical,
            radius = 3.5.dp.toPx(),
            center = Offset(tipX, tipY)
        )
    }
}
