package com.example.nettool

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

@Composable
fun PingChart(
    times: List<Double>,
    modifier: Modifier = Modifier
) {
    if (times.isEmpty()) return

    val maxTime = times.maxOrNull()?.coerceAtLeast(1.0) ?: 1.0
    val minTime = times.minOrNull()?.coerceAtLeast(0.0) ?: 0.0
    val range = (maxTime - minTime).coerceAtLeast(1.0)

    Canvas(modifier = modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height
        val padding = 20.dp.toPx()
        val chartWidth = width - padding * 2
        val chartHeight = height - padding * 2
        val stepX = chartWidth / (times.size - 1).coerceAtLeast(1)

        // 绘制网格线
        val gridColor = Color.Gray.copy(alpha = 0.3f)
        for (i in 0..4) {
            val y = padding + chartHeight * i / 4
            drawLine(gridColor, Offset(padding, y), Offset(width - padding, y), strokeWidth = 1f)
        }

        // 绘制数据线
        val path = androidx.compose.ui.graphics.Path()
        times.forEachIndexed { index, time ->
            val x = padding + index * stepX
            val normalized = ((time - minTime) / range).toFloat().coerceIn(0f, 1f)
            val y = padding + chartHeight * (1 - normalized)
            if (index == 0) {
                path.moveTo(x, y)
            } else {
                path.lineTo(x, y)
            }
        }
        drawPath(path, Color(0xFF4CAF50), style = Stroke(width = 3f))

        // 绘制数据点
        times.forEachIndexed { index, time ->
            val x = padding + index * stepX
            val normalized = ((time - minTime) / range).toFloat().coerceIn(0f, 1f)
            val y = padding + chartHeight * (1 - normalized)
            drawCircle(Color(0xFF2196F3), radius = 4f, center = Offset(x, y))
        }
    }
}
