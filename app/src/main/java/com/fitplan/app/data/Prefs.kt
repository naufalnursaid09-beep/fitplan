package com.fitplan.app.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.DayOfWeek
import java.time.LocalDate

/** Semua pengaturan aplikasi. Waktu disimpan sebagai menit sejak 00:00. */
data class Settings(
    val startDate: LocalDate? = null,
    /** Jam pengingat latihan per hari latihan. */
    val workoutTimes: Map<DayOfWeek, Int> = Program.weekly.keys.associateWith { DEFAULT_WORKOUT_TIME },
    val workoutReminderOn: Boolean = true,
    val weighReminderOn: Boolean = true,
    val weighTime: Int = 7 * 60,
    val proteinReminderOn: Boolean = false,
    val proteinTime: Int = 12 * 60,
    val targetKcal: Int = 2400,
    val targetProtein: Int = 160,
    val startWeight: Float = 90f,
    val goalWeight: Float = 85f,
) {
    companion object {
        const val DEFAULT_WORKOUT_TIME = 17 * 60
    }
}

class Prefs(context: Context) {
    private val sp: SharedPreferences =
        context.getSharedPreferences("fitplan_settings", Context.MODE_PRIVATE)

    private val _settings = MutableStateFlow(load())
    val settings: StateFlow<Settings> = _settings.asStateFlow()

    val current: Settings get() = _settings.value

    private fun load(): Settings {
        val d = Settings()
        val start = sp.getLong(K_START, Long.MIN_VALUE)
        return Settings(
            startDate = if (start == Long.MIN_VALUE) null else LocalDate.ofEpochDay(start),
            workoutTimes = Program.weekly.keys.associateWith {
                sp.getInt(K_WORKOUT_TIME + it.name, Settings.DEFAULT_WORKOUT_TIME)
            },
            workoutReminderOn = sp.getBoolean(K_WORKOUT_ON, d.workoutReminderOn),
            weighReminderOn = sp.getBoolean(K_WEIGH_ON, d.weighReminderOn),
            weighTime = sp.getInt(K_WEIGH_TIME, d.weighTime),
            proteinReminderOn = sp.getBoolean(K_PROTEIN_ON, d.proteinReminderOn),
            proteinTime = sp.getInt(K_PROTEIN_TIME, d.proteinTime),
            targetKcal = sp.getInt(K_KCAL, d.targetKcal),
            targetProtein = sp.getInt(K_PROTEIN, d.targetProtein),
            startWeight = sp.getFloat(K_START_WEIGHT, d.startWeight),
            goalWeight = sp.getFloat(K_GOAL_WEIGHT, d.goalWeight),
        )
    }

    /** Ubah pengaturan lalu simpan. */
    fun update(change: (Settings) -> Settings) {
        val s = change(_settings.value)
        sp.edit().apply {
            if (s.startDate == null) remove(K_START) else putLong(K_START, s.startDate.toEpochDay())
            s.workoutTimes.forEach { (day, min) -> putInt(K_WORKOUT_TIME + day.name, min) }
            putBoolean(K_WORKOUT_ON, s.workoutReminderOn)
            putBoolean(K_WEIGH_ON, s.weighReminderOn)
            putInt(K_WEIGH_TIME, s.weighTime)
            putBoolean(K_PROTEIN_ON, s.proteinReminderOn)
            putInt(K_PROTEIN_TIME, s.proteinTime)
            putInt(K_KCAL, s.targetKcal)
            putInt(K_PROTEIN, s.targetProtein)
            putFloat(K_START_WEIGHT, s.startWeight)
            putFloat(K_GOAL_WEIGHT, s.goalWeight)
        }.apply()
        _settings.value = s
    }

    private companion object {
        const val K_START = "start_date"
        const val K_WORKOUT_TIME = "workout_time_"
        const val K_WORKOUT_ON = "workout_on"
        const val K_WEIGH_ON = "weigh_on"
        const val K_WEIGH_TIME = "weigh_time"
        const val K_PROTEIN_ON = "protein_on"
        const val K_PROTEIN_TIME = "protein_time"
        const val K_KCAL = "target_kcal"
        const val K_PROTEIN = "target_protein"
        const val K_START_WEIGHT = "start_weight"
        const val K_GOAL_WEIGHT = "goal_weight"
    }
}

/** 1020 -> "17:00" */
fun Int.asClock(): String = "%02d:%02d".format(this / 60, this % 60)
