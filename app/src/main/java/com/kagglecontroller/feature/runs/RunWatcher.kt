package com.kagglecontroller.feature.runs

import android.Manifest
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.Constraints
import com.kagglecontroller.KaggleControllerApp
import com.kagglecontroller.domain.model.RunStatus
import java.util.concurrent.TimeUnit

/**
 * Best-effort background watcher: checks a run's status a few times with growing delays and posts a
 * notification when it finishes. This does NOT control the Kaggle session (Kaggle runs it on its own
 * servers) and Android may delay it under Doze or battery saver. It stops as soon as the run ends.
 */
object RunWatcher {
    private const val KEY_REF = "ref"
    private const val KEY_ATTEMPT = "attempt"
    private const val MAX_ATTEMPTS = 24

    fun schedule(context: Context, ref: String, attempt: Int = 0) {
        // 1 min, then growing to a 10 minute ceiling: gentle on battery and on Kaggle's rate limits.
        val delayMinutes = (1L shl attempt.coerceAtMost(4)).coerceIn(1L, 10L)
        val request = OneTimeWorkRequestBuilder<Worker>()
            .setInitialDelay(delayMinutes, TimeUnit.MINUTES)
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .setInputData(Data.Builder().putString(KEY_REF, ref).putInt(KEY_ATTEMPT, attempt).build())
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork("watch:$ref", ExistingWorkPolicy.REPLACE, request)
    }

    class Worker(private val ctx: Context, params: WorkerParameters) : CoroutineWorker(ctx, params) {
        override suspend fun doWork(): Result {
            val ref = inputData.getString(KEY_REF) ?: return Result.success()
            val attempt = inputData.getInt(KEY_ATTEMPT, 0)
            val container = (ctx.applicationContext as KaggleControllerApp).container
            if (!container.auth.hasToken()) return Result.success()
            val status = try {
                container.notebooks.status(ref)
            } catch (e: Exception) {
                if (attempt < MAX_ATTEMPTS) schedule(ctx, ref, attempt + 1)
                return Result.success()
            }
            container.runs.refresh()
            val record = container.runs.runs.value.firstOrNull { it.ref == ref && !it.runStatus.terminal }
            if (record != null) {
                container.runs.upsert(record.copy(
                    status = status.status.name,
                    endedAt = if (status.status.terminal) System.currentTimeMillis() else null,
                    failureMessage = status.failureMessage,
                ))
            }
            if (status.status.terminal) {
                if (container.settings.settings.value.notifyRuns) notifyDone(ref, status.status)
            } else if (attempt < MAX_ATTEMPTS) {
                schedule(ctx, ref, attempt + 1)
            }
            return Result.success()
        }

        private fun notifyDone(ref: String, status: RunStatus) {
            if (Build.VERSION.SDK_INT >= 33 &&
                ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
            ) return
            val ok = status == RunStatus.COMPLETE
            val notification = NotificationCompat.Builder(ctx, KaggleControllerApp.CHANNEL_RUNS)
                .setSmallIcon(android.R.drawable.stat_notify_sync_noanim)
                .setContentTitle(if (ok) "Run completed" else "Run ${status.label.lowercase()}")
                .setContentText(ref)
                .setAutoCancel(true)
                .build()
            ctx.getSystemService(NotificationManager::class.java).notify(ref.hashCode(), notification)
        }
    }
}
