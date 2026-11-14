package com.fitplan.app.reminders

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.fitplan.app.FitPlanApp

/** Setelah HP restart, jam diubah, atau aplikasi di-update: jadwalkan ulang semua pengingat. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val app = context.applicationContext as FitPlanApp
        ReminderScheduler.rescheduleAll(context, app.prefs.current)
    }
}
