package com.fitplan.app.ui.schedule

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fitplan.app.FitPlanApp
import com.fitplan.app.data.Program
import com.fitplan.app.data.indo
import com.fitplan.app.ui.common.ExerciseTable
import com.fitplan.app.ui.common.ScreenColumn
import com.fitplan.app.ui.common.SectionCard
import java.time.DayOfWeek
import java.time.LocalDate

@Composable
fun ScheduleScreen() {
    val settings by FitPlanApp.instance.prefs.settings.collectAsStateWithLifecycle()
    val start = settings.startDate
    val todayDow = remember { LocalDate.now().dayOfWeek }

    ScreenColumn(title = "Jadwal Mingguan") {
        if (start != null) {
            Text(
                "Program 12 minggu: ${start.indo()} sampai ${Program.endDate(start).indo()}",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            Text("Tanggal mulai belum dipilih (menu Atur).",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.tertiary)
        }
        Text("Ketuk hari latihan untuk melihat gerakannya.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant)

        DayOfWeek.entries.forEach { dow ->
            DayRow(dow, isToday = dow == todayDow)
        }
    }
}

@Composable
private fun DayRow(dow: DayOfWeek, isToday: Boolean) {
    val workout = Program.workoutFor(dow)
    var open by rememberSaveable(dow) { mutableStateOf(isToday && workout != null) }
    SectionCard(
        modifier = Modifier
            .animateContentSize()
            .clickable(enabled = workout != null) { open = !open },
        color = if (isToday) MaterialTheme.colorScheme.primaryContainer
        else MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                dow.indo() + if (isToday) " (hari ini)" else "",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f),
            )
            Text(
                workout?.title ?: "Libur",
                style = MaterialTheme.typography.bodyLarge,
                color = if (workout != null) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (workout != null) {
                Icon(if (open) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore, null)
            }
        }
        if (open && workout != null) ExerciseTable(workout)
    }
}
