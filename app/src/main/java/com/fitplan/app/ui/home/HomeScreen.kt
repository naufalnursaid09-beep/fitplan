package com.fitplan.app.ui.home

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EventAvailable
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.fitplan.app.ui.theme.Amber
import kotlin.math.roundToInt
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fitplan.app.FitPlanApp
import com.fitplan.app.data.Program
import com.fitplan.app.data.indo
import com.fitplan.app.data.indoShort
import com.fitplan.app.ui.common.BigButton
import com.fitplan.app.ui.common.ExerciseTable
import com.fitplan.app.ui.common.ScreenColumn
import com.fitplan.app.ui.common.SectionCard
import java.time.LocalDate

@Composable
fun HomeScreen(onOpenSettings: () -> Unit, onStartWorkout: (Long, String) -> Unit) {
    val settings by FitPlanApp.instance.prefs.settings.collectAsStateWithLifecycle()
    val today = remember { LocalDate.now() }
    val start = settings.startDate
    val status = Program.status(start, today)

    ScreenColumn(title = "Hari Ini") {
        Text(today.indo(), style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant)

        // Kalori & protein hari ini
        val foods by remember { FitPlanApp.instance.db.foods().observeDate(today.toEpochDay()) }
            .collectAsStateWithLifecycle(emptyList())
        val kcal = foods.sumOf { it.kcal }
        val protein = foods.sumOf { it.protein.toDouble() }.toFloat()
        SectionCard {
            Text("Nutrisi hari ini", style = MaterialTheme.typography.titleLarge)
            TargetBar("Kalori", kcal.toFloat(), settings.targetKcal.toFloat(), "kkal")
            TargetBar("Protein", protein, settings.targetProtein.toFloat(), "g")
            Text("Catat makanan di menu Makanan.", style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        when (status) {
            Program.Status.NotSet -> {
                SectionCard {
                    Text("Selamat datang di FitPlan!", style = MaterialTheme.typography.titleLarge)
                    Text("Pilih dulu tanggal mulai program 12 minggu Anda.",
                        style = MaterialTheme.typography.bodyLarge)
                }
                BigButton("Pilih tanggal mulai", onOpenSettings, icon = Icons.Filled.EventAvailable)
            }
            is Program.Status.NotStarted -> SectionCard {
                Text("Program mulai ${start!!.indo()}", style = MaterialTheme.typography.titleMedium)
                Text("${status.daysLeft} hari lagi. Siapkan dumbbell Anda!",
                    style = MaterialTheme.typography.bodyLarge)
            }
            is Program.Status.Running -> SectionCard(color = MaterialTheme.colorScheme.primaryContainer) {
                Text("Minggu ${status.week} dari ${Program.TOTAL_WEEKS}",
                    style = MaterialTheme.typography.titleLarge)
                Text("Program selesai ${Program.endDate(start!!).indo()}",
                    style = MaterialTheme.typography.bodyMedium)
            }
            Program.Status.Finished -> SectionCard(color = MaterialTheme.colorScheme.primaryContainer) {
                Text("Program 12 minggu selesai! 🎉", style = MaterialTheme.typography.titleLarge)
                Text("Anda bisa memulai putaran baru dengan mengganti tanggal mulai di menu Atur.",
                    style = MaterialTheme.typography.bodyLarge)
            }
        }

        val workout = Program.workoutOn(start, today)
        if (workout != null) {
            SectionCard {
                Text("Latihan hari ini: ${workout.title}", style = MaterialTheme.typography.titleLarge)
                ExerciseTable(workout)
            }
            BigButton("Mulai latihan", onClick = { onStartWorkout(today.toEpochDay(), workout.key) },
                icon = Icons.Filled.FitnessCenter)
        } else if (status is Program.Status.Running) {
            SectionCard {
                Text("Hari libur 😴", style = MaterialTheme.typography.titleLarge)
                Text("Istirahat, cukup tidur, dan tetap kejar target protein.",
                    style = MaterialTheme.typography.bodyLarge)
            }
            SectionCard {
                Text("Kelewatan latihan? Kerjakan susulan hari ini:",
                    style = MaterialTheme.typography.bodyLarge)
                Program.weekly.values.forEach { w ->
                    BigButton(w.title, outlined = true,
                        onClick = { onStartWorkout(today.toEpochDay(), w.key) })
                }
            }
        }

        // Latihan berikutnya (setelah hari ini)
        if (status !is Program.Status.Finished) {
            val from = if (start != null && start.isAfter(today)) start.minusDays(1) else today
            val next = (1..14L).map { from.plusDays(it) }
                .firstOrNull { Program.workoutOn(start, it) != null }
            if (next != null) {
                val w = Program.workoutOn(start, next)!!
                SectionCard {
                    Text("Latihan berikutnya", style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("${next.dayOfWeek.indo()} ${next.indoShort()}: ${w.title}",
                        style = MaterialTheme.typography.bodyLarge)
                }
            }
        }
    }
}

@Composable
private fun TargetBar(label: String, value: Float, target: Float, unit: String) {
    val over = value > target
    Row(Modifier.fillMaxWidth()) {
        Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        Text("${value.roundToInt()} / ${target.roundToInt()} $unit",
            style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
    }
    LinearProgressIndicator(
        progress = { if (target <= 0f) 0f else (value / target).coerceIn(0f, 1f) },
        color = if (over) Amber else MaterialTheme.colorScheme.primary,
        trackColor = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.fillMaxWidth().height(14.dp).clip(RoundedCornerShape(7.dp)),
    )
    val left = target - value
    Text(
        if (left > 0) "Sisa ${left.roundToInt()} $unit" else "Target tercapai (lebih ${(-left).roundToInt()} $unit)",
        style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}
