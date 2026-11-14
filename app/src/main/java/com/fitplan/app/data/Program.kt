package com.fitplan.app.data

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/** Jenis target satu gerakan. */
enum class TargetType { REPS, SECONDS, NEAR_FAILURE }

/**
 * Satu gerakan dalam program.
 * @param id kunci tetap untuk menyimpan log (jangan diubah setelah dipakai).
 */
data class Exercise(
    val id: String,
    val name: String,
    val sets: Int,
    val repMin: Int,
    val repMax: Int,
    val type: TargetType = TargetType.REPS,
    val perSide: Boolean = false,
    val usesWeight: Boolean = true,
) {
    /** Teks target, mis. "4 x 8-10", "3 x 10 per kaki", "3 x 40 detik". */
    val targetText: String
        get() = when (type) {
            TargetType.NEAR_FAILURE -> "$sets x mendekati gagal"
            TargetType.SECONDS -> "$sets x $repMax detik"
            TargetType.REPS -> {
                val reps = if (repMin == repMax) "$repMax" else "$repMin-$repMax"
                "$sets x $reps" + if (perSide) " per kaki" else ""
            }
        }
}

data class WorkoutDay(
    val key: String,
    val title: String,
    val exercises: List<Exercise>,
)

object Program {
    const val TOTAL_WEEKS = 12
    const val TOTAL_DAYS = TOTAL_WEEKS * 7

    val upper = WorkoutDay(
        key = "upper",
        title = "Upper Body",
        exercises = listOf(
            Exercise("upper_floor_press", "Floor press", 4, 8, 10),
            Exercise("upper_bent_row", "Bent-over row", 4, 8, 10),
            Exercise("upper_ohp", "Overhead press", 3, 8, 10),
            Exercise("upper_one_arm_row", "One-arm row", 3, 10, 10),
            Exercise("upper_curl", "Curl", 2, 12, 12),
            Exercise("upper_triceps", "Triceps extension", 2, 12, 12),
        ),
    )

    val leg = WorkoutDay(
        key = "leg",
        title = "Leg Day",
        exercises = listOf(
            Exercise("leg_squat", "Goblet / front squat", 4, 8, 10),
            Exercise("leg_rdl", "Romanian deadlift", 4, 8, 10),
            Exercise("leg_lunge", "Lunge", 3, 10, 10, perSide = true),
            Exercise("leg_glute_bridge", "Glute bridge", 3, 12, 12),
            Exercise("leg_calf", "Calf raise", 3, 15, 15),
            Exercise("leg_plank", "Plank", 3, 40, 40, TargetType.SECONDS, usesWeight = false),
        ),
    )

    val full = WorkoutDay(
        key = "full",
        title = "Full Body",
        exercises = listOf(
            Exercise("full_squat", "Squat", 3, 8, 8),
            Exercise("full_press", "Push-up / floor press", 3, 10, 10),
            Exercise("full_rdl", "Romanian deadlift", 3, 10, 10),
            Exercise("full_row", "Row", 3, 10, 10),
            Exercise("full_ohp", "Overhead press", 3, 10, 10),
        ),
    )

    val upperLight = WorkoutDay(
        key = "upper_light",
        title = "Upper Body Ringan",
        exercises = listOf(
            Exercise("light_pushup", "Push-up", 3, 0, 0, TargetType.NEAR_FAILURE, usesWeight = false),
            Exercise("light_lateral", "Lateral raise", 3, 12, 12),
            Exercise("light_row", "Row", 3, 12, 12),
            Exercise("light_curl", "Curl", 3, 12, 12),
            Exercise("light_triceps", "Triceps extension", 3, 12, 12),
            Exercise("light_plank", "Plank", 3, 40, 40, TargetType.SECONDS, usesWeight = false),
        ),
    )

    /** Jadwal mingguan tetap. Hari yang tidak ada = libur. */
    val weekly: Map<DayOfWeek, WorkoutDay> = mapOf(
        DayOfWeek.TUESDAY to upper,
        DayOfWeek.WEDNESDAY to leg,
        DayOfWeek.FRIDAY to full,
        DayOfWeek.SATURDAY to upperLight,
    )

    fun workoutFor(dayOfWeek: DayOfWeek): WorkoutDay? = weekly[dayOfWeek]

    fun dayByKey(key: String): WorkoutDay? = weekly.values.firstOrNull { it.key == key }

    /** Status program pada tanggal tertentu. */
    sealed interface Status {
        data object NotSet : Status
        data class NotStarted(val daysLeft: Long) : Status
        data class Running(val week: Int, val dayIndex: Long) : Status
        data object Finished : Status
    }

    fun status(start: LocalDate?, date: LocalDate): Status {
        if (start == null) return Status.NotSet
        val diff = ChronoUnit.DAYS.between(start, date)
        return when {
            diff < 0 -> Status.NotStarted(-diff)
            diff >= TOTAL_DAYS -> Status.Finished
            else -> Status.Running(week = (diff / 7).toInt() + 1, dayIndex = diff)
        }
    }

    /** Latihan pada tanggal tertentu, hanya jika program sedang berjalan. */
    fun workoutOn(start: LocalDate?, date: LocalDate): WorkoutDay? =
        if (status(start, date) is Status.Running) workoutFor(date.dayOfWeek) else null

    fun endDate(start: LocalDate): LocalDate = start.plusDays(TOTAL_DAYS - 1L)
}

/** Nama hari dalam bahasa Indonesia. */
fun DayOfWeek.indo(): String = when (this) {
    DayOfWeek.MONDAY -> "Senin"
    DayOfWeek.TUESDAY -> "Selasa"
    DayOfWeek.WEDNESDAY -> "Rabu"
    DayOfWeek.THURSDAY -> "Kamis"
    DayOfWeek.FRIDAY -> "Jumat"
    DayOfWeek.SATURDAY -> "Sabtu"
    DayOfWeek.SUNDAY -> "Minggu"
}

private val bulan = listOf(
    "Jan", "Feb", "Mar", "Apr", "Mei", "Jun", "Jul", "Agu", "Sep", "Okt", "Nov", "Des",
)

/** Contoh: "Selasa, 17 Nov 2026". */
fun LocalDate.indo(): String = "${dayOfWeek.indo()}, $dayOfMonth ${bulan[monthValue - 1]} $year"

/** Contoh: "17 Nov". */
fun LocalDate.indoShort(): String = "$dayOfMonth ${bulan[monthValue - 1]}"
