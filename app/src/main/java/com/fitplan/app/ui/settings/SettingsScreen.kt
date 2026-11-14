package com.fitplan.app.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EventAvailable
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fitplan.app.FitPlanApp
import com.fitplan.app.data.Program
import com.fitplan.app.data.indo
import com.fitplan.app.ui.common.BigButton
import com.fitplan.app.ui.common.ScreenColumn
import com.fitplan.app.ui.common.SectionCard
import java.time.LocalDate

private const val DAY_MS = 86_400_000L

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen() {
    val prefs = FitPlanApp.instance.prefs
    val settings by prefs.settings.collectAsStateWithLifecycle()
    var showPicker by remember { mutableStateOf(false) }

    ScreenColumn(title = "Pengaturan") {
        // ---- Program ----
        SectionCard {
            Text("Program 12 minggu", style = MaterialTheme.typography.titleLarge)
            val start = settings.startDate
            if (start == null) {
                Text("Tanggal mulai belum dipilih.", style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.tertiary)
            } else {
                Text("Mulai: ${start.indo()}", style = MaterialTheme.typography.bodyLarge)
                Text("Selesai: ${Program.endDate(start).indo()}", style = MaterialTheme.typography.bodyLarge)
            }
            BigButton(
                if (start == null) "Pilih tanggal mulai" else "Ganti tanggal mulai",
                onClick = { showPicker = true },
                icon = Icons.Filled.EventAvailable,
            )
        }

        // ---- Target harian ----
        SectionCard {
            Text("Target harian", style = MaterialTheme.typography.titleLarge)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                NumberField("Kalori (kkal)", settings.targetKcal.toString(), Modifier.weight(1f)) { v ->
                    v.toIntOrNull()?.takeIf { it in 500..10000 }?.let { n -> prefs.update { it.copy(targetKcal = n) } }
                }
                NumberField("Protein (g)", settings.targetProtein.toString(), Modifier.weight(1f)) { v ->
                    v.toIntOrNull()?.takeIf { it in 10..500 }?.let { n -> prefs.update { it.copy(targetProtein = n) } }
                }
            }
        }

        // ---- Berat badan ----
        SectionCard {
            Text("Berat badan", style = MaterialTheme.typography.titleLarge)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                NumberField("Berat awal (kg)", fmt(settings.startWeight), Modifier.weight(1f), decimal = true) { v ->
                    v.replace(',', '.').toFloatOrNull()?.takeIf { it in 30f..300f }
                        ?.let { n -> prefs.update { it.copy(startWeight = n) } }
                }
                NumberField("Target (kg)", fmt(settings.goalWeight), Modifier.weight(1f), decimal = true) { v ->
                    v.replace(',', '.').toFloatOrNull()?.takeIf { it in 30f..300f }
                        ?.let { n -> prefs.update { it.copy(goalWeight = n) } }
                }
            }
        }

        Text("Pengaturan pengingat dan API key akan muncul di sini pada tahap berikutnya.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
    }

    if (showPicker) {
        val initial = (settings.startDate ?: LocalDate.now()).toEpochDay() * DAY_MS
        val state = rememberDatePickerState(initialSelectedDateMillis = initial)
        DatePickerDialog(
            onDismissRequest = { showPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { ms ->
                        val date = LocalDate.ofEpochDay(Math.floorDiv(ms, DAY_MS))
                        prefs.update { it.copy(startDate = date) }
                    }
                    showPicker = false
                }) { Text("Simpan") }
            },
            dismissButton = { TextButton(onClick = { showPicker = false }) { Text("Batal") } },
        ) {
            DatePicker(state = state, title = { Text("  Tanggal mulai program") })
        }
    }
}

private fun fmt(f: Float): String = if (f % 1f == 0f) f.toInt().toString() else f.toString()


/** Kolom angka: disimpan otomatis saat angkanya valid. */
@Composable
fun NumberField(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    decimal: Boolean = false,
    onValid: (String) -> Unit,
) {
    var text by remember(value) { mutableStateOf(value) }
    OutlinedTextField(
        value = text,
        onValueChange = {
            text = it
            onValid(it)
        },
        label = { Text(label) },
        singleLine = true,
        textStyle = MaterialTheme.typography.titleMedium,
        keyboardOptions = KeyboardOptions(
            keyboardType = if (decimal) KeyboardType.Decimal else KeyboardType.Number,
        ),
        modifier = modifier,
    )
}
