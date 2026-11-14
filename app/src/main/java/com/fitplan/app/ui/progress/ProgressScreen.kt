package com.fitplan.app.ui.progress

import android.widget.Toast
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MonitorWeight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fitplan.app.FitPlanApp
import com.fitplan.app.data.Program
import com.fitplan.app.data.db.FoodLog
import com.fitplan.app.data.db.SetLog
import com.fitplan.app.data.db.WeightLog
import com.fitplan.app.data.indo
import com.fitplan.app.ui.common.BigButton
import com.fitplan.app.ui.common.ScreenColumn
import com.fitplan.app.ui.common.SectionCard
import com.fitplan.app.ui.food.num
import com.fitplan.app.ui.workout.kgText
import kotlinx.coroutines.launch
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProgressScreen() {
    val app = FitPlanApp.instance
    val settings by app.prefs.settings.collectAsStateWithLifecycle()
    val weights by remember { app.db.weights().observeAll() }.collectAsStateWithLifecycle(emptyList())
    val sets by remember { app.db.sets().observeAllDone() }.collectAsStateWithLifecycle(emptyList())
    val foods by remember { app.db.foods().observeAll() }.collectAsStateWithLifecycle(emptyList())
    var tab by rememberSaveable { mutableStateOf(0) }

    ScreenColumn(title = "Progres") {
        WeightSection(weights, settings.startDate, settings.startWeight, settings.goalWeight)

        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
            listOf("Riwayat latihan", "Riwayat makanan").forEachIndexed { i, label ->
                SegmentedButton(
                    selected = tab == i,
                    onClick = { tab = i },
                    shape = SegmentedButtonDefaults.itemShape(i, 2),
                ) { Text(label) }
            }
        }
        if (tab == 0) WorkoutHistory(sets) else FoodHistory(foods, settings.targetKcal, settings.targetProtein)
    }
}

@Composable
private fun WeightSection(weights: List<WeightLog>, start: LocalDate?, startWeight: Float, goalWeight: Float) {
    val app = FitPlanApp.instance
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var input by remember { mutableStateOf("") }
    var toDelete by remember { mutableStateOf<WeightLog?>(null) }
    val today = LocalDate.now()

    SectionCard {
        Text("Berat badan", style = MaterialTheme.typography.titleLarge)
        val latest = weights.maxByOrNull { it.date }
        if (latest != null) {
            val lost = startWeight - latest.kg
            val toGo = latest.kg - goalWeight
            Text("Terakhir: ${fmtKg(latest.kg)} kg (${LocalDate.ofEpochDay(latest.date).indo()})",
                style = MaterialTheme.typography.titleMedium)
            Text(
                (if (lost >= 0) "Turun ${fmtKg(lost)} kg" else "Naik ${fmtKg(-lost)} kg") + " dari awal · " +
                    (if (toGo > 0) "${fmtKg(toGo)} kg lagi menuju target" else "Target tercapai! 🎉"),
                style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.primary,
            )
        } else {
            Text("Belum ada catatan. Timbang tiap Minggu pagi lalu catat di sini.",
                style = MaterialTheme.typography.bodyLarge)
        }
        OutlinedTextField(
            value = input,
            onValueChange = { v -> input = v.filter { it.isDigit() || it == ',' || it == '.' }.take(5) },
            label = { Text("Berat hari ini (kg)") },
            singleLine = true,
            textStyle = MaterialTheme.typography.titleMedium,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth(),
        )
        val kg = input.replace(',', '.').toFloatOrNull()
        BigButton("Simpan berat", icon = Icons.Filled.MonitorWeight, enabled = kg != null && kg in 30f..300f, onClick = {
            scope.launch {
                app.db.weights().put(WeightLog(date = today.toEpochDay(), kg = kg!!))
                input = ""
                Toast.makeText(context, "Berat tersimpan", Toast.LENGTH_SHORT).show()
            }
        })

        val startDay = start?.toEpochDay() ?: weights.minOfOrNull { it.date } ?: today.toEpochDay()
        WeightChart(weights, startDay, startWeight, goalWeight)

        if (weights.isNotEmpty()) {
            Text("Catatan", style = MaterialTheme.typography.titleMedium)
            weights.sortedByDescending { it.date }.forEach { w ->
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(LocalDate.ofEpochDay(w.date).indo(), style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.weight(1f))
                    Text("${fmtKg(w.kg)} kg", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                    IconButton(onClick = { toDelete = w }) { Icon(Icons.Filled.Delete, "Hapus") }
                }
            }
        }
    }
    toDelete?.let { w ->
        AlertDialog(
            onDismissRequest = { toDelete = null },
            title = { Text("Hapus catatan berat?") },
            text = { Text("${fmtKg(w.kg)} kg pada ${LocalDate.ofEpochDay(w.date).indo()}") },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch { app.db.weights().delete(w) }
                    toDelete = null
                }) { Text("Hapus") }
            },
            dismissButton = { TextButton(onClick = { toDelete = null }) { Text("Batal") } },
        )
    }
}

@Composable
private fun WorkoutHistory(sets: List<SetLog>) {
    if (sets.isEmpty()) {
        SectionCard { Text("Belum ada latihan tercatat.", style = MaterialTheme.typography.bodyLarge) }
        return
    }
    val names = remember { Program.weekly.values.flatMap { it.exercises }.associate { it.id to it.name } }
    sets.groupBy { it.date to it.dayKey }.forEach { (group, daySets) ->
        val (date, dayKey) = group
        key(date, dayKey) {
        var open by rememberSaveable { mutableStateOf(false) }
        SectionCard(Modifier.animateContentSize().clickable { open = !open }) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(LocalDate.ofEpochDay(date).indo(), style = MaterialTheme.typography.titleMedium)
                    Text(Program.dayByKey(dayKey)?.title ?: dayKey, style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.primary)
                }
                Text("${daySets.size} set", style = MaterialTheme.typography.bodyLarge)
            }
            if (open) {
                daySets.groupBy { it.exerciseId }.forEach { (exId, list) ->
                    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                    Text(names[exId] ?: exId, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                    Text(
                        list.sortedBy { it.setIndex }.joinToString("  ·  ") { s ->
                            (s.weightKg?.let { "${it.kgText()} kg × " } ?: "") + (s.reps?.toString() ?: "-")
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        }
    }
}

@Composable
private fun FoodHistory(foods: List<FoodLog>, targetKcal: Int, targetProtein: Int) {
    if (foods.isEmpty()) {
        SectionCard { Text("Belum ada makanan tercatat.", style = MaterialTheme.typography.bodyLarge) }
        return
    }
    foods.groupBy { it.date }.forEach { (date, list) ->
        key(date) {
        var open by rememberSaveable { mutableStateOf(false) }
        val kcal = list.sumOf { it.kcal }
        val protein = list.sumOf { it.protein.toDouble() }.toFloat()
        SectionCard(Modifier.animateContentSize().clickable { open = !open }) {
            Text(LocalDate.ofEpochDay(date).indo(), style = MaterialTheme.typography.titleMedium)
            Text("$kcal / $targetKcal kkal · ${num(protein)} / $targetProtein g protein",
                style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.primary)
            if (open) {
                list.forEach { f ->
                    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                    Text(f.name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                    Text("${f.portion} · ${f.kcal} kkal · P ${num(f.protein)} g · K ${num(f.carbs)} g · L ${num(f.fat)} g",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 4.dp))
                }
            }
        }
    }
        }
}
