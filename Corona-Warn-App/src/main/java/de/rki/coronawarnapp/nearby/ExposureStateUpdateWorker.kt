package de.rki.coronawarnapp.nearby

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.nearby.exposurenotification.ExposureNotificationClient
import de.rki.coronawarnapp.R
import de.rki.coronawarnapp.exception.ExceptionCategory
import de.rki.coronawarnapp.exception.NoTokenException
import de.rki.coronawarnapp.exception.TransactionException
import de.rki.coronawarnapp.exception.reporting.report
import de.rki.coronawarnapp.storage.ExposureSummaryRepository
import de.rki.coronawarnapp.transaction.RiskLevelTransaction
import de.rki.coronawarnapp.util.ForegroundPocTracker
import de.rki.coronawarnapp.worker.BackgroundWorkHelper
import timber.log.Timber
import java.util.Date

class ExposureStateUpdateWorker(val context: Context, workerParams: WorkerParameters) :
    CoroutineWorker(context, workerParams) {
    companion object {
        private val TAG = ExposureStateUpdateWorker::class.java.simpleName
        private val notificationID = TAG.hashCode()
    }

    override suspend fun doWork(): Result {

            var result = Result.success()
        try {
            //BackgroundWorkHelper.moveCoroutineWorkerToForeground(context.getString(R.string.notification_headline), "", notificationID, this)

            Timber.v("worker to persist exposure summary started")
            val token = inputData.getString(ExposureNotificationClient.EXTRA_TOKEN)
                ?: throw NoTokenException(IllegalArgumentException("no token was found in the intent"))

            Timber.v("valid token $token retrieved")

            val exposureSummary = InternalExposureNotificationClient
                .asyncGetExposureSummary(token)

            ExposureSummaryRepository.getExposureSummaryRepository()
                .insertExposureSummaryEntity(exposureSummary)
            Timber.v("exposure summary state updated: $exposureSummary")

            RiskLevelTransaction.start()
            Timber.v("risk level calculation triggered")
        } catch (e: ApiException) {
            e.report(ExceptionCategory.EXPOSURENOTIFICATION)
        } catch (e: TransactionException) {
            e.report(ExceptionCategory.INTERNAL)
        }

        ForegroundPocTracker.save(context, TAG, Date(), result)
        return result
    }
}
