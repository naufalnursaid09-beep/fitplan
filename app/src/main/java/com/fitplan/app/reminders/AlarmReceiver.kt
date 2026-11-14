package com.fitplan.app.reminders

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.fitplan.app.FitPlanApp
import com.fitplan.app.data.Program
import java.time.LocalDate

/** Dipanggil AlarmManager saat waktunya tiba: tampilkan notifikasi lalu jadwalkan berikutnya. */
class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val prefs = (context.applicationContext as FitPlanApp).prefs
        val s = prefs.current
        val today = LocalDate.now()
        val type = runCatching {
            ReminderType.valueOf(intent.getStringExtra(ReminderScheduler.EXTRA_TYPE) ?: "")
        }.getOrNull()

        when (type) {
            ReminderType.WORKOUT -> {
                val w = Program.workoutOn(s.startDate, today)
                if (w != null) {
                    val st = Program.status(s.startDate, today) as? Program.Status.Running
                    Notifications.show(
                        context, type.requestCode,
                        "Waktunya latihan: ${w.title} 💪",
                        "Minggu ${st?.week ?: "-"} dari ${Program.TOTAL_WEEKS}. " +
                            "${w.exercises.size} gerakan menunggu. Buka FitPlan untuk mulai.",
                    )
                }
            }
            ReminderType.WEIGH -> Notifications.show(
                context, type.requestCode,
                "Timbang badan hari Minggu ⚖️",
                "Timbang setelah bangun tidur dan ke toilet, sebelum makan. Catat di menu Progres.",
            )
            ReminderType.PROTEIN -> {
                val rest = Program.workoutOn(s.startDate, today) == null
                Notifications.show(
                    context, type.requestCode,
                    if (rest) "Hari libur: istirahat & protein 😴" else "Jangan lupa protein 🍗",
                    if (rest) "Hari ini tidak latihan. Istirahat yang cukup dan kejar target ${s.targetProtein} g protein."
                    else "Target hari ini ${s.targetProtein} g protein dan ${s.targetKcal} kkal. Catat makanan di FitPlan.",
                )
            }
            ReminderType.TEST -> Notifications.show(
                context, type.requestCode,
                "Tes pengingat berhasil ✅",
                "Notifikasi ini dijadwalkan 1 menit lalu. Pengingat FitPlan akan bekerja walau aplikasi ditutup.",
            )
            null -> Unit
        }
        // Jadwalkan pengingat berikutnya
        ReminderScheduler.rescheduleAll(context, s)
    }
}
