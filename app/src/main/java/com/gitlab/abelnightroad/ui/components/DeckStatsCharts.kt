package com.gitlab.abelnightroad.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.nativeCanvas
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
        val maxCount = data.maxOfOrNull { it.count } ?: 1
        val barWidth = size.width / data.size * 0.7f
        val spacing = size.width / data.size * 0.3f
        val bottomPadding = 30f
        val topPadding = 20f
        val chartHeight = size.height - bottomPadding - topPadding

        data.forEachIndexed { index, stat ->
            val barHeight = if (maxCount > 0) (stat.count.toFloat() / maxCount) * chartHeight else 0f
            val x = index * (barWidth + spacing) + spacing / 2
            val y = size.height - bottomPadding - barHeight

            drawRect(
                color = chartColors[index % chartColors.size],
                topLeft = Offset(x, y),
                size = Size(barWidth, barHeight)
            )

            drawContext.canvas.nativeCanvas.apply {
                val paint = android.graphics.Paint().apply {
                    color = textColor.hashCode()
                    textSize = 24f
                    textAlign = android.graphics.Paint.Align.CENTER
                    isAntiAlias = true
                }
                drawText(stat.cmc.toString(), x + barWidth / 2, size.height - 5f, paint)
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
    val textColor = MaterialTheme.colorScheme.onSurface
    Canvas(modifier.fillMaxWidth().height(200.dp)) {
        if (data.isEmpty()) return@Canvas
        val total = data.sumOf { it.count }.toFloat()
        val strokeWidth = 40f
        val diameter = minOf(size.width, size.height) - strokeWidth * 2
        val topLeft = Offset(
            (size.width - diameter) / 2f,
            (size.height - diameter) / 2f
        )
        var startAngle = -90f

        data.forEachIndexed { index, stat ->
            val sweep = (stat.count / total) * 360f
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

        drawContext.canvas.nativeCanvas.apply {
            val paint = android.graphics.Paint().apply {
                this.color = textColor.hashCode()
                textSize = 28f
                textAlign = android.graphics.Paint.Align.CENTER
                isAntiAlias = true
            }
            drawText(total.toInt().toString(), size.width / 2, size.height / 2 + 10f, paint)
        }
    }
}

@Composable
internal fun HorizontalBarChart(
    entries: List<Pair<String, Int>>,
    colors: List<Color> = chartColors,
    modifier: Modifier = Modifier
) {
    val textColor = MaterialTheme.colorScheme.onSurface
    Canvas(modifier.fillMaxWidth().height((entries.size * 32 + 20).dp)) {
        if (entries.isEmpty()) return@Canvas
        val maxCount = entries.maxOfOrNull { it.second } ?: 1
        val barHeight = 20f
        val spacing = 12f
        val labelWidth = 120f
        val chartWidth = size.width - labelWidth - 60f

        entries.forEachIndexed { index, (label, count) ->
            val y = index * (barHeight + spacing)
            val barWidth = if (maxCount > 0) (count.toFloat() / maxCount) * chartWidth else 0f

            drawContext.canvas.nativeCanvas.apply {
                val paint = android.graphics.Paint().apply {
                    color = textColor.hashCode()
                    textSize = 22f
                    isAntiAlias = true
                }
                drawText(label, 0f, y + barHeight - 2f, paint)
            }

            drawRect(
                color = colors[index % colors.size],
                topLeft = Offset(labelWidth, y),
                size = Size(barWidth, barHeight)
            )

            drawContext.canvas.nativeCanvas.apply {
                val paint = android.graphics.Paint().apply {
                    color = textColor.hashCode()
                    textSize = 22f
                    isAntiAlias = true
                }
                drawText(count.toString(), labelWidth + barWidth + 8f, y + barHeight - 2f, paint)
            }
        }
    }
}
