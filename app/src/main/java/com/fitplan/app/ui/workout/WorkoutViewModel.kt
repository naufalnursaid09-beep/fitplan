package com.fitplan.app.ui.workout

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fitplan.app.FitPlanApp
import com.fitplan.app.data.Exercise
import com.fitplan.app.data.TargetType
import com.fitplan.app.data.WorkoutDay
import com.fitplan.app.data.db.SetLog
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** Isian satu set di layar (teks supaya bisa diketik bebas). */
data class SetInput(val kg: String = "", val reps: String = "", val done: Boolean = false)

class ExerciseState(
    val exercise: Exercise,
    val previous: List<SetLog>,
    initial: List<SetInput>,
) {
    val sets = mutableStateListOf<SetInput>().apply { addAll(initial) }

    /** Beban sesi lalu yang paling berat. */
    val previousTopKg: Float? = previous.mapNotNull { it.weightKg }.maxOrNull()

    /** Sesi lalu: semua set selesai di batas atas repetisi → sarankan naik 2,5 kg. */
    val suggestFromPrevious: Float? = run {
        if (!canProgress(exercise)) return@run null
        val ok = previous.size >= exercise.sets &&
            previous.all { (it.reps ?: 0) >= exercise.repMax && it.weightKg != null }
        if (ok) previousTopKg?.plus(INCREMENT) else null
    }

    /** Sesi ini: semua set dicentang dan mencapai batas atas → sarankan naik di sesi berikutnya. */
    fun suggestNext(): Float? {
        if (!canProgress(exercise)) return null
        val all = sets.all { it.done && (it.reps.toIntOrNull() ?: 0) >= exercise.repMax }
        val kg = sets.mapNotNull { it.kg.toKg() }.maxOrNull() ?: return null
        return if (all) kg + INCREMENT else null
    }

    companion object {
        const val INCREMENT = 2.5f
        fun canProgress(e: Exercise) = e.usesWeight && e.type == TargetType.REPS
    }
}

fun String.toKg(): Float? = replace(',', '.').toFloatOrNull()

fun Float.kgText(): String = if (this % 1f == 0f) toInt().toString() else "%.1f".format(this).replace('.', ',')

class WorkoutViewModel(val date: Long, val day: WorkoutDay) : ViewModel() {
    private val dao = FitPlanApp.instance.db.sets()
    private val mutex = Mutex()

    var exercises by mutableStateOf<List<ExerciseState>?>(null)
        private set

    init {
        viewModelScope.launch {
            val saved = dao.forDay(date, day.key)
            exercises = day.exercises.map { ex ->
                val prev = dao.previousSession(ex.id, date)
                val state = ExerciseState(ex, prev, emptyList())
                val suggestion = state.suggestFromPrevious
                val inputs = (0 until ex.sets).map { i ->
                    val s = saved.firstOrNull { it.exerciseId == ex.id && it.setIndex == i }
                    if (s != null) {
                        SetInput(s.weightKg?.kgText() ?: "", s.reps?.toString() ?: "", s.done)
                    } else {
                        // Isi otomatis beban: saran naik, atau beban set yang sama di sesi lalu.
                        val kg = suggestion ?: prev.getOrNull(i)?.weightKg ?: prev.lastOrNull()?.weightKg
                        SetInput(kg = if (ex.usesWeight) kg?.kgText() ?: "" else "")
                    }
                }
                ExerciseState(ex, prev, inputs)
            }
        }
    }

    fun update(ex: ExerciseState, index: Int, input: SetInput) {
        ex.sets[index] = input
        viewModelScope.launch {
            mutex.withLock {
                val existing = dao.find(date, ex.exercise.id, index)
                dao.upsert(
                    SetLog(
                        id = existing?.id ?: 0,
                        date = date,
                        dayKey = day.key,
                        exerciseId = ex.exercise.id,
                        setIndex = index,
                        weightKg = input.kg.toKg(),
                        reps = input.reps.toIntOrNull(),
                        done = input.done,
                    ),
                )
            }
        }
    }
}
