package com.fitplan.app.ui.food

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.fitplan.app.FitPlanApp
import com.fitplan.app.data.db.FoodLog
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

/** Catat makanan secara manual. */
class FoodViewModel(app: Application) : AndroidViewModel(app) {
    private val dao = (app as FitPlanApp).db.foods()
    val today: Long = LocalDate.now().toEpochDay()

    val todayFoods: StateFlow<List<FoodLog>> =
        dao.observeDate(today).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun add(name: String, portion: String, kcal: Int, protein: Float) {
        viewModelScope.launch {
            dao.insert(
                FoodLog(
                    date = today,
                    time = System.currentTimeMillis(),
                    name = name.ifBlank { "Makanan" },
                    portion = portion,
                    kcal = kcal,
                    protein = protein,
                    carbs = 0f,
                    fat = 0f,
                    confidence = "manual",
                    photoPath = null,
                ),
            )
        }
    }

    fun delete(f: FoodLog) {
        viewModelScope.launch { dao.delete(f) }
    }
}
