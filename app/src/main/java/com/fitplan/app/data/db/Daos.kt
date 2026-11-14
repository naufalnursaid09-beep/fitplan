package com.fitplan.app.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

/** Ringkasan satu hari latihan untuk riwayat. */
data class WorkoutDaySummary(
    val date: Long,
    val dayKey: String,
    val doneSets: Int,
)

@Dao
interface SetDao {
    @Query("SELECT * FROM set_log WHERE date = :date AND dayKey = :dayKey ORDER BY exerciseId, setIndex")
    suspend fun forDay(date: Long, dayKey: String): List<SetLog>

    @Query("SELECT * FROM set_log WHERE date = :date ORDER BY exerciseId, setIndex")
    fun observeDate(date: Long): Flow<List<SetLog>>

    /** Set-set dari sesi terakhir (sebelum [date]) untuk gerakan ini. */
    @Query(
        """
        SELECT * FROM set_log
        WHERE exerciseId = :exerciseId AND done = 1 AND date = (
            SELECT MAX(date) FROM set_log WHERE exerciseId = :exerciseId AND done = 1 AND date < :date
        )
        ORDER BY setIndex
        """,
    )
    suspend fun previousSession(exerciseId: String, date: Long): List<SetLog>

    @Query(
        """
        SELECT date, dayKey, SUM(CASE WHEN done THEN 1 ELSE 0 END) AS doneSets
        FROM set_log GROUP BY date, dayKey HAVING doneSets > 0 ORDER BY date DESC
        """,
    )
    fun observeHistory(): Flow<List<WorkoutDaySummary>>

    @Upsert
    suspend fun upsert(set: SetLog): Long

    @Query("SELECT * FROM set_log WHERE date = :date AND exerciseId = :exerciseId AND setIndex = :setIndex LIMIT 1")
    suspend fun find(date: Long, exerciseId: String, setIndex: Int): SetLog?
}

@Dao
interface FoodDao {
    @Query("SELECT * FROM food_log WHERE date = :date ORDER BY time")
    fun observeDate(date: Long): Flow<List<FoodLog>>

    @Query("SELECT DISTINCT date FROM food_log ORDER BY date DESC")
    fun observeDates(): Flow<List<Long>>

    @Insert
    suspend fun insert(food: FoodLog): Long

    @Delete
    suspend fun delete(food: FoodLog)
}

@Dao
interface WeightDao {
    @Query("SELECT * FROM weight_log ORDER BY date")
    fun observeAll(): Flow<List<WeightLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun put(w: WeightLog)

    @Delete
    suspend fun delete(w: WeightLog)
}
