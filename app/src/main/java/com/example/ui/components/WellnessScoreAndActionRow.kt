package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.example.ui.theme.VitalsTheme

@Composable
fun WellnessScoreAndActionRow(
    score: Int = 85,
    onOpenLogSheet: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = VitalsTheme.colors

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left: Cardiovascular Wellness Score Card
        Card(
            modifier = Modifier
                .weight(1.3f)
                .height(115.dp)
                .testTag("wellness_score_card"),
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Cardiovascular\nWellness score",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    lineHeight = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Geometric wireframe heart Canvas
                    GeometricWireframeHeart(
                        tintColor = colors.chart30d,
                        modifier = Modifier.size(46.dp)
                    )

                    // Score with glowing ambient particle dots
                    Box(contentAlignment = Alignment.Center) {
                        // Ambient particle dots
                        Canvas(modifier = Modifier.size(56.dp)) {
                            val dotColor = Color(0xFF34D399) // Emerald
                            drawCircle(dotColor.copy(alpha = 0.7f), radius = 2.5.dp.toPx(), center = Offset(4.dp.toPx(), 10.dp.toPx()))
                            drawCircle(dotColor.copy(alpha = 0.5f), radius = 2.dp.toPx(), center = Offset(14.dp.toPx(), 4.dp.toPx()))
                            drawCircle(dotColor.copy(alpha = 0.8f), radius = 3.dp.toPx(), center = Offset(44.dp.toPx(), 8.dp.toPx()))
                            drawCircle(dotColor.copy(alpha = 0.6f), radius = 2.5.dp.toPx(), center = Offset(50.dp.toPx(), 28.dp.toPx()))
                            drawCircle(dotColor.copy(alpha = 0.7f), radius = 2.dp.toPx(), center = Offset(42.dp.toPx(), 44.dp.toPx()))
                            drawCircle(dotColor.copy(alpha = 0.4f), radius = 2.dp.toPx(), center = Offset(18.dp.toPx(), 48.dp.toPx()))
                        }

                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(
                                text = "$score",
                                style = MaterialTheme.typography.headlineLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF10B981),
                                fontSize = 28.sp
                            )
                            Text(
                                text = "/100",
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        // Right: + Log BP Action Capsule Button
        Button(
            onClick = onOpenLogSheet,
            modifier = Modifier
                .weight(0.9f)
                .height(58.dp)
                .testTag("action_log_bp_btn"),
            shape = RoundedCornerShape(20.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 3.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = "Log BP",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            }
        }
    }
}

@Composable
private fun GeometricWireframeHeart(
    tintColor: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val stroke = Stroke(width = 1.6.dp.toPx(), cap = StrokeCap.Round)

        // Outer faceted heart polygon
        val p = Path()
        val topCenter = Offset(w * 0.5f, h * 0.28f)
        val leftPeak = Offset(w * 0.22f, h * 0.12f)
        val leftTop = Offset(w * 0.08f, h * 0.32f)
        val leftMid = Offset(w * 0.15f, h * 0.60f)
        val bottomTip = Offset(w * 0.5f, h * 0.92f)
        val rightMid = Offset(w * 0.85f, h * 0.60f)
        val rightTop = Offset(w * 0.92f, h * 0.32f)
        val rightPeak = Offset(w * 0.78f, h * 0.12f)

        // Outer silhouette
        p.moveTo(topCenter.x, topCenter.y)
        p.lineTo(leftPeak.x, leftPeak.y)
        p.lineTo(leftTop.x, leftTop.y)
        p.lineTo(leftMid.x, leftMid.y)
        p.lineTo(bottomTip.x, bottomTip.y)
        p.lineTo(rightMid.x, rightMid.y)
        p.lineTo(rightTop.x, rightTop.y)
        p.lineTo(rightPeak.x, rightPeak.y)
        p.close()

        drawPath(p, tintColor, style = stroke)

        // Internal faceted facets lines
        val centerInner = Offset(w * 0.5f, h * 0.52f)
        drawLine(tintColor.copy(alpha = 0.6f), topCenter, centerInner, strokeWidth = 1.2.dp.toPx())
        drawLine(tintColor.copy(alpha = 0.6f), centerInner, bottomTip, strokeWidth = 1.2.dp.toPx())
        drawLine(tintColor.copy(alpha = 0.6f), leftPeak, centerInner, strokeWidth = 1.2.dp.toPx())
        drawLine(tintColor.copy(alpha = 0.6f), rightPeak, centerInner, strokeWidth = 1.2.dp.toPx())
        drawLine(tintColor.copy(alpha = 0.6f), leftTop, centerInner, strokeWidth = 1.2.dp.toPx())
        drawLine(tintColor.copy(alpha = 0.6f), rightTop, centerInner, strokeWidth = 1.2.dp.toPx())
        drawLine(tintColor.copy(alpha = 0.6f), leftMid, centerInner, strokeWidth = 1.2.dp.toPx())
        drawLine(tintColor.copy(alpha = 0.6f), rightMid, centerInner, strokeWidth = 1.2.dp.toPx())
    }
}
