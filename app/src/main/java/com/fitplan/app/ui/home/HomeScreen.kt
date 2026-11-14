package com.fitplan.app.ui.home

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EventAvailable
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material3.MaterialTheme
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
