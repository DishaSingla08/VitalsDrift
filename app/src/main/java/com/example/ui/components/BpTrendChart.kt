package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.ChartDataPoint
import com.example.ui.theme.VitalsTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt

enum class ChartRangeOption(val label: String, val days: Int) {
    DAYS_30("30D", 30),
    DAYS_90("90D", 90),
    DAYS_180("180D", 180),
    ALL("All", 999)
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BpTrendChart(
    dataPoints: List<ChartDataPoint>,
    selectedRange: ChartRangeOption,
    onRangeSelected: (ChartRangeOption) -> Unit,
    modifier: Modifier = Modifier
) {
    var showRawSpots by remember { mutableStateOf(true) }
    var show30dMa by remember { mutableStateOf(true) }
    var show90dMa by remember { mutableStateOf(true) }
    var show180dMa by remember { mutableStateOf(true) }

    var selectedPointIndex by remember { mutableIntStateOf(-1) }

    val semanticColors = VitalsTheme.colors

    val now = System.currentTimeMillis()
    val filteredPoints = remember(dataPoints, selectedRange) {
        if (selectedRange == ChartRangeOption.ALL) {
            dataPoints
        } else {
            val cutoff = now - TimeUnit.DAYS.toMillis(selectedRange.days.toLong())
            dataPoints.filter { it.timestamp >= cutoff }
        }
    }

    val textMeasurer = rememberTextMeasurer()

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("bp_trend_chart_card"),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
        tonalElevation = 2.dp
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Header with range pills
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Vascular Drift Curves",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Moving Averages vs. Spot Logs (mmHg)",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    ChartRangeOption.values().forEach { option ->
                        FilterChip(
                            selected = selectedRange == option,
                            onClick = {
                                onRangeSelected(option)
                                selectedPointIndex = -1
                            },
                            label = { Text(option.label, fontSize = 11.sp) },
                            modifier = Modifier
                                .height(32.dp)
                                .testTag("range_chip_${option.label}"),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Interactive Canvas
            if (filteredPoints.size < 2) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Log at least 2 readings in this window to view trend lines",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(230.dp)
                ) {
                    val gridColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
                    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant

                    Canvas(
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("canvas_chart")
                            .pointerInput(filteredPoints) {
                                detectTapGestures { offset ->
                                    selectedPointIndex = findNearestPointIndex(offset.x, size.width.toFloat(), filteredPoints.size)
                                }
                            }
                            .pointerInput(filteredPoints) {
                                detectDragGestures { change, _ ->
                                    change.consume()
                                    selectedPointIndex = findNearestPointIndex(change.position.x, size.width.toFloat(), filteredPoints.size)
                                }
                            }
                    ) {
                        drawChartCanvas(
                            points = filteredPoints,
                            showSpots = showRawSpots,
                            show30d = show30dMa,
                            show90d = show90dMa,
                            show180d = show180dMa,
                            selectedIndex = selectedPointIndex,
                            textMeasurer = textMeasurer,
                            gridColor = gridColor,
                            labelColor = labelColor,
                            c30 = semanticColors.chart30d,
                            c90 = semanticColors.chart90d,
                            c180 = semanticColors.chart180d,
                            cSpot = semanticColors.chartSpot
                        )
                    }
                }
            }

            // Scrubber tooltip bar
            val activePoint = if (selectedPointIndex in filteredPoints.indices) filteredPoints[selectedPointIndex] else null
            AnimatedVisibility(visible = activePoint != null) {
                activePoint?.let { pt ->
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surface,
                        tonalElevation = 4.dp
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                val dateStr = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(pt.timestamp))
                                Text(
                                    text = dateStr,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "${pt.rawSystolic}/${pt.rawDiastolic} mmHg (HR ${pt.heartRate})",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                pt.ma30Systolic?.let { ma30 ->
                                    Text(
                                        text = "30d MA: ${ma30.roundToInt()} mmHg",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = semanticColors.chart30d
                                    )
                                }
                                pt.ma180Systolic?.let { ma180 ->
                                    Text(
                                        text = "180d Base: ${ma180.roundToInt()} mmHg",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = semanticColors.chart180d
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Legend & Series Toggles
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                LegendToggle(
                    label = "30-Day MA",
                    color = semanticColors.chart30d,
                    isActive = show30dMa,
                    onToggle = { show30dMa = !show30dMa }
                )
                LegendToggle(
                    label = "90-Day MA",
                    color = semanticColors.chart90d,
                    isActive = show90dMa,
                    onToggle = { show90dMa = !show90dMa }
                )
                LegendToggle(
                    label = "180-Day Baseline",
                    color = semanticColors.chart180d,
                    isDashed = true,
                    isActive = show180dMa,
                    onToggle = { show180dMa = !show180dMa }
                )
                LegendToggle(
                    label = "Spot Logs",
                    color = semanticColors.chartSpot,
                    isDot = true,
                    isActive = showRawSpots,
                    onToggle = { showRawSpots = !showRawSpots }
                )
            }
        }
    }
}

private fun findNearestPointIndex(touchX: Float, width: Float, count: Int): Int {
    if (count <= 0 || width <= 0) return -1
    val leftPadding = 40f
    val rightPadding = 20f
    val usableWidth = width - leftPadding - rightPadding
    val clampedX = (touchX - leftPadding).coerceIn(0f, usableWidth)
    val ratio = clampedX / usableWidth
    val index = (ratio * (count - 1)).roundToInt()
    return index.coerceIn(0, count - 1)
}

private fun DrawScope.drawChartCanvas(
    points: List<ChartDataPoint>,
    showSpots: Boolean,
    show30d: Boolean,
    show90d: Boolean,
    show180d: Boolean,
    selectedIndex: Int,
    textMeasurer: TextMeasurer,
    gridColor: Color,
    labelColor: Color,
    c30: Color,
    c90: Color,
    c180: Color,
    cSpot: Color
) {
    val count = points.size
    if (count < 2) return

    val leftPadding = 42.dp.toPx()
    val rightPadding = 16.dp.toPx()
    val topPadding = 16.dp.toPx()
    val bottomPadding = 26.dp.toPx()

    val plotWidth = size.width - leftPadding - rightPadding
    val plotHeight = size.height - topPadding - bottomPadding

    // Determine scale: range from ~90 to ~160 mmHg
    val minSystolic = (points.minOfOrNull { it.rawSystolic } ?: 100).coerceAtMost(95)
    val maxSystolic = (points.maxOfOrNull { it.rawSystolic } ?: 150).coerceAtLeast(155)

    val yMin = (minSystolic / 10) * 10f
    val yMax = (((maxSystolic + 9) / 10) * 10f).coerceAtLeast(yMin + 40f)

    fun getY(value: Double): Float {
        val normalized = (value.toFloat() - yMin) / (yMax - yMin)
        return topPadding + plotHeight * (1f - normalized.coerceIn(0f, 1f))
    }

    fun getX(index: Int): Float {
        val fraction = index.toFloat() / (count - 1)
        return leftPadding + plotWidth * fraction
    }

    // Draw horizontal reference lines & clinical markers (120, 140 mmHg)
    val step = 20
    var currentYVal = ((yMin.toInt() / step) * step)
    while (currentYVal <= yMax.toInt()) {
        if (currentYVal >= yMin) {
            val yPos = getY(currentYVal.toDouble())
            val isClinicalLine = currentYVal == 120 || currentYVal == 140
            val lineEffect = if (isClinicalLine) PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f) else null

            drawLine(
                color = if (isClinicalLine) gridColor.copy(alpha = 0.35f) else gridColor,
                start = Offset(leftPadding, yPos),
                end = Offset(size.width - rightPadding, yPos),
                strokeWidth = if (isClinicalLine) 1.5.dp.toPx() else 1.dp.toPx(),
                pathEffect = lineEffect
            )

            // Label text on the left
            drawText(
                textMeasurer = textMeasurer,
                text = "$currentYVal",
                topLeft = Offset(4.dp.toPx(), yPos - 8.dp.toPx()),
                style = TextStyle(
                    color = if (isClinicalLine) labelColor else labelColor.copy(alpha = 0.6f),
                    fontSize = 9.sp
                )
            )
        }
        currentYVal += step
    }

    // 180-Day Baseline curve (Dashed amber)
    if (show180d) {
        val path180 = Path()
        var hasStarted = false
        for (i in 0 until count) {
            val ma = points[i].ma180Systolic
            if (ma != null) {
                val x = getX(i)
                val y = getY(ma)
                if (!hasStarted) {
                    path180.moveTo(x, y)
                    hasStarted = true
                } else {
                    path180.lineTo(x, y)
                }
            }
        }
        if (hasStarted) {
            drawPath(
                path = path180,
                color = c180,
                style = Stroke(
                    width = 2.5.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f), 0f),
                    cap = StrokeCap.Round
                )
            )
        }
    }

    // 90-Day Moving Average curve
    if (show90d) {
        val path90 = Path()
        var hasStarted = false
        for (i in 0 until count) {
            val ma = points[i].ma90Systolic
            if (ma != null) {
                val x = getX(i)
                val y = getY(ma)
                if (!hasStarted) {
                    path90.moveTo(x, y)
                    hasStarted = true
                } else {
                    path90.lineTo(x, y)
                }
            }
        }
        if (hasStarted) {
            drawPath(
                path = path90,
                color = c90,
                style = Stroke(width = 2.2.dp.toPx(), cap = StrokeCap.Round)
            )
        }
    }

    // 30-Day Moving Average curve
    if (show30d) {
        val path30 = Path()
        var hasStarted = false
        for (i in 0 until count) {
            val ma = points[i].ma30Systolic
            if (ma != null) {
                val x = getX(i)
                val y = getY(ma)
                if (!hasStarted) {
                    path30.moveTo(x, y)
                    hasStarted = true
                } else {
                    path30.lineTo(x, y)
                }
            }
        }
        if (hasStarted) {
            drawPath(
                path = path30,
                color = c30,
                style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
            )
        }
    }

    // Raw spots
    if (showSpots) {
        for (i in 0 until count) {
            val pt = points[i]
            val x = getX(i)
            val y = getY(pt.rawSystolic.toDouble())
            val dotColor = if (pt.isResting) cSpot.copy(alpha = 0.55f) else Color(0xFFEF4444).copy(alpha = 0.8f)

            drawCircle(
                color = dotColor,
                radius = 3.dp.toPx(),
                center = Offset(x, y)
            )
        }
    }

    // Scrubber indicator if a point is selected
    if (selectedIndex in 0 until count) {
        val x = getX(selectedIndex)
        drawLine(
            color = Color.White.copy(alpha = 0.8f),
            start = Offset(x, topPadding),
            end = Offset(x, topPadding + plotHeight),
            strokeWidth = 1.5.dp.toPx(),
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)
        )

        val pt = points[selectedIndex]
        val ySpot = getY(pt.rawSystolic.toDouble())
        drawCircle(
            color = Color.White,
            radius = 6.dp.toPx(),
            center = Offset(x, ySpot)
        )
        drawCircle(
            color = c30,
            radius = 4.dp.toPx(),
            center = Offset(x, ySpot)
        )
    }

    // X-axis timestamps: first, middle, last
    val firstDate = SimpleDateFormat("MMM d", Locale.getDefault()).format(Date(points.first().timestamp))
    val midDate = SimpleDateFormat("MMM d", Locale.getDefault()).format(Date(points[count / 2].timestamp))
    val lastDate = SimpleDateFormat("MMM d", Locale.getDefault()).format(Date(points.last().timestamp))

    val yText = size.height - 10.dp.toPx()
    drawText(
        textMeasurer = textMeasurer,
        text = firstDate,
        topLeft = Offset(leftPadding, yText),
        style = TextStyle(color = labelColor, fontSize = 9.sp)
    )
    drawText(
        textMeasurer = textMeasurer,
        text = midDate,
        topLeft = Offset(leftPadding + (plotWidth / 2) - 15.dp.toPx(), yText),
        style = TextStyle(color = labelColor, fontSize = 9.sp)
    )
    drawText(
        textMeasurer = textMeasurer,
        text = lastDate,
        topLeft = Offset(size.width - rightPadding - 35.dp.toPx(), yText),
        style = TextStyle(color = labelColor, fontSize = 9.sp)
    )
}

@Composable
private fun LegendToggle(
    label: String,
    color: Color,
    isActive: Boolean,
    onToggle: () -> Unit,
    isDashed: Boolean = false,
    isDot: Boolean = false
) {
    Surface(
        onClick = onToggle,
        shape = RoundedCornerShape(8.dp),
        color = if (isActive) color.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surface.copy(alpha = 0.4f),
        modifier = Modifier.height(28.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            if (isDot) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(color, CircleShape)
                )
            } else {
                Box(
                    modifier = Modifier
                        .width(14.dp)
                        .height(3.dp)
                        .background(color, RoundedCornerShape(1.dp))
                )
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = if (isActive) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                fontSize = 11.sp
            )
        }
    }
}
