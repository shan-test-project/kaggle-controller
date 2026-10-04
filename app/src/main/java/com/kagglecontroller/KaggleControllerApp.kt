package com.kagglecontroller

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build

class KaggleControllerApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL_RUNS, "Notebook runs", NotificationManager.IMPORTANCE_DEFAULT)
                .apply { description = "Completed and failed notebook runs" }
        )
    }

    companion object {
        const val CHANNEL_RUNS = "runs"
    }
}
