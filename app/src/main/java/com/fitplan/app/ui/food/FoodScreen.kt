package com.fitplan.app.ui.food

import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fitplan.app.data.db.FoodLog
import com.fitplan.app.food.FoodEstimate
import com.fitplan.app.ui.common.BigButton
import com.fitplan.app.ui.common.ScreenColumn
import com.fitplan.app.ui.common.SectionCard
import com.fitplan.app.ui.theme.Amber
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun FoodScreen(onOpenSettings: () -> Unit) {
    val vm: FoodViewModel = viewModel()
    val context = LocalContext.current
    val foods by vm.todayFoods.collectAsStateWithLifecycle()
    var cameraUri by rememberSaveable { mutableStateOf<Uri?>(null) }

    val takePicture = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        val uri = cameraUri
        if (ok && uri != null) vm.analyze(uri)
    }
    val pickPhoto = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) vm.analyze(uri)
    }

    fun openCamera() {
        val dir = File(context.cacheDir, "camera").apply { mkdirs() }
        val file = File(dir, "photo_${System.currentTimeMillis()}.jpg")
        val uri = FileProvider.getUriForFile(context, context.packageName + ".fileprovider", file)
        cameraUri = uri
        takePicture.launch(uri)
    }

    ScreenColumn(title = "Makanan") {
        when (val ui = vm.ui) {
            FoodUi.Idle -> {
                if (!vm.hasApiKey()) {
                    SectionCard {
                        Text("API key Claude belum diisi", style = MaterialTheme.typography.titleMedium,
                            color = Amber)
                        Text("Isi dulu di menu Atur supaya foto bisa dianalisis. Anda tetap bisa mencatat manual.",
                            style = MaterialTheme.typography.bodyLarge)
                        BigButton("Buka pengaturan", onClick = onOpenSettings, outlined = true)
                    }
                }
                BigButton("Ambil foto makanan", onClick = { openCamera() }, icon = Icons.Filled.PhotoCamera)
                BigButton("Pilih dari galeri", outlined = true, icon = Icons.Filled.PhotoLibrary, onClick = {
                    pickPhoto.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                })
                BigButton("Catat manual", outlined = true, icon = Icons.Filled.EditNote, onClick = { vm.manual() })
            }
            FoodUi.Analyzing -> SectionCard {
                PhotoPreview(vm.photo)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(Modifier.size(32.dp))
                    Text("  Claude sedang menganalisis foto…", style = MaterialTheme.typography.bodyLarge)
                }
                Text("Biasanya 5–20 detik.", style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            is FoodUi.Problem -> SectionCard {
                PhotoPreview(vm.photo)
                Text("Tidak bisa dianalisis", style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.error)
                Text(ui.message, style = MaterialTheme.typography.bodyLarge)
                BigButton("Coba lagi", onClick = { vm.reset() })
                BigButton("Catat manual", outlined = true, onClick = { vm.manual() })
            }
            is FoodUi.Result -> ResultCard(
                photo = vm.photo,
                estimate = ui.estimate,
                startEditing = ui.estimate.confidence == "manual",
                onSave = { vm.save(it) },
                onCancel = { vm.reset() },
            )
        }

        TodayLog(foods, onDelete = { vm.delete(it) })
    }
}

@Composable
private fun PhotoPreview(jpeg: ByteArray?) {
    if (jpeg == null) return
    val bmp = remember(jpeg) { BitmapFactory.decodeByteArray(jpeg, 0, jpeg.size)?.asImageBitmap() } ?: return
    Image(
        bitmap = bmp,
        contentDescription = "Foto makanan",
        contentScale = ContentScale.Crop,
        modifier = Modifier.fillMaxWidth().height(200.dp).clip(RoundedCornerShape(12.dp)),
    )
}

@Composable
private fun EstimateBadge() {
    Box(
        Modifier.background(Amber.copy(alpha = 0.18f), RoundedCornerShape(8.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
        Text(
            "⚠️ ESTIMASI: angka ini hanya perkiraan dari foto, bukan hasil timbangan.",
            style = MaterialTheme.typography.bodyMedium, color = Amber, fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun ResultCard(
    photo: ByteArray?,
    estimate: FoodEstimate,
    startEditing: Boolean,
    onSave: (FoodEstimate) -> Unit,
    onCancel: () -> Unit,
) {
    var editing by remember(estimate) { mutableStateOf(startEditing) }
    var name by remember(estimate) { mutableStateOf(estimate.name) }
    var portion by remember(estimate) { mutableStateOf(estimate.portion) }
    var kcal by remember(estimate) { mutableStateOf(estimate.kcal.toString()) }
    var protein by remember(estimate) { mutableStateOf(num(estimate.protein)) }
    var carbs by remember(estimate) { mutableStateOf(num(estimate.carbs)) }
    var fat by remember(estimate) { mutableStateOf(num(estimate.fat)) }

    fun current() = estimate.copy(
        name = name.trim(),
        portion = portion.trim(),
        kcal = kcal.toIntOrNull() ?: 0,
        protein = protein.toF(),
        carbs = carbs.toF(),
        fat = fat.toF(),
    )

    SectionCard {
        PhotoPreview(photo)
        if (estimate.confidence != "manual") EstimateBadge()

        if (editing) {
            Text(if (estimate.confidence == "manual") "Catat manual" else "Edit hasil",
                style = MaterialTheme.typography.titleLarge)
            OutlinedTextField(name, { name = it }, label = { Text("Nama makanan") },
                singleLine = true, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(portion, { portion = it }, label = { Text("Porsi") },
                singleLine = true, modifier = Modifier.fillMaxWidth())
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                NumBox("Kalori (kkal)", kcal, Modifier.weight(1f), decimal = false) { kcal = it }
                NumBox("Protein (g)", protein, Modifier.weight(1f)) { protein = it }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                NumBox("Karbo (g)", carbs, Modifier.weight(1f)) { carbs = it }
                NumBox("Lemak (g)", fat, Modifier.weight(1f)) { fat = it }
            }
        } else {
            Text(name, style = MaterialTheme.typography.titleLarge)
            Text("Porsi: $portion", style = MaterialTheme.typography.bodyLarge)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Big("$kcal", "kkal")
                Big(protein, "g protein")
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Small("Karbo $carbs g")
                Small("Lemak $fat g")
                Small("Yakin: ${estimate.confidence}")
            }
            if (estimate.note.isNotBlank()) {
                Text(estimate.note, style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            BigButton("Edit", outlined = true, icon = Icons.Filled.Edit, onClick = { editing = true })
        }
        BigButton("Simpan ke log hari ini", icon = Icons.Filled.Save, onClick = { onSave(current()) },
            enabled = (kcal.toIntOrNull() ?: -1) >= 0)
        TextButton(onClick = onCancel, modifier = Modifier.fillMaxWidth()) { Text("Batal") }
    }
}

@Composable
private fun Big(value: String, unit: String) {
    Column {
        Text(value, style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary)
        Text(unit, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun Small(text: String) {
    Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
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

private fun String.toF(): Float = replace(',', '.').toFloatOrNull() ?: 0f
