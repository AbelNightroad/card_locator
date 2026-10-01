package com.gitlab.abelnightroad.ui.components

import androidx.compose.foundation.Canvas
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gitlab.abelnightroad.ui.CmcStat
import com.gitlab.abelnightroad.ui.ColorStat

private val mtgColors = mapOf(
    "W" to Color(0xFFF9FAF4),
    "U" to Color(0xFF0E68AB),
    "B" to Color(0xFF150B00),
    "R" to Color(0xFFD3202A),
    "G" to Color(0xFF00733E),
    "C" to Color(0xFFCCCCCC),
)

private val chartColors = listOf(
    Color(0xFF0E68AB),
    Color(0xFFD3202A),
    Color(0xFF00733E),
    Color(0xFFA855F7),
    Color(0xFFF59E0B),
    Color(0xFFEC4899),
    Color(0xFF6366F1),
    Color(0xFF14B8A6)
)

private val rarityColors = mapOf(
    "common" to Color(0xFF888888),
    "uncommon" to Color(0xFFC0C0C0),
    "rare" to Color(0xFFDAA520),
    "mythic" to Color(0xFFFF6347),
    "special" to Color(0xFF9370DB),
)

@Composable
internal fun BarChart(
    data: List<CmcStat>,
    modifier: Modifier = Modifier
) {
    val textColor = MaterialTheme.colorScheme.onSurface
    Canvas(modifier.fillMaxWidth().height(200.dp)) {
        if (data.isEmpty()) return@Canvas
        val maxCount = (data.maxOfOrNull { it.count } ?: 1).coerceAtLeast(1)
        val barWidth = (size.width / data.size * 0.7f).coerceAtLeast(0f)
        val spacing = size.width / data.size * 0.3f
        val bottomPadding = 30f
        val topPadding = 20f
        val chartHeight = (size.height - bottomPadding - topPadding).coerceAtLeast(0f)

        data.forEachIndexed { index, stat ->
            val barHeight = (stat.count.toFloat() / maxCount) * chartHeight
            val x = index * (barWidth + spacing) + spacing / 2
            val y = size.height - bottomPadding - barHeight

            drawRect(
                color = chartColors[index % chartColors.size],
                topLeft = Offset(x, y),
                size = Size(barWidth, barHeight)
            )

            drawContext.canvas.nativeCanvas.apply {
                val paint = android.graphics.Paint().apply {
                    color = textColor.toArgb()
                    textSize = 24f
                    textAlign = android.graphics.Paint.Align.CENTER
                    isAntiAlias = true
                }
                drawText(stat.label, x + barWidth / 2, size.height - 5f, paint)
                if (stat.count > 0) {
                    drawText(stat.count.toString(), x + barWidth / 2, y - 5f, paint)
                }
            }
        }
    }
}

@Composable
internal fun DonutChart(
    data: List<ColorStat>,
    modifier: Modifier = Modifier
) {
    Canvas(modifier.fillMaxWidth().height(160.dp)) {
        if (data.isEmpty()) return@Canvas
        val total = data.sumOf { it.count }.toFloat()
        if (total <= 0f || !total.isFinite()) return@Canvas
        val strokeWidth = 40f
        val diameter = (minOf(size.width, size.height) - strokeWidth * 2).coerceAtLeast(0f)
        val topLeft = Offset(
            (size.width - diameter) / 2f,
            (size.height - diameter) / 2f
        )
        var startAngle = -90f

        data.forEachIndexed { index, stat ->
            val sweep = (stat.count / total) * 360f
            if (!sweep.isFinite() || sweep <= 0f) return@forEachIndexed
            val color = mtgColors[stat.color] ?: chartColors[index % chartColors.size]
            drawArc(
                color = color,
                startAngle = startAngle,
                sweepAngle = sweep,
                useCenter = false,
                topLeft = topLeft,
                size = Size(diameter, diameter),
                style = androidx.compose.ui.graphics.drawscope.Stroke(
                    width = strokeWidth,
                    cap = androidx.compose.ui.graphics.StrokeCap.Butt
                )
            )
            startAngle += sweep
        }
    }
}

@Composable
internal fun PieChartWithLegend(
    entries: List<Pair<String, Int>>,
    modifier: Modifier = Modifier
) {
    val total = entries.sumOf { it.second }.coerceAtLeast(1)
    Column(modifier.fillMaxWidth()) {
        Canvas(Modifier.fillMaxWidth().height(200.dp)) {
            if (entries.isEmpty()) return@Canvas
            val sum = entries.sumOf { it.second }.toFloat()
            if (sum <= 0f || !sum.isFinite()) return@Canvas
            val diameter = minOf(size.width, size.height)
            val topLeft = Offset((size.width - diameter) / 2f, (size.height - diameter) / 2f)
            var startAngle = -90f
            entries.forEachIndexed { index, (_, count) ->
                val sweep = (count / sum) * 360f
                if (!sweep.isFinite() || sweep <= 0f) return@forEachIndexed
                drawArc(
                    color = chartColors[index % chartColors.size],
                    startAngle = startAngle,
                    sweepAngle = sweep,
                    useCenter = true,
                    topLeft = topLeft,
                    size = Size(diameter, diameter)
                )
                startAngle += sweep
            }
        }
        Spacer(Modifier.height(8.dp))
        entries.forEachIndexed { index, (label, count) ->
            Row(
                Modifier.fillMaxWidth().padding(vertical = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        Modifier.size(10.dp).background(
                            chartColors[index % chartColors.size],
                            CircleShape
                        )
                    )
                    Text("$label: $count", style = MaterialTheme.typography.bodyMedium)
                }
                Text(
                    "%.0f%%".format(count * 100f / total),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}
