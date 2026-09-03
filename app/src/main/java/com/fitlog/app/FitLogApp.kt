package com.fitlog.app

import android.app.Application
import com.fitlog.app.data.db.AppDatabase
import com.fitlog.app.data.prefs.SettingsStore
import com.fitlog.app.data.repo.FitnessRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class FitLogApp : Application() {
    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    val db by lazy { AppDatabase.build(this) }
    val settings by lazy { SettingsStore(this) }
    val repo by lazy { FitnessRepository(this) }

    override fun onCreate() {
        super.onCreate()
        appScope.launch { repo.ensureSeed() }
    }
}
