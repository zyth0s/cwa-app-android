package de.rki.coronawarnapp.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import de.rki.coronawarnapp.R
import de.rki.coronawarnapp.notification.NotificationHelper
import timber.log.Timber

/**
 * Periodic diagnosis key retrieval work
 * Executes the scheduling of one time diagnosis key retrieval work
 *
 * @see BackgroundWorkScheduler
 * @see DiagnosisKeyRetrievalOneTimeWorker
 */
class DiagnosisKeyRetrievalPeriodicWorker(val context: Context, workerParams: WorkerParameters) :
    CoroutineWorker(context, workerParams) {

    companion object {
        private val TAG: String? = DiagnosisKeyRetrievalPeriodicWorker::class.simpleName
        private val notificationID = TAG.hashCode()
    }

    /**
     * Work execution
     *
     * @return Result
     *
     * @see BackgroundWorkScheduler.scheduleDiagnosisKeyPeriodicWork()
     * @see BackgroundWorkScheduler.scheduleDiagnosisKeyOneTimeWork()
     */
    override suspend fun doWork(): Result {
        Timber.d("Background job started. Run attempt: $runAttemptCount")
        BackgroundWorkHelper.sendDebugNotification(
            "KeyPeriodic Executing: Start", "KeyPeriodic started. Run attempt: $runAttemptCount ")

        var result = Result.success()
        try {

            BackgroundWorkHelper.moveCoroutineWorkerToForeground(context.getString(R.string.notification_headline), "", notificationID, this)
            BackgroundWorkScheduler.scheduleDiagnosisKeyOneTimeWork()

        } catch (e: Exception) {
            if (runAttemptCount > BackgroundConstants.WORKER_RETRY_COUNT_THRESHOLD) {

                BackgroundWorkHelper.sendDebugNotification(
                    "KeyPeriodic Executing: Failure", "KeyPeriodic failed with $runAttemptCount attempts")

                return Result.failure()
            } else {
                result = Result.retry()
            }
        }

        BackgroundWorkHelper.sendDebugNotification(
            "KeyPeriodic Executing: End", "KeyPeriodic result: $result ")

        return result
    }
}
