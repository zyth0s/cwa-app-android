package de.rki.coronawarnapp.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import de.rki.coronawarnapp.R
import de.rki.coronawarnapp.notification.NotificationHelper
import de.rki.coronawarnapp.storage.LocalData
import de.rki.coronawarnapp.util.ForegroundPocTracker
import de.rki.coronawarnapp.worker.BackgroundWorkScheduler.stop
import org.joda.time.DateTime
import org.joda.time.DateTimeZone
import timber.log.Timber
import java.util.Date

/**
 * Periodic background noise worker
 *
 * @see BackgroundWorkScheduler
 */
class BackgroundNoisePeriodicWorker(
    val context: Context,
    workerParams: WorkerParameters
) :
    CoroutineWorker(context, workerParams) {

    companion object {
        private val TAG: String = BackgroundNoisePeriodicWorker::class.java.simpleName
        private val notificationID = TAG.hashCode()
    }

    /**
     * Work execution
     *
     * @return Result
     *
     * @see BackgroundConstants.NUMBER_OF_DAYS_TO_RUN_PLAYBOOK
     */
    override suspend fun doWork(): Result {
        Timber.d("Background job started. Run attempt: $runAttemptCount")

        var result = Result.success()
        try {

            //BackgroundWorkHelper.moveCoroutineWorkerToForeground(context.getString(R.string.notification_headline), "", notificationID, this)

            val initialPairingDate = DateTime(
                LocalData.devicePairingSuccessfulTimestamp(),
                DateTimeZone.UTC
            )

            // Check if the numberOfDaysToRunPlaybook are over
            if (initialPairingDate.plusDays(BackgroundConstants.NUMBER_OF_DAYS_TO_RUN_PLAYBOOK).isBeforeNow) {
                stopWorker()
                return result
            }

            BackgroundWorkScheduler.scheduleBackgroundNoiseOneTimeWork()
        } catch (e: Exception) {
            result = if (runAttemptCount > BackgroundConstants.WORKER_RETRY_COUNT_THRESHOLD) {
                Result.failure()
            } else {
                Result.retry()
            }
        }

        ForegroundPocTracker.save(context, TAG, Date(), result)
        return result
    }

    private fun stopWorker() {
        BackgroundWorkScheduler.WorkType.BACKGROUND_NOISE_PERIODIC_WORK.stop()
    }
}
