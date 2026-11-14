package com.fitplan.app.ui.food

import android.app.Application
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.fitplan.app.FitPlanApp
import com.fitplan.app.data.db.FoodLog
import com.fitplan.app.food.AnalyzeResult
import com.fitplan.app.food.FoodAnalyzer
import com.fitplan.app.food.FoodEstimate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.time.LocalDate

sealed interface FoodUi {
    data object Idle : FoodUi
    data object Analyzing : FoodUi
    data class Result(val estimate: FoodEstimate) : FoodUi
    data class Problem(val message: String) : FoodUi
}

class FoodViewModel(app: Application) : AndroidViewModel(app) {
    private val fitApp = app as FitPlanApp
    private val dao = fitApp.db.foods()
    val today: Long = LocalDate.now().toEpochDay()

    val todayFoods: StateFlow<List<FoodLog>> =
        dao.observeDate(today).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    var ui by mutableStateOf<FoodUi>(FoodUi.Idle)
        private set

    /** Foto terakhir (JPEG kecil) yang sedang dianalisis. */
    var photo by mutableStateOf<ByteArray?>(null)
        private set

    fun hasApiKey(): Boolean = fitApp.apiKeys.get() != null

    fun analyze(uri: Uri) {
        val key = fitApp.apiKeys.get()
        if (key == null) {
            ui = FoodUi.Problem("API key Claude belum diisi. Buka menu Atur → API key Claude.")
            return
        }
        ui = FoodUi.Analyzing
        viewModelScope.launch {
            val jpeg = withContext(Dispatchers.IO) {
                runCatching { FoodAnalyzer.loadAndCompress(fitApp, uri) }.getOrNull()
            }
            if (jpeg == null) {
                ui = FoodUi.Problem("Foto tidak bisa dibuka. Coba foto lain.")
                return@launch
            }
            photo = jpeg
            ui = when (val r = FoodAnalyzer.analyze(fitApp, key, jpeg)) {
                is AnalyzeResult.Ok -> FoodUi.Result(r.estimate)
                is AnalyzeResult.NotFood -> FoodUi.Problem(r.message)
                is AnalyzeResult.Error -> FoodUi.Problem(r.message)
            }
        }
    }

    /** Isi manual tanpa foto. */
    fun manual() {
        photo = null
        ui = FoodUi.Result(FoodEstimate("", "1 porsi", 0, 0f, 0f, 0f, "manual", ""))
    }

    fun save(e: FoodEstimate) {
        val jpeg = photo
        viewModelScope.launch {
            val path = jpeg?.let {
                withContext(Dispatchers.IO) {
                    val dir = File(fitApp.filesDir, "food").apply { mkdirs() }
                    val f = File(dir, "food_${System.currentTimeMillis()}.jpg")
                    f.writeBytes(it)
                    f.absolutePath
                }
            }
            dao.insert(
                FoodLog(
                    date = today,
                    time = System.currentTimeMillis(),
                    name = e.name.ifBlank { "Makanan" },
                    portion = e.portion,
                    kcal = e.kcal,
                    protein = e.protein,
                    carbs = e.carbs,
                    fat = e.fat,
                    confidence = e.confidence,
                    photoPath = path,
                ),
            )
            reset()
        }
    }

    fun delete(f: FoodLog) {
        viewModelScope.launch {
            dao.delete(f)
            f.photoPath?.let { p -> withContext(Dispatchers.IO) { File(p).delete() } }
        }
    }

    fun reset() {
        ui = FoodUi.Idle
        photo = null
    }
}
