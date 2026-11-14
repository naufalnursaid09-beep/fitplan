package com.fitplan.app

import android.app.Application
import com.fitplan.app.data.Prefs

class FitPlanApp : Application() {
    lateinit var prefs: Prefs
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this
        prefs = Prefs(this)
    }

    companion object {
        lateinit var instance: FitPlanApp
            private set
    }
}
