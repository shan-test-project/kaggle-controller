package com.kaggle.controller.service

import android.app.Notification
import android.app.Service
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.kaggle.controller.R

class SyncService : Service() {

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        val notification = NotificationCompat.Builder(this, "sync_channel")
            .setContentTitle("Kaggle Controller")
            .setContentText("Syncing data with Kaggle...")
            .setSmallIcon(R.drawable.ic_schedule)
            .build()
        startForeground(1, notification)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Background sync logic here
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        stopForeground(true)
    }
}
