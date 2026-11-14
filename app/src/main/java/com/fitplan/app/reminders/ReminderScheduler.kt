package com.fitplan.app.reminders

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.fitplan.app.data.Program
import com.fitplan.app.data.Settings
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

/** Jenis pengingat. Setiap jenis punya satu alarm berikutnya yang terjadwal. */
enum class ReminderType(val requestCode: Int) {
    WORKOUT(100),
    WEIGH(200),
    PROTEIN(300),
    TEST(900),
}

object ReminderScheduler {
    const val EXTRA_TYPE = "type"

    /** Jadwalkan ulang semua pengingat berdasarkan pengaturan terbaru. */
    fun rescheduleAll(context: Context, s: Settings) {
        val now = LocalDateTime.now()
        set(context, ReminderType.WORKOUT, if (s.workoutReminderOn) nextWorkout(s, now) else null)
        set(context, ReminderType.WEIGH, if (s.weighReminderOn) nextWeekly(DayOfWeek.SUNDAY, s.weighTime, now) else null)
        set(context, ReminderType.PROTEIN, if (s.proteinReminderOn) nextDaily(s.proteinTime, now) else null)
    }

    /** Alarm tes: berbunyi [seconds] detik dari sekarang. */
    fun scheduleTest(context: Context, seconds: Long) {
        set(context, ReminderType.TEST, LocalDateTime.now().plusSeconds(seconds))
    }

    fun canExact(context: Context): Boolean {
        val am = context.getSystemService(AlarmManager::class.java)
        return Build.VERSION.SDK_INT < 31 || am.canScheduleExactAlarms()
    }

    // ---------------- perhitungan waktu ----------------

    fun nextWorkout(s: Settings, now: LocalDateTime): LocalDateTime? {
        val start = s.startDate ?: return null
        val from = if (start.isAfter(now.toLocalDate())) start else now.toLocalDate()
        for (i in 0..14L) {
            val date = from.plusDays(i)
            if (Program.workoutOn(start, date) == null) continue
            val minutes = s.workoutTimes[date.dayOfWeek] ?: Settings.DEFAULT_WORKOUT_TIME
            val at = date.atTime(minutes / 60, minutes % 60)
            if (at.isAfter(now)) return at
        }
        return null
    }

    fun nextWeekly(day: DayOfWeek, minutes: Int, now: LocalDateTime): LocalDateTime {
        var date: LocalDate = now.toLocalDate()
        while (true) {
            if (date.dayOfWeek == day) {
                val at = date.atTime(minutes / 60, minutes % 60)
                if (at.isAfter(now)) return at
            }
            date = date.plusDays(1)
        }
    }

    fun nextDaily(minutes: Int, now: LocalDateTime): LocalDateTime {
        val today = now.toLocalDate().atTime(minutes / 60, minutes % 60)
        return if (today.isAfter(now)) today else today.plusDays(1)
    }

    // ---------------- AlarmManager ----------------

    private fun pending(context: Context, type: ReminderType): PendingIntent {
        val intent = Intent(context, AlarmReceiver::class.java)
            .setAction("com.fitplan.app.REMINDER_" + type.name)
            .putExtra(EXTRA_TYPE, type.name)
        return PendingIntent.getBroadcast(
            context, type.requestCode, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
    }

    private fun set(context: Context, type: ReminderType, at: LocalDateTime?) {
        val am = context.getSystemService(AlarmManager::class.java)
        val pi = pending(context, type)
        am.cancel(pi)
        if (at == null) return
        val millis = at.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        try {
            if (canExact(context)) {
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, millis, pi)
            } else {
                am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, millis, pi)
            }
        } catch (_: SecurityException) {
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, millis, pi)
        }
    }
}
