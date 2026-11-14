package com.fitplan.app.data.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/** Satu set latihan yang dicatat. [date] = LocalDate.toEpochDay(). */
@Entity(
    tableName = "set_log",
    indices = [Index(value = ["date", "exerciseId", "setIndex"], unique = true), Index("exerciseId")],
)
data class SetLog(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: Long,
    val dayKey: String,
    val exerciseId: String,
    val setIndex: Int,
    val weightKg: Float?,
    val reps: Int?,
    val done: Boolean,
    val updatedAt: Long = System.currentTimeMillis(),
)

/** Satu catatan makanan. */
@Entity(tableName = "food_log", indices = [Index("date")])
data class FoodLog(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: Long,
    val time: Long,
    val name: String,
    val portion: String,
    val kcal: Int,
    val protein: Float,
    val carbs: Float,
    val fat: Float,
    val confidence: String,
    val photoPath: String?,
)

/** Berat badan (satu catatan per tanggal). */
@Entity(tableName = "weight_log", indices = [Index(value = ["date"], unique = true)])
data class WeightLog(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: Long,
    val kg: Float,
)
