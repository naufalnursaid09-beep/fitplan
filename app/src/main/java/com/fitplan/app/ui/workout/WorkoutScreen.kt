package com.fitplan.app.ui.workout

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.fitplan.app.data.Program
import com.fitplan.app.data.TargetType
import com.fitplan.app.data.indo
import com.fitplan.app.ui.common.BigButton
import com.fitplan.app.ui.common.SectionCard
import com.fitplan.app.ui.theme.Amber
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutScreen(date: Long, dayKey: String, onBack: () -> Unit) {
    val day = Program.dayByKey(dayKey)
    if (day == null) {
        Text("Latihan tidak ditemukan.")
        return
    }
    val vm: WorkoutViewModel = viewModel(
        key = "workout_${date}_$dayKey",
        factory = viewModelFactory { initializer { WorkoutViewModel(date, day) } },
    )
    val context = LocalContext.current
    val exercises = vm.exercises

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(day.title, style = MaterialTheme.typography.titleLarge)
                        Text(LocalDate.ofEpochDay(date).indo(), style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
    ) { padding ->
        if (exercises == null) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            WorkoutList(exercises, padding, vm, onFinish = {
                Toast.makeText(context, "Latihan tersimpan 💪", Toast.LENGTH_SHORT).show()
                onBack()
            })
        }
    }
}

@Composable
private fun WorkoutList(
    exercises: List<ExerciseState>,
    padding: androidx.compose.foundation.layout.PaddingValues,
    vm: WorkoutViewModel,
    onFinish: () -> Unit,
) {
        val total = exercises.sumOf { it.sets.size }
        val done = exercises.sumOf { e -> e.sets.count { it.done } }

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item {
                Text("$done dari $total set selesai", style = MaterialTheme.typography.titleMedium)
                LinearProgressIndicator(
                    progress = { if (total == 0) 0f else done.toFloat() / total },
                    modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                )
            }
            items(exercises, key = { it.exercise.id }) { ex ->
                ExerciseCard(ex) { index, input -> vm.update(ex, index, input) }
            }
            item {
                BigButton("Selesai latihan", icon = Icons.Filled.Check, onClick = onFinish)
            }
        }
}

@Composable
private fun ExerciseCard(ex: ExerciseState, onChange: (Int, SetInput) -> Unit) {
    val e = ex.exercise
    SectionCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(e.name, style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
            Text(e.targetText, style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary)
        }

        // Acuan sesi sebelumnya
        if (ex.previous.isNotEmpty()) {
            val prevText = ex.previous.joinToString("  ·  ") { s ->
                val kg = s.weightKg?.let { "${it.kgText()} kg × " } ?: ""
                kg + (s.reps?.toString() ?: "-") + if (e.type == TargetType.SECONDS) " dtk" else ""
            }
            Text("Sesi lalu: $prevText", style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            Text("Belum ada catatan sesi sebelumnya.", style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        ex.suggestFromPrevious?.let { kg ->
            Suggestion("Sesi lalu semua set mencapai ${e.repMax} repetisi. Coba naik ke ${kg.kgText()} kg (+2,5 kg).")
        }

        // Baris set
        ex.sets.forEachIndexed { i, input ->
            SetRow(i, e.usesWeight, e.type, input, target = e.repMax) { onChange(i, it) }
        }

        ex.suggestNext()?.let { kg ->
            Suggestion("Mantap! Semua set mencapai batas atas. Sesi berikutnya naik ke ${kg.kgText()} kg.")
        }
    }
}

@Composable
private fun Suggestion(text: String) {
    Row(
        Modifier
            .fillMaxWidth()
            .background(Amber.copy(alpha = 0.15f), RoundedCornerShape(10.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Filled.TrendingUp, contentDescription = null, tint = Amber)
        Text(text, style = MaterialTheme.typography.bodyLarge, color = Amber,
            modifier = Modifier.padding(start = 10.dp))
    }
}

@Composable
private fun SetRow(
    index: Int,
    usesWeight: Boolean,
    type: TargetType,
    input: SetInput,
    target: Int,
    onChange: (SetInput) -> Unit,
) {
    val bg = if (input.done) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f) else Color.Transparent
    Row(
        Modifier.fillMaxWidth().background(bg, RoundedCornerShape(10.dp)).padding(vertical = 4.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text("Set ${index + 1}", style = MaterialTheme.typography.titleMedium, modifier = Modifier.width(56.dp))
        if (usesWeight) {
            NumField(input.kg, "kg", decimal = true, modifier = Modifier.weight(1f)) {
                onChange(input.copy(kg = it))
            }
        }
        NumField(
            input.reps,
            when (type) { TargetType.SECONDS -> "detik"; else -> "repetisi" },
            decimal = false,
            modifier = Modifier.weight(1f),
        ) { onChange(input.copy(reps = it)) }
        Checkbox(
            checked = input.done,
            onCheckedChange = { checked ->
                // Saat dicentang dan repetisi kosong, isi otomatis dengan target.
                val reps = if (checked && input.reps.isBlank() && target > 0) target.toString() else input.reps
                onChange(input.copy(done = checked, reps = reps))
            },
            colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colorScheme.primary),
            modifier = Modifier.scale(1.4f).size(48.dp),
        )
    }
}

@Composable
private fun NumField(
    value: String,
    label: String,
    decimal: Boolean,
    modifier: Modifier = Modifier,
    onChange: (String) -> Unit,
) {
    OutlinedTextField(
        value = value,
        onValueChange = { v -> onChange(v.filter { it.isDigit() || (decimal && (it == ',' || it == '.')) }.take(5)) },
        label = { Text(label) },
        singleLine = true,
        textStyle = MaterialTheme.typography.titleMedium.copy(
            textAlign = TextAlign.Center, fontWeight = FontWeight.Bold,
        ),
        keyboardOptions = KeyboardOptions(keyboardType = if (decimal) KeyboardType.Decimal else KeyboardType.Number),
        modifier = modifier,
    )
}
