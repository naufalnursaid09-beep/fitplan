package com.fitplan.app.ui.food

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fitplan.app.data.db.FoodLog
import com.fitplan.app.ui.common.BigButton
import com.fitplan.app.ui.common.ScreenColumn
import com.fitplan.app.ui.common.SectionCard
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun FoodScreen() {
    val vm: FoodViewModel = viewModel()
    val context = LocalContext.current
    val foods by vm.todayFoods.collectAsStateWithLifecycle()

    var name by rememberSaveable { mutableStateOf("") }
    var portion by rememberSaveable { mutableStateOf("") }
    var kcal by rememberSaveable { mutableStateOf("") }
    var protein by rememberSaveable { mutableStateOf("") }

    ScreenColumn(title = "Makanan") {
        SectionCard {
            Text("Catat makanan", style = MaterialTheme.typography.titleLarge)
            OutlinedTextField(name, { name = it }, label = { Text("Nama makanan") },
                singleLine = true, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(portion, { portion = it }, label = { Text("Porsi (opsional)") },
                singleLine = true, modifier = Modifier.fillMaxWidth())
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                NumBox("Kalori (kkal)", kcal, Modifier.weight(1f), decimal = false) { kcal = it }
                NumBox("Protein (g)", protein, Modifier.weight(1f)) { protein = it }
            }
            BigButton("Simpan", icon = Icons.Filled.Add, enabled = kcal.toIntOrNull() != null, onClick = {
                vm.add(name.trim(), portion.trim().ifBlank { "1 porsi" }, kcal.toInt(),
                    protein.replace(',', '.').toFloatOrNull() ?: 0f)
                name = ""; portion = ""; kcal = ""; protein = ""
                Toast.makeText(context, "Tersimpan", Toast.LENGTH_SHORT).show()
            })
        }

        TodayLog(foods, onDelete = { vm.delete(it) })
    }
}

@Composable
private fun NumBox(label: String, value: String, modifier: Modifier, decimal: Boolean = true, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = { v -> onChange(v.filter { it.isDigit() || (decimal && (it == ',' || it == '.')) }.take(6)) },
        label = { Text(label) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = if (decimal) KeyboardType.Decimal else KeyboardType.Number),
        modifier = modifier,
    )
}

@Composable
private fun TodayLog(foods: List<FoodLog>, onDelete: (FoodLog) -> Unit) {
    var toDelete by remember { mutableStateOf<FoodLog?>(null) }
    val fmt = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    SectionCard {
        Text("Log makanan hari ini", style = MaterialTheme.typography.titleLarge)
        if (foods.isEmpty()) {
            Text("Belum ada makanan dicatat.", style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            val kcal = foods.sumOf { it.kcal }
            val protein = foods.sumOf { it.protein.toDouble() }
            Text("Total: $kcal kkal · ${num(protein.toFloat())} g protein",
                style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            foods.forEachIndexed { i, f ->
                if (i > 0) HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(f.name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                        Text("${fmt.format(Date(f.time))} · ${f.portion}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("${f.kcal} kkal · ${num(f.protein)} g protein",
                            style = MaterialTheme.typography.bodyMedium)
                    }
                    IconButton(onClick = { toDelete = f }) {
                        Icon(Icons.Filled.Delete, contentDescription = "Hapus")
                    }
                }
            }
        }
    }
    toDelete?.let { f ->
        AlertDialog(
            onDismissRequest = { toDelete = null },
            title = { Text("Hapus catatan?") },
            text = { Text("${f.name} (${f.kcal} kkal) akan dihapus dari log.") },
            confirmButton = { TextButton(onClick = { onDelete(f); toDelete = null }) { Text("Hapus") } },
            dismissButton = { TextButton(onClick = { toDelete = null }) { Text("Batal") } },
        )
    }
}

fun num(f: Float): String = if (f % 1f == 0f) f.toInt().toString() else "%.1f".format(Locale.US, f)

