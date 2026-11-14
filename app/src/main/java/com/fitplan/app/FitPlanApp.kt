package com.fitplan.app

import android.app.Application
import com.fitplan.app.data.Prefs
import com.fitplan.app.data.db.AppDatabase
import com.fitplan.app.reminders.Notifications
import com.fitplan.app.reminders.ReminderScheduler

class FitPlanApp : Application() {
    lateinit var prefs: Prefs
        private set

    /** Database lokal (dibuat saat pertama dipakai). */
    val db: AppDatabase by lazy { AppDatabase.create(this) }

    override fun onCreate() {
        super.onCreate()
        instance = this
        prefs = Prefs(this)
        Notifications.createChannel(this)
        // Setiap pengaturan berubah, jadwal pengingat ikut diperbarui.
        prefs.onChange = { ReminderScheduler.rescheduleAll(this, it) }
        ReminderScheduler.rescheduleAll(this, prefs.current)
    }

    companion object {
        lateinit var instance: FitPlanApp
            private set
    }
}
