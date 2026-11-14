package com.fitplan.app.ui.progress

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.unit.dp
import com.fitplan.app.data.Program
import com.fitplan.app.data.db.WeightLog
import kotlin.math.ceil
import kotlin.math.floor

/**
 * Grafik berat badan. Sumbu X = minggu program (0–12), sumbu Y = kg.
 * Garis putus-putus abu-abu = target (dari berat awal ke berat target).
 */
@Composable
fun WeightChart(
    entries: List<WeightLog>,
    startEpochDay: Long,
    startWeight: Float,
    goalWeight: Float,
) {
    val line = MaterialTheme.colorScheme.primary
    val surface = MaterialTheme.colorScheme.surfaceContainer
    val grid = MaterialTheme.colorScheme.surfaceVariant
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    val target = Color(0xFF9E9E9E)

    val lastDay = maxOf(Program.TOTAL_DAYS.toLong(), (entries.maxOfOrNull { it.date } ?: 0L) - startEpochDay)
    val values = entries.map { it.kg } + listOf(startWeight, goalWeight)
    val yMin = floor(values.min() - 1f)
    val yMax = ceil(values.max() + 1f)

    Canvas(Modifier.fillMaxWidth().height(240.dp)) {
        val left = 44.dp.toPx()
        val bottom = 26.dp.toPx()
        val w = size.width - left - 8.dp.toPx()
        val h = size.height - bottom - 8.dp.toPx()
        val top = 8.dp.toPx()
        fun x(day: Long) = left + w * (day.toFloat() / lastDay.toFloat()).coerceIn(0f, 1f)
        fun y(kg: Float) = top + h * (1f - (kg - yMin) / (yMax - yMin))

        val paint = android.graphics.Paint().apply {
            color = android.graphics.Color.argb(255, (muted.red * 255).toInt(), (muted.green * 255).toInt(), (muted.blue * 255).toInt())
            textSize = 12.dp.toPx()
            isAntiAlias = true
        }

        // Grid horizontal tipis + label kg
        val step = if (yMax - yMin > 12) 4 else if (yMax - yMin > 6) 2 else 1
        var g = yMin
        while (g <= yMax + 0.01f) {
            drawLine(grid, Offset(left, y(g)), Offset(left + w, y(g)), strokeWidth = 1.dp.toPx())
            drawContext.canvas.nativeCanvas.drawText("${g.toInt()}", 4.dp.toPx(), y(g) + 4.dp.toPx(), paint)
            g += step
        }
        // Label minggu
        for (week in 0..Program.TOTAL_WEEKS step 2) {
            val px = x(week * 7L)
            drawContext.canvas.nativeCanvas.drawText("M$week", px - 8.dp.toPx(), size.height - 6.dp.toPx(), paint)
        }

        // Garis target
        drawLine(
            target, Offset(x(0), y(startWeight)), Offset(x(Program.TOTAL_DAYS.toLong()), y(goalWeight)),
            strokeWidth = 2.dp.toPx(),
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f)),
        )

        // Garis aktual
        val pts = entries.sortedBy { it.date }.map { Offset(x(it.date - startEpochDay), y(it.kg)) }
        if (pts.size > 1) {
            val path = Path().apply {
                moveTo(pts.first().x, pts.first().y)
                pts.drop(1).forEach { lineTo(it.x, it.y) }
            }
            drawPath(path, line, style = Stroke(width = 2.dp.toPx()))
        }
        pts.forEach {
            drawCircle(surface, radius = 6.dp.toPx(), center = it)
            drawCircle(line, radius = 4.dp.toPx(), center = it)
        }
    }

    // Legenda
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(10.dp).background(line, CircleShape))
        Text("  Berat Anda", style = MaterialTheme.typography.bodyMedium, color = muted)
        Box(Modifier.padding(start = 16.dp).size(width = 18.dp, height = 2.dp).background(target))
        Text("  Target ${fmtKg(startWeight)} → ${fmtKg(goalWeight)} kg",
            style = MaterialTheme.typography.bodyMedium, color = muted)
    }
}

fun fmtKg(f: Float): String = if (f % 1f == 0f) f.toInt().toString() else "%.1f".format(f).replace('.', ',')
