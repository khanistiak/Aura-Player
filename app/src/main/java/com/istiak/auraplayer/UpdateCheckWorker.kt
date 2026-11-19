package com.istiak.auraplayer

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import java.util.concurrent.TimeUnit

/**
 * Periodic background GitHub Releases checker (`UpdateCheckWorker.kt`):
 * - Runs every 6 hours when `NetworkType.CONNECTED` is satisfied via Android `WorkManager`.
 * - Reuses [AppUpdateManager] to query the public GitHub Releases API (`releases/latest`)
 *   and compares the remote semantic tag against the installed version (`BuildConfig.VERSION_NAME`).
 * - If a newer release exists and `tag_name != notified_update_version`, persists
 *   `notified_update_version` in [SettingsDataStore] and posts a system notification on the
 *   `"app_updates"` (`"App Updates"`) channel whose tap intent opens [MainActivity] with
 *   `EXTRA_SHOW_UPDATE_DIALOG = true`.
 */
class UpdateCheckWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val updateManager = AppUpdateManager(applicationContext)
        val settingsStore = SettingsDataStore(applicationContext)
        val completed = updateManager.checkInBackgroundAndNotifyIfNeeded(settingsStore)
        return if (completed) {
            Result.success()
        } else if (runAttemptCount < MAX_RETRY_ATTEMPTS) {
            Result.retry()
        } else {
            Result.success()
        }
    }

    companion object {
        const val UNIQUE_WORK_NAME = "aura_player_periodic_update_check"
        private const val WORK_TAG = "aura_player_update_check"
        private const val REPEAT_INTERVAL_HOURS = 6L
        private const val MAX_RETRY_ATTEMPTS = 2

        /**
         * Schedules a 6-hour periodic WorkManager job with `NetworkType.CONNECTED`.
         * Uses [ExistingPeriodicWorkPolicy.KEEP] so repeated app launches preserve the active cadence.
         */
        fun schedulePeriodicCheck(context: Context) {
            runCatching {
                val constraints = Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()

                val request = PeriodicWorkRequestBuilder<UpdateCheckWorker>(
                    REPEAT_INTERVAL_HOURS,
                    TimeUnit.HOURS
                )
                    .setConstraints(constraints)
                    .addTag(WORK_TAG)
                    .build()

                WorkManager.getInstance(context.applicationContext).enqueueUniquePeriodicWork(
                    UNIQUE_WORK_NAME,
                    ExistingPeriodicWorkPolicy.KEEP,
                    request
                )
            }
        }
    }
}
