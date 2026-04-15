package com.example.nettool

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Stroke
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

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF1A1A2E)) // 深色背景
    ) {
        val width = size.width
        val height = size.height
        val padding = 20.dp.toPx()
        val chartWidth = width - padding * 2
        val chartHeight = height - padding * 2
        val stepX = chartWidth / (times.size - 1).coerceAtLeast(1)

        // 绘制半透明网格线
        val gridColor = Color.White.copy(alpha = 0.15f)
        for (i in 0..4) {
            val y = padding + chartHeight * i / 4
            drawLine(gridColor, Offset(padding, y), Offset(width - padding, y), strokeWidth = 1f)
        }

        // 绘制数据线（带渐变效果）
        val path = Path()
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
        drawPath(
            path = path,
            color = Color(0xFF00FF88),
            style = Stroke(width = 3f)
        )

        // 绘制数据点
        times.forEachIndexed { index, time ->
            val x = padding + index * stepX
            val normalized = ((time - minTime) / range).toFloat().coerceIn(0f, 1f)
            val y = padding + chartHeight * (1 - normalized)
            drawCircle(
                color = Color(0xFF00FF88),
                radius = 4f,
                center = Offset(x, y)
            )
        }
    }
}
